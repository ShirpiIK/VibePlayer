package com.vibeplayer.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val VibeBlack       = Color(0xFF0A0A0A)
val VibeDarkSurface = Color(0xFF141414)
val VibePrimary     = Color(0xFFD4AFFF)
val VibeAccent      = Color(0xFFE8D5FF)
val VibeOnSurface   = Color(0xFFF5F5F5)

private val DarkColorScheme = darkColorScheme(
    primary             = VibePrimary,
    onPrimary           = Color(0xFF2D004D),
    primaryContainer    = Color(0xFF44006B),
    onPrimaryContainer  = Color(0xFFEAB8FF),
    secondary           = VibeAccent,
    onSecondary         = Color(0xFF36004A),
    background          = VibeBlack,
    onBackground        = VibeOnSurface,
    surface             = VibeDarkSurface,
    onSurface           = VibeOnSurface,
    surfaceVariant      = Color(0xFF1E1E1E),
    onSurfaceVariant    = Color(0xFFCCCCCC)
)

val VibeTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

@Composable
fun VibePlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = VibeTypography,
        content     = content
    )
}
