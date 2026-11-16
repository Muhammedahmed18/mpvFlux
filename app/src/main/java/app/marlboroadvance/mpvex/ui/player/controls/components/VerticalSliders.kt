package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.ui.res.vectorResource
import app.marlboroadvance.mpvex.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
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

// ---------------------------------------------------------------------------
// M3 Expressive floating tonal capsule, shared by the volume and brightness
// gesture sliders:
//
//   [ value pill ]   <- current value, stable above the capsule, never under the fill
//   (  capsule    )  <- solid surface-container track; primary fill grows up from
//   (   fill      )     the bottom, volume boost overlays in tertiary
//   (   icon      )  <- 24dp Material Symbols icon pinned near the capsule's bottom
//
// The capsule is a solid tonal surface (no glass, borders, gradients or shadows) so
// it stays readable over bright and dark video in both themes. The value itself
// updates instantly; only the fill glides.
// ---------------------------------------------------------------------------

private val SliderCapsuleWidth = 56.dp
private val SliderCapsuleHeight = 200.dp
private val SliderInnerPadding = 4.dp
private val SliderIconSize = 24.dp
private val SliderIconBottomPadding = 14.dp

/** Fraction of the inner track the icon zone occupies; past it the fill sits behind the icon. */
private const val IconCoveredThreshold = 0.16f

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
  val capsuleColor = MaterialTheme.colorScheme.surfaceContainerHigh

  // Fill height glides; the gesture's value (and the label) update immediately.
  val fillFraction by animateFloatAsState(
    targetValue = currentPercentage,
    animationSpec = sliderSpring,
    label = "vsliderheight",
  )

  val overflowPercentage = if (overflowRange != null && overflowValue != null) {
    percentage(overflowValue, overflowRange)
  } else {
    0f
  }
  val overflowFraction by animateFloatAsState(
    targetValue = overflowPercentage,
    animationSpec = sliderSpring,
    label = "vslideroverflowheight",
  )

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Value indicator: a small tonal pill at a stable position above the capsule, so
    // it never collides with the moving fill and stays readable over the video.
    if (label != null) {
      Box(
        modifier = Modifier
          .padding(bottom = 8.dp)
          .clip(CircleShape)
          .background(capsuleColor)
          .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum",
          ),
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
          maxLines = 1,
        )
      }
    }

    Box(
      modifier = Modifier
        .width(SliderCapsuleWidth)
        .height(SliderCapsuleHeight)
        .clip(CircleShape)
        .background(capsuleColor),
      contentAlignment = Alignment.BottomCenter,
    ) {
      // Inner track, inset so the fill reads as floating inside the capsule.
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(SliderInnerPadding)
          .clip(CircleShape),
        contentAlignment = Alignment.BottomCenter,
      ) {
        Box(
          Modifier
            .fillMaxWidth()
            .fillMaxHeight(fillFraction)
            .background(MaterialTheme.colorScheme.primary),
        )

        // Volume boost above 100%: a tertiary overlay growing from the bottom, so the
        // boosted amount is clearly told apart from the (full) primary fill under it.
        if (overflowRange != null && overflowValue != null) {
          Box(
            Modifier
              .fillMaxWidth()
              .fillMaxHeight(overflowFraction)
              .background(MaterialTheme.colorScheme.tertiary),
          )
        }
      }

      if (icon != null) {
        // The icon sits near the capsule's bottom; its tint follows whatever is behind
        // it (track, primary fill or tertiary boost) so it stays readable at any level.
        val iconTint by animateColorAsState(
          targetValue = when {
            isBoost && overflowFraction > IconCoveredThreshold -> MaterialTheme.colorScheme.onTertiary
            fillFraction > IconCoveredThreshold -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          },
          animationSpec = tween(150),
          label = "vslidericontint",
        )
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
              .padding(bottom = SliderIconBottomPadding)
              .size(SliderIconSize)
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
    label = "${(percentage * 100).toInt()}%",
    icon = when (percentage) {
      in 0f..0.3f -> ImageVector.vectorResource(R.drawable.ic_sym_brightness_low_fill)
      in 0.3f..0.6f -> ImageVector.vectorResource(R.drawable.ic_sym_brightness_medium_fill)
      in 0.6f..1f -> ImageVector.vectorResource(R.drawable.ic_sym_brightness_high_fill)
      else -> ImageVector.vectorResource(R.drawable.ic_sym_brightness_medium_fill)
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
      mpvVolume == 0 -> ImageVector.vectorResource(R.drawable.ic_sym_volume_off_fill)
      mpvVolume in 1..30 -> ImageVector.vectorResource(R.drawable.ic_sym_volume_mute_fill)
      mpvVolume in 31..60 -> ImageVector.vectorResource(R.drawable.ic_sym_volume_down_fill)
      else -> ImageVector.vectorResource(R.drawable.ic_sym_volume_up_fill)
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
        if (displayAsPercentage) "${percentage + boostVolume}%" else "${volume}+${boostVolume}"
      }
      else -> {
        if (displayAsPercentage) "$percentage%" else "$volume"
      }
    }
  }
