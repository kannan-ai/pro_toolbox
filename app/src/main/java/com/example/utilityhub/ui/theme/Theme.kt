package com.example.utilityhub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

@Composable
fun UtilityHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: String = "AMBER",
    isOledStealth: Boolean = false,
    pulseMode: String = "NEUTRAL",
    content: @Composable () -> Unit
) {
    val (primary, primaryHover) = when {
        pulseMode == "EFFICIENT" -> Color(0xFF00B0FF) to Color(0xFF0091EA) // Sharp Cobalt Blue
        pulseMode == "AMBIENT" -> Color(0xFF9C27B0) to Color(0xFF7B1FA2)   // Calm Purple
        accentColor == "BLUE" -> PrimaryBlue to PrimaryBlueHover
        accentColor == "GREEN" -> PrimaryGreen to PrimaryGreenHover
        accentColor == "AMBER" -> PrimaryAmber to PrimaryAmberHover
        else -> {
            try {
                val color = Color(accentColor.toColorInt())
                color to color.copy(alpha = 0.8f)
            } catch (_: Exception) {
                PrimaryAmber to PrimaryAmberHover
            }
        }
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primary,
            secondary = primaryHover,
            tertiary = DarkBorder,
            background = if (isOledStealth) Color.Black else DarkBg,
            surface = if (isOledStealth) Color.Black else DarkCardBg,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = DarkText,
            onSurface = DarkText,
            onSurfaceVariant = DarkTextSecondary,
            surfaceVariant = if (isOledStealth) Color(0xFF0A0A0A) else DarkInputBg,
            outline = DarkBorder,
            outlineVariant = Color.Transparent
        )
    } else {
        lightColorScheme(
            primary = primary,
            secondary = primaryHover,
            tertiary = LightBorder,
            background = LightBg,
            surface = LightCardBg,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = LightText,
            onSurface = LightText,
            onSurfaceVariant = LightTextSecondary,
            surfaceVariant = LightInputBg,
            outline = LightBorder,
            outlineVariant = LightText.copy(alpha = 0.08f)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
