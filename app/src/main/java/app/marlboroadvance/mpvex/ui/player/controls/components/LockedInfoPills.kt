package app.marlboroadvance.mpvex.ui.player.controls.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.marlboroadvance.mpvex.ui.player.PlayerViewModel
import `is`.xyz.mpv.Utils
import kotlinx.coroutines.delay
import java.util.Date

/**
 * Pills shown while the player controls are locked (the seekbar and buttons are hidden):
 * - [LockedInfoPill]: playback time / duration, a thin progress bar and battery level.
 * - [LockedClockPill]: the phone's clock (the status bar is hidden in fullscreen).
 *
 * Both are display-only (no seeking while locked) and only exist while visible, so the
 * battery receiver and clock ticker don't run the rest of the time.
 */
@Composable
fun LockedInfoPill(
  viewModel: PlayerViewModel,
  modifier: Modifier = Modifier,
) {
  val position by viewModel.precisePosition.collectAsState()
  val duration by viewModel.effectiveDuration.collectAsState()
  val battery = rememberBatteryState()

  val progress = if (duration > 0f) (position / duration).coerceIn(0f, 1f) else 0f
  val lowBattery = battery.level in 0..LOW_BATTERY_PERCENT && !battery.charging

  LockedPillSurface(modifier = modifier) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "${Utils.prettyTime(position.toInt())} / ${Utils.prettyTime(duration.toInt())}",
        style = pillTextStyle(),
        color = MaterialTheme.colorScheme.onSurface,
      )

      // Display-only progress: rounded track with the played part in the theme color.
      Box(
        modifier = Modifier
          .width(PROGRESS_WIDTH)
          .height(4.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
      ) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(progress)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        )
      }

      if (battery.level >= 0) {
        val batteryColor = when {
          lowBattery -> MaterialTheme.colorScheme.error
          battery.charging -> MaterialTheme.colorScheme.primary
          else -> MaterialTheme.colorScheme.onSurface
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Icon(
            imageVector = if (battery.charging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryFull,
            contentDescription = null,
            tint = batteryColor,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "${battery.level}%",
            style = pillTextStyle(),
            color = batteryColor,
          )
        }
      }
    }
  }
}

@Composable
fun LockedClockPill(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  // Follows the phone's 12/24-hour setting; ticks on the minute while visible.
  val time by produceState(initialValue = formatClock(context)) {
    while (true) {
      val now = System.currentTimeMillis()
      delay(MINUTE_MS - now % MINUTE_MS + 50)
      value = formatClock(context)
    }
  }

  LockedPillSurface(modifier = modifier) {
    Text(
      text = time,
      style = pillTextStyle(),
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

/** Glass capsule matching the unlock button. */
@Composable
private fun LockedPillSurface(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Surface(
    modifier = modifier,
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
      content()
    }
  }
}

@Composable
private fun pillTextStyle(): TextStyle =
  MaterialTheme.typography.labelLargeEmphasized.copy(fontFeatureSettings = "tnum")

private fun formatClock(context: Context): String =
  DateFormat.getTimeFormat(context).format(Date())

@Immutable
private data class BatteryState(val level: Int, val charging: Boolean)

/**
 * Battery level and charging state. Reads the sticky ACTION_BATTERY_CHANGED broadcast
 * immediately and listens for updates only while the pill is on screen.
 */
@Composable
private fun rememberBatteryState(): BatteryState {
  val context = LocalContext.current
  var state by remember { mutableStateOf(BatteryState(level = -1, charging = false)) }

  DisposableEffect(context) {
    fun update(intent: Intent?) {
      if (intent == null) return
      val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
      val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
      val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
      val percent = if (level >= 0 && scale > 0) level * 100 / scale else -1
      val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
      state = BatteryState(percent, charging)
    }

    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) = update(intent)
    }
    val sticky = ContextCompat.registerReceiver(
      context,
      receiver,
      IntentFilter(Intent.ACTION_BATTERY_CHANGED),
      ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    update(sticky)

    onDispose {
      runCatching { context.unregisterReceiver(receiver) }
    }
  }
  return state
}

private const val LOW_BATTERY_PERCENT = 15
private const val MINUTE_MS = 60_000L
private val PROGRESS_WIDTH = 96.dp
