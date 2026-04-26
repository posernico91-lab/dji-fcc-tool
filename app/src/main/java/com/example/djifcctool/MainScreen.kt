package com.example.djifcctool

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.djifcctool.ads.AdmobBanner
import com.example.djifcctool.ads.AdmobNative
import com.example.djifcctool.ads.ConsentManager

/**
 * Hauptbildschirm. Vier-Schritt-Wizard mit klar erkennbarem
 * aktuellem Schritt und kontextbezogener Aktions-Schaltfläche.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Column(modifier = Modifier
                .padding(16.dp)
                .verticalScroll(scrollState)
            ) {
                if (state.safeMode) SafeModeBanner()
                Spacer(Modifier.height(8.dp))
                StepIndicator(state.step)
                Spacer(Modifier.height(12.dp))
                StatusCard(state)
                Spacer(Modifier.height(12.dp))
                ActionCard(state, viewModel)
                Spacer(Modifier.height(12.dp))
                AdmobNative(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                SafetyCard()
                Spacer(Modifier.height(12.dp))
                LogsCard(state.logs, viewModel::clearLogs)
                Spacer(Modifier.height(12.dp))
                PrivacyOptionsButton()
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.developer_credit),
                    fontSize = 11.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                AdmobBanner(modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (state.showConfirmPatch) {
        ConfirmPatchDialog(
            onConfirm = viewModel::confirmAndApplyFccPatch,
            onDismiss = viewModel::cancelPatchConfirmation
        )
    }
}

@Composable
private fun ConfirmPatchDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("FCC-Patch wirklich senden?") },
        text = {
            Column {
                Text(
                    "Es werden 2 Datenpakete (37 Bytes) an die DJI-Remote " +
                        "geschickt, die den Funkmodus auf FCC umstellen.",
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                Text("• Drohne wird NICHT verändert (nur die Remote).", fontSize = 13.sp)
                Text("• Reset = Drohne + Remote aus- und wieder einschalten.", fontSize = 13.sp)
                Text("• In CE-Regionen rechtlich problematisch.", fontSize = 13.sp)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Ja, FCC senden") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@Composable
private fun SafeModeBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE082)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("⚠️  SAFE-MODE aktiv (Trockenlauf)", fontWeight = FontWeight.Bold)
            Text(
                "Es werden KEINE Bytes an die Hardware gesendet. " +
                    "Die App protokolliert nur, welche Pakete sie senden würde. " +
                    "Zum Aktivieren des realen Sendens DjiFccProtocol.SAFE_MODE = false setzen.",
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun StepIndicator(current: WizardStep) {
    val steps = listOf(
        WizardStep.SCAN to "Scan",
        WizardStep.CONNECT to "Verbinden",
        WizardStep.DETECT to "Erkennen",
        WizardStep.PATCH to "Patch"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, (step, label) ->
            val isDone = step.ordinal < current.ordinal
            val isActive = step == current
            val color = when {
                isDone -> MaterialTheme.colorScheme.primary
                isActive -> MaterialTheme.colorScheme.secondary
                else -> Color(0xFFBDBDBD)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color = color, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (index + 1).toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(label, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StatusCard(state: MainUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (state.isBusy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            Spacer(Modifier.height(8.dp))
            InfoRow("Schritt", "${state.step.ordinal + 1} – ${state.step.name}")
            InfoRow("Modus", state.currentMode.name)
            InfoRow("Gerät", state.deviceName ?: "—")
            if (state.vendorId != null) InfoRow("VID/PID", "0x${"%04X".format(state.vendorId)} / 0x${"%04X".format(state.productId ?: 0)}")
            Spacer(Modifier.height(6.dp))
            Text(state.statusMessage, fontWeight = FontWeight.SemiBold)
            if (!state.lastError.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Fehler: ${state.lastError}", color = Color(0xFFD32F2F))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label:", modifier = Modifier.width(90.dp), color = Color(0xFF555555))
        Text(value)
    }
}

@Composable
private fun ActionCard(state: MainUiState, vm: MainViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Nächster Schritt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            when (state.step) {
                WizardStep.SCAN -> {
                    HelpText(
                        "Vorbereitung:\n" +
                            "1. DJI Fly App komplett schließen (aus »Letzte Apps« wischen).\n" +
                            "2. DJI-Remote (N1/N2 ohne Display) einschalten — Kontroll-LEDs müssen leuchten.\n" +
                            "3. Drohne kann an oder aus sein — egal für diesen Schritt.\n" +
                            "4. USB-OTG-Kabel in den UNTEREN USB-C-Port der Remote stecken,\n" +
                            "   das andere Ende in dein Telefon.\n" +
                            "5. Falls Android fragt, welche App geöffnet werden soll » RangeBoost FCC « wählen.\n" +
                            "6. Tippe unten auf »Remote suchen«."
                    )
                    PrimaryButton("Remote suchen", state.isBusy, vm::scanForRemote)
                }
                WizardStep.CONNECT -> {
                    HelpText(
                        "Erlaube den USB-Zugriff im System-Dialog (Häkchen bei »Immer verwenden« setzen, " +
                            "dann erscheint der Dialog beim nächsten Mal nicht mehr).\n" +
                            "Falls kein Dialog erscheint, drücke erneut »Verbinden«."
                    )
                    PrimaryButton("Verbinden", state.isBusy, vm::scanForRemote)
                }
                WizardStep.DETECT -> {
                    HelpText(
                        "Verbindung steht. Tippe auf »Weiter zum FCC-Patch«.\n" +
                            "(Hinweis: Die Remote sendet keinen Status-Query, daher gehen wir vom " +
                            "Werkszustand CE aus — typisch für EU-Auslieferung.)"
                    )
                    PrimaryButton("Weiter zum FCC-Patch", state.isBusy, vm::detectMode)
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton("Trennen", state.isBusy, vm::disconnect)
                }
                WizardStep.PATCH -> {
                    if (state.patchSuccess) {
                        PostPatchInstructions(onDisconnect = vm::disconnect)
                    } else {
                        HelpText(
                            "Was passiert beim »FCC aktivieren«:\n" +
                                "• Die App schickt 2 verifizierte DUML-Pakete (37 Bytes total) an die Remote.\n" +
                                "• Die Remote schaltet ihren Funkchip in den FCC-Modus (mehr Sendeleistung,\n" +
                                "  mehr Reichweite, andere Frequenzbänder).\n" +
                                "• Die Drohne selbst wird NICHT verändert.\n" +
                                "• Vor dem Senden erscheint ein Bestätigungsdialog."
                        )
                        PrimaryButton("FCC aktivieren", state.isBusy, vm::applyFccPatch)
                        Spacer(Modifier.height(8.dp))
                        SecondaryButton("Auf CE zurücksetzen (Power-Cycle)", state.isBusy, vm::resetToCe)
                        Spacer(Modifier.height(8.dp))
                        SecondaryButton("Trennen", state.isBusy, vm::disconnect)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth()
    ) { Text(text) }
}

@Composable
private fun SecondaryButton(text: String, busy: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth()
    ) { Text(text) }
}

@Composable
private fun HelpText(text: String) {
    Text(text, fontSize = 13.sp, color = Color(0xFF424242))
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun PostPatchInstructions(onDisconnect: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "✅ FCC-Patch erfolgreich gesendet",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(10.dp))
            Text("So geht es jetzt weiter — bitte EXAKT in dieser Reihenfolge:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            NumberedStep(1, "USB-Kabel vom UNTEREN Port der Remote abziehen.")
            NumberedStep(2,
                "Remote AUS- und wieder EINSCHALTEN " +
                    "(langer Druck auf den Power-Button bis alle LEDs aus sind, " +
                    "dann erneut langer Druck zum Einschalten).\n" +
                    "⚠️ WICHTIG: Drohne dabei NICHT ausschalten — sonst geht der Patch verloren " +
                    "und du müsstest ihn auf der Remote erneut anwenden."
            )
            NumberedStep(3,
                "Warten bis die Remote vollständig hochgefahren ist (alle Status-LEDs leuchten konstant)."
            )
            NumberedStep(4,
                "Drohne einschalten (falls noch nicht an) und warten, bis Remote + Drohne gekoppelt sind " +
                    "(Beep-Ton oder konstantes Leuchten der Verbindungs-LED)."
            )
            NumberedStep(5,
                "USB-OTG-Kabel JETZT in den OBEREN USB-C-Port der Remote stecken (nicht mehr in den unteren!)."
            )
            NumberedStep(6,
                "DJI Fly App öffnen — sie verbindet sich automatisch mit der Remote."
            )

            Spacer(Modifier.height(12.dp))
            Text("Wie erkenne ich in DJI Fly, dass FCC aktiv ist?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            BulletText("In der Karten-/Kameraansicht auf das Antennen-/Signal-Symbol oben rechts tippen → Reichweiten-Anzeige.")
            BulletText(
                "Im Menü: Zahnrad (Einstellungen) → »Übertragung« (oder »HD Übertragung«) → Eintrag »Übertragungsmodus« / »Bandbreite«." +
                    " CE zeigt ~ 6 km Theorie / 1 km Praxis, FCC zeigt ~ 10–15 km Theorie."
            )
            BulletText(
                "Im selben Menü das Feld »Frequenzband« / »Channel«: CE = nur 2.4 GHz im EU-Profil, " +
                    "FCC = 2.4 GHz UND 5.8 GHz wählbar."
            )
            BulletText("Praxistest: Reichweitentest im freien Feld — über ~1,5 km Verbindung = FCC ist aktiv.")

            Spacer(Modifier.height(12.dp))
            Text("Was bricht den FCC-Modus?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            BulletText("Remote ausschalten → zurück zu CE. Patch muss erneut angewendet werden.")
            BulletText("Remote-Firmware-Update → kann den Patch entfernen.")
            BulletText("Drohne ausschalten allein bricht NICHT — solange die Remote an bleibt.")

            Spacer(Modifier.height(12.dp))
            SecondaryButton("Fertig — App schließen / trennen", false, onDisconnect)
        }
    }
}

@Composable
private fun NumberedStep(num: Int, text: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(num.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = Color(0xFF1B5E20))
    }
}

@Composable
private fun BulletText(text: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text("•  ", fontSize = 13.sp, color = Color(0xFF1B5E20))
        Text(text, fontSize = 13.sp, color = Color(0xFF1B5E20))
    }
}

@Composable
private fun SafetyCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("⚖️  Rechtlicher Hinweis", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Die FCC-Aktivierung ist außerhalb von FCC-Regionen (z. B. EU) " +
                    "rechtlich problematisch. Verwende diese App nur dort, wo es " +
                    "erlaubt ist. Es entsteht keinerlei Garantie- oder Haftungs­anspruch.",
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PrivacyOptionsButton() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity ?: return
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = {
                if (ConsentManager.isPrivacyOptionsRequired()) {
                    ConsentManager.showPrivacyOptionsForm(activity)
                } else {
                    // Erzwinge erneutes Laden + Anzeigen des Consent-Forms
                    ConsentManager.forceShowConsentForm(activity)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Datenschutzeinstellungen / Werbung") }
        if (BuildConfig.DEBUG) {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = { ConsentManager.resetAndReshow(activity) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("\uD83D\uDD27 Consent zurücksetzen (Debug)") }
        }
    }
}

@Composable
private fun LogsCard(logs: List<String>, onClear: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onClear, enabled = logs.isNotEmpty()) { Text("Leeren") }
            }
            Spacer(Modifier.height(8.dp))
            if (logs.isEmpty()) {
                Text("Noch keine Einträge.", color = Color(0xFF888888), fontSize = 12.sp)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(logs) { line ->
                        Text(
                            text = line,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
