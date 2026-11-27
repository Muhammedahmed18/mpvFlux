package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.player.SmartSkipDetector
import app.marlboroadvance.mpvex.ui.player.SmartSkipRange
import dev.vivvvek.seeker.Segment
import `is`.xyz.mpv.MPVLib
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A skipped range stays dismissed until playback has been outside it for this long.
 * While seeking, mpv first reports the seek target and can then report an earlier
 * keyframe that is still inside the range; without the settle time that second
 * report slid the chip back in for a moment.
 */
private const val SETTLE_MS = 1200L

/** If the seek clearly did nothing after this long, the chip is offered again. */
private const val RETRY_MS = 3000L

/** A keyframe seek may land this far before the range end and still count as done. */
private const val KEYFRAME_SLACK_SECONDS = 12f

/** Fade durations for the chip appearing and leaving. */
private const val FADE_IN_MS = 350
private const val FADE_OUT_MS = 280

/** How long the chip takes to glide up/down when the controls show or hide. */
private const val LIFT_MS = 450

/** Gap between the chip and the controls it sits above. */
private val ControlsGap = 12.dp

/**
 * Skip intro/outro chip, bottom-right. It is intentionally NOT gated by the player
 * controls' visibility or lock: it rests near the bottom edge and lifts above the
 * seekbar cluster while the controls are shown.
 *
 * All mpv state is collected here rather than in PlayerControls, so position ticks
 * and seek bursts recompose only this overlay and never the whole controls tree
 * (that root recomposition was dropping frames in the exit animation).
 *
 * @param hideBackground kept for call-site compatibility; the button always shows its
 *   container, so this is not used.
 * @param raisedClearancePx distance from the bottom edge to the top of the bottom
 *   controls, or 0 when they are not laid out.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun SmartSkipOverlay(
  chapters: ImmutableList<Segment>,
  controlsRaised: Boolean,
  raisedClearancePx: () -> Int,
  restingBottom: Dp,
  endPadding: Dp,
  hideBackground: Boolean,
  reduceMotion: Boolean,
  onSeek: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val mediaKey by MPVLib.propString["path"].collectAsState()
  val rawPosition by MPVLib.propInt["time-pos"].collectAsState()
  val rawDuration by MPVLib.propInt["duration"].collectAsState()
  val duration = rawDuration ?: 0

  val ranges = remember(chapters, duration) { SmartSkipDetector.ranges(chapters, duration) }
  // The position is read inside derived states, so the 1 Hz time-pos ticks only recompose this
  // overlay when the active range (or the latch release below) actually changes.
  val range by remember(ranges) {
    derivedStateOf { SmartSkipDetector.active(ranges, rawPosition ?: -1) }
  }

  // Keyed on mediaKey so the latch resets synchronously when the media changes.
  var skipped by remember(mediaKey) { mutableStateOf<SmartSkipRange?>(null) }
  var lastShown by remember(mediaKey) { mutableStateOf<SmartSkipRange?>(null) }

  val visibleRange = range?.takeIf { it != skipped }
  SideEffect { if (visibleRange != null) lastShown = visibleRange }
  // Keep the last range around so the exit animation still has content to draw.
  val displayedRange = visibleRange ?: lastShown

  // Release the latch only once playback has stayed outside the skipped range.
  // An unknown position (-1) or a single out-of-range report proves nothing.
  val latched = skipped
  val outside by remember(latched) {
    derivedStateOf {
      val position = rawPosition ?: -1
      latched != null && position >= 0 &&
        (position < latched.start || position >= latched.end)
    }
  }
  LaunchedEffect(latched, outside) {
    if (latched != null && outside) {
      delay(SETTLE_MS)
      skipped = null
    }
  }

  // Failed seek: still deep inside the range after a while, so offer the chip again.
  LaunchedEffect(latched) {
    if (latched == null) return@LaunchedEffect
    delay(RETRY_MS)
    // Read after the delay, so this is the current position, not the one at launch.
    val p = (rawPosition ?: -1).toFloat()
    if (p >= latched.start && latched.end - p > KEYFRAME_SLACK_SECONDS) skipped = null
  }

  // Vertical position: rest near the bottom edge, lift above the controls when shown.
  // Read inside offset {} so the glide re-places the chip without recomposing it.
  val density = LocalDensity.current
  val clearancePx = raisedClearancePx()
  val liftTarget = with(density) {
    if (controlsRaised && clearancePx > 0) clearancePx + ControlsGap.toPx() else restingBottom.toPx()
  }
  val lift by animateFloatAsState(
    targetValue = liftTarget,
    // Always a visible glide, never a snap: the chip stays on screen while it changes
    // place, so an instant move reads as a glitch even with reduce-motion enabled.
    animationSpec = tween(durationMillis = LIFT_MS, easing = FastOutSlowInEasing),
    label = "SmartSkipLift",
  )

  val scope = rememberCoroutineScope()
  val currentOnSeek by rememberUpdatedState(onSeek)
  val haptic = LocalHapticFeedback.current

  AnimatedVisibility(
    visible = visibleRange != null,
    // Pure fade in place, no sideways travel: a slow ease-out in, a slightly quicker
    // ease-in out. Both are single uninterrupted tweens, so there is no hitch between
    // movement and opacity.
    // Like the rest of the player controls, the fade also runs with reduce-motion on
    // (that setting removes slides and squeezes, not fades).
    enter = fadeIn(animationSpec = tween(FADE_IN_MS, easing = LinearOutSlowInEasing)),
    exit = fadeOut(animationSpec = tween(FADE_OUT_MS, easing = FastOutLinearInEasing)),
    modifier = modifier.offset { IntOffset(0, -lift.roundToInt()) },
  ) {
    val current = displayedRange
    if (current != null) {
      Box(modifier = Modifier.padding(end = endPadding)) {
        SmartSkipChip(
          label = stringResource(current.kind.labelRes),
          enabled = visibleRange != null,
          reduceMotion = reduceMotion,
          onClick = {
            val selected = visibleRange
            if (selected != null && skipped != selected) {
              val target = SmartSkipDetector.seekTarget(selected, duration)
              if (target > (rawPosition ?: -1)) {
                skipped = selected
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                // Let the exit animation draw its first frames before mpv starts the
                // seek, so the slide-out never begins on a dropped frame.
                scope.launch {
                  withFrameNanos { }
                  withFrameNanos { }
                  currentOnSeek(target)
                }
              }
            }
          },
        )
      }
    }
  }
}

/**
 * Material 3 filled tonal button: full pill, leading skip icon, label. It always keeps
 * its container (it ignores the "hide button backgrounds" setting) so it stays readable
 * over any video frame.
 */
@Composable
private fun SmartSkipChip(
  label: String,
  enabled: Boolean,
  reduceMotion: Boolean,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (pressed && !reduceMotion) 0.95f else 1f,
    animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow),
    label = "SmartSkipPressScale",
  )
  val container = MaterialTheme.colorScheme.secondaryContainer
  val content = MaterialTheme.colorScheme.onSecondaryContainer

  FilledTonalButton(
    onClick = onClick,
    enabled = enabled,
    shape = CircleShape,
    // The button is disabled while it fades out after a tap; keep the normal colours
    // so it doesn't flash grey on the way out.
    colors = ButtonDefaults.filledTonalButtonColors(
      containerColor = container,
      contentColor = content,
      disabledContainerColor = container,
      disabledContentColor = content,
    ),
    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    interactionSource = interactionSource,
    modifier = Modifier
      .graphicsLayer { scaleX = scale; scaleY = scale }
      .heightIn(min = 48.dp),
  ) {
    Icon(
      imageVector = ImageVector.vectorResource(R.drawable.ic_sym_skip_next_fill),
      contentDescription = null,
      modifier = Modifier.size(ButtonDefaults.IconSize),
    )
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
  }
}
