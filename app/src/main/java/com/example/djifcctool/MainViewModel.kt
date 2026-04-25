package com.example.djifcctool

import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/**
 * Zentrale UI-Logik. Steuert die 4 Wizard-Schritte:
 *   1. SCAN     – Remote per USB finden
 *   2. CONNECT  – USB-Berechtigung + Verbindung
 *   3. DETECT   – Aktuellen Modus (CE/FCC) erkennen
 *   4. PATCH    – FCC anwenden / CE wiederherstellen
 *
 * Während [DjiFccProtocol.SAFE_MODE] aktiv ist, werden keine
 * Schreib­befehle abgesetzt – nur eine Trockensimulation.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val usb: DjiUsbSerialManager
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var connection: DjiUsbSerialManager.DjiUsbConnection? = null
    private var pendingDevice: UsbDevice? = null

    init { log("App gestartet. Safe-Mode = ${DjiFccProtocol.SAFE_MODE}.") }

    // ---------- Wizard Step 1: Scan ----------
    fun scanForRemote() = viewModelScope.launch {
        setBusy(true, "USB wird gescannt …")
        val device = usb.findDjiRemote()
        if (device == null) {
            log("Kein DJI-Remote gefunden. Bitte Remote per USB-OTG-Kabel verbinden.")
            _state.update {
                it.copy(
                    step = WizardStep.SCAN,
                    deviceName = null,
                    isBusy = false,
                    statusMessage = "Kein Gerät erkannt"
                )
            }
            return@launch
        }
        log("DJI-Remote erkannt: ${device.productName ?: device.deviceName}")
        _state.update {
            it.copy(
                deviceName = device.productName ?: device.deviceName,
                vendorId = device.vendorId,
                productId = device.productId,
                step = WizardStep.CONNECT,
                isBusy = false,
                statusMessage = "Remote gefunden"
            )
        }
        ensurePermissionAndConnect(device)
    }

    private fun ensurePermissionAndConnect(device: UsbDevice) = viewModelScope.launch {
        if (!usb.hasUsbPermission(device)) {
            pendingDevice = device
            _events.send(UiEvent.RequestUsbPermission(device))
            log("USB-Berechtigung wird angefordert …")
            return@launch
        }
        connect(device)
    }

    fun onUsbPermissionResult(device: UsbDevice, granted: Boolean) = viewModelScope.launch {
        if (!granted) {
            log("❌ USB-Berechtigung abgelehnt.")
            _state.update { it.copy(lastError = "USB-Berechtigung verweigert") }
            return@launch
        }
        log("✅ USB-Berechtigung erteilt.")
        connect(device)
    }

    // ---------- Wizard Step 2: Connect ----------
    private fun connect(device: UsbDevice) = viewModelScope.launch {
        setBusy(true, "Verbindung wird hergestellt …")
        val conn = usb.openConnection(device)
        if (conn == null) {
            log("⚠️  USB-Verbindung fehlgeschlagen.")
            _state.update {
                it.copy(
                    isBusy = false,
                    lastError = "Verbindung konnte nicht geöffnet werden",
                    statusMessage = "Verbindung fehlgeschlagen"
                )
            }
            return@launch
        }
        connection = conn
        log("USB-Serial geöffnet (CDC-ACM, ${DjiFccProtocol.BAUD_RATE} 8N1).")
        _state.update {
            it.copy(
                step = WizardStep.DETECT,
                isBusy = false,
                statusMessage = "Verbunden",
                lastError = null
            )
        }
        detectMode()
    }

    // ---------- Wizard Step 3: Detect ----------
    /**
     * Hinweis: Es gibt keinen verifizierten Status-Query-Befehl. Wir nehmen
     * an, dass die Remote im CE-Modus startet (Default in EU) und gehen
     * direkt zum Patch-Schritt über.
     */
    fun detectMode() = viewModelScope.launch {
        if (connection == null) { log("Nicht verbunden."); return@launch }
        log("Annahme: Remote ist im CE-Modus (kein verifizierter Status-Query bekannt).")
        _state.update {
            it.copy(
                isBusy = false,
                currentMode = DjiFccProtocol.FccState.CE,
                step = WizardStep.PATCH,
                statusMessage = "Bereit für FCC-Patch"
            )
        }
    }

    // ---------- Wizard Step 4: Patch ----------
    /**
     * Öffnet zuerst den Bestätigungsdialog. Der eigentliche Versand erfolgt
     * erst nach [confirmAndApplyFccPatch].
     */
    fun applyFccPatch() = viewModelScope.launch {
        if (connection == null) { log("Nicht verbunden."); return@launch }
        _state.update { it.copy(showConfirmPatch = true) }
    }

    fun cancelPatchConfirmation() {
        _state.update { it.copy(showConfirmPatch = false) }
    }

    fun confirmAndApplyFccPatch() = viewModelScope.launch {
        val conn = connection ?: return@launch
        _state.update { it.copy(showConfirmPatch = false) }

        // Pre-flight: jeden Frame VOR dem Öffnen des Schreibvorgangs validieren.
        val packets = DjiFccProtocol.fccPatchSequence
        packets.forEachIndexed { i, packet ->
            val v = DjiFccProtocol.validateFrame(packet)
            if (v is DjiFccProtocol.FrameValidation.Invalid) {
                log("❌ Pre-Flight-Check Frame ${i + 1} fehlgeschlagen: ${v.reason}")
                _state.update {
                    it.copy(
                        isBusy = false,
                        lastError = "Pre-Flight-Check fehlgeschlagen – nichts gesendet",
                        statusMessage = "Abgebrochen"
                    )
                }
                return@launch
            }
        }
        // Strikte Geräte-Prüfung: nur exakt N1-Remote.
        if (!DjiFccProtocol.matchesDjiN1Remote(conn.device.vendorId, conn.device.productId)) {
            log("❌ Falsches USB-Gerät – Patch verweigert.")
            _state.update { it.copy(isBusy = false, lastError = "Falsches USB-Gerät") }
            return@launch
        }

        if (DjiFccProtocol.SAFE_MODE) {
            log("[SAFE-MODE] FCC-Patch nicht gesendet (Trockenlauf).")
            packets.forEachIndexed { i, b ->
                log("  [Trockenlauf] Paket ${i + 1}/${packets.size}: ${b.toHex()}")
            }
            _state.update { it.copy(statusMessage = "Trockenlauf abgeschlossen (SAFE_MODE)") }
            return@launch
        }
        setBusy(true, "FCC-Patch wird angewendet …")
        var ok = true
        packets.forEachIndexed { i, packet ->
            val written = usb.sendCommand(conn, packet)
            if (!written) { ok = false; log("Paket ${i + 1} fehlgeschlagen.") }
            else log("Paket ${i + 1}/${packets.size} OK (${packet.size} B): ${packet.toHex()}")
        }
        _state.update {
            it.copy(
                isBusy = false,
                currentMode = if (ok) DjiFccProtocol.FccState.FCC else it.currentMode,
                patchSuccess = ok,
                statusMessage = if (ok) "✅ FCC-Patch gesendet" else "Patch-Fehler",
                lastError = if (ok) null else "FCC-Patch unvollständig"
            )
        }
    }

    fun resetToCe() = viewModelScope.launch {
        log("ℹ️  Auf CE zurücksetzen geht NUR per Power-Cycle der Remote:")
        log("   1) USB-Kabel abziehen")
        log("   2) Remote ausschalten (langer Power-Druck)")
        log("   3) Remote wieder einschalten — sie startet automatisch im CE-Modus.")
        log("   (Drohne kann dabei an bleiben.)")
        _state.update { it.copy(statusMessage = "Bitte Remote aus- und wieder einschalten", patchSuccess = false) }
    }

    fun disconnect() = viewModelScope.launch {
        connection?.let { usb.closeConnection(it); log("Verbindung geschlossen.") }
        connection = null
        _state.update { MainUiState() }
        log("Zurück zum Start.")
    }

    fun clearLogs() = _state.update { it.copy(logs = emptyList()) }

    // ---------- Helpers ----------
    private fun setBusy(busy: Boolean, msg: String) =
        _state.update { it.copy(isBusy = busy, statusMessage = msg) }

    private fun log(msg: String) {
        val line = "[${LocalTime.now().withNano(0)}] $msg"
        _state.update {
            val newLogs = (listOf(line) + it.logs).take(120)
            it.copy(logs = newLogs)
        }
    }

    private fun ByteArray.toHex(): String =
        joinToString(" ") { "%02X".format(it) }
}

enum class WizardStep { SCAN, CONNECT, DETECT, PATCH }

data class MainUiState(
    val step: WizardStep = WizardStep.SCAN,
    val deviceName: String? = null,
    val vendorId: Int? = null,
    val productId: Int? = null,
    val currentMode: DjiFccProtocol.FccState = DjiFccProtocol.FccState.UNKNOWN,
    val statusMessage: String = "Bereit",
    val isBusy: Boolean = false,
    val lastError: String? = null,
    val logs: List<String> = emptyList(),
    val safeMode: Boolean = DjiFccProtocol.SAFE_MODE,
    val showConfirmPatch: Boolean = false,
    val patchSuccess: Boolean = false
)

sealed interface UiEvent {
    data class RequestUsbPermission(val device: UsbDevice) : UiEvent
}
