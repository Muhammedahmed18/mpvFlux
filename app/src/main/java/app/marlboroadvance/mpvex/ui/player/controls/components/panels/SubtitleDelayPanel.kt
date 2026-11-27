package app.marlboroadvance.mpvex.ui.player.controls.components.panels

import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import app.marlboroadvance.mpvex.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import app.marlboroadvance.mpvex.ui.player.controls.components.panels.components.PanelSection
import app.marlboroadvance.mpvex.preferences.SubtitlesPreferences
import app.marlboroadvance.mpvex.presentation.components.OutlinedNumericChooser
import app.marlboroadvance.mpvex.ui.player.controls.CARDS_MAX_WIDTH
import app.marlboroadvance.mpvex.ui.player.controls.panelCardsColors
import app.marlboroadvance.mpvex.ui.theme.spacing
import `is`.xyz.mpv.MPVLib
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
fun SubtitleDelayPanel(
  onDismissRequest: () -> Unit,
  subDelay: StateFlow<Float>,
  subSpeed: StateFlow<Float>,
  modifier: Modifier = Modifier,
  onBack: (() -> Unit)? = null,
) {
  val preferences = koinInject<SubtitlesPreferences>()

  OverlayPanelContent(
    modifier = modifier,
    title = stringResource(R.string.player_sheets_sub_delay_card_title),
    onClose = onDismissRequest,
    onBack = onBack,
  ) {
    val delayFloat by subDelay.collectAsState()
    val speedFloat by subSpeed.collectAsState()

    SubtitleDelayCardContent(
      delay = delayFloat,
      onDelayChange = { MPVLib.setPropertyDouble("sub-delay", it.toDouble()) },
      speed = speedFloat,
      onSpeedChange = { MPVLib.setPropertyDouble("sub-speed", it.toDouble()) },
      onApply = {
        preferences.defaultSubDelay.set((delayFloat * 1000).roundToInt())
        if (speedFloat in 0.1f..10f) preferences.defaultSubSpeed.set(speedFloat)
      },
      onReset = {
        MPVLib.setPropertyDouble("sub-delay", preferences.defaultSubDelay.get() / 1000.0)
        MPVLib.setPropertyDouble("sub-speed", preferences.defaultSubSpeed.get().toDouble())
      },
    )
  }
}

// Extracted content to avoid nested cards since the overlay host already provides the surface
@Composable
private fun SubtitleDelayCardContent(
  delay: Float,
  onDelayChange: (Float) -> Unit,
  speed: Float,
  onSpeedChange: (Float) -> Unit,
  onApply: () -> Unit,
  onReset: () -> Unit,
) {
    DelayCardContent(
      delay = delay,
      onDelayChange = onDelayChange,
      onApply = onApply,
      onReset = onReset,
      delayType = DelayType.Subtitle,
      extraSettings = {
        OutlinedNumericChooser(
          label = { Text(stringResource(R.string.player_sheets_sub_delay_card_speed)) },
          value = speed,
          onChange = onSpeedChange,
          max = 10f,
          step = 0.01f,
          min = 0.1f,
          increaseIcon = Icons.Filled.Add,
          decreaseIcon = Icons.Filled.Remove,
          valueFormatter = { "%.2f".format(it) }
        )
      }
    )
}

@Suppress("LambdaParameterInRestartableEffect")
@Composable
fun DelayCardContent(
  delay: Float,
  onDelayChange: (Float) -> Unit,
  onApply: () -> Unit,
  onReset: () -> Unit,
  delayType: DelayType,
  extraSettings: (@Composable ColumnScope.() -> Unit)? = null,
) {
  val haptics = LocalHapticFeedback.current
  val unit = stringResource(R.string.player_unit_seconds_short)
  var showValueDialog by remember { mutableStateOf(false) }

  // Measuring: true (heard -> spotted), false (spotted -> heard), null when idle.
  var isDirectionPositive by remember { mutableStateOf<Boolean?>(null) }
  val isMeasuring = isDirectionPositive != null
  var timerStart by remember { mutableStateOf<Long?>(null) }
  var finalDelay by remember { mutableStateOf(delay) }
  LaunchedEffect(isDirectionPositive) {
    if (isDirectionPositive == null) {
      onDelayChange(finalDelay)
      return@LaunchedEffect
    }
    finalDelay = delay
    val startTime = System.currentTimeMillis()
    timerStart = startTime
    val startingDelay: Float = finalDelay
    while (isDirectionPositive != null && timerStart != null) {
      val elapsed = System.currentTimeMillis() - startTime
      val direction = isDirectionPositive ?: break
      finalDelay = startingDelay + (if (direction) elapsed / 1000f else -elapsed / 1000f)
      // Arbitrary delay of 20ms
      delay(20)
    }
  }

  // Note: verticalScroll is handled by OverlayPanelContent.
  Column(
    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.small, vertical = MaterialTheme.spacing.extraSmall),
    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
  ) {
    PanelSection(title = stringResource(R.string.player_sheets_sub_delay_card_delay)) {
      Column(
        modifier = Modifier.padding(MaterialTheme.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
      ) {
        // Tap the readout to type an exact value.
        Text(
          text = "%+.2f %s".format(delay, unit),
          style = MaterialTheme.typography.displaySmall,
          modifier =
            Modifier
              .clip(RoundedCornerShape(16.dp))
              .clickable(enabled = !isMeasuring) { showValueDialog = true }
              .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.extraSmall),
        )
        var lastTick by remember { mutableIntStateOf((delay * 10).roundToInt()) }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Slider(
            value = delay.coerceIn(DELAY_SLIDER_RANGE),
            onValueChange = { raw ->
              val value = (raw * 100).roundToInt() / 100f
              val tick = (value * 10).roundToInt()
              if (tick != lastTick) {
                lastTick = tick
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              }
              onDelayChange(value)
            },
            valueRange = DELAY_SLIDER_RANGE,
            enabled = !isMeasuring,
          )
          // Zero mark at the slider's center.
          Box(
            modifier =
              Modifier
                .width(2.dp)
                .height(10.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), RoundedCornerShape(1.dp)),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
        ) {
          DELAY_QUICK_STEPS.forEach { step ->
            FilledTonalButton(
              onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDelayChange(((delay + step) * 1000).roundToInt() / 1000f)
              },
              enabled = !isMeasuring,
              modifier = Modifier.weight(1f),
              contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
              Text("%+.2f".format(step), maxLines = 1)
            }
          }
        }
      }
    }

    if (extraSettings != null) {
      PanelSection {
        Column(
          modifier = Modifier.padding(MaterialTheme.spacing.medium),
          verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
        ) { extraSettings() }
      }
    }

    PanelSection(title = stringResource(R.string.player_sheets_delay_measure_title)) {
      Row(
        modifier = Modifier.padding(MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
      ) {
        FilledTonalButton(
          onClick = {
            isDirectionPositive = if (isDirectionPositive == null) delayType == DelayType.Audio else null
          },
          modifier = Modifier.weight(1f),
          enabled = isDirectionPositive != (delayType == DelayType.Audio),
        ) {
          Text(
            stringResource(
              if (delayType == DelayType.Audio) {
                R.string.player_sheets_sub_delay_audio_sound_heard
              } else {
                R.string.player_sheets_sub_delay_subtitle_voice_heard
              },
            ),
          )
        }
        FilledTonalButton(
          onClick = {
            isDirectionPositive = if (isDirectionPositive == null) delayType != DelayType.Audio else null
          },
          modifier = Modifier.weight(1f),
          enabled = isDirectionPositive != (delayType == DelayType.Subtitle),
        ) {
          Text(
            stringResource(
              if (delayType == DelayType.Audio) {
                R.string.player_sheets_sub_delay_sound_sound_spotted
              } else {
                R.string.player_sheets_sub_delay_subtitle_text_seen
              },
            ),
          )
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
    ) {
      Button(
        onClick = onApply,
        modifier = Modifier.weight(1.5f),
        enabled = !isMeasuring,
      ) {
        Text(stringResource(R.string.player_sheets_delay_set_as_default))
      }
      FilledTonalButton(
        onClick = onReset,
        modifier = Modifier.weight(1f),
        enabled = !isMeasuring,
      ) {
        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(MaterialTheme.spacing.smaller))
        Text(stringResource(R.string.generic_reset))
      }
    }
  }

  if (showValueDialog) {
    var text by remember { mutableStateOf("%.3f".format(delay)) }
    AlertDialog(
      onDismissRequest = { showValueDialog = false },
      title = { Text(stringResource(R.string.player_sheets_sub_delay_card_delay)) },
      text = {
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          suffix = { Text(unit) },
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            text.replace(',', '.').toFloatOrNull()?.let(onDelayChange)
            showValueDialog = false
          },
        ) { Text(stringResource(R.string.generic_ok)) }
      },
      dismissButton = {
        TextButton(onClick = { showValueDialog = false }) { Text(stringResource(R.string.generic_cancel)) }
      },
    )
  }
}

// Slider covers a practical range; larger values stay reachable via the step buttons or typing.
private val DELAY_SLIDER_RANGE = -5f..5f
private val DELAY_QUICK_STEPS = listOf(-0.1f, -0.01f, 0.01f, 0.1f)

enum class DelayType {
  Audio,
  Subtitle,
}
