package com.stem.heartsync.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CardiacCrimson600,
    onPrimary = SurgicalWhite,
    primaryContainer = RosePulse50,
    onPrimaryContainer = CardiacCrimson900,
    secondary = RosePulse300,
    onSecondary = DeepAnchorNavy900,
    secondaryContainer = RosePulse100,
    onSecondaryContainer = CardiacCrimson900,
    tertiary = DeepAnchorNavy800,
    onTertiary = SurgicalWhite,
    tertiaryContainer = DeepAnchorNavy100,
    onTertiaryContainer = DeepAnchorNavy900,
    background = PaperBackground,
    onBackground = DeepAnchorNavy900,
    surface = SurgicalWhite,
    onSurface = DeepAnchorNavy900,
    surfaceVariant = DeepAnchorNavy100,
    onSurfaceVariant = DeepAnchorNavy600,
    outline = Color(0xFFD2D7DF),
    error = CardiacCrimson600,
    onError = SurgicalWhite
)

@Composable
fun HeartSyncTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
