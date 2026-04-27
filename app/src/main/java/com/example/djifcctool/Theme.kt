package com.example.djifcctool

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Modernes "Drone-HUD" Dark-Theme.
 *  – tiefes Schwarz/Navy als Basis
 *  – Cyan/Electric als Hauptakzent (Funk = elektrisch)
 *  – Magenta als sekundärer Highlight
 */
object FccColors {
    val DeepSpace      = Color(0xFF05080F)
    val Surface        = Color(0xFF0E1622)
    val SurfaceHigh    = Color(0xFF15202F)
    val SurfaceLine    = Color(0xFF1F2D40)

    val Cyan           = Color(0xFF00E5FF)
    val CyanDim        = Color(0xFF0095A8)
    val Electric       = Color(0xFF7DF9FF)
    val Magenta        = Color(0xFFFF2D95)
    val Amber          = Color(0xFFFFC400)
    val Lime           = Color(0xFF7CFF6B)
    val Danger         = Color(0xFFFF4D6D)

    val TextHigh       = Color(0xFFE6F1FF)
    val TextMid        = Color(0xFFA5B4C9)
    val TextDim        = Color(0xFF6B7A92)

    val GlowGradient = Brush.linearGradient(listOf(Cyan, Magenta))
    val PanelGradient = Brush.verticalGradient(listOf(SurfaceHigh, Surface))
    val HudGradient = Brush.linearGradient(
        listOf(Color(0xFF0A1424), Color(0xFF071120), Color(0xFF050A14))
    )
}

private val DarkColors = darkColorScheme(
    primary = FccColors.Cyan,
    onPrimary = Color(0xFF00131A),
    primaryContainer = FccColors.CyanDim,
    onPrimaryContainer = FccColors.TextHigh,
    secondary = FccColors.Magenta,
    onSecondary = Color.White,
    tertiary = FccColors.Lime,
    onTertiary = Color(0xFF002A0A),
    background = FccColors.DeepSpace,
    onBackground = FccColors.TextHigh,
    surface = FccColors.Surface,
    onSurface = FccColors.TextHigh,
    surfaceVariant = FccColors.SurfaceHigh,
    onSurfaceVariant = FccColors.TextMid,
    outline = FccColors.SurfaceLine,
    error = FccColors.Danger,
    onError = Color.White
)

private val TechTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp, letterSpacing = 0.8.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp, letterSpacing = 0.6.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp, letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp, letterSpacing = 1.sp
    )
)

@Composable
fun DjiFccToolTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = TechTypography,
        content = content
    )
}
