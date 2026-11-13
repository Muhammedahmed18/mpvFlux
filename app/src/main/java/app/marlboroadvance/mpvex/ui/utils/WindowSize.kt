package app.marlboroadvance.mpvex.ui.utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 window width classes (Compact < 600dp <= Medium < 840dp <= Expanded).
 * Based on the current window, so split screen and freeform windows are handled too.
 */
enum class WindowWidthClass { Compact, Medium, Expanded }

@Composable
@ReadOnlyComposable
fun currentWindowWidthClass(): WindowWidthClass {
  val widthDp = LocalConfiguration.current.screenWidthDp
  return when {
    widthDp < 600 -> WindowWidthClass.Compact
    widthDp < 840 -> WindowWidthClass.Medium
    else -> WindowWidthClass.Expanded
  }
}

/** Widest a column of list or settings content should get before lines become hard to read. */
val ReadableContentMaxWidth: Dp = 840.dp

/**
 * Centers content and caps its width on wide windows (tablets, foldables, landscape).
 * No effect on phones in portrait, where the window is narrower than [maxWidth].
 */
fun Modifier.readableContentWidth(maxWidth: Dp = ReadableContentMaxWidth): Modifier =
  this
    .fillMaxWidth()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = maxWidth)
    .fillMaxWidth()
