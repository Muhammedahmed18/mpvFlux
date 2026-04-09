package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.presentation.components.PlayerSheet
import app.marlboroadvance.mpvex.ui.player.TrackNode
import app.marlboroadvance.mpvex.ui.theme.spacing
import kotlinx.collections.immutable.ImmutableList

@Composable
fun getTrackTitle(track: TrackNode): String {
  val title = if (track.external == true) {
    track.title?.substringBeforeLast(".")
  } else {
    track.title
  }
  val lang = track.lang?.uppercase()
  val id = track.id

  return when {
    !title.isNullOrBlank() && !lang.isNullOrBlank() -> {
      stringResource(R.string.player_sheets_track_title_w_lang, id, title, lang)
    }
    !title.isNullOrBlank() -> {
      stringResource(R.string.player_sheets_track_title_wo_lang, id, title)
    }
    !lang.isNullOrBlank() -> {
      stringResource(R.string.player_sheets_track_lang_wo_title, id, lang)
    }
    else -> {
      val fallbackRes = if (track.type == "audio") {
        R.string.player_sheets_chapter_title_substitute_audio
      } else {
        R.string.player_sheets_chapter_title_substitute_subtitle
      }
      stringResource(fallbackRes, id)
    }
  }
}

@Composable
fun <T> GenericTracksSheet(
  tracks: ImmutableList<T>,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  lazyListState: LazyListState? = null,
  customMaxWidth: androidx.compose.ui.unit.Dp? = null,
  header: @Composable () -> Unit = {},
  track: @Composable (T) -> Unit = {},
  footer: @Composable () -> Unit = {},
) {
  val listState = lazyListState ?: rememberLazyListState()
  val configuration = LocalConfiguration.current
  
  val calculatedMaxWidth = customMaxWidth ?: if (configuration.orientation == ORIENTATION_PORTRAIT) {
    2000.dp 
  } else {
    640.dp
  }

  PlayerSheet(onDismissRequest, customMaxWidth = calculatedMaxWidth) {
    Column(
      modifier = modifier
        .fillMaxWidth()
        .padding(bottom = MaterialTheme.spacing.medium)
    ) {
      header()
      LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f, fill = false),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(tracks) {
          track(it)
        }
      }
      Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = MaterialTheme.spacing.medium)
      ) {
        footer()
      }
    }
  }
}

@Composable
fun TrackMetadataBadge(
  text: String,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
) {
  val containerColor = if (isSelected) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
  } else {
    MaterialTheme.colorScheme.surfaceVariant
  }
  
  val contentColor = if (isSelected) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.onSurfaceVariant
  }

  Surface(
    color = containerColor,
    shape = RoundedCornerShape(4.dp),
    modifier = modifier
  ) {
    Text(
      text = text.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = contentColor,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}

@Composable
fun TrackSelectableBar(
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  metadata: List<String> = emptyList(),
  trailingContent: @Composable (RowScope.() -> Unit)? = null,
) {
  val containerColor = if (isSelected) {
    MaterialTheme.colorScheme.primaryContainer
  } else {
    Color.Transparent
  }

  Surface(
    onClick = onClick,
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp)),
    shape = RoundedCornerShape(12.dp),
    color = containerColor,
  ) {
    ListItem(
      colors = ListItemDefaults.colors(
        containerColor = Color.Transparent,
      ),
      headlineContent = {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
      },
      supportingContent = if (metadata.isNotEmpty()) {
        {
          Text(
            text = metadata.joinToString(" • "),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) {
              MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            }
          )
        }
      } else null,
      trailingContent = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          if (trailingContent != null) {
            trailingContent()
          }
          RadioButton(
            selected = isSelected,
            onClick = null // Handled by the surface click
          )
        }
      }
    )
  }
}

data class TrackAction(
  val label: String,
  val icon: ImageVector,
  val onClick: () -> Unit,
)

@Composable
fun TrackActionsRow(
  actions: List<TrackAction>,
  modifier: Modifier = Modifier,
) {
  LazyRow(
    modifier = modifier
      .fillMaxWidth()
      .padding(bottom = MaterialTheme.spacing.small),
    contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    items(actions) { action ->
      AssistChip(
        onClick = action.onClick,
        label = {
          Text(
            text = action.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
          )
        },
        leadingIcon = {
          Icon(
            action.icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
        },
        shape = RoundedCornerShape(16.dp),
        colors = AssistChipDefaults.assistChipColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          labelColor = MaterialTheme.colorScheme.onSurface,
          leadingIconContentColor = MaterialTheme.colorScheme.primary,
        ),
        border = null,
      )
    }
  }
}

@Composable
fun AddTrackRow(
  title: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  actions: @Composable RowScope.() -> Unit = {},
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(MaterialTheme.spacing.medium),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
  ) {
    FilledTonalButton(
      onClick = onClick,
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(12.dp),
      contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium, vertical = 8.dp)
    ) {
      Icon(
        Icons.Default.Add,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.size(MaterialTheme.spacing.small))
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
      )
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.smaller),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      actions()
    }
  }
}
