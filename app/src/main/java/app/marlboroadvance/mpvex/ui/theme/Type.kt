package app.marlboroadvance.mpvex.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

/**
 * M3 Expressive type scale (system font).
 *
 * Uses the Material spec baseline styles plus their built-in *Emphasized* variants
 * (e.g. `titleMediumEmphasized`). The app previously made the baseline styles themselves
 * Bold/SemiBold, so every title was heavy and nothing could stand out; now the baseline
 * stays calm and key text (card titles, screen titles, counts) opts into the emphasized
 * style for real hierarchy.
 */
val AppTypography = Typography()

/**
 * Typography for code/technical displays.
 */
val CodeTypography = AppTypography.copy(
    bodyMedium = AppTypography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
)
