package com.mdksolutions.flowr.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

val DarkLuxuryColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFD4AF37),      // Gold
    onPrimary = Color.Black,
    background = Color(0xFF0B1F1A),   // Deep green-black
    onBackground = Color.White,
    surface = Color(0xFF1A1A1A),
    onSurface = Color.White
)

val GradientModernColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF4FCF9C),     // Mint green
    onPrimary = Color.White,
    background = Color(0xFF3A91E5),  // Sky blue
    onBackground = Color.White,
    surface = Color.White,
    onSurface = Color.Black
)

val PhotoBackgroundColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),     // Soft green
    onPrimary = Color.Black,
    background = Color(0xFF1E4620),  // Forest green
    onBackground = Color.White,
    surface = Color(0xFF2E7D32),
    onSurface = Color.White
)

val MinimalCreamColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),     // Natural green
    onPrimary = Color.White,
    background = Color(0xFFFDF6EE),  // Cream
    onBackground = Color(0xFF333333),
    surface = Color.White,
    onSurface = Color.Black
)
