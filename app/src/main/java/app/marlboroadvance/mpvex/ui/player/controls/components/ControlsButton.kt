package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.player.controls.LocalPlayerButtonsClickEvent
import app.marlboroadvance.mpvex.ui.theme.controlColor
import app.marlboroadvance.mpvex.ui.theme.spacing
import org.koin.compose.koinInject

enum class ControlsButtonType {
    Filled,
    Tonal,
    Outlined,
    Transparent
}

@Suppress("ModifierClickableOrder")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ControlsButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    title: String? = null,
    color: Color? = null,
    type: ControlsButtonType = ControlsButtonType.Tonal,
    shape: Shape = CircleShape,
    iconSize: Dp = 24.dp,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )

    val appearancePreferences = koinInject<AppearancePreferences>()
    val hideBackground by appearancePreferences.hidePlayerButtonsBackground.collectAsState()

    val clickEvent = LocalPlayerButtonsClickEvent.current

    // Glassmorphism update: Use dark translucent colors for high contrast visibility on video
    val containerColor = when {
        hideBackground || !enabled -> Color.Transparent
        type == ControlsButtonType.Filled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        type == ControlsButtonType.Tonal -> Color.Black.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    val baseContentColor = color ?: when {
        type == ControlsButtonType.Filled -> MaterialTheme.colorScheme.primary
        else -> controlColor // controlColor is white (0xFFFFFFFF)
    }
    
    val contentColor = if (enabled) baseContentColor else baseContentColor.copy(alpha = 0.38f)

    // Ultra-thin "glass edge" border using white transparency
    val border = when {
        hideBackground || !enabled -> null
        else -> BorderStroke(
            0.5.dp,
            Color.White.copy(alpha = 0.12f)
        )
    }

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .combinedClickable(
                enabled = enabled,
                onClick = {
                    clickEvent()
                    onClick()
                },
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
            ),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = border,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
fun ControlsGroup(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val spacing = MaterialTheme.spacing

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement
            .spacedBy(spacing.extraSmall),
        content = content,
    )
}

@Preview
@Composable
private fun PreviewControlsButton() {
    ControlsButton(
        Icons.Default.PlayArrow,
        onClick = {},
    )
}
