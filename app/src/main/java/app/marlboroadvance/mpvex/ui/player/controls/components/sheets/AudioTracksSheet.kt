package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AudioChannels
import app.marlboroadvance.mpvex.preferences.AudioPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.player.TrackNode
import app.marlboroadvance.mpvex.ui.theme.spacing
import `is`.xyz.mpv.MPVLib
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.koinInject

sealed class AudioItem {
  data class Track(val node: TrackNode) : AudioItem()
  data class Header(val title: String) : AudioItem()
  object Divider : AudioItem()
}

@Composable
fun AudioTracksSheet(
  tracks: ImmutableList<TrackNode>,
  onSelect: (TrackNode) -> Unit,
  onAddAudioTrack: () -> Unit,
  onOpenDelayPanel: () -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val audioPreferences = koinInject<AudioPreferences>()
  val audioChannels by audioPreferences.audioChannels.collectAsState()

  val items = remember(tracks) {
    val list = mutableListOf<AudioItem>()
    val internal = tracks.filter { it.external != true }
    val external = tracks.filter { it.external == true }

    if (internal.isNotEmpty()) {
      list.add(AudioItem.Header("Embedded Audio"))
      list.addAll(internal.map { AudioItem.Track(it) })
    }

    if (external.isNotEmpty()) {
      if (internal.isNotEmpty()) list.add(AudioItem.Divider)
      list.add(AudioItem.Header("External Audio"))
      list.addAll(external.map { AudioItem.Track(it) })
    }

    list.toImmutableList()
  }

  GenericTracksSheet(
    tracks = items,
    onDismissRequest = onDismissRequest,
    header = {
      val audioActions = remember {
        listOf(
          TrackAction(
            label = "Add",
            icon = Icons.Default.Add,
            onClick = onAddAudioTrack
          ),
          TrackAction(
            label = "Delay",
            icon = Icons.Default.MoreTime,
            onClick = onOpenDelayPanel
          )
        )
      }
      TrackActionsRow(actions = audioActions)
    },
    track = { item ->
      when (item) {
        is AudioItem.Track -> {
          val node = item.node
          val externalLabel = stringResource(R.string.generic_external)
          val metadata = remember(node) {
            mutableListOf<String>().apply {
              if (!node.codec.isNullOrBlank()) add(node.codec)
              if (node.audioChannels != null) {
                add(node.demuxChannels ?: "${node.audioChannels}ch")
              }
              if (node.external == true) add(externalLabel)
              if (!node.lang.isNullOrBlank() && node.title?.contains(node.lang, ignoreCase = true) != true) {
                add(node.lang)
              }
            }
          }

          TrackSelectableBar(
            title = getTrackTitle(node),
            isSelected = node.isSelected,
            onClick = { onSelect(node) },
            metadata = metadata
          )
        }
        is AudioItem.Header -> {
          Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .padding(horizontal = 16.dp)
              .padding(top = 16.dp, bottom = 8.dp)
          )
        }
        AudioItem.Divider -> {
          HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
          )
        }
      }
    },
    footer = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp, bottom = 16.dp)
      ) {
        Text(
          text = stringResource(id = R.string.pref_audio_channels),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        SingleChoiceSegmentedButtonRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
        ) {
          AudioChannels.entries.forEachIndexed { index, channel ->
            this@SingleChoiceSegmentedButtonRow.SegmentedButton(
              selected = audioChannels == channel,
              onClick = {
                audioPreferences.audioChannels.set(channel)
                if (channel == AudioChannels.ReverseStereo) {
                  MPVLib.setPropertyString(AudioChannels.AutoSafe.property, AudioChannels.AutoSafe.value)
                } else {
                  MPVLib.setPropertyString(AudioChannels.ReverseStereo.property, "")
                }
                MPVLib.setPropertyString(channel.property, channel.value)
              },
              shape = SegmentedButtonDefaults.itemShape(index = index, count = AudioChannels.entries.size),
              label = { 
                Text(
                  text = stringResource(id = channel.title),
                  style = MaterialTheme.typography.labelMedium
                ) 
              }
            )
          }
        }
      }
    },
    modifier = modifier,
  )
}
