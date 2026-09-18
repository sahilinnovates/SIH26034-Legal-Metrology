package com.legalmetrology.inspector.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============================================================
// THEME — Dark-only (enforcement field app, dark mode is standard
// for AR-adjacent apps to minimize glare over live camera feed)
// ============================================================

private val DarkColorScheme = darkColorScheme(
    // Primary (electric indigo)
    primary = Indigo500,
    onPrimary = White,
    primaryContainer = Indigo900,
    onPrimaryContainer = Indigo200,

    // Secondary (emerald — used for pass/success states)
    secondary = Emerald500,
    onSecondary = Navy900,
    secondaryContainer = Color(0xFF003D2E),
    onSecondaryContainer = Emerald400,

    // Error (coral — used for fail/violation states)
    error = Coral500,
    onError = White,
    errorContainer = Color(0xFF4A0000),
    onErrorContainer = Coral400,

    // Tertiary (amber — used for pending/warning states)
    tertiary = Amber500,
    onTertiary = Navy900,
    tertiaryContainer = Color(0xFF3B2900),
    onTertiaryContainer = Amber400,

    // Backgrounds / Surfaces
    background = Navy900,
    onBackground = Gray100,
    surface = Navy800,
    onSurface = Gray100,
    surfaceVariant = Navy700,
    onSurfaceVariant = Gray300,
    surfaceTint = Indigo500,

    // Outline
    outline = Navy600,
    outlineVariant = Color(0xFF1F2937),

    // Inverse (for snackbars etc.)
    inverseSurface = Gray100,
    inverseOnSurface = Navy900,
    inversePrimary = Indigo700,

    // Scrim for dialogs
    scrim = Color(0xCC050812),
)

@Composable
fun LegalMetrologyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
