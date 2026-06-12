package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

fun percentage(
  value: Float,
  range: ClosedFloatingPointRange<Float>,
): Float = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

fun percentage(
  value: Int,
  range: ClosedRange<Int>,
): Float = ((value - range.start - 0f) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

private val sliderSpring = spring<Float>(
  stiffness = Spring.StiffnessMediumLow,
  dampingRatio = Spring.DampingRatioNoBouncy,
)

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
  isZero: Boolean = false,
) {
  val coercedValue = value.coerceIn(range)
  val currentPercentage = percentage(coercedValue, range)
  val sliderShape = MaterialTheme.shapes.extraLarge

  Box(
    modifier =
      modifier
        .width(52.dp)
        .height(200.dp)
        .clip(sliderShape)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)),
    contentAlignment = Alignment.BottomCenter,
  ) {
    val targetHeight by animateFloatAsState(
      targetValue = currentPercentage,
      animationSpec = sliderSpring,
      label = "vsliderheight",
    )

    Box(
      Modifier
        .fillMaxWidth()
        .fillMaxHeight(targetHeight)
        .background(
          if (isBoost) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.primary
        ),
    )

    if (overflowRange != null && overflowValue != null) {
      val overflowPercentage = percentage(overflowValue, overflowRange)
      val overflowHeight by animateFloatAsState(
        targetValue = overflowPercentage,
        animationSpec = sliderSpring,
        label = "vslideroverflowheight",
      )
      Box(
        Modifier
          .fillMaxWidth()
          .fillMaxHeight(overflowHeight)
          .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)),
      )
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      if (label != null) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = if (currentPercentage > 0.85f) {
            MaterialTheme.colorScheme.onPrimary
          } else {
            MaterialTheme.colorScheme.onSurface
          },
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
      } else {
        Spacer(modifier = Modifier.height(1.dp))
      }

      if (icon != null) {
        val iconTint = if (currentPercentage > 0.12f) {
          MaterialTheme.colorScheme.onPrimary
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        }
        val crossColor = MaterialTheme.colorScheme.error
        AnimatedContent(
          targetState = icon,
          transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
          label = "iconanim",
        ) { currentIcon ->
          Icon(
            imageVector = currentIcon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
              .size(20.dp)
              .drawWithContent {
                drawContent()
                if (isZero) {
                  drawLine(
                    color = crossColor,
                    start = Offset(size.width * 0.05f, size.height * 0.05f),
                    end = Offset(size.width * 0.95f, size.height * 0.95f),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                  )
                }
              },
          )
        }
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
  isZero: Boolean = false,
) {
  VerticalSlider(
    value = value.toFloat(),
    range = range.start.toFloat()..range.endInclusive.toFloat(),
    modifier = modifier,
    icon = icon,
    label = label,
    overflowValue = overflowValue?.toFloat(),
    overflowRange = overflowRange?.let { it.start.toFloat()..it.endInclusive.toFloat() },
    isBoost = isBoost,
    isZero = isZero,
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
    },
    isZero = percentage == 0f,
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
      mpvVolume == 0 -> Icons.AutoMirrored.Rounded.VolumeOff
      mpvVolume in 1..30 -> Icons.AutoMirrored.Rounded.VolumeMute
      mpvVolume in 31..60 -> Icons.AutoMirrored.Rounded.VolumeDown
      else -> Icons.AutoMirrored.Rounded.VolumeUp
    },
    overflowValue = if (mpvVolume > 100) boostVolume.toFloat() else null,
    overflowRange = boostRange?.let { it.start.toFloat()..it.endInclusive.toFloat() },
    isBoost = mpvVolume > 100,
    isZero = mpvVolume == 0,
  )
}

val getVolumeSliderText: @Composable (Int, Int, Int, Int, Boolean) -> String =
  { volume, mpvVolume, boostVolume, percentage, displayAsPercentage ->
    when {
      mpvVolume > 100 -> {
        if (displayAsPercentage) "${percentage + boostVolume}" else "${volume}+${boostVolume}"
      }
      else -> {
        if (displayAsPercentage) "$percentage" else "$volume"
      }
    }
  }
