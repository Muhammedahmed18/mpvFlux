package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.player.Decoder
import app.marlboroadvance.mpvex.ui.theme.spacing
import app.marlboroadvance.mpvex.utils.media.HardwareDecoderSupport
import `is`.xyz.mpv.MPVLib
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Decoder picker (SW / HW / HW+ / Auto).
 *
 * For the current video it asks Android which hardware decoders exist. When the phone
 * can't decode this video in hardware (e.g. AV1 without an AV1 chip, or 10-bit H.264),
 * HW and HW+ are shown dimmed with the reason and can't be selected, and the header
 * explains that the video plays in software.
 */
@Composable
fun DecodersSheet(
  selectedDecoder: Decoder,
  onSelect: (Decoder) -> Unit,
  onDismissRequest: () -> Unit,
) {
  val decoders = remember { Decoder.entries.minusElement(Decoder.Auto).toImmutableList() }

  // Codec / profile / size of the playing video, read once when the sheet opens.
  val videoInfo = remember {
    VideoCodecInfo(
      codec = runCatching { MPVLib.getPropertyString("current-tracks/video/codec") }.getOrNull(),
      profile = runCatching { MPVLib.getPropertyString("current-tracks/video/codec-profile") }.getOrNull(),
      width = runCatching { MPVLib.getPropertyInt("video-params/w") }.getOrNull() ?: 0,
      height = runCatching { MPVLib.getPropertyInt("video-params/h") }.getOrNull() ?: 0,
    )
  }

  // MediaCodecList is queried off the main thread; results are cached per codec/profile/size.
  val support by produceState<HardwareDecoderSupport.Result>(HardwareDecoderSupport.Result.Unknown, videoInfo) {
    value = withContext(Dispatchers.Default) {
      HardwareDecoderSupport.check(videoInfo.codec, videoInfo.profile, videoInfo.width, videoInfo.height)
    }
  }
  val unavailable = support as? HardwareDecoderSupport.Result.Unavailable
  val codecLabel = videoInfo.displayCodec()

  val reasonText = unavailable?.let {
    when (it.reason) {
      HardwareDecoderSupport.Reason.NoHardwareDecoder ->
        stringResource(R.string.decoder_reason_no_hw, codecLabel)
      HardwareDecoderSupport.Reason.ProfileNotSupported ->
        stringResource(R.string.decoder_reason_profile, codecLabel, videoInfo.profile.orEmpty())
      HardwareDecoderSupport.Reason.ResolutionTooHigh ->
        stringResource(R.string.decoder_reason_resolution, codecLabel, videoInfo.width, videoInfo.height)
    }
  }

  GenericTracksSheet(
    tracks = decoders,
    header = {
      Column {
        SheetHeader(
          title = stringResource(R.string.decoder_sheet_title),
          label = videoInfo.headerLabel(codecLabel),
        )
        if (reasonText != null) {
          Text(
            text = stringResource(R.string.decoder_sheet_sw_note, reasonText),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
              .fillMaxWidth()
              .padding(
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
                top = MaterialTheme.spacing.small,
              ),
          )
        }
      }
    },
    track = { decoder ->
      val isHardwareOption = decoder == Decoder.HW || decoder == Decoder.HWPlus
      val blocked = isHardwareOption && unavailable != null
      val metadata = when {
        blocked -> listOf(
          TrackMetadata(stringResource(R.string.decoder_not_supported), MetadataType.WARNING),
        )
        isHardwareOption && support == HardwareDecoderSupport.Result.Available -> listOf(
          TrackMetadata(stringResource(R.string.decoder_supported), MetadataType.PRIMARY),
        )
        else -> emptyList()
      }
      TrackSelectableBar(
        id = decoders.indexOf(decoder) + 1,
        title = stringResource(R.string.player_sheets_decoder_formatted, decoder.title, decoder.value),
        isSelected = selectedDecoder == decoder,
        onClick = { if (!blocked) onSelect(decoder) },
        metadata = metadata,
        enabled = !blocked,
      )
    },
    onDismissRequest = onDismissRequest,
  )
}

private data class VideoCodecInfo(
  val codec: String?,
  val profile: String?,
  val width: Int,
  val height: Int,
) {
  fun displayCodec(): String =
    when (codec?.lowercase()) {
      "h264" -> "H.264"
      "hevc" -> "HEVC"
      "av1" -> "AV1"
      "vp9" -> "VP9"
      "vp8" -> "VP8"
      "mpeg2video" -> "MPEG-2"
      "mpeg4" -> "MPEG-4"
      null -> ""
      else -> codec?.uppercase().orEmpty()
    }

  /** e.g. "AV1 · MAIN · 1920×1080" shown above the sheet title. */
  fun headerLabel(codecLabel: String): String? {
    val parts = buildList {
      if (codecLabel.isNotBlank()) add(codecLabel)
      if (!profile.isNullOrBlank()) add(profile)
      if (width > 0 && height > 0) add("$width×$height")
    }
    return if (parts.isEmpty()) null else parts.joinToString(" · ").uppercase()
  }
}
