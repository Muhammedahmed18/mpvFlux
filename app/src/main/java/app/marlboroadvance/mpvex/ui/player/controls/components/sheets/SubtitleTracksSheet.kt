package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.player.TrackNode
import app.marlboroadvance.mpvex.ui.theme.spacing
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

sealed class SubtitleItem {
  data class Track(val node: TrackNode) : SubtitleItem()
  data class Header(val title: String) : SubtitleItem()
  object Divider : SubtitleItem()
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
  val items = remember(tracks) {
    val list = mutableListOf<SubtitleItem>()
    
    val internal = tracks.filter { it.external != true }
    val external = tracks.filter { it.external == true }
    
    if (internal.isNotEmpty() || external.isNotEmpty()) {
        if (internal.isNotEmpty()) {
          list.add(SubtitleItem.Header("Embedded Subtitles"))
          list.addAll(internal.map { SubtitleItem.Track(it) })
        }
        
        if (external.isNotEmpty()) {
          if (internal.isNotEmpty()) {
            list.add(SubtitleItem.Divider)
          }
          list.add(SubtitleItem.Header("External Subtitles"))
          list.addAll(external.map { SubtitleItem.Track(it) })
        }
    }

    list.toImmutableList()
  }

  GenericTracksSheet(
    tracks = items,
    onDismissRequest = onDismissRequest,
    header = {
      val subtitleActions = remember {
        listOf(
          TrackAction(
            label = "Add",
            icon = Icons.Default.Add,
            onClick = onAddSubtitle
          ),
          TrackAction(
            label = "Search Online",
            icon = Icons.Default.Search,
            onClick = onOpenOnlineSearch
          ),
          TrackAction(
            label = "Settings",
            icon = Icons.Default.Palette,
            onClick = onOpenSubtitleSettings
          ),
          TrackAction(
            label = "Delay",
            icon = Icons.Default.MoreTime,
            onClick = onOpenSubtitleDelay
          )
        )
      }
      TrackActionsRow(actions = subtitleActions)
    },
    track = { item ->
      when (item) {
        is SubtitleItem.Track -> {
          val track = item.node
          val isSelected = isSubtitleSelected(track.id)
          val externalLabel = stringResource(R.string.generic_external)
          
          val metadata = remember(track) {
            mutableListOf<String>().apply {
              if (!track.codec.isNullOrBlank()) add(track.codec)
              if (track.external == true) add(externalLabel)
              if (!track.lang.isNullOrBlank() && track.title?.contains(track.lang, ignoreCase = true) != true) {
                add(track.lang)
              }
            }
          }

          TrackSelectableBar(
            title = getTrackTitle(track),
            isSelected = isSelected,
            onClick = { onToggleSubtitle(track.id) },
            metadata = metadata,
            trailingContent = if (track.external == true) {
              {
                IconButton(onClick = { onRemoveSubtitle(track.id) }) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = if (isSelected) {
                      MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                      MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    }
                  )
                }
              }
            } else null
          )
        }
        is SubtitleItem.Header -> {
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
        SubtitleItem.Divider -> {
            HorizontalDivider(
              modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
      }
    },
    footer = {
      Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    },
    modifier = modifier,
  )
}
