package com.example.volumereader.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val MachineColorScheme = darkColorScheme(
    primary = HoloBlue,
    secondary = HoloGreen,
    tertiary = HoloOrange,
    background = MachineDark,
    surface = MachineSurface,
    onPrimary = MachineDark,
    onSecondary = MachineDark,
    onTertiary = MachineDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun VolumeReaderTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MachineColorScheme,
        typography = Typography,
        content = content
    )
}
