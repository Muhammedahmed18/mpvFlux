package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SubtitlesOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.player.TrackNode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

sealed class SubtitleItem {
  data object Off : SubtitleItem()
  data class Track(val node: TrackNode) : SubtitleItem()
  data class Header(val title: String) : SubtitleItem()
}

@Composable
fun SubtitlesSheet(
  tracks: ImmutableList<TrackNode>,
  onToggleSubtitle: (Int) -> Unit,
  isSubtitleSelected: (Int) -> Boolean,
  onAddSubtitle: () -> Unit,
  onOpenSubtitleSettings: () -> Unit,
  onOpenSubtitleDelay: () -> Unit,
  onRemoveSubtitle: (Int) -> Unit,
  onOpenOnlineSearch: () -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier
) {
  val embeddedLabel = stringResource(R.string.track_header_embedded)
  val externalLabel = stringResource(R.string.track_header_external)
  val items = remember(tracks, embeddedLabel, externalLabel) {
    val list = mutableListOf<SubtitleItem>()
    // "Off" row sits at the very top so disabling subtitles is one tap from the top of the list.
    list.add(SubtitleItem.Off)

    val internal = tracks.filter { it.external != true }
    val external = tracks.filter { it.external == true }

    if (internal.isNotEmpty()) {
      list.add(SubtitleItem.Header(embeddedLabel))
      list.addAll(internal.map { SubtitleItem.Track(it) })
    }

    if (external.isNotEmpty()) {
      list.add(SubtitleItem.Header(externalLabel))
      list.addAll(external.map { SubtitleItem.Track(it) })
    }

    list.toImmutableList()
  }

  GenericTracksSheet(
    tracks = items,
    onDismissRequest = onDismissRequest,
    header = {
      Column {
        SheetHeader(
          title    = stringResource(R.string.pref_subtitles),
          trailing = if (tracks.isNotEmpty()) {
            { TrackCountPill(count = tracks.size) }
          } else {
            null
          },
        )
        // Leading: find/add (intent = "get more subs"). Trailing: configure (style/timing).
        val addLabel = stringResource(R.string.add)
        val searchLabel = stringResource(R.string.settings_search_title)
        val styleLabel = stringResource(R.string.track_action_style)
        val syncLabel = stringResource(R.string.track_action_sync)
        val leadingSubtitleActions = remember(addLabel, searchLabel) {
          listOf(
            TrackAction(label = addLabel,    icon = Icons.Default.Add,    onClick = onAddSubtitle),
            TrackAction(label = searchLabel, icon = Icons.Default.Search, onClick = onOpenOnlineSearch),
          )
        }
        val trailingSubtitleActions = remember(styleLabel, syncLabel) {
          listOf(
            TrackAction(label = styleLabel, icon = Icons.Default.Palette,  onClick = onOpenSubtitleSettings),
            TrackAction(label = syncLabel,  icon = Icons.Default.MoreTime, onClick = onOpenSubtitleDelay),
          )
        }
        TrackActionsRow(
          leadingActions  = leadingSubtitleActions,
          trailingActions = trailingSubtitleActions,
        )
      }
    },
    track = { item ->
      when (item) {
        is SubtitleItem.Off -> {
          val isOffSelected = tracks.none { isSubtitleSelected(it.id) }
          TrackSelectableBar(
            id         = 0,
            title      = stringResource(R.string.player_sheets_tracks_off),
            isSelected = isOffSelected,
            onClick    = {
              // Tap "Off" → toggle whichever track is currently selected so subtitles disable
              if (!isOffSelected) {
                tracks.firstOrNull { isSubtitleSelected(it.id) }?.let { onToggleSubtitle(it.id) }
              }
            },
            badge      = {
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .background(
                    if (isOffSelected) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                    },
                  ),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector        = Icons.Outlined.SubtitlesOff,
                  contentDescription = null,
                  tint               = if (isOffSelected) {
                    MaterialTheme.colorScheme.onPrimary
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                  modifier           = Modifier.size(14.dp),
                )
              }
            },
          )
        }
        is SubtitleItem.Track -> {
          val track = item.node
          val isSelected = isSubtitleSelected(track.id)
          val externalLabel = stringResource(R.string.generic_external)
          
          val metadata = remember(track) {
            mutableListOf<TrackMetadata>().apply {
              if (!track.codec.isNullOrBlank()) {
                add(TrackMetadata(track.codec, MetadataType.PRIMARY))
              }
              if (track.external == true) {
                add(TrackMetadata(externalLabel, MetadataType.WARNING))
              }
              if (!track.lang.isNullOrBlank() && track.title?.contains(track.lang, ignoreCase = true) != true) {
                add(TrackMetadata(track.lang))
              }
            }
          }

          TrackSelectableBar(
            id = track.id,
            title = getTrackTitle(track),
            isSelected = isSelected,
            onClick = { onToggleSubtitle(track.id) },
            metadata = metadata,
            trailingContent = if (track.external == true) {
              {
                IconButton(
                  onClick = { onRemoveSubtitle(track.id) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_remove_subtitle),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                  )
                }
              }
            } else null
          )
        }
        is SubtitleItem.Header -> {
          TrackHeaderPill(
            title = item.title,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
        }
      }
    },
    footer = {
      // Clean bottom padding is handled by GenericTracksSheet
    },
    modifier = modifier,
  )
}
