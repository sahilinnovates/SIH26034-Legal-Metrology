package com.legalmetrology.inspector.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============================================================
// THEME — Premium Light Design System
// Refined editorial/fintech aesthetic for government inspection app
// Clean, human-crafted, professional light mode
// ============================================================

private val LightColorScheme = lightColorScheme(
    // Primary (Executive slate)
    primary = Slate950,
    onPrimary = Color.White,
    primaryContainer = Slate100,
    onPrimaryContainer = Slate950,

    // Secondary (Refined cobalt for interactive elements)
    secondary = Cobalt600,
    onSecondary = Color.White,
    secondaryContainer = Cobalt100,
    onSecondaryContainer = Cobalt600,

    // Error (Crimson for violations)
    error = Crimson600,
    onError = Color.White,
    errorContainer = Crimson50,
    onErrorContainer = Crimson600,

    // Tertiary (Emerald for compliance/pass states)
    tertiary = Emerald600,
    onTertiary = Color.White,
    tertiaryContainer = Emerald50,
    onTertiaryContainer = Emerald600,

    // Backgrounds / Surfaces (Crisp warm off-whites)
    background = Slate100,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = Slate50,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Slate950,

    // Outline (Subtle crisp borders)
    outline = Slate300,
    outlineVariant = Slate200,

    // Inverse (for snackbars etc.)
    inverseSurface = Slate900,
    inverseOnSurface = Slate100,
    inversePrimary = Cobalt500,

    // Scrim for dialogs
    scrim = Color(0x66000000),
)

@Composable
fun LegalMetrologyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
