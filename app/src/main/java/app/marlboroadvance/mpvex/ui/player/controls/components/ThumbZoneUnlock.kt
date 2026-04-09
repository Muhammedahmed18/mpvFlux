package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * A modern "Liquid Hold" unlock button.
 * Requires a sustained press to unlock, preventing accidental triggers.
 * Uses a squircle shape and glassmorphism for a premium feel.
 */
@Composable
fun ThumbZoneUnlock(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    // Logic to handle the hold-to-unlock progress
    LaunchedEffect(isPressed) {
        if (isPressed) {
            // Animate progress to 1 over 800ms
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(800, easing = LinearEasing)
            )
            // If it reached the end, trigger unlock and reset
            if (progress.value == 1f) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onUnlock()
                progress.snapTo(0f)
            }
        } else {
            // Rapidly reset progress if released early
            progress.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val squircleShape = RoundedCornerShape(35) // Modern squircle geometry

    Box(
        modifier = modifier
            .size(52.dp)
            .pointerInput(Unit) {
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
            .clip(squircleShape)
            .background(Color.White.copy(alpha = 0.08f)) // Glass base
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.15f),
                shape = squircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // The "Liquid" fill layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    // Expand and fade in as progress increases
                    val scale = 0.5f + (progress.value * 0.5f)
                    scaleX = scale
                    scaleY = scale
                    alpha = progress.value
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.7f),
                            primaryColor.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Subtle center focal point
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.6f))
                .graphicsLayer {
                    // Slight pulse when pressed
                    val scaleFactor = if (isPressed) 1.2f else 1f
                    scaleX = scaleFactor
                    scaleY = scaleFactor
                }
        )
    }
}
