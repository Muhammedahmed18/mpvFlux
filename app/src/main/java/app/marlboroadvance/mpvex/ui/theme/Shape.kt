package app.marlboroadvance.mpvex.ui.theme

import androidx.compose.material3.Shapes

/**
 * M3 Expressive shape scale, using the official corner sizes:
 * extraSmall 4dp, small 8dp, medium 12dp, large 16dp, largeIncreased 20dp,
 * extraLarge 28dp, extraLargeIncreased 32dp, extraExtraLarge 48dp.
 *
 * The app previously used custom values (medium 16, large 28, extraLarge 36), which made
 * cards and tiles noticeably rounder than Android's own apps and left no room for the
 * Expressive "increased" sizes. Components that want a softer look should pick a larger
 * step (e.g. shapes.largeIncreased) rather than inflating the base scale.
 */
val AppShapes = Shapes()
