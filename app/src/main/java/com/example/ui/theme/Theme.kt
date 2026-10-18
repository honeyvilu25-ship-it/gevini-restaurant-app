package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = GeviniBurgundy,
  onPrimary = Color.White,
  primaryContainer = GeviniBurgundyDark,
  onPrimaryContainer = GeviniGold,
  secondary = GeviniGold,
  onSecondary = Color.Black,
  secondaryContainer = GeviniCharcoalBorder,
  onSecondaryContainer = GeviniGold,
  background = GeviniCharcoal,
  onBackground = GeviniCream,
  surface = GeviniCharcoalSurface,
  onSurface = GeviniCream,
  surfaceVariant = GeviniCharcoalCard,
  onSurfaceVariant = Color(0xFFD4CFCA),
  outline = GeviniCharcoalBorder
)

private val LightColorScheme = lightColorScheme(
  primary = GeviniBurgundy,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFFFDAD6),
  onPrimaryContainer = GeviniBurgundyDark,
  secondary = GeviniGoldDark,
  onSecondary = Color.White,
  secondaryContainer = GeviniCreamCard,
  onSecondaryContainer = Color(0xFF261904),
  background = GeviniCream,
  onBackground = GeviniCharcoal,
  surface = GeviniCreamSurface,
  onSurface = GeviniCharcoal,
  surfaceVariant = GeviniCreamCard,
  onSurfaceVariant = Color(0xFF4A443C),
  outline = Color(0xFFD4C8B8)
)

@Composable
fun GeviniTheme(
  darkTheme: Boolean = true, // Default to sleek premium dark theme for restaurant terminals
  content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
