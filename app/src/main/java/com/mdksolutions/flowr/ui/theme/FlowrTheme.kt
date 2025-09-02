package com.mdksolutions.flowr.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable

@Composable
fun FlowrTheme(
    themeType: FlowrThemeType = FlowrThemeType.DARK_LUXURY,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeType) {
        FlowrThemeType.DARK_LUXURY -> DarkLuxuryColorScheme
        FlowrThemeType.GRADIENT_MODERN -> GradientModernColorScheme
        FlowrThemeType.PHOTO_BACKGROUND -> PhotoBackgroundColorScheme
        FlowrThemeType.MINIMAL_CREAM -> MinimalCreamColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(), // You can customize later
        shapes = Shapes(),         // Default for now
        content = content
    )
}
