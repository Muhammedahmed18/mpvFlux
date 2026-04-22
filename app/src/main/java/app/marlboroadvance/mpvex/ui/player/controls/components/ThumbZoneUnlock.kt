package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * A modern "Tonal M3 Circular Progress" unlock button.
 * Requires a sustained press to unlock, preventing accidental triggers.
 * Uses Material 3 tonal palettes and a clear circular progress indicator.
 */
@Composable
fun ThumbZoneUnlock(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    var isUnlocked by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    // Logic to handle the hold-to-unlock progress
    LaunchedEffect(isPressed) {
        if (isPressed && !isUnlocked) {
            // Animate progress to 1 over 800ms
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(800, easing = LinearEasing)
            )
            // If it reached the end, trigger unlock and reset
            if (progress.value == 1f) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isUnlocked = true
                onUnlock()
                // Keep it unlocked for a brief moment before it unmounts or resets
            }
        } else if (!isUnlocked) {
            // Rapidly reset progress if released early
            progress.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
        }
    }

    // Scale animation for the press effect
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "PressScale"
    )

    Box(
        modifier = modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
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
            },
        contentAlignment = Alignment.Center
    ) {
        // Background container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    if (isUnlocked) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer
                ),
            contentAlignment = Alignment.Center
        ) {
            // Icon
            Icon(
                imageVector = if (isUnlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                contentDescription = "Unlock",
                tint = if (isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        // Circular progress indicator that draws around the edge
        if (progress.value > 0f && !isUnlocked) {
            CircularProgressIndicator(
                progress = { progress.value },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                trackColor = Color.Transparent,
            )
        }
    }
}
