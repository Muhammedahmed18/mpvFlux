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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.preferences.PreferenceCard
import app.marlboroadvance.mpvex.ui.preferences.PreferenceIcon
import app.marlboroadvance.mpvex.ui.preferences.PreferenceItem
import app.marlboroadvance.mpvex.utils.media.DeviceCodecReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads the codec report once in the background; null while it's being built. */
@Composable
fun rememberDeviceCodecReport(): List<DeviceCodecReport.Entry>? {
  val entries by produceState<List<DeviceCodecReport.Entry>?>(initialValue = null) {
    value = withContext(Dispatchers.Default) { DeviceCodecReport.build() }
  }
  return entries
}

/**
 * Row in Settings → Decoder that opens the Device codec support screen.
 * Shows how many formats play in hardware once the background check is done.
 */
@Composable
fun DeviceCodecSupportRow(onClick: () -> Unit) {
  val entries = rememberDeviceCodecReport()
  PreferenceItem(
    title = stringResource(R.string.codec_support_title),
    summary = entries?.let {
      stringResource(R.string.codec_support_row_count, it.count { e -> e.hardware }, it.size)
    } ?: stringResource(R.string.codec_support_checking),
    icon = { PreferenceIcon(imageVector = ImageVector.vectorResource(R.drawable.ic_sym_memory)) },
    onClick = onClick,
    showChevron = true,
  )
}

/** Top card of the screen: phone, hardware count and explanation, in one segment. */
@Composable
fun DeviceCodecSummaryCard(entries: List<DeviceCodecReport.Entry>) {
  PreferenceCard {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        TonalIcon(R.drawable.ic_sym_memory)
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            style = MaterialTheme.typography.titleMediumEmphasized,
          )
          Text(
            text = stringResource(R.string.codec_support_android_version, Build.VERSION.RELEASE),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = stringResource(R.string.codec_support_summary, entries.count { it.hardware }, entries.size),
          style = MaterialTheme.typography.headlineSmallEmphasized,
        )
        Text(
          text = stringResource(R.string.codec_support_explainer),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

/** One card per group (Common, 10-bit and HDR, Older) with a row per format. */
@Composable
fun CodecGroupCard(entries: List<DeviceCodecReport.Entry>) {
  PreferenceCard {
    entries.forEach { entry ->
      CodecRow(entry)
    }
  }
}

/** Top bar action: copies the plain-text codec report, handy for bug reports. */
@Composable
fun CopyCodecReportAction(entries: List<DeviceCodecReport.Entry>) {
  val context = LocalContext.current
  val copied = stringResource(R.string.codec_support_copied)
  val label = stringResource(R.string.codec_support_copy_report)
  val report = remember(entries) { DeviceCodecReport.asText(entries) }
  IconButton(
    onClick = {
      context.getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText("MpvFlux codec support", report))
      // Android 13+ shows its own "Copied" confirmation.
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
      }
    },
  ) {
    Icon(
      imageVector = ImageVector.vectorResource(R.drawable.ic_sym_content_copy),
      contentDescription = label,
      tint = MaterialTheme.colorScheme.secondary,
    )
  }
}

@Composable
private fun TonalIcon(icon: Int) {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier = Modifier.size(40.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(ImageVector.vectorResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
    }
  }
}

@Composable
private fun CodecRow(entry: DeviceCodecReport.Entry) {
  var expanded by rememberSaveable(entry.label) { mutableStateOf(false) }
  val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "codec_expand")
  val stateText = stringResource(if (expanded) R.string.cd_expanded else R.string.cd_collapsed)

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
      .clickable(role = Role.Button) { expanded = !expanded }
      .semantics { stateDescription = stateText }
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
