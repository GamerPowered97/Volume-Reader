package com.example.volumereader.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MachineColorScheme = darkColorScheme(
    primary       = HoloBlue,
    secondary     = HoloGreen,
    tertiary      = HoloOrange,
    error         = HoloRed,
    background    = MachineDark,
    surface       = MachineSurface,
    surfaceVariant = MachineBezel,
    onPrimary     = MachineDark,
    onSecondary   = MachineDark,
    onTertiary    = MachineDark,
    onError       = TextPrimary,
    onBackground  = TextPrimary,
    onSurface     = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline       = MachineRidge,
    outlineVariant = MachineHighlight,
)

@Composable
fun VolumeReaderTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MachineColorScheme,
        typography  = Typography,
        content     = content
    )
}
