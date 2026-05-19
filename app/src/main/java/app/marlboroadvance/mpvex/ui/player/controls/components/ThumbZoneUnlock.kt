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
import app.marlboroadvance.mpvex.ui.theme.controlColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A redesigned "Thumb Zone" unlock button for one-handed operation.
 * Features glass-morphism, a circular progress arc, and an atmospheric "light bleed" aura.
 * Visually anchored to the corner while maintaining a generous hit area.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ThumbZoneUnlock(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    var isPressed by remember { mutableStateOf(false) }
    var isUnlocked by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val glassColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
    val glassBorderColor = Color.White.copy(alpha = 0.15f)

    // Handle the hold-to-unlock progress logic
    LaunchedEffect(isPressed) {
        if (isPressed && !isUnlocked) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(800, easing = LinearEasing)
            )
            if (progress.value == 1f) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isUnlocked = true
                onUnlock()
            }
        } else if (!isUnlocked) {
            // Spring back to 0 on early release
            progress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
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
                // Aura flash payoff
                auraFlashAlpha.animateTo(1f, tween(100))
                auraFlashAlpha.animateTo(0f, tween(800))
            }
            scope.launch {
                // Icon scale pop (1.0 -> 1.3 -> 1.0)
                successPopScale.animateTo(
                    targetValue = 1f,
                    initialVelocity = 15f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            // Fade out the entire button after 1 second
            delay(1000)
            isVisible = false
        }
    }

    val componentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(800),
        label = "ComponentAlpha"
    )

    // Press feedback scale
    val buttonScale by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else if (isPressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "ButtonScale"
    )

    val density = LocalDensity.current
    val arcStrokeWidth = with(density) { 3.dp.toPx() }
    val borderStrokeWidth = with(density) { 0.5.dp.toPx() }

    Box(
        modifier = modifier
            .size(96.dp) // Generous hit area for thumb zone
            .graphicsLayer {
                alpha = componentAlpha
                scaleX = buttonScale
                scaleY = buttonScale
            }
            .drawBehind {
                val circleRadius = 36.dp.toPx() // Visible container radius (72dp diameter)
                
                // Anchor the visual elements to the bottom-right corner of the hit area
                // This makes the button flush with the screen edges if the parent is aligned to BottomEnd.
                val visualCenter = Offset(size.width - circleRadius, size.height - circleRadius)

                // 1. Atmospheric Aura Glow
                // Radiates inward from the absolute corner for a light bleed effect
                val baseAuraAlpha = if (isUnlocked) auraFlashAlpha.value else (progress.value * 0.12f)
                if (baseAuraAlpha > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = baseAuraAlpha),
                                primaryColor.copy(alpha = baseAuraAlpha * 0.2f),
                                Color.Transparent
                            ),
                            center = Offset(size.width, size.height), // Edge of the device
                            radius = circleRadius * 2.5f
                        ),
                        radius = circleRadius * 2.5f,
                        center = visualCenter
                    )
                }

                // 2. Glass Background Circle
                drawCircle(
                    color = glassColor,
                    radius = circleRadius,
                    center = visualCenter
                )

                // 3. Subtle Glass Border
                drawCircle(
                    color = glassBorderColor,
                    radius = circleRadius,
                    center = visualCenter,
                    style = Stroke(width = borderStrokeWidth)
                )

                // 4. Progress Arc (Primary Hold Feedback)
                // Sweeps counter-clockwise away from the corner towards screen center
                if (progress.value > 0f && !isUnlocked) {
                    drawArc(
                        color = primaryColor,
                        startAngle = 0f,
                        sweepAngle = progress.value * -360f,
                        useCenter = false,
                        topLeft = Offset(visualCenter.x - circleRadius, visualCenter.y - circleRadius),
                        size = Size(circleRadius * 2, circleRadius * 2),
                        style = Stroke(width = arcStrokeWidth)
                    )
                }
            }
            .pointerInput(isUnlocked) {
                if (!isUnlocked) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            try {
                                awaitRelease()
                            } finally {
                                isPressed = false
                            }
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Offset the icon container to match the shifted visual center
        // (96dp box center is 48, 48. Visual center is 60, 60. Offset is +12, +12)
        Box(
            modifier = Modifier.offset(x = 12.dp, y = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isUnlocked,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(200, delayMillis = 50)) + 
                     scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                        .togetherWith(fadeOut(animationSpec = tween(100)))
                },
                label = "LockIconTransition"
            ) { unlocked ->
                Icon(
                    imageVector = if (unlocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = if (unlocked) Color.White else controlColor.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            scaleX = successPopScale.value
                            scaleY = successPopScale.value
                        }
                )
            }
        }
    }
}
