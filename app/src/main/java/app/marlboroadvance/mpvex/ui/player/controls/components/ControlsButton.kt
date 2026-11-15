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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.player.controls.LocalPlayerButtonsClickEvent
import app.marlboroadvance.mpvex.ui.player.controls.PlayerChrome
import app.marlboroadvance.mpvex.ui.player.controls.playerChromeContentColor
import app.marlboroadvance.mpvex.ui.player.controls.playerGlassFill
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
    // Pass the "hide button background" setting when the caller already has it, so each
    // button doesn't read the preference itself. Null = read it here (old behaviour).
    hideBackgroundOverride: Boolean? = null,
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

    val hideBackground = hideBackgroundOverride ?: run {
        val appearancePreferences = koinInject<AppearancePreferences>()
        appearancePreferences.hidePlayerButtonsBackground.collectAsState().value
    }

    val clickEvent = LocalPlayerButtonsClickEvent.current

    val containerColor = when {
        hideBackground || !enabled -> Color.Transparent
        type == ControlsButtonType.Filled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
        type == ControlsButtonType.Tonal -> playerGlassFill()
        else -> Color.Transparent
    }

    val baseContentColor = color ?: when {
        type == ControlsButtonType.Filled -> MaterialTheme.colorScheme.onPrimary
        // No fill when backgrounds are hidden: keep the glyph light over the video.
        hideBackground || type == ControlsButtonType.Tonal -> playerChromeContentColor(hideBackground)
        else -> controlColor
    }

    val contentColor = if (enabled) baseContentColor else baseContentColor.copy(alpha = PlayerChrome.DisabledAlpha)

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
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
            ),
        shape = RoundedCornerShape(24.dp), // Static shape for shadow/surface identity
        color = Color.Transparent, // Draw background manually to avoid recomposition
        contentColor = contentColor,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .drawBehind {
                    val pxRadius = cornerRadius.toPx()
                    // Standard player glass (PlayerChromeStyle): fill only, no rim.
                    drawRoundRect(
                        color = containerColor,
                        cornerRadius = CornerRadius(pxRadius)
                    )
                }
                // Fill the 48dp button and centre the icon, so iconSize is honoured
                // (a fixed 12dp padding used to cap the icon at 24dp).
                .fillMaxSize()
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
