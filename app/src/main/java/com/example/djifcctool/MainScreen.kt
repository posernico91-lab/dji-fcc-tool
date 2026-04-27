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
                                    stringResource(R.string.hud_console_label),
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
        title = { Text(stringResource(R.string.confirm_title)) },
        text = {
            Column {
                Text(stringResource(R.string.confirm_body), fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.confirm_b1), fontSize = 13.sp)
                Text(stringResource(R.string.confirm_b2), fontSize = 13.sp)
                Text(stringResource(R.string.confirm_b3), fontSize = 13.sp)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.btn_confirm_send)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
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
                    stringResource(R.string.safe_mode_title),
                    fontWeight = FontWeight.Black,
                    color = FccColors.Amber,
                    fontSize = 13.sp,
                    letterSpacing = 1.5.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.safe_mode_body),
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
                    stringResource(R.string.status_header),
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
            InfoRow(stringResource(R.string.label_step), "${state.step.ordinal + 1} / 4 — ${state.step.name}")
            InfoRow(stringResource(R.string.label_mode), state.currentMode.name)
            InfoRow(stringResource(R.string.label_device), state.deviceName ?: "—")
            if (state.vendorId != null) InfoRow(
                stringResource(R.string.label_vidpid),
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
                stringResource(R.string.action_header),
                style = MaterialTheme.typography.labelLarge,
                color = FccColors.Magenta
            )
            Spacer(Modifier.height(10.dp))
            when (state.step) {
                WizardStep.SCAN -> {
                    HelpText(stringResource(R.string.scan_help))
                    PrimaryButton(stringResource(R.string.btn_scan_controller), state.isBusy, vm::scanForRemote)
                }
                WizardStep.CONNECT -> {
                    HelpText(stringResource(R.string.connect_help))
                    PrimaryButton(stringResource(R.string.btn_connect), state.isBusy, vm::scanForRemote)
                }
                WizardStep.DETECT -> {
                    HelpText(stringResource(R.string.detect_help))
                    PrimaryButton(stringResource(R.string.btn_next_to_patch), state.isBusy, vm::detectMode)
                    Spacer(Modifier.height(8.dp))
                    SecondaryButton(stringResource(R.string.btn_disconnect), state.isBusy, vm::disconnect)
                }
                WizardStep.PATCH -> {
                    if (state.patchSuccess) {
                        PostPatchInstructions(onDisconnect = vm::disconnect)
                    } else {
                        HelpText(stringResource(R.string.patch_help))
                        PrimaryButton(stringResource(R.string.btn_apply_fcc), state.isBusy, vm::applyFccPatch)
                        Spacer(Modifier.height(8.dp))
                        SecondaryButton(stringResource(R.string.btn_reset_ce), state.isBusy, vm::resetToCe)
                        Spacer(Modifier.height(8.dp))
                        SecondaryButton(stringResource(R.string.btn_disconnect), state.isBusy, vm::disconnect)
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
                stringResource(R.string.postpatch_header),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.postpatch_intro), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            NumberedStep(1, stringResource(R.string.postpatch_step_1))
            NumberedStep(2, stringResource(R.string.postpatch_step_2))
            NumberedStep(3, stringResource(R.string.postpatch_step_3))
            NumberedStep(4, stringResource(R.string.postpatch_step_4))
            NumberedStep(5, stringResource(R.string.postpatch_step_5))
            NumberedStep(6, stringResource(R.string.postpatch_step_6))

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.check_title), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            BulletText(stringResource(R.string.check_1))
            BulletText(stringResource(R.string.check_2))
            BulletText(stringResource(R.string.check_3))

            Spacer(Modifier.height(14.dp))
            DjiFlyMockup()

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.breaks_title), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            BulletText(stringResource(R.string.breaks_1))
            BulletText(stringResource(R.string.breaks_2))
            BulletText(stringResource(R.string.breaks_3))

            Spacer(Modifier.height(12.dp))
            SecondaryButton(stringResource(R.string.btn_done), false, onDisconnect)
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
private fun DjiFlyMockup() {
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.mockup_title),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color(0xFF1B5E20)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.mockup_caption),
            fontSize = 11.sp,
            color = Color(0xFF424242)
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlyPhoneMockup(
                isFcc = true,
                modifier = Modifier.weight(1f)
            )
            FlyPhoneMockup(
                isFcc = false,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(Color(0xFF00C853), shape = CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.mockup_legend_fcc),
                fontSize = 11.sp,
                color = Color(0xFF1B5E20),
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(Color(0xFFB71C1C), shape = CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.mockup_legend_ce),
                fontSize = 11.sp,
                color = Color(0xFF424242)
            )
        }
    }
}

@Composable
private fun FlyPhoneMockup(isFcc: Boolean, modifier: Modifier = Modifier) {
    val accent = if (isFcc) Color(0xFF00C853) else Color(0xFFB71C1C)
    val badge = if (isFcc) "FCC" else "CE"
    val band = if (isFcc) "2.4 + 5.8 GHz" else "2.4 GHz"
    val distance = if (isFcc) "~12 km" else "~6 km"
    Column(
        modifier
            .background(Color(0xFF101418), shape = RoundedCornerShape(14.dp))
            .border(2.dp, accent, shape = RoundedCornerShape(14.dp))
            .padding(8.dp)
    ) {
        // Top bar
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("DJI Fly", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Box(
                Modifier
                    .background(accent, shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(badge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        // Settings row (highlighted)
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1F24), shape = RoundedCornerShape(6.dp))
                .padding(6.dp)
        ) {
            Text(
                stringResource(R.string.mockup_band_label),
                color = Color(0xFFB0BEC5),
                fontSize = 9.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(band, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        // Distance HUD
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("D", color = Color(0xFF607D8B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text(distance, color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        // Signal bars
        Row(verticalAlignment = Alignment.Bottom) {
            val bars = if (isFcc) 5 else 2
            for (i in 1..5) {
                Box(
                    Modifier
                        .padding(end = 1.dp)
                        .width(4.dp)
                        .height((4 + i * 2).dp)
                        .background(
                            if (i <= bars) accent else Color(0xFF37474F),
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun SafetyCard() {
    HudPanel(modifier = Modifier.fillMaxWidth(), accent = FccColors.Amber) {
        Column(Modifier.padding(14.dp)) {
            Text(
                stringResource(R.string.safety_header),
                style = MaterialTheme.typography.labelLarge,
                color = FccColors.Amber
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.safety_body),
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
        ) { Text(stringResource(R.string.privacy_options)) }
        if (BuildConfig.DEBUG) {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = {
                    ConsentManager.resetBuiltinDecision(context)
                    ConsentManager.resetAndReshow(activity)
                    showDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.privacy_reset_debug)) }
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
        title = { Text(stringResource(R.string.consent_title)) },
        text = {
            Column {
                Text(stringResource(R.string.consent_body), fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.consent_choices),
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
                if (current != ConsentManager.BuiltinDecision.NONE) {
                    Spacer(Modifier.height(8.dp))
                    val label = when (current) {
                        ConsentManager.BuiltinDecision.ACCEPT_PERSONALIZED -> stringResource(R.string.consent_choice_personalized)
                        ConsentManager.BuiltinDecision.ACCEPT_NPA -> stringResource(R.string.consent_choice_npa)
                        ConsentManager.BuiltinDecision.REJECT -> stringResource(R.string.consent_choice_reject)
                        else -> "-"
                    }
                    Text(
                        stringResource(R.string.consent_current, label),
                        fontSize = 11.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onChoice(ConsentManager.BuiltinDecision.ACCEPT_PERSONALIZED) }) {
                Text(stringResource(R.string.consent_choice_personalized))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onChoice(ConsentManager.BuiltinDecision.ACCEPT_NPA) }) {
                    Text(stringResource(R.string.consent_choice_npa))
                }
                TextButton(onClick = { onChoice(ConsentManager.BuiltinDecision.REJECT) }) {
                    Text(stringResource(R.string.consent_choice_reject))
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
                    stringResource(R.string.logs_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = FccColors.Cyan
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onClear,
                    enabled = logs.isNotEmpty()
                ) { Text(stringResource(R.string.logs_clear), fontSize = 11.sp, letterSpacing = 1.sp) }
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
                    Text(stringResource(R.string.logs_empty), color = FccColors.TextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
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
