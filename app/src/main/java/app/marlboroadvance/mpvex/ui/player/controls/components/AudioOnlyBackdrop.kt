package app.marlboroadvance.mpvex.ui.player.controls.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.ui.player.TrackNode

/**
 * Full-screen "now playing" view for files without a video track, so audio doesn't play over a
 * black screen. Static UI (no animation, no input handling): it costs nothing while playing and
 * the player's gestures and controls work on top of it unchanged.
 */
@Composable
fun AudioOnlyBackdrop(
  title: String?,
  track: TrackNode,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val pills = remember(track) { audioPills(track) }

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
      .background(
        Brush.radialGradient(
          colors = listOf(colors.primary.copy(alpha = 0.28f), Color.Transparent),
        ),
      ),
    contentAlignment = Alignment.Center,
  ) {
    // Artwork scales with the screen: big in portrait, smaller in landscape so the title fits.
    val artSize = (minOf(maxWidth, maxHeight) * 0.42f).coerceIn(120.dp, 280.dp)
    val isLandscape = maxWidth > maxHeight

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 32.dp),
    ) {
      // The centre play/pause button sits over the artwork when controls are shown.
      Surface(
        modifier = Modifier.size(artSize),
        shape = RoundedCornerShape(32.dp),
        color = colors.primaryContainer,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
      ) {
        Icon(
          imageVector = Icons.Rounded.MusicNote,
          contentDescription = null,
          tint = colors.onPrimaryContainer,
          modifier = Modifier.padding(artSize * 0.25f),
        )
      }

      Spacer(Modifier.height(if (isLandscape) 16.dp else 28.dp))

      if (!title.isNullOrBlank()) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          color = Color.White,
          textAlign = TextAlign.Center,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (pills.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          pills.forEach { label ->
            Surface(
              shape = CircleShape,
              color = Color.White.copy(alpha = 0.12f),
              contentColor = Color.White,
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              )
            }
          }
        }
      }
    }
  }
}

/** e.g. ["AAC", "48 kHz", "Stereo"]. */
private fun audioPills(track: TrackNode): List<String> {
  val rawCodec = track.codec
  val codec = when (rawCodec?.lowercase()) {
    null, "" -> null
    "aac" -> "AAC"
    "opus" -> "Opus"
    "mp3" -> "MP3"
    "flac" -> "FLAC"
    "vorbis" -> "Vorbis"
    "ac3" -> "AC3"
    "eac3" -> "E-AC3"
    "dts" -> "DTS"
    "truehd" -> "TrueHD"
    "alac" -> "ALAC"
    else -> rawCodec?.uppercase()
  }
  val rate = track.demuxSampleRate?.takeIf { it > 0 }?.let { hz ->
    val khz = hz / 1000.0
    if (khz % 1.0 == 0.0) "${khz.toInt()} kHz" else "%.1f kHz".format(khz)
  }
  val channels = when (val count = (track.demuxChannelCount ?: track.audioChannels)?.toInt()) {
    null, 0 -> null
    1 -> "Mono"
    2 -> "Stereo"
    6 -> "5.1"
    8 -> "7.1"
    else -> "$count ch"
  }
  return listOfNotNull(codec, rate, channels)
}
