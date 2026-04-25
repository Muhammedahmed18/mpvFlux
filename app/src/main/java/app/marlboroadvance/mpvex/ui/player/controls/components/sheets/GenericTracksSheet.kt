package app.marlboroadvance.mpvex.ui.player.controls.components.sheets

import android.content.res.Configuration.ORIENTATION_PORTRAIT
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
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
  
  return if (!title.isNullOrBlank()) {
    title
  } else {
    if (track.type == "audio") {
      stringResource(R.string.player_sheets_chapter_title_substitute_audio, track.id)
    } else {
      stringResource(R.string.player_sheets_chapter_title_substitute_subtitle, track.id)
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
    640.dp 
  } else {
    560.dp
  }

  PlayerSheet(
    onDismissRequest = onDismissRequest,
    customMaxWidth = calculatedMaxWidth,
    surfaceColor = MaterialTheme.colorScheme.surfaceContainerLow
  ) {
    Column(
      modifier = modifier
        .fillMaxWidth()
        .padding(bottom = MaterialTheme.spacing.medium)
    ) {
      header()
      LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f, fill = false),
        contentPadding = PaddingValues(
          horizontal = MaterialTheme.spacing.medium,
          vertical = MaterialTheme.spacing.small
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp) // Tighter spacing
      ) {
        items(tracks) {
          track(it)
        }
      }
      
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
    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)
  } else {
    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f)
  }
  
  val contentColor = if (isSelected) {
    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
  } else {
    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
  }

  Surface(
    color = containerColor,
    shape = MaterialTheme.shapes.extraSmall, // More subtle than Circle
    modifier = modifier
  ) {
    Text(
      text = text, // No uppercase
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Medium,
      color = contentColor,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
  }
}

@Composable
fun TrackSelectableBar(
  id: Int,
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  metadata: List<String> = emptyList(),
  trailingContent: @Composable (RowScope.() -> Unit)? = null,
) {
  val haptic = LocalHapticFeedback.current
  val containerColor = if (isSelected) {
    MaterialTheme.colorScheme.surfaceContainerHighest
  } else {
    MaterialTheme.colorScheme.surfaceContainerLow
  }

  Surface(
    onClick = {
      haptic.performHapticFeedback(HapticFeedbackType.LongPress)
      onClick()
    },
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    color = containerColor,
  ) {
    ListItem(
      modifier = Modifier.padding(vertical = 0.dp), // Tighter density
      colors = ListItemDefaults.colors(
        containerColor = Color.Transparent,
      ),
      leadingContent = {
        Text(
          text = "#$id",
          style = MaterialTheme.typography.labelMedium,
          color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          fontWeight = FontWeight.Bold
        )
      },
      headlineContent = {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall, // 14sp
          fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
          color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )
      },
      supportingContent = if (metadata.isNotEmpty()) {
        {
          Row(
            modifier = Modifier.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            metadata.forEach { text ->
              TrackMetadataBadge(text, isSelected)
            }
          }
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
          
          AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
          ) {
            // Tonal token: Circle with checkmark
            Box(
              modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }
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
      .padding(vertical = MaterialTheme.spacing.smaller),
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
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
          )
        },
        leadingIcon = {
          Icon(
            action.icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
        },
        shape = CircleShape,
        colors = AssistChipDefaults.assistChipColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
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
      shape = MaterialTheme.shapes.medium,
      contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.medium, vertical = 6.dp)
    ) {
      Icon(
        Icons.Default.Add,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
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
