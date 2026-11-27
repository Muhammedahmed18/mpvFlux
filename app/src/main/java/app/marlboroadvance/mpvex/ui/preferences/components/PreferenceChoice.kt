package app.marlboroadvance.mpvex.ui.preferences.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.ui.preferences.PreferenceTitle

/**
 * Single-choice setting shown as one connected group of M3 toggle buttons, for a short list of
 * options (2 to 3). Meant to be the only child of a PreferenceCard row, so the title, the
 * buttons and the optional [footer] (for example a live preview) stay in one tonal segment.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> PreferenceChoice(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    icon: ((T) -> ImageVector)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (title != null) {
            PreferenceTitle(title)
            Spacer(Modifier.height(12.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            options.forEachIndexed { index, option ->
                val checked = option == selected
                ToggleButton(
                    checked = checked,
                    onCheckedChange = { if (it) onSelect(option) },
                    shapes = connectedShapes(index, options.size),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .semantics {
                            role = Role.RadioButton
                            this.selected = checked
                        },
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon(option),
                            contentDescription = null,
                            modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                        )
                        Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                    }
                    Text(text = label(option), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (footer != null) {
            Spacer(Modifier.height(12.dp))
            footer()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun connectedShapes(index: Int, count: Int): ToggleButtonShapes = when {
    count <= 1 -> ToggleButtonDefaults.shapesFor(ButtonDefaults.MinHeight)
    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
    index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
}
