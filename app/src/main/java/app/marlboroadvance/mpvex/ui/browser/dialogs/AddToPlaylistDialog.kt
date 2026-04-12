package app.marlboroadvance.mpvex.ui.browser.dialogs

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.database.entities.PlaylistEntity
import app.marlboroadvance.mpvex.database.repository.PlaylistRepository
import app.marlboroadvance.mpvex.domain.media.model.Video
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
  isOpen: Boolean,
  videos: List<Video>,
  onDismiss: () -> Unit,
  onSuccess: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val repository = koinInject<PlaylistRepository>()
  val playlistsFromDb by repository.observeAllPlaylists().collectAsState(initial = emptyList())
  val playlists = remember(playlistsFromDb) {
    playlistsFromDb.sortedBy { it.name.lowercase() }
  }
  val scope = rememberCoroutineScope()
  var showCreateDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

  if (!isOpen) return

  if (showCreateDialog) {
    CreatePlaylistDialog(
      onDismiss = { showCreateDialog = false },
      onConfirm = { name ->
        scope.launch {
          val playlistId = repository.createPlaylist(name)
          val items = videos.map { video ->
            video.path to video.displayName
          }
          repository.addItemsToPlaylist(playlistId.toInt(), items)
          val message = if (videos.size == 1) {
            "Video added to \"$name\""
          } else {
            "${videos.size} videos added to \"$name\""
          }
          Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
          showCreateDialog = false
          onSuccess()
          onDismiss()
        }
      }
    )
    return
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Header Section
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Add to Playlist",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = if (videos.size == 1) {
              "Adding 1 video"
            } else {
              "Adding ${videos.size} videos"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Create new playlist button
      OutlinedButton(
        onClick = { showCreateDialog = true },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
      ) {
        Icon(
          imageVector = Icons.Filled.Add,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Create New Playlist",
          fontWeight = FontWeight.Medium,
        )
      }

      // Existing playlists
      if (playlists.isNotEmpty()) {
        Text(
          text = "Existing Playlists",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )

        LazyColumn(
          modifier = Modifier.heightIn(max = 400.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(vertical = 4.dp),
        ) {
          items(playlists, key = { it.id }) { playlist ->
            PlaylistItemCard(
              playlist = playlist,
              repository = repository,
              onClick = {
                scope.launch {
                  val items = videos.map { video ->
                    video.path to video.displayName
                  }
                  repository.addItemsToPlaylist(playlist.id, items)
                  val message = if (videos.size == 1) {
                    "Video added to \"${playlist.name}\""
                  } else {
                    "${videos.size} videos added to \"${playlist.name}\""
                  }
                  Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                  onSuccess()
                  onDismiss()
                }
              },
            )
          }
        }
      } else {
        EmptyPlaylistsMessage()
      }
    }
  }
}

@Composable
private fun PlaylistItemCard(
  playlist: PlaylistEntity,
  repository: PlaylistRepository,
  onClick: () -> Unit,
) {
  val itemCount by repository.observePlaylistItemCount(playlist.id).collectAsState(initial = 0)

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
        contentDescription = null,
        modifier = Modifier.size(40.dp),
        tint = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = playlist.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "$itemCount videos • ${formatDate(playlist.updatedAt)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun EmptyPlaylistsMessage() {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = "No playlists yet",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = "Create your first playlist above",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePlaylistDialog(
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var playlistName by remember { mutableStateOf("") }
  
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val density = LocalDensity.current
  val ime = WindowInsets.ime

  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true,
    confirmValueChange = { targetValue ->
      if (targetValue == SheetValue.Hidden) {
        val isKeyboardVisible = ime.getBottom(density) > 0
        if (isKeyboardVisible) {
          focusManager.clearFocus()
          keyboardController?.hide()
          false
        } else {
          true
        }
      } else {
        true
      }
    }
  )

  ModalBottomSheet(
    onDismissRequest = {
      val isKeyboardVisible = ime.getBottom(density) > 0
      if (isKeyboardVisible) {
        focusManager.clearFocus()
        keyboardController?.hide()
      } else {
        onDismiss()
      }
    },
    sheetState = sheetState,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Header Section
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Create New Playlist",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f)
        )

        IconButton(
          onClick = {
            if (playlistName.isNotBlank()) {
              onConfirm(playlistName.trim())
            }
          },
          enabled = playlistName.isNotBlank(),
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
          ),
          modifier = Modifier.size(56.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Create Playlist",
            modifier = Modifier.size(32.dp)
          )
        }
      }

      OutlinedTextField(
        value = playlistName,
        onValueChange = { playlistName = it },
        label = { Text("Playlist Name", fontWeight = FontWeight.Medium) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
      )
    }
  }
}

private fun formatDate(timestamp: Long): String {
  val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
  return sdf.format(Date(timestamp))
}
