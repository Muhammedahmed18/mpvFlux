package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.player.controls.LocalPlayerButtonsClickEvent
import app.marlboroadvance.mpvex.ui.player.controls.playerControlsEnterAnimationSpec
import app.marlboroadvance.mpvex.ui.player.controls.playerControlsExitAnimationSpec
import kotlinx.coroutines.delay

/** Visual diameter of the tonal circle. */
private val ButtonSize = 60.dp

/** Hit area around the circle; also leaves room for the progress ring. */
private val TouchTargetSize = 92.dp

private val IconSize = 26.dp
private val RingStrokeWidth = 3.dp

/** Gap between the circle edge and the inner edge of the progress ring. */
private val RingGap = 4.dp

/** Hold time needed to unlock. */
private const val HOLD_TO_UNLOCK_MS = 800

/** Time the success state (open-lock icon) is shown before the locked UI is dismissed. */
private const val SUCCESS_SETTLE_MS = 220L

private const val PRESSED_SCALE = 0.96f
private const val TRACK_ALPHA = 0.24f

/**
 * Unlock button for the locked player: a flat Material 3 Expressive tonal circle shown in
 * the top-left corner (see PlayerControls). It auto-hides with the other controls; pressing
 * it restarts the auto-hide timer so it never disappears during a hold.
 *
 * Hold-to-unlock: pressing scales the button to 96% with a light haptic tick and starts a
 * progress ring at 12 o'clock that fills clockwise, linearly, over 800 ms. Releasing early
 * animates the ring back to zero; a quick tap does nothing. On completion a strong haptic
 * fires, the icon switches to the open lock, and [onUnlock] is called once the short success
 * state has been seen, so the parent's fade-out doesn't cut it off.
 */
@Composable
fun ThumbZoneUnlock(
  onUnlock: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  // Restarts the player's auto-hide timer, so the button can't fade out mid-hold.
  val resetAutoHide = LocalPlayerButtonsClickEvent.current
  val currentOnUnlock by rememberUpdatedState(onUnlock)

  var isPressed by remember { mutableStateOf(false) }
  var isUnlocked by remember { mutableStateOf(false) }
  val progress = remember { Animatable(0f) }

  val containerColor = MaterialTheme.colorScheme.secondaryContainer
  val iconColor = MaterialTheme.colorScheme.onSecondaryContainer
  val ringColor = MaterialTheme.colorScheme.primary
  val trackColor = ringColor.copy(alpha = TRACK_ALPHA)

  // Hold timer. Cancelled (and so never unlocks) as soon as isPressed goes false.
  LaunchedEffect(isPressed) {
    if (isUnlocked) return@LaunchedEffect
    if (isPressed) {
      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      // Linear and proportional to what's left, so the ring always shows the real time
      // remaining, even when a new press starts while the ring is still rewinding.
      val remainingMs = (HOLD_TO_UNLOCK_MS * (1f - progress.value)).toInt().coerceAtLeast(1)
      progress.animateTo(1f, tween(durationMillis = remainingMs, easing = LinearEasing))
      // Only reached if the hold wasn't interrupted.
      if (!isUnlocked) {
        isUnlocked = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      }
    } else {
      progress.animateTo(0f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
    }
  }

  // Success: show the open lock briefly, then hand off to the parent, which fades this
  // button out and restores the controls. isUnlocked guards against repeat callbacks.
  LaunchedEffect(isUnlocked) {
    if (!isUnlocked) return@LaunchedEffect
    delay(SUCCESS_SETTLE_MS)
    currentOnUnlock()
    // Normally the parent removes this composable after its exit fade. If the player is
    // re-locked before that finishes, the same instance is reused, so return to the
    // initial locked state once the fade is over.
    delay(PARENT_EXIT_MS)
    isUnlocked = false
    isPressed = false
    progress.snapTo(0f)
  }

  val buttonScale by animateFloatAsState(
    targetValue = if (isPressed && !isUnlocked) PRESSED_SCALE else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow,
    ),
    label = "UnlockButtonScale",
  )

  Box(
    modifier = modifier
      .size(TouchTargetSize)
      .drawBehind {
        val p = progress.value
        if (p <= 0f) return@drawBehind
        val stroke = RingStrokeWidth.toPx()
        val radius = (ButtonSize / 2 + RingGap).toPx() + stroke / 2
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)
        // Subtle inactive track, only while a hold is in progress or rewinding.
        drawCircle(
          color = trackColor,
          radius = radius,
          center = center,
          style = Stroke(width = stroke),
        )
        // -90° is 12 o'clock; a positive sweep goes clockwise.
        drawArc(
          color = ringColor,
          startAngle = -90f,
          sweepAngle = 360f * p,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
      }
      .pointerInput(isUnlocked) {
        if (isUnlocked) return@pointerInput
        detectTapGestures(
          onPress = {
            resetAutoHide()
            isPressed = true
            // tryAwaitRelease returns false (instead of throwing) when the gesture is
            // cancelled; either way the hold ends here.
            try {
              tryAwaitRelease()
            } finally {
              isPressed = false
            }
          },
        )
      },
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .size(ButtonSize)
        .graphicsLayer {
          scaleX = buttonScale
          scaleY = buttonScale
        }
        .background(containerColor, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      AnimatedContent(
        targetState = isUnlocked,
        transitionSpec = {
          (fadeIn(playerControlsEnterAnimationSpec()) + scaleIn(initialScale = 0.85f, animationSpec = tween(200)))
            .togetherWith(fadeOut(playerControlsExitAnimationSpec()))
        },
        label = "UnlockIconTransition",
      ) { unlocked ->
        Icon(
          imageVector = ImageVector.vectorResource(
            if (unlocked) R.drawable.ic_sym_lock_open_fill else R.drawable.ic_sym_lock,
          ),
          contentDescription = null,
          tint = iconColor,
          modifier = Modifier.size(IconSize),
        )
      }
    }
  }
}

/** Slightly longer than playerControlsExitAnimationSpec(), the parent's fade-out. */
private const val PARENT_EXIT_MS = 450L
