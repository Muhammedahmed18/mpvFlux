package app.marlboroadvance.mpvex.ui.player.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Player chrome style — the single source of truth for every control drawn
// over the video (buttons, chips, timers, title pill, transport, sliders,
// unlock button).
//
// MPV draws to a SurfaceView, which can't be sampled for a real backdrop blur,
// so "glass" is approximated with a translucent tonal fill. There are exactly
// two weights:
//
//   Standard → fill only. Icon buttons, value chips, button groups, timers,
//              title pill. These are small and numerous; a rim on each would
//              be visual noise.
//   Strong   → denser fill + white rim light. Prev/Next, volume/brightness
//              sliders, unlock button. Fixed-shape, larger surfaces that sit
//              alone over the video and need a defined edge.
//
// Play/Pause is the one exception: it stays a solid `primary` focal.
// ---------------------------------------------------------------------------

enum class GlassWeight { Standard, Strong }

object PlayerChrome {
  const val StandardFillAlpha = 0.55f
  const val StrongFillAlpha = 0.72f

  /** M3 disabled-content alpha. */
  const val DisabledAlpha = 0.38f

  val RimWidth: Dp = 1.dp

  /** Vertical rim light: brighter at the top edge, fading towards the bottom. */
  val RimBrush: Brush = Brush.verticalGradient(
    listOf(
      Color.White.copy(alpha = 0.25f),
      Color.White.copy(alpha = 0.05f),
    ),
  )
}

/** Fill colour for a glass surface of the given [weight]. */
@Composable
@ReadOnlyComposable
fun playerGlassFill(weight: GlassWeight = GlassWeight.Standard): Color =
  MaterialTheme.colorScheme.surfaceContainerHighest.copy(
    alpha = when (weight) {
      GlassWeight.Standard -> PlayerChrome.StandardFillAlpha
      GlassWeight.Strong -> PlayerChrome.StrongFillAlpha
    },
  )

/**
 * Text and icon colour for player chrome. On the glass fill it is the theme's onSurface.
 * With "hide button background" on there is no fill, so content sits straight on the
 * (dark-scrimmed) video and must stay light in every theme; in light theme onSurface is
 * near-black and disappears.
 */
@Composable
@ReadOnlyComposable
fun playerChromeContentColor(hideBackground: Boolean): Color =
  if (hideBackground) Color.White else MaterialTheme.colorScheme.onSurface

/**
 * Clips to [shape] and paints the player glass behind the content. With
 * [hideBackground] on (Settings → "hide button background") only the clip is applied,
 * so ripples stay shaped but nothing is drawn over the video.
 */
@Composable
fun Modifier.playerGlass(
  shape: Shape,
  hideBackground: Boolean,
  weight: GlassWeight = GlassWeight.Standard,
): Modifier {
  if (hideBackground) return this.clip(shape)
  val filled = this
    .clip(shape)
    .background(playerGlassFill(weight), shape)
  return if (weight == GlassWeight.Strong) {
    filled.border(width = PlayerChrome.RimWidth, brush = PlayerChrome.RimBrush, shape = shape)
  } else {
    filled
  }
}
