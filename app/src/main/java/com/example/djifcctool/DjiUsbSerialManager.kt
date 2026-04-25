package com.example.djifcctool

import android.app.PendingIntent
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.annotation.WorkerThread
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Spricht die DJI N1/N2-Remote als CDC-ACM-Serial an
 * (basierend auf usb-serial-for-android, mik3y).
 */
@Singleton
class DjiUsbSerialManager @Inject constructor(
    private val usbManager: UsbManager,
    @ApplicationContext private val context: Context
) {
    /** Hält geöffnete Verbindung + Port. */
    data class DjiUsbConnection(
        val device: UsbDevice,
        val port: UsbSerialPort
    )

    /**
     * Erkennt eine angeschlossene DJI N1/N2-Remote.
     * STRIKTE Prüfung: nur exakte VID+PID-Kombination wird akzeptiert,
     * damit niemals Pakete an ein anderes USB-Gerät (z. B. eine direkt
     * angeschlossene Drohne) gesendet werden können.
     */
    suspend fun findDjiRemote(): UsbDevice? = withContext(Dispatchers.IO) {
        usbManager.deviceList.values.firstOrNull { device ->
            DjiFccProtocol.matchesDjiN1Remote(device.vendorId, device.productId)
        }
    }

    fun hasUsbPermission(device: UsbDevice): Boolean = usbManager.hasPermission(device)

    fun requestUsbPermission(device: UsbDevice, pendingIntent: PendingIntent) {
        usbManager.requestPermission(device, pendingIntent)
    }

    /**
     * Öffnet die DJI-Remote als CDC-ACM-Serial-Port mit 19200 8N1.
     * Gibt null zurück, wenn kein Port verfügbar oder Permission fehlt.
     */
    @WorkerThread
    fun openConnection(device: UsbDevice): DjiUsbConnection? {
        val probeTable = ProbeTable().apply {
            addProduct(
                DjiFccProtocol.DJI_VENDOR_ID,
                DjiFccProtocol.DJI_N1_REMOTE_PID,
                CdcAcmSerialDriver::class.java
            )
        }
        val driver = UsbSerialProber(probeTable).probeDevice(device) ?: return null
        val connection = usbManager.openDevice(driver.device) ?: return null
        val port = driver.ports.firstOrNull() ?: run {
            connection.close()
            return null
        }
        return try {
            port.open(connection)
            port.setParameters(
                DjiFccProtocol.BAUD_RATE,
                DjiFccProtocol.DATA_BITS,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )
            DjiUsbConnection(device, port)
        } catch (t: Throwable) {
            try { port.close() } catch (_: Throwable) {}
            null
        }
    }

    @WorkerThread
    fun closeConnection(connection: DjiUsbConnection) {
        try { connection.port.close() } catch (_: Throwable) {}
    }

    /**
     * Sendet ein einzelnes Frame.
     * Vor jedem realen Schreibzugriff wird der Frame per
     * [DjiFccProtocol.validateFrame] geprüft – schlägt die Prüfung fehl,
     * werden KEINE Bytes auf die Leitung gegeben.
     */
    suspend fun sendCommand(
        connection: DjiUsbConnection,
        payload: ByteArray,
        timeoutMs: Int = 1000
    ): Boolean = withContext(Dispatchers.IO) {
        val validation = DjiFccProtocol.validateFrame(payload)
        if (validation is DjiFccProtocol.FrameValidation.Invalid) return@withContext false
        if (!DjiFccProtocol.matchesDjiN1Remote(connection.device.vendorId, connection.device.productId)) {
            return@withContext false
        }
        try {
            connection.port.write(payload, timeoutMs)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
