package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * A minimal, circular button for unlocking player controls with a single tap.
 * Designed for the "thumb zone" at the bottom right of the screen.
 * Highly optimized for low CPU/battery impact during playback.
 */
@Composable
fun ThumbZoneUnlock(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }
    
    // Scale and glow feedback - hardware accelerated via graphicsLayer
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "unlock_button_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 0.6f,
        animationSpec = tween(durationMillis = 150),
        label = "unlock_button_alpha"
    )

    // Static gradient brush - rendered once
    val primaryColor = MaterialTheme.colorScheme.primary
    val gradientBrush = remember(primaryColor) {
        Brush.linearGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.5f),
                primaryColor.copy(alpha = 0.3f)
            )
        )
    }

    Box(
        modifier = modifier
            .size(56.dp) // Standard touch target size
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(gradientBrush)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = {
                        onUnlock()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Minimal visual indicator - a small inner dot to suggest interaction
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.4f))
        )
    }
}
