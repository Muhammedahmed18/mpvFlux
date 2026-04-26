package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AudioChannels
import app.marlboroadvance.mpvex.preferences.AudioPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.player.TrackNode
import app.marlboroadvance.mpvex.ui.theme.spacing
import `is`.xyz.mpv.MPVLib
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.koin.compose.koinInject

sealed class AudioItem {
  data class Track(val node: TrackNode) : AudioItem()
  data class Header(val title: String) : AudioItem()
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
  var isChannelSelectionMode by remember { mutableStateOf(false) }

  val configuration = LocalConfiguration.current
  val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  val calculatedMaxWidth = if (configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
    640.dp 
  } else {
    560.dp
  }

  PlayerSheet(
    onDismissRequest = onDismissRequest,
    customMaxWidth = calculatedMaxWidth,
    surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.95f)
  ) {
    AnimatedContent(
      targetState = isChannelSelectionMode,
      transitionSpec = {
        if (targetState) {
          slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
        } else {
          slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
        }
      },
      label = "AudioSheetTransition"
    ) { selectionMode ->
      if (selectionMode) {
        Column(
          modifier = modifier
            .fillMaxWidth()
            .padding(bottom = navBarPadding.coerceAtLeast(MaterialTheme.spacing.medium))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            IconButton(onClick = { isChannelSelectionMode = false }) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null
              )
            }
            Text(
              text = "Channels",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(AudioChannels.entries.toTypedArray()) { channel ->
              val isSelected = audioChannels == channel
              TrackSelectableBar(
                id = AudioChannels.entries.indexOf(channel) + 1,
                title = stringResource(channel.title),
                isSelected = isSelected,
                onClick = {
                  audioPreferences.audioChannels.set(channel)
                  if (channel == AudioChannels.ReverseStereo) {
                    MPVLib.setPropertyString(AudioChannels.AutoSafe.property, AudioChannels.AutoSafe.value)
                  } else {
                    MPVLib.setPropertyString(AudioChannels.ReverseStereo.property, "")
                  }
                  MPVLib.setPropertyString(channel.property, channel.value)
                  onDismissRequest()
                }
              )
            }
          }
          Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        }
      } else {
        Column(
          modifier = modifier
            .fillMaxWidth()
            .padding(bottom = navBarPadding.coerceAtLeast(MaterialTheme.spacing.medium))
        ) {
          Column(modifier = Modifier.padding(top = MaterialTheme.spacing.medium)) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Audio",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              
              // Header Status Badge
              Surface(
                onClick = { isChannelSelectionMode = true },
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .background(MaterialTheme.colorScheme.primary, CircleShape)
                  )
                  Text(
                    text = stringResource(audioChannels.title).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                  )
                }
              }
            }
            
            val audioActions = remember {
              listOf(
                TrackAction(
                  label = "Add Track",
                  icon = Icons.Default.Add,
                  onClick = onAddAudioTrack
                ),
                TrackAction(
                  label = "Sync Delay",
                  icon = Icons.Default.MoreTime,
                  onClick = onOpenDelayPanel
                )
              )
            }
            TrackActionsRow(actions = audioActions)
          }

          val audioItems = remember(tracks) {
            val list = mutableListOf<AudioItem>()
            val internal = tracks.filter { it.external != true }
            val external = tracks.filter { it.external == true }

            if (internal.isNotEmpty()) {
              list.add(AudioItem.Header("EMBEDDED"))
              list.addAll(internal.map { AudioItem.Track(it) })
            }

            if (external.isNotEmpty()) {
              list.add(AudioItem.Header("EXTERNAL"))
              list.addAll(external.map { AudioItem.Track(it) })
            }

            list.toImmutableList()
          }

          LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            contentPadding = PaddingValues(
              horizontal = MaterialTheme.spacing.medium,
              vertical = MaterialTheme.spacing.small
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(audioItems) { item ->
              when (item) {
                is AudioItem.Track -> {
                  val node = item.node
                  val externalLabel = stringResource(R.string.generic_external)
                  val metadata = remember(node) {
                    mutableListOf<TrackMetadata>().apply {
                      if (!node.codec.isNullOrBlank()) {
                        add(TrackMetadata(node.codec, MetadataType.PRIMARY))
                      }
                      if (node.audioChannels != null) {
                        add(TrackMetadata(node.demuxChannels ?: "${node.audioChannels}CH"))
                      }
                      if (node.external == true) {
                        add(TrackMetadata(externalLabel, MetadataType.WARNING))
                      }
                      if (!node.lang.isNullOrBlank() && node.title?.contains(node.lang, ignoreCase = true) != true) {
                        add(TrackMetadata(node.lang))
                      }
                    }
                  }

                  TrackSelectableBar(
                    id = node.id,
                    title = getTrackTitle(node),
                    isSelected = node.isSelected,
                    onClick = { onSelect(node) },
                    metadata = metadata
                  )
                }
                is AudioItem.Header -> {
                  TrackHeaderPill(
                    title = item.title,
                    modifier = Modifier.padding(horizontal = 16.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
