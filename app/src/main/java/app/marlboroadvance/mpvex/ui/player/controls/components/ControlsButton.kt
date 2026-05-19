package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
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
    
    // M3 Expressive Spring Animations
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 12.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_shape"
    )

    val appearancePreferences = koinInject<AppearancePreferences>()
    val hideBackground by appearancePreferences.hidePlayerButtonsBackground.collectAsState()

    val clickEvent = LocalPlayerButtonsClickEvent.current

    val containerColor = when {
        hideBackground || !enabled -> Color.Transparent
        type == ControlsButtonType.Filled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
        type == ControlsButtonType.Tonal -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
        else -> Color.Transparent
    }

    val baseContentColor = color ?: when {
        type == ControlsButtonType.Filled -> MaterialTheme.colorScheme.onPrimary
        type == ControlsButtonType.Tonal -> MaterialTheme.colorScheme.onSurface
        else -> controlColor
    }
    
    val contentColor = if (enabled) baseContentColor else baseContentColor.copy(alpha = 0.38f)

    // Modern glass edge border for M3 Expressive
    val showBorder = !hideBackground && type != ControlsButtonType.Transparent && enabled
    val borderColor = Color.White.copy(alpha = 0.15f)

    Surface(
        modifier = modifier
            .size(48.dp) // Minimum 48dp touch target
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                // Move morphing to draw phase to prevent recomposition
                this.shape = RoundedCornerShape(cornerRadius)
                clip = true
            }
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
        shape = RoundedCornerShape(24.dp), // Static shape for shadow/surface identity
        color = Color.Transparent, // Draw background manually to avoid recomposition
        contentColor = contentColor,
        tonalElevation = 2.dp,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .drawBehind {
                    val pxRadius = cornerRadius.toPx()
                    // 1. Draw Expressive Background
                    drawRoundRect(
                        color = containerColor,
                        cornerRadius = CornerRadius(pxRadius)
                    )
                    // 2. Draw Glass Edge Border
                    if (showBorder) {
                        drawRoundRect(
                            color = borderColor,
                            style = Stroke(width = 1.dp.toPx()),
                            cornerRadius = CornerRadius(pxRadius)
                        )
                    }
                }
                .padding(12.dp)
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
            .spacedBy(spacing.medium), // Increased spacing for Expressive UI
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
