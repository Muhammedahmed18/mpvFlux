package app.marlboroadvance.mpvex.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.marlboroadvance.mpvex.R

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
 * Bundled Roboto, so the "off" state of the system-font switch is not affected by device or
 * theme-store fonts that replace the system sans-serif.
 */
private val RobotoFontFamily = FontFamily(
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_bold, FontWeight.Bold),
)

/**
 * App typography. With [useSystemFont] the stock styles follow the device font; otherwise every
 * style uses the bundled [RobotoFontFamily].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun appTypography(useSystemFont: Boolean): Typography =
    if (useSystemFont) {
        AppTypography
    } else {
        fun TextStyle.sans() = copy(fontFamily = RobotoFontFamily)
        AppTypography.copy(
            displayLarge = AppTypography.displayLarge.sans(),
            displayMedium = AppTypography.displayMedium.sans(),
            displaySmall = AppTypography.displaySmall.sans(),
            headlineLarge = AppTypography.headlineLarge.sans(),
            headlineMedium = AppTypography.headlineMedium.sans(),
            headlineSmall = AppTypography.headlineSmall.sans(),
            titleLarge = AppTypography.titleLarge.sans(),
            titleMedium = AppTypography.titleMedium.sans(),
            titleSmall = AppTypography.titleSmall.sans(),
            bodyLarge = AppTypography.bodyLarge.sans(),
            bodyMedium = AppTypography.bodyMedium.sans(),
            bodySmall = AppTypography.bodySmall.sans(),
            labelLarge = AppTypography.labelLarge.sans(),
            labelMedium = AppTypography.labelMedium.sans(),
            labelSmall = AppTypography.labelSmall.sans(),
            displayLargeEmphasized = AppTypography.displayLargeEmphasized.sans(),
            displayMediumEmphasized = AppTypography.displayMediumEmphasized.sans(),
            displaySmallEmphasized = AppTypography.displaySmallEmphasized.sans(),
            headlineLargeEmphasized = AppTypography.headlineLargeEmphasized.sans(),
            headlineMediumEmphasized = AppTypography.headlineMediumEmphasized.sans(),
            headlineSmallEmphasized = AppTypography.headlineSmallEmphasized.sans(),
            titleLargeEmphasized = AppTypography.titleLargeEmphasized.sans(),
            titleMediumEmphasized = AppTypography.titleMediumEmphasized.sans(),
            titleSmallEmphasized = AppTypography.titleSmallEmphasized.sans(),
            bodyLargeEmphasized = AppTypography.bodyLargeEmphasized.sans(),
            bodyMediumEmphasized = AppTypography.bodyMediumEmphasized.sans(),
            bodySmallEmphasized = AppTypography.bodySmallEmphasized.sans(),
            labelLargeEmphasized = AppTypography.labelLargeEmphasized.sans(),
            labelMediumEmphasized = AppTypography.labelMediumEmphasized.sans(),
            labelSmallEmphasized = AppTypography.labelSmallEmphasized.sans(),
        )
    }

/**
 * Typography for code/technical displays.
 */
val CodeTypography = AppTypography.copy(
    bodyMedium = AppTypography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
)
