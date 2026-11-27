package app.marlboroadvance.mpvex.ui.preferences.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import app.marlboroadvance.mpvex.ui.preferences.PreferenceTitle

/**
 * Material 3 switch that shows a check when on and a cross when off.
 * The thumb, track and colors animate through the stock Switch; the icon cross-fades.
 */
@Composable
fun PreferenceSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = {
            Crossfade(
                targetState = checked,
                animationSpec = tween(durationMillis = 150),
                label = "preference_switch_icon",
            ) { on ->
                Icon(
                    imageVector = if (on) Icons.Rounded.Check else Icons.Rounded.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        },
    )
}

/**
 * Drop-in replacement for me.zhanghai's `SwitchPreference` (same parameters used by the
 * settings screens), rendered with the shared list-item layout and [PreferenceSwitch].
 */
@Composable
fun SwitchPreference(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    summary: (@Composable () -> Unit)? = null,
) {
    // The whole row toggles, and TalkBack reads title + summary + on/off as one switch.
    ListItem(
        headlineContent = { PreferenceTitle(title) },
        supportingContent = summary,
        leadingContent = icon,
        trailingContent = { PreferenceSwitch(checked = value, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
            .alpha(if (enabled) 1f else 0.38f)
            .toggleable(
                value = value,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onValueChange,
            ),
    )
}
