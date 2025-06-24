package com.example.worm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Definisi warna Hijau utama
val Hijau = Color(0xFF2B524A)

// Skema warna untuk dark mode
private val DarkColorScheme = darkColorScheme(
    primary      = Hijau,
    onPrimary    = Color.White,
    secondary    = Hijau,
    onSecondary  = Color.White,
    tertiary     = Hijau,
    onTertiary   = Color.White,
    background   = Color.Black,
    onBackground = Color.White,
    surface      = Color.DarkGray,
    onSurface    = Color.White
)

// Skema warna untuk light mode
private val LightColorScheme = lightColorScheme(
    primary      = Hijau,
    onPrimary    = Color.White,
    secondary    = Hijau,
    onSecondary  = Color.White,
    tertiary     = Hijau,
    onTertiary   = Color.White,
    background   = Color.White,
    onBackground = Color.Black,
    surface      = Color.LightGray,
    onSurface    = Color.Black
)

@Composable
fun WormTheme(
    // Ikuti sistem dark theme perangkat
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Pilih skema statis berdasarkan darkTheme
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography  = Typography,
        content     = content
    )
}
