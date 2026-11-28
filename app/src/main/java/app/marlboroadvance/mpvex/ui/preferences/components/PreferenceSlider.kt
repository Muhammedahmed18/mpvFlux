package app.marlboroadvance.mpvex.ui.preferences.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

    fun snap(raw: Float): Int =
        (min + ((raw - minF) / step).roundToInt() * step).coerceIn(min, max)

    var dragging by remember { mutableStateOf(false) }
    var draft by remember { mutableFloatStateOf(value.toFloat()) }

    // While dragging the thumb follows the draft; otherwise it follows the saved value,
    // so external changes sync automatically.
    val sliderValue = if (dragging) draft else value.toFloat().coerceIn(minF, maxF)
    val shown = if (dragging) snap(draft) else value

    ListItem(
        headlineContent = { PreferenceTitle(title) },
        supportingContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = valueLabel(shown))
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        dragging = true
                        draft = it
                    },
                    onValueChangeFinished = {
                        val snapped = snap(draft)
                        dragging = false
                        onValueChangeFinished(snapped)
                    },
                    valueRange = minF..maxF,
                    steps = steps,
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
