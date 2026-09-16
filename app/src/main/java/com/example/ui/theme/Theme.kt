package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val QuantDarkColorScheme =
  darkColorScheme(
    primary = QuantBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = QuantSurfaceVariant,
    onPrimaryContainer = QuantTextPrimary,
    secondary = QuantGreen,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = QuantGreenDim,
    onSecondaryContainer = QuantGreen,
    tertiary = QuantAmber,
    background = QuantBackground,
    onBackground = QuantTextPrimary,
    surface = QuantSurface,
    onSurface = QuantTextPrimary,
    surfaceVariant = QuantSurfaceVariant,
    onSurfaceVariant = QuantTextSecondary,
    outline = QuantBorder,
    outlineVariant = QuantBorder,
    error = QuantRed,
    onError = androidx.compose.ui.graphics.Color.White,
    errorContainer = QuantRedDim,
    onErrorContainer = QuantRed
  )

@Composable
fun QtY64Theme(
  darkTheme: Boolean = true, // Force high-fidelity quant dark theme
  dynamicColor: Boolean = false, // Preserve strict financial terminal tokens
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = QuantDarkColorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  QtY64Theme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

