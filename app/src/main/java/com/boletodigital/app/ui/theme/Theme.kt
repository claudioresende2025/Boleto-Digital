package com.boletodigital.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VerdeEscuro = Color(0xFF0B3D2E)
private val Verde = Color(0xFF1B7A57)
private val VerdeClaro = Color(0xFF2ECC71)
private val Fundo = Color(0xFFF3F7F5)
private val Superficie = Color(0xFFFFFFFF)
private val Alerta = Color(0xFFC45C26)
private val Texto = Color(0xFF12261F)

private val LightColors = lightColorScheme(
    primary = VerdeEscuro,
    onPrimary = Color.White,
    secondary = Verde,
    onSecondary = Color.White,
    tertiary = VerdeClaro,
    background = Fundo,
    onBackground = Texto,
    surface = Superficie,
    onSurface = Texto,
    error = Alerta,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = VerdeClaro,
    onPrimary = VerdeEscuro,
    secondary = Verde,
    onSecondary = Color.White,
    tertiary = Color(0xFF7DFFB2),
    background = Color(0xFF0A1612),
    onBackground = Color(0xFFE8F5F0),
    surface = Color(0xFF12261F),
    onSurface = Color(0xFFE8F5F0),
    error = Color(0xFFFFB088),
    onError = Color(0xFF3B1600)
)

@Composable
fun BoletoDigitalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
