package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import kotlin.math.roundToInt

fun percentage(
  value: Float,
  range: ClosedFloatingPointRange<Float>,
): Float = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

fun percentage(
  value: Int,
  range: ClosedRange<Int>,
): Float = ((value - range.start - 0f) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

@Composable
fun VerticalSlider(
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  label: String? = null,
  overflowValue: Float? = null,
  overflowRange: ClosedFloatingPointRange<Float>? = null,
  isBoost: Boolean = false,
) {
  val coercedValue = value.coerceIn(range)
  val currentPercentage = percentage(coercedValue, range)
  val sliderShape = MaterialTheme.shapes.extraLarge // Modern M3 extraLarge shape

  Box(
    modifier =
      modifier
        .width(48.dp) // Slightly narrower for a more elegant look
        .height(200.dp) // Taller for better precision
        .clip(sliderShape)
        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)),
    contentAlignment = Alignment.BottomCenter,
  ) {
    // Active Fill
    val targetHeight by animateFloatAsState(currentPercentage, label = "vsliderheight")
    
    val activeBrush = if (isBoost) {
      Brush.verticalGradient(
        colors = listOf(
          MaterialTheme.colorScheme.tertiary,
          MaterialTheme.colorScheme.primary,
        )
      )
    } else {
      Brush.verticalGradient(
        colors = listOf(
          MaterialTheme.colorScheme.primary,
          MaterialTheme.colorScheme.primary,
        )
      )
    }

    Box(
      Modifier
        .fillMaxWidth()
        .fillMaxHeight(targetHeight)
        .background(brush = activeBrush),
    )

    // Overflow Fill (Volume Boost)
    if (overflowRange != null && overflowValue != null) {
      val overflowPercentage = percentage(overflowValue, overflowRange)
      val overflowHeight by animateFloatAsState(
        overflowPercentage,
        label = "vslideroverflowheight",
      )
      Box(
        Modifier
          .fillMaxWidth()
          .fillMaxHeight(overflowHeight)
          .background(
            brush = Brush.verticalGradient(
              colors = listOf(
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                MaterialTheme.colorScheme.primaryContainer,
              )
            )
          ),
      )
    }

    // Integrated elements (Icon at bottom, Text at top)
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(vertical = 20.dp), // Increased padding
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Text at top
      if (label != null) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = if (currentPercentage > 0.85f) {
            MaterialTheme.colorScheme.onPrimary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
          textAlign = TextAlign.Center,
        )
      } else {
        Spacer(modifier = Modifier.height(1.dp))
      }

      // Icon at bottom
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (currentPercentage > 0.15f) {
            MaterialTheme.colorScheme.onPrimary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
          modifier = Modifier.size(24.dp)
        )
      }
    }
  }
}

@Composable
fun VerticalSlider(
  value: Int,
  range: ClosedRange<Int>,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  label: String? = null,
  overflowValue: Int? = null,
  overflowRange: ClosedRange<Int>? = null,
  isBoost: Boolean = false,
) {
  VerticalSlider(
    value = value.toFloat(),
    range = range.start.toFloat()..range.endInclusive.toFloat(),
    modifier = modifier,
    icon = icon,
    label = label,
    overflowValue = overflowValue?.toFloat(),
    overflowRange = overflowRange?.let { it.start.toFloat()..it.endInclusive.toFloat() },
    isBoost = isBoost
  )
}

@Composable
fun BrightnessSlider(
  brightness: Float,
  range: ClosedFloatingPointRange<Float>,
  modifier: Modifier = Modifier,
) {
  val coercedBrightness = brightness.coerceIn(range)
  val percentage = percentage(coercedBrightness, range)

  VerticalSlider(
    value = coercedBrightness,
    range = range,
    modifier = modifier,
    label = (percentage * 100).toInt().toString(),
    icon = when (percentage) {
      in 0f..0.3f -> Icons.Rounded.BrightnessLow
      in 0.3f..0.6f -> Icons.Rounded.BrightnessMedium
      in 0.6f..1f -> Icons.Rounded.BrightnessHigh
      else -> Icons.Rounded.BrightnessMedium
    }
  )
}

@Composable
fun VolumeSlider(
  volume: Int,
  mpvVolume: Int,
  range: ClosedRange<Int>,
  boostRange: ClosedRange<Int>?,
  modifier: Modifier = Modifier,
  displayAsPercentage: Boolean = false,
) {
  val percentageValue = (percentage(volume, range) * 100).roundToInt()
  val boostVolume = (mpvVolume - 100).coerceAtLeast(0)
  
  val label = getVolumeSliderText(volume, mpvVolume, boostVolume, percentageValue, displayAsPercentage)
  
  VerticalSlider(
    value = if (displayAsPercentage) percentageValue.toFloat() else volume.toFloat(),
    range = if (displayAsPercentage) 0f..100f else range.start.toFloat()..range.endInclusive.toFloat(),
    modifier = modifier,
    label = label,
    icon = when {
      mpvVolume == 0 -> Icons.Rounded.VolumeOff
      mpvVolume in 1..30 -> Icons.Rounded.VolumeMute
      mpvVolume in 31..60 -> Icons.Rounded.VolumeDown
      else -> Icons.Rounded.VolumeUp
    },
    overflowValue = if (mpvVolume > 100) boostVolume.toFloat() else null,
    overflowRange = boostRange?.let { it.start.toFloat()..it.endInclusive.toFloat() },
    isBoost = mpvVolume > 100
  )
}

val getVolumeSliderText: @Composable (Int, Int, Int, Int, Boolean) -> String =
  { volume, mpvVolume, boostVolume, percentage, displayAsPercentage ->
    when {
      mpvVolume == 100 ->
        if (displayAsPercentage) {
          "$percentage"
        } else {
          "$volume"
        }

      mpvVolume > 100 -> {
        if (displayAsPercentage) {
          "${percentage + boostVolume}"
        } else {
          stringResource(R.string.volume_slider_absolute_value, volume + boostVolume)
        }
      }

      mpvVolume < 100 -> {
        if (displayAsPercentage) {
          "${percentage + boostVolume}"
        } else {
          stringResource(R.string.volume_slider_absolute_value, volume + boostVolume)
        }
      }

      else -> {
        if (displayAsPercentage) {
          "$percentage"
        } else {
          "$volume"
        }
      }
    }
  }
