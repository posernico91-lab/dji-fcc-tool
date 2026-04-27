package com.example.djifcctool

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.djifcctool.ads.AdmobBanner
import com.example.djifcctool.ads.AdmobNative
import com.example.djifcctool.ads.AdsManager
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
        containerColor = FccColors.DeepSpace,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FccColors.HudGradient)
                    .border(width = 0.5.dp, brush = FccColors.GlowGradient, shape = RoundedCornerShape(0.dp))
            ) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .shadow(8.dp, CircleShape, ambientColor = FccColors.Cyan, spotColor = FccColors.Cyan)
                                    .clip(CircleShape)
                                    .background(FccColors.SurfaceHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(R.drawable.app_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp).clip(CircleShape)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    stringResource(R.string.app_name).uppercase(),
                                    fontWeight = FontWeight.Black,
                                    color = FccColors.TextHigh,
                                    fontSize = 18.sp,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    "• FCC PATCH CONSOLE •",
                                    fontFamily = FontFamily.Monospace,
                                    color = FccColors.Cyan,
                                    fontSize = 9.sp,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = FccColors.TextHigh
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FccColors.HudGradient)
                .padding(padding)
        ) {
            Column(modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
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
    HudPanel(
        accent = FccColors.Amber,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(FccColors.Amber)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "SAFE-MODE • DRY RUN",
                    fontWeight = FontWeight.Black,
                    color = FccColors.Amber,
                    fontSize = 13.sp,
                    letterSpacing = 1.5.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Es werden KEINE Bytes an die Hardware gesendet. " +
                    "Die App protokolliert nur, welche Pakete sie senden würde.",
                fontSize = 12.sp,
                color = FccColors.TextMid
            )
        }
    }
}

/**
 * Dunkles HUD-Panel mit subtilem Glow-Border. Ersetzt Material3-Card
 * für ein deutlich technischeres Look-and-Feel.
 */
@Composable
private fun HudPanel(
    modifier: Modifier = Modifier,
    accent: Color = FccColors.Cyan,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(14.dp), ambientColor = accent.copy(alpha = 0.5f), spotColor = accent.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(14.dp))
            .background(FccColors.PanelGradient)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.7f), accent.copy(alpha = 0.05f), accent.copy(alpha = 0.4f))
                ),
                shape = RoundedCornerShape(14.dp)
            )
    ) { content() }
}

@Composable
private fun StepIndicator(current: WizardStep) {
    val steps = listOf(
        WizardStep.SCAN to "SCAN",
        WizardStep.CONNECT to "LINK",
        WizardStep.DETECT to "PROBE",
        WizardStep.PATCH to "PATCH"
    )
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    HudPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, (step, label) ->
                val isDone = step.ordinal < current.ordinal
                val isActive = step == current
                val color = when {
                    isDone -> FccColors.Lime
                    isActive -> FccColors.Cyan
                    else -> FccColors.SurfaceLine
                }
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .shadow(
                                elevation = if (isActive) 18.dp else 0.dp,
                                shape = CircleShape,
                                ambientColor = color,
                                spotColor = color
                            )
                            .clip(CircleShape)
                            .background(
                                if (isActive) color.copy(alpha = pulse)
                                else if (isDone) color
                                else FccColors.SurfaceHigh
                            )
                            .border(1.dp, color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            color = if (isActive || isDone) Color.Black else FccColors.TextDim,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        color = if (isActive) FccColors.Cyan else if (isDone) FccColors.Lime else FccColors.TextDim
                    )
                }
                if (index < steps.lastIndex) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(2.dp)
                            .padding(horizontal = 4.dp)
                            .background(
                                if (step.ordinal < current.ordinal) FccColors.Lime
                                else FccColors.SurfaceLine
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(state: MainUiState) {
    val isLive = !state.isBusy
    val pulse by rememberInfiniteTransition(label = "live").animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "liveAlpha"
    )
    HudPanel(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background((if (isLive) FccColors.Lime else FccColors.Amber).copy(alpha = pulse))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "// SYSTEM STATUS",
                    style = MaterialTheme.typography.labelLarge,
                    color = FccColors.Cyan
                )
                Spacer(Modifier.weight(1f))
                if (state.isBusy) CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = FccColors.Cyan
                )
            }
            Spacer(Modifier.height(10.dp))
            InfoRow("STEP", "${state.step.ordinal + 1} / 4 — ${state.step.name}")
            InfoRow("MODE", state.currentMode.name)
            InfoRow("DEVICE", state.deviceName ?: "—")
            if (state.vendorId != null) InfoRow(
                "VID/PID",
                "0x${"%04X".format(state.vendorId)} / 0x${"%04X".format(state.productId ?: 0)}"
            )
            Spacer(Modifier.height(8.dp))
            Text(
                state.statusMessage,
                fontWeight = FontWeight.SemiBold,
                color = FccColors.TextHigh,
                fontSize = 14.sp
            )
            if (!state.lastError.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "⚠  ${state.lastError}",
                    color = FccColors.Danger,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            modifier = Modifier.width(72.dp),
            color = FccColors.TextDim,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
        Text(
            value,
            color = FccColors.TextHigh,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ActionCard(state: MainUiState, vm: MainViewModel) {
    HudPanel(modifier = Modifier.fillMaxWidth(), accent = FccColors.Magenta) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "// NEXT ACTION",
                style = MaterialTheme.typography.labelLarge,
                color = FccColors.Magenta
            )
            Spacer(Modifier.height(10.dp))
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
    Text(text, fontSize = 13.sp, color = FccColors.TextMid, lineHeight = 18.sp)
    Spacer(Modifier.height(12.dp))
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
    HudPanel(modifier = Modifier.fillMaxWidth(), accent = FccColors.Amber) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "// LEGAL NOTICE",
                style = MaterialTheme.typography.labelLarge,
                color = FccColors.Amber
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Die FCC-Aktivierung ist außerhalb von FCC-Regionen (z. B. EU) " +
                    "rechtlich problematisch. Verwende diese App nur dort, wo es " +
                    "erlaubt ist. Es entsteht keinerlei Garantie- oder Haftungs­anspruch.",
                fontSize = 12.sp,
                color = FccColors.TextMid,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun PrivacyOptionsButton() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity ?: return
    var showDialog by remember { mutableStateOf(false) }

    // Beim allerersten Start (noch keine Entscheidung gespeichert) automatisch zeigen.
    LaunchedEffect(Unit) {
        if (ConsentManager.getBuiltinDecision(context) == ConsentManager.BuiltinDecision.NONE) {
            showDialog = true
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Datenschutzeinstellungen / Werbung") }
        if (BuildConfig.DEBUG) {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = {
                    ConsentManager.resetBuiltinDecision(context)
                    ConsentManager.resetAndReshow(activity)
                    showDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("\uD83D\uDD27 Consent zurücksetzen (Debug)") }
        }
    }

    if (showDialog) {
        BuiltinConsentDialog(
            current = ConsentManager.getBuiltinDecision(context),
            onChoice = { decision ->
                ConsentManager.setBuiltinDecision(context, decision)
                showDialog = false
                if (decision != ConsentManager.BuiltinDecision.REJECT) {
                    AdsManager.initializeIfAllowed(activity.application)
                    (activity.application as? DjiFccApp)?.onConsentReady()
                }
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun BuiltinConsentDialog(
    current: ConsentManager.BuiltinDecision,
    onChoice: (ConsentManager.BuiltinDecision) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Datenschutz & Werbung") },
        text = {
            Column {
                Text(
                    "Diese App ist kostenlos und wird durch Werbung finanziert (Google AdMob).\n\n" +
                        "Bitte wähle, wie wir Werbung anzeigen dürfen:",
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "• Personalisiert: Werbung passend zu deinen Interessen (Cookies/IDs).\n" +
                        "• Nicht personalisiert: Allgemeine Werbung, ohne Profilbildung.\n" +
                        "• Ablehnen: Keine Werbung, App funktioniert weiter.\n\n" +
                        "Du kannst die Auswahl jederzeit über „Datenschutzeinstellungen“ ändern.",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
                if (current != ConsentManager.BuiltinDecision.NONE) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Aktuelle Auswahl: " + when (current) {
                            ConsentManager.BuiltinDecision.ACCEPT_PERSONALIZED -> "Personalisiert"
                            ConsentManager.BuiltinDecision.ACCEPT_NPA -> "Nicht personalisiert"
                            ConsentManager.BuiltinDecision.REJECT -> "Abgelehnt"
                            else -> "-"
                        },
                        fontSize = 11.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onChoice(ConsentManager.BuiltinDecision.ACCEPT_PERSONALIZED) }) {
                Text("Personalisiert")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onChoice(ConsentManager.BuiltinDecision.ACCEPT_NPA) }) {
                    Text("Nicht personalisiert")
                }
                TextButton(onClick = { onChoice(ConsentManager.BuiltinDecision.REJECT) }) {
                    Text("Ablehnen")
                }
            }
        }
    )
}

@Composable
private fun LogsCard(logs: List<String>, onClear: () -> Unit) {
    HudPanel(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "// TELEMETRY LOG",
                    style = MaterialTheme.typography.labelLarge,
                    color = FccColors.Cyan
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onClear,
                    enabled = logs.isNotEmpty()
                ) { Text("CLEAR", fontSize = 11.sp, letterSpacing = 1.sp) }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF03070D))
                    .border(1.dp, FccColors.SurfaceLine, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (logs.isEmpty()) {
                    Text("$ awaiting events…", color = FccColors.TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 2.dp),
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(logs) { line ->
                            Text(
                                text = "› $line",
                                fontSize = 11.sp,
                                color = FccColors.Lime,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
