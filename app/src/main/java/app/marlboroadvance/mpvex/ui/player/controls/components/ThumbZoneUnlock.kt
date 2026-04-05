package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A modern, pill-shaped button for unlocking player controls with a hold-to-unlock gesture.
 * Designed for the "thumb zone" at the bottom right of the screen.
 * Highly optimized for low CPU/battery impact using hardware-accelerated graphicsLayer and tactile feedback.
 */
@Composable
fun ThumbZoneUnlock(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val haptic = LocalHapticFeedback.current

    // Constants for the "Hold to Unlock" logic
    val holdDuration = 800L // ms

    LaunchedEffect(isPressed) {
        if (isPressed) {
            val startTime = System.currentTimeMillis()
            while (isPressed && progress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                progress = (elapsed.toFloat() / holdDuration).coerceAtMost(1f)
                if (progress >= 1f) {
                    // Distinct "thump" feedback on successful unlock
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUnlock()
                    break
                }
                delay(16) // ~60fps smooth progress update
            }
        } else {
            progress = 0f
        }
    }

    // Physics-based spring animations for a more natural, elastic feel
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "unlock_button_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0.7f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "unlock_button_alpha"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .width(84.dp)
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(24.dp))
            // Glassmorphism effect: semi-transparent surface with a subtle border
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            )
            .drawBehind {
                if (progress > 0f) {
                    // Radial expansion from the center that grows as you hold
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.35f),
                        radius = size.maxDimension * progress,
                        center = center
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        // Subtle tactile "click" when the user first touches the button
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Lock icon that subtly grows as progress increases to provide visual momentum
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Unlock controls",
            tint = Color.White,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    val iconScale = 1f + (progress * 0.2f)
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
    }
}
