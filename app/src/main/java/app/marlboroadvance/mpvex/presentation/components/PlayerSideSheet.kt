@file:Suppress("DEPRECATION")

package app.marlboroadvance.mpvex.presentation.components

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val sideSheetAnimationSpec = tween<Float>(350)

/**
 * Right-anchored modal side sheet for the player overlay. Slides in from the right
 * edge so the video stays watchable while the user picks a track. Mirrors the
 * existing [PlayerSheet] semantics but horizontal instead of vertical.
 *
 * Dismiss gestures:
 *  - Drag right past the positional threshold
 *  - Tap on the scrim
 *  - Back gesture (BackHandler — gets predictive back on Android 14+)
 *
 * Sizing:
 *  - `sheetWidth` is the visible width; consumers should compute based on screen
 *    size (e.g. min(360.dp, screenWidth × 0.5f)).
 *  - Height fills the screen; content is window-inset-padded for status/nav bars.
 *
 * Shape: only the LEFT corners are rounded (right side is the screen edge).
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun PlayerSideSheet(
  onDismissRequest: () -> Unit,
  sheetWidth: Dp,
  modifier: Modifier = Modifier,
  tonalElevation: Dp = 1.dp,
  surfaceColor: Color? = null,
  content: @Composable () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current
  val latestOnDismissRequest by rememberUpdatedState(onDismissRequest)

  val sheetWidthPx = with(density) { sheetWidth.toPx() }

  var backgroundAlpha by remember { mutableFloatStateOf(0f) }
  val alpha by animateFloatAsState(
    backgroundAlpha,
    animationSpec = sideSheetAnimationSpec,
    label = "side_sheet_scrim_alpha",
  )

  val decayAnimationSpec = rememberSplineBasedDecay<Float>()
  // 0 = Open (sheet fully visible), 1 = Closed (sheet pushed off-screen to the right).
  val anchoredDraggableState = remember(sheetWidthPx) {
    AnchoredDraggableState(
      initialValue        = 1,
      snapAnimationSpec   = sideSheetAnimationSpec,
      decayAnimationSpec  = decayAnimationSpec,
      positionalThreshold = { with(density) { 56.dp.toPx() } },
      velocityThreshold   = { with(density) { 125.dp.toPx() } },
    ).also {
      it.updateAnchors(
        DraggableAnchors {
          0 at 0f
          1 at sheetWidthPx
        },
      )
    }
  }

  val internalOnDismissRequest = {
    if (anchoredDraggableState.currentValue == 0) {
      scope.launch {
        backgroundAlpha = 0f
        anchoredDraggableState.animateTo(1)
      }
    }
  }

  Box(
    modifier = Modifier
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication        = null,
        onClick           = internalOnDismissRequest,
      )
      .fillMaxSize()
      .background(Color.Black.copy(alpha)),
    contentAlignment = Alignment.CenterEnd,
  ) {
    Surface(
      modifier = Modifier
        .fillMaxHeight()
        .width(sheetWidth)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication        = null,
          onClick           = {},
        )
        .then(modifier)
        .offset {
          IntOffset(
            anchoredDraggableState.offset
              .takeIf { it.isFinite() }
              ?.roundToInt()
              ?: 0,
            0,
          )
        }
        .anchoredDraggable(
          state       = anchoredDraggableState,
          orientation = Orientation.Horizontal,
        )
        .windowInsetsPadding(
          WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.End),
        ),
      shape          = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
      color          = surfaceColor ?: MaterialTheme.colorScheme.surface,
      tonalElevation = tonalElevation,
      content = {
        BackHandler(
          enabled = anchoredDraggableState.targetValue == 0,
          onBack  = internalOnDismissRequest,
        )
        content()
      },
    )

    LaunchedEffect(Unit) {
      backgroundAlpha = 0.5f
    }

    LaunchedEffect(anchoredDraggableState) {
      scope.launch { anchoredDraggableState.animateTo(0) }
      snapshotFlow { anchoredDraggableState.currentValue }
        .drop(1)
        .filter { it == 1 }
        .collectLatest { latestOnDismissRequest() }
    }
  }
}

/**
 * Helper for sheet-width sizing. Phones get ~360dp or half the screen, whichever
 * is smaller; tablets get up to ~480dp or 40% of the screen.
 */
@Composable
@SuppressLint("ConfigurationScreenWidthHeight")
fun rememberSideSheetWidth(): Dp {
  val configuration = LocalConfiguration.current
  val screenWidthDp = configuration.screenWidthDp.dp
  // Tablets typically have screenWidthDp ≥ 600
  val isTablet = configuration.screenWidthDp >= 600
  return remember(screenWidthDp, isTablet) {
    if (isTablet) {
      minOf(480.dp, screenWidthDp * 0.4f)
    } else {
      minOf(360.dp, screenWidthDp * 0.5f)
    }
  }
}
