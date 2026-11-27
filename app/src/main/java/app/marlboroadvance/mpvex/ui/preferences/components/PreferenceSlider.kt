package app.marlboroadvance.mpvex.ui.preferences.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import app.marlboroadvance.mpvex.ui.preferences.PreferenceTitle
import kotlin.math.roundToInt

/**
 * Integer slider row in the shared list-item layout, snapped to [step] between [min] and [max].
 *
 * [value] may be off the grid (for example a custom value saved earlier). The thumb then shows
 * the nearest step, but nothing is written until the user moves the slider, and [valueLabel]
 * keeps showing the real saved value until then. [onValueChangeFinished] gets the snapped value
 * when a drag ends. [trailing] is an optional action next to the title.
 */
@Composable
fun PreferenceSlider(
    value: Int,
    onValueChangeFinished: (Int) -> Unit,
    min: Int,
    max: Int,
    step: Int,
    title: @Composable () -> Unit,
    valueLabel: @Composable (Int) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val minF = min.toFloat()
    val maxF = max.toFloat()
    val steps = ((max - min) / step - 1).coerceAtLeast(0)

    fun snap(raw: Float): Int = min + ((raw - minF) / step).roundToInt() * step

    var dragging by remember { mutableStateOf(false) }
    var draft by remember { mutableFloatStateOf(value.toFloat()) }

    // Drags update the state themselves; saved changes are pushed in when not dragging.
    val sliderState = remember(min, max, step) {
        SliderState(value = value.toFloat().coerceIn(minF, maxF), steps = steps, trackRange = minF..maxF)
    }
    SideEffect {
        val target = value.toFloat().coerceIn(minF, maxF)
        if (!dragging && sliderState.value != target) sliderState.value = target
    }

    val shown = if (dragging) snap(draft) else value

    ListItem(
        headlineContent = { PreferenceTitle(title) },
        supportingContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = valueLabel(shown))
                Slider(
                    state = sliderState,
                    onValueChange = {
                        dragging = true
                        draft = it
                    },
                    onValueChangeFinished = {
                        val snapped = snap(sliderState.value).coerceIn(min, max)
                        dragging = false
                        onValueChangeFinished(snapped)
                    },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier.alpha(if (enabled) 1f else 0.38f),
    )
}
