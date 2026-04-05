package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
  
  // Ensure the sheet is full-width in portrait to avoid the "centered pillar" look
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
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
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
    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
  } else {
    MaterialTheme.colorScheme.surfaceContainerHighest
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

@OptIn(ExperimentalLayoutApi::class)
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
    MaterialTheme.colorScheme.surfaceContainerLow
  }

  val contentColor = if (isSelected) {
    MaterialTheme.colorScheme.onPrimaryContainer
  } else {
    MaterialTheme.colorScheme.onSurface
  }

  Surface(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    color = containerColor,
    contentColor = contentColor,
  ) {
    Row(
      modifier = Modifier
        .padding(vertical = MaterialTheme.spacing.small),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Selection Indicator Bar
      Box(
        modifier = Modifier
          .width(4.dp)
          .height(32.dp)
          .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
          .background(
            if (isSelected) MaterialTheme.colorScheme.primary 
            else Color.Transparent
          )
      )

      Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
        if (metadata.isNotEmpty()) {
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            metadata.forEach { 
               TrackMetadataBadge(text = it, isSelected = isSelected)
            }
          }
        }
      }

      if (trailingContent != null || isSelected) {
        Row(
          modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
        ) {
          if (trailingContent != null) {
            trailingContent()
          }
          if (isSelected) {
            Icon(
              Icons.Default.Check,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }
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
      .padding(MaterialTheme.spacing.medium),
    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    items(actions) { action ->
      AssistChip(
        onClick = action.onClick,
        label = {
          Text(
            text = action.label,
            style = MaterialTheme.typography.labelLarge,
          )
        },
        leadingIcon = {
          Icon(
            action.icon,
            contentDescription = null,
            modifier = Modifier.size(AssistChipDefaults.IconSize),
          )
        },
        shape = CircleShape,
        colors = AssistChipDefaults.assistChipColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          labelColor = MaterialTheme.colorScheme.onSurface,
          leadingIconContentColor = MaterialTheme.colorScheme.onSurface,
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

/**
 * Get a displayable title for a track node.
 * Uses title, language, or a default substitute.
 */
@Composable
fun getTrackTitle(
  track: TrackNode,
): String {
  // Handle external subtitles
  if (track.isSubtitle && track.external == true && track.externalFilename != null) {
    val decoded = Uri.decode(track.externalFilename)
    val fileName = decoded.substringAfterLast("/")
    return stringResource(R.string.player_sheets_track_title_wo_lang, track.id, fileName)
  }

  // Build title from available metadata
  val hasTitle = !track.title.isNullOrBlank()
  val hasLang = !track.lang.isNullOrBlank()

  return when {
    hasTitle && hasLang ->
      stringResource(
        R.string.player_sheets_track_title_w_lang,
        track.id,
        track.title,
        track.lang,
      )
    hasTitle -> stringResource(R.string.player_sheets_track_title_wo_lang, track.id, track.title)
    hasLang -> stringResource(R.string.player_sheets_track_lang_wo_title, track.id, track.lang)
    track.isSubtitle -> stringResource(R.string.player_sheets_chapter_title_substitute_subtitle, track.id)
    track.isAudio -> stringResource(R.string.player_sheets_chapter_title_substitute_audio, track.id)
    else -> ""
  }
}
