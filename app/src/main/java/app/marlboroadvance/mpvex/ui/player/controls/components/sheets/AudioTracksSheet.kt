package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AudioChannels
import app.marlboroadvance.mpvex.preferences.AudioPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.player.TrackNode
import app.marlboroadvance.mpvex.ui.theme.spacing
import `is`.xyz.mpv.MPVLib
import kotlinx.collections.immutable.ImmutableList
import org.koin.compose.koinInject

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

  GenericTracksSheet(
    tracks,
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
    track = {
      val externalLabel = stringResource(R.string.generic_external)
      val metadata = remember(it) {
        mutableListOf<String>().apply {
          if (!it.codec.isNullOrBlank()) add(it.codec)
          if (it.audioChannels != null) {
            add(it.demuxChannels ?: "${it.audioChannels}ch")
          }
          if (it.external == true) add(externalLabel)
        }
      }

      TrackSelectableBar(
        title = getTrackTitle(it),
        isSelected = it.isSelected,
        onClick = { onSelect(it) },
        metadata = metadata
      )
    },
    footer = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = MaterialTheme.spacing.small)
      ) {
        Text(
          text = stringResource(id = R.string.pref_audio_channels),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        
        SingleChoiceSegmentedButtonRow(
          modifier = Modifier.fillMaxWidth()
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
