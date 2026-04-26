package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.ui.theme.controlColor

/**
 * A modern "Glassmorphic Squircle" unlock button.
 * Requires a sustained press to unlock, preventing accidental triggers.
 * Matches the updated player UI with translucent surfaces and outlined icons.
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

    val cornerRadius = 18.dp
    val unlockShape = RoundedCornerShape(cornerRadius)
    val glassBorder = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))

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
            }
        } else if (!isUnlocked) {
            // Rapidly reset progress if released early
            progress.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
        }
    }

    // Scale animation synchronized with the Play/Pause Hero button
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
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
        // Main Surface with Glassmorphism
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = unlockShape,
            color = if (isUnlocked) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
            },
            border = glassBorder,
            tonalElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Icon - Switch to Outlined for modern feel
                Icon(
                    imageVector = if (isUnlocked) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                    contentDescription = "Unlock",
                    tint = if (isUnlocked) Color.White else controlColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Custom Squircle Progress Border
        if (progress.value > 0f && !isUnlocked) {
            val primaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.25.dp) // Offset to align with the physical border
            ) {
                val strokeWidthPx = 2.5.dp.toPx()
                val cornerRadiusPx = cornerRadius.toPx()
                
                // Create the squircle path
                val squirclePath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(Offset.Zero, size),
                            cornerRadius = CornerRadius(cornerRadiusPx)
                        )
                    )
                }

                // Measure the path and get the segment
                val pathMeasure = PathMeasure()
                pathMeasure.setPath(squirclePath, false)
                
                val drawPath = Path()
                // PathMeasure starts from the right side by default in Android for addRoundRect.
                // For a natural look, we draw the segment based on progress.
                pathMeasure.getSegment(
                    startDistance = 0f,
                    stopDistance = progress.value * pathMeasure.length,
                    destination = drawPath
                )

                drawPath(
                    path = drawPath,
                    color = primaryColor,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}
