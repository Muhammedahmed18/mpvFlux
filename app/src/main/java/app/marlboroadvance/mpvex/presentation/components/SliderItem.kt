package app.marlboroadvance.mpvex.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.ui.theme.spacing
import kotlin.math.abs

@Composable
fun SliderItem(
  label: String,
  value: Int,
  valueText: String,
  onChange: (Int) -> Unit,
  max: Int,
  modifier: Modifier = Modifier,
  min: Int = 0,
  icon: @Composable () -> Unit = {},
) {
  val haptic = LocalHapticFeedback.current

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(
          horizontal = MaterialTheme.spacing.medium,
          vertical = MaterialTheme.spacing.smaller,
        ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
  ) {
    icon()
    Column(modifier = Modifier.weight(0.5f)) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(valueText)
    }

    Slider(
      value = value.toFloat(),
      onValueChange = {
        val newValue = it.toInt()
        if (newValue != value) {
          onChange(newValue)
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
      },
      modifier = Modifier.weight(1.5f),
      valueRange = min.toFloat()..max.toFloat(),
      steps = max - min,
    )
  }
}

@Composable
fun SliderItem(
  label: String,
  value: Float,
  valueText: String,
  onChange: (Float) -> Unit,
  max: Float,
  modifier: Modifier = Modifier,
  steps: Int = 0,
  min: Float = 0f,
  icon: @Composable () -> Unit = {},
) {
  val haptic = LocalHapticFeedback.current
  // Non-state mutable holder: writes don't trigger recomposition.
  // Tracks the last value at which haptic fired so we can throttle to one pulse per tick.
  val lastHapticValue = remember { floatArrayOf(value) }

  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(
          horizontal = MaterialTheme.spacing.medium,
          vertical = MaterialTheme.spacing.smaller,
        ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
  ) {
    icon()
    Column(modifier = Modifier.weight(0.5f)) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(valueText)
    }

    Slider(
      value = value,
      onValueChange = {
        if (it != value) {
          onChange(it)
          // For stepped sliders fire at each step crossing; for continuous sliders
          // fire at most once per 1% of range to avoid ~60 Hz haptic pulses.
          val range = max - min
          val threshold = if (steps > 0) range / (steps + 1) else range * 0.01f
          if (abs(it - lastHapticValue[0]) >= threshold) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastHapticValue[0] = it
          }
        }
      },
      modifier = Modifier.weight(1.5f),
      valueRange = min..max,
      steps = steps,
    )
  }
}

@Composable
fun VerticalSliderItem(
  label: String,
  value: Int,
  valueText: String,
  onChange: (Int) -> Unit,
  max: Int,
  modifier: Modifier = Modifier,
  min: Int = 0,
  icon: @Composable () -> Unit = {},
) {
  val haptic = LocalHapticFeedback.current
  // Tracks the last int value that fired haptic; updated immediately on each tick
  // so repeated callbacks with the same int (while float hovers near a boundary)
  // don't produce extra pulses before the parent state catches up.
  val lastHapticInt = remember { intArrayOf(value) }

  Column(
    modifier =
      modifier
        .fillMaxHeight()
        .padding(
          horizontal = MaterialTheme.spacing.medium,
          vertical = MaterialTheme.spacing.smaller,
        ),
    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.larger),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    icon()
    VerticalSlider(
      value = value,
      min = min,
      max = max,
      onValueChange = { newValue ->
        if (newValue != lastHapticInt[0]) {
          lastHapticInt[0] = newValue
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onChange(newValue)
        }
      },
      modifier = Modifier.weight(1f),
    )
    Column {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(valueText)
    }
  }
}

@Composable
fun VerticalSlider(
  value: Int,
  min: Int,
  max: Int,
  onValueChange: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Slider(
    modifier =
      modifier
        .graphicsLayer {
          rotationZ = 270f
          transformOrigin = TransformOrigin(0f, 0f)
        }.layout { measurable, constraints ->
          val placeable =
            measurable.measure(
              Constraints(
                minWidth = constraints.minHeight,
                maxWidth = constraints.maxHeight,
                minHeight = constraints.minWidth,
                maxHeight = constraints.maxWidth,
              ),
            )
          layout(placeable.height, placeable.width) {
            placeable.place(-placeable.width, 0)
          }
        }.width(180.dp)
        .height(50.dp),
    value = value.toFloat(),
    valueRange = min.toFloat()..max.toFloat(),
    onValueChange = { onValueChange(it.toInt()) },
  )
}

@Preview
@Composable
private fun PreviewVerticalSliderItem() {
  VerticalSliderItem(
    "sex",
    1,
    "2",
    {},
    5,
  )
}
