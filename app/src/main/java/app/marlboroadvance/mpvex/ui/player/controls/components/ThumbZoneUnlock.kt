package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.ui.player.controls.playerControlsEnterAnimationSpec
import app.marlboroadvance.mpvex.ui.player.controls.playerControlsExitAnimationSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A redesigned "Thumb Zone" unlock button for one-handed operation.
 * Features glass-morphism, a circular progress arc with a track baseline,
 * and an atmospheric "light bleed" aura.
 * Visually anchored to the corner while maintaining a generous hit area.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ThumbZoneUnlock(
  onUnlock: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val scope  = rememberCoroutineScope()

  var isPressed  by remember { mutableStateOf(false) }
  var isUnlocked by remember { mutableStateOf(false) }
  val progress = remember { Animatable(0f) }

  val primaryColor = MaterialTheme.colorScheme.primary

  // Glass circle: slightly more opaque (0.72f) to match the FAB alpha used in Phase 2,
  // ensuring all glass surfaces in the player share the same visual weight.
  val glassColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)

  // outlineVariant replaces hardcoded Color.White.copy(alpha = 0.15f) —
  // correct M3 token for subtle borders, adapts to light/dark theme automatically.
  val glassBorderColor = MaterialTheme.colorScheme.outlineVariant

  // onPrimary replaces hardcoded Color.White for the unlocked state —
  // semantically correct on a primary-tinted surface.
  // onSurface replaces controlColor.copy(alpha = 0.8f) for the locked state —
  // uses the M3 token directly, removes the custom extension dependency and manual alpha.
  val lockedIconTint   = MaterialTheme.colorScheme.onSurface
  val unlockedIconTint = MaterialTheme.colorScheme.onPrimary

  // Handle the hold-to-unlock progress logic
  LaunchedEffect(isPressed) {
    if (isPressed && !isUnlocked) {
      progress.animateTo(
        targetValue   = 1f,
        // FastOutSlowInEasing replaces LinearEasing — consistent with Phase 4 motion alignment.
        // Hold progress now accelerates smoothly rather than ticking at a constant rate.
        animationSpec = tween(800, easing = FastOutSlowInEasing),
      )
      // >= 0.99f replaces == 1f — eliminates fragile exact float equality check.
      if (progress.value >= 0.99f) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        isUnlocked = true
        onUnlock()
      }
    } else if (!isUnlocked) {
      // Snap-back on early release: DampingRatioMediumBouncy + StiffnessMediumLow gives a
      // slight physical bounce, consistent with the Expressive motion language from Phase 1–3.
      progress.animateTo(
        targetValue   = 0f,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness    = Spring.StiffnessMediumLow,
        ),
      )
    }
  }

  // Post-unlock visibility and payoff animations
  var isVisible by remember { mutableStateOf(true) }
  val auraFlashAlpha = remember { Animatable(0f) }
  val successPopScale = remember { Animatable(1f) }

  LaunchedEffect(isUnlocked) {
    if (isUnlocked) {
      scope.launch {
        // Aura flash payoff — unchanged
        auraFlashAlpha.animateTo(1f, tween(100))
        auraFlashAlpha.animateTo(0f, tween(800))
      }
      scope.launch {
        // Icon scale pop — unchanged (good spring feel, keep it)
        successPopScale.animateTo(
          targetValue      = 1f,
          initialVelocity  = 15f,
          animationSpec    = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow,
          ),
        )
      }
      delay(1000)
      isVisible = false
    }
  }

  // playerControlsExitAnimationSpec() (350ms) replaces tween(800) —
  // consistent with all other controls leaving the screen in the player.
  val componentAlpha by animateFloatAsState(
    targetValue   = if (isVisible) 1f else 0f,
    animationSpec = if (isVisible) playerControlsEnterAnimationSpec() else playerControlsExitAnimationSpec(),
    label         = "ComponentAlpha",
  )

  // Press feedback scale — unchanged logic, aligned label
  val buttonScale by animateFloatAsState(
    targetValue   = if (isUnlocked) 1f else if (isPressed) 0.92f else 1f,
    animationSpec = spring(stiffness = Spring.StiffnessMedium),
    label         = "ButtonScale",
  )

  val density          = LocalDensity.current
  val arcStrokeWidth   = with(density) { 3.dp.toPx() }
  val borderStrokeWidth = with(density) { 0.5.dp.toPx() }

  Box(
    modifier = modifier
      .size(96.dp)
      .graphicsLayer {
        alpha  = componentAlpha
        scaleX = buttonScale
        scaleY = buttonScale
      }
      .drawBehind {
        val circleRadius = 36.dp.toPx()
        val visualCenter = Offset(size.width - circleRadius, size.height - circleRadius)

        // 1. Atmospheric Aura Glow — unchanged
        val baseAuraAlpha = if (isUnlocked) auraFlashAlpha.value else (progress.value * 0.12f)
        if (baseAuraAlpha > 0f) {
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                primaryColor.copy(alpha = baseAuraAlpha),
                primaryColor.copy(alpha = baseAuraAlpha * 0.2f),
                Color.Transparent,
              ),
              center = Offset(size.width, size.height),
              radius = circleRadius * 2.5f,
            ),
            radius = circleRadius * 2.5f,
            center = visualCenter,
          )
        }

        // 2. Glass Background Circle
        drawCircle(
          color  = glassColor,
          radius = circleRadius,
          center = visualCenter,
        )

        // 3. Subtle Glass Border — now uses outlineVariant token
        drawCircle(
          color  = glassBorderColor,
          radius = circleRadius,
          center = visualCenter,
          style  = Stroke(width = borderStrokeWidth),
        )

        // 4a. Progress Arc Track — faint full-circle baseline behind the progress arc.
        // Consistent with LinearProgressIndicator having a trackColor in M3.
        // Only drawn while progress is active and not yet unlocked.
        if (progress.value > 0f && !isUnlocked) {
          drawCircle(
            color  = primaryColor.copy(alpha = 0.15f),
            radius = circleRadius,
            center = visualCenter,
            style  = Stroke(width = arcStrokeWidth),
          )
        }

        // 4b. Progress Arc — sweeps counter-clockwise (unchanged direction/logic)
        if (progress.value > 0f && !isUnlocked) {
          drawArc(
            color      = primaryColor,
            startAngle = 0f,
            sweepAngle = progress.value * -360f,
            useCenter  = false,
            topLeft    = Offset(visualCenter.x - circleRadius, visualCenter.y - circleRadius),
            size       = Size(circleRadius * 2, circleRadius * 2),
            style      = Stroke(width = arcStrokeWidth),
          )
        }
      }
      .pointerInput(isUnlocked) {
        if (!isUnlocked) {
          detectTapGestures(
            onPress = {
              isPressed = true
              try { awaitRelease() }
              finally { isPressed = false }
            }
          )
        }
      },
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier         = Modifier.offset(x = 12.dp, y = 12.dp),
      contentAlignment = Alignment.Center,
    ) {
      AnimatedContent(
        targetState = isUnlocked,
        transitionSpec = {
          // playerControlsEnterAnimationSpec / playerControlsExitAnimationSpec replace
          // the bespoke tween(200, delayMillis = 50) / tween(100) values —
          // lock icon transition timing now matches all other player controls.
          // scaleIn spring kept — it provides the good entrance pop feel.
          (fadeIn(animationSpec = playerControlsEnterAnimationSpec()) +
            scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
            .togetherWith(fadeOut(animationSpec = playerControlsExitAnimationSpec()))
        },
        label = "LockIconTransition",
      ) { unlocked ->
        Icon(
          imageVector        = if (unlocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
          contentDescription = null,
          tint               = if (unlocked) unlockedIconTint else lockedIconTint,
          modifier           = Modifier
            .size(28.dp)
            .graphicsLayer {
              scaleX = successPopScale.value
              scaleY = successPopScale.value
            },
        )
      }
    }
  }
}
