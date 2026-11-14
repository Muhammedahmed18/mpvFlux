package app.marlboroadvance.mpvex.ui.preferences.components

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.preferences.PreferenceCard
import app.marlboroadvance.mpvex.ui.preferences.PreferenceDivider
import app.marlboroadvance.mpvex.utils.media.DeviceCodecReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Settings → Decoder → "Device codec support": one row per video format with a
 * Hardware / Software chip, the top resolution and frame rate, 10-bit and HDR support.
 * Tap a row to see the Android decoder names. "Copy report" puts it all on the clipboard.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeviceCodecSupportCard() {
  val entries by produceState<List<DeviceCodecReport.Entry>?>(initialValue = null) {
    value = withContext(Dispatchers.Default) { DeviceCodecReport.build() }
  }

  PreferenceCard {
    val list = entries
    if (list == null) {
      Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        LoadingIndicator(modifier = Modifier.size(40.dp))
      }
      return@PreferenceCard
    }

    SummaryRow(hardwareCount = list.count { it.hardware }, total = list.size)
    PreferenceDivider()
    list.forEachIndexed { index, entry ->
      if (index > 0) PreferenceDivider()
      CodecRow(entry)
    }
    PreferenceDivider()
    CopyReportRow(list)
  }
}

@Composable
private fun SummaryRow(hardwareCount: Int, total: Int) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.secondaryContainer,
      contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      modifier = Modifier.size(40.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(ImageVector.vectorResource(R.drawable.ic_sym_memory), contentDescription = null, modifier = Modifier.size(22.dp))
      }
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = stringResource(R.string.codec_support_summary, hardwareCount, total),
        style = MaterialTheme.typography.titleMediumEmphasized,
      )
      Text(
        text = stringResource(R.string.codec_support_explainer),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun CodecRow(entry: DeviceCodecReport.Entry) {
  var expanded by rememberSaveable(entry.label) { mutableStateOf(false) }
  val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "codec_expand")

  val details = if (entry.hardware) {
    listOfNotNull(
      entry.maxResolution?.let { res ->
        entry.maxFps?.let { stringResource(R.string.codec_support_up_to_fps, res, it) }
          ?: stringResource(R.string.codec_support_up_to, res)
      },
      if (entry.hdr) stringResource(R.string.codec_support_hdr) else null,
    ).joinToString(" · ").ifEmpty { stringResource(R.string.codec_support_hardware) }
  } else {
    stringResource(R.string.codec_support_software_detail)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { expanded = !expanded }
      .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f)) {
        Text(text = entry.label, style = MaterialTheme.typography.titleMediumEmphasized)
        Text(
          text = details,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      StatusChip(hardware = entry.hardware)
      Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_sym_expand_more),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp).size(24.dp).rotate(arrowRotation),
      )
    }

    AnimatedVisibility(
      visible = expanded,
      enter = expandVertically() + fadeIn(),
      exit = shrinkVertically() + fadeOut(),
    ) {
      Text(
        text = entry.decoders.joinToString("\n").ifEmpty { stringResource(R.string.codec_support_no_android_decoder) },
        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
      )
    }
  }
}

/** Primary tonal pill for hardware, neutral pill for software. */
@Composable
private fun StatusChip(hardware: Boolean) {
  Surface(
    shape = CircleShape,
    color = if (hardware) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor = if (hardware) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
  ) {
    Text(
      text = stringResource(if (hardware) R.string.codec_support_hardware else R.string.codec_support_software),
      style = MaterialTheme.typography.labelLarge,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    )
  }
}

@Composable
private fun CopyReportRow(entries: List<DeviceCodecReport.Entry>) {
  val context = LocalContext.current
  val copied = stringResource(R.string.codec_support_copied)
  val report = remember(entries) { DeviceCodecReport.asText(entries) }
  Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
    FilledTonalButton(
      onClick = {
        context.getSystemService(ClipboardManager::class.java)
          ?.setPrimaryClip(ClipData.newPlainText("MpvFlux codec support", report))
        // Android 13+ shows its own "Copied" confirmation.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
          Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
        }
      },
    ) {
      Icon(ImageVector.vectorResource(R.drawable.ic_sym_content_copy), contentDescription = null, modifier = Modifier.size(18.dp))
      Box(modifier = Modifier.width(8.dp))
      Text(stringResource(R.string.codec_support_copy_report))
    }
  }
}
