package app.marlboroadvance.mpvex.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/**
 * Window width classes using the Material 3 breakpoints (600dp / 840dp).
 *
 * The player picks its layout from the window's width, not the device orientation, so
 * an upright tablet, an unfolded foldable or a split-screen window each get the layout
 * that fits the space they actually have:
 *
 *  - [Compact]  (< 600dp)  phone held upright, narrow split-screen → stacked layout,
 *                          bottom sheets.
 *  - [Medium]   (600–839dp) phone in landscape, small tablet, unfolded foldable →
 *                          corner-group layout, side sheets.
 *  - [Expanded] (≥ 840dp)  tablets and large screens → same as Medium.
 */
enum class PlayerWidthClass { Compact, Medium, Expanded }

val PlayerWidthClass.isCompact: Boolean get() = this == PlayerWidthClass.Compact

/** The current window's [PlayerWidthClass]; recomposes when the window is resized. */
@Composable
fun rememberPlayerWidthClass(): PlayerWidthClass {
  val containerWidthPx = LocalWindowInfo.current.containerSize.width
  val widthDp = if (containerWidthPx > 0) {
    with(LocalDensity.current) { containerWidthPx.toDp() }
  } else {
    // Before the window is measured, fall back to the configuration width.
    LocalConfiguration.current.screenWidthDp.dp
  }
  return when {
    widthDp < 600.dp -> PlayerWidthClass.Compact
    widthDp < 840.dp -> PlayerWidthClass.Medium
    else -> PlayerWidthClass.Expanded
  }
}
