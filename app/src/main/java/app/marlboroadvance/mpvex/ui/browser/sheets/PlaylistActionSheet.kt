package app.marlboroadvance.mpvex.ui.browser.sheets

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.database.repository.PlaylistRepository
import kotlinx.coroutines.launch

private enum class PlaylistSheetScreen {
  Main, Create
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistActionSheet(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  repository: PlaylistRepository,
  context: android.content.Context,
  modifier: Modifier = Modifier,
) {
  var currentScreen by remember(isOpen) { mutableStateOf(PlaylistSheetScreen.Main) }
  val scope = rememberCoroutineScope()

  if (!isOpen) return

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val density = LocalDensity.current
  val ime = WindowInsets.ime

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
    modifier = modifier,
  ) {
    AnimatedContent(
      targetState = currentScreen,
      transitionSpec = {
        if (targetState != PlaylistSheetScreen.Main) {
          (slideInHorizontally { it } + fadeIn()) togetherWith
            (slideOutHorizontally { -it } + fadeOut())
        } else {
          (slideInHorizontally { -it } + fadeIn()) togetherWith
            (slideOutHorizontally { it } + fadeOut())
        }
      },
      label = "PlaylistSheetTransition"
    ) { screen ->
      when (screen) {
        PlaylistSheetScreen.Main -> {
          MainOptionsContent(
            onCreateClick = { currentScreen = PlaylistSheetScreen.Create }
          )
        }
        PlaylistSheetScreen.Create -> {
          CreatePlaylistContent(
            onBack = { currentScreen = PlaylistSheetScreen.Main },
            onConfirm = { name ->
              scope.launch {
                try {
                  repository.createPlaylist(name)
                  Toast.makeText(context, "Playlist created successfully", Toast.LENGTH_SHORT).show()
                  onDismiss()
                } catch (e: Exception) {
                  Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
              }
            }
          )
        }
      }
    }
  }
}

@Composable
private fun MainOptionsContent(
  onCreateClick: () -> Unit,
) {
  Column(
    modifier =
    Modifier
      .fillMaxWidth()
      .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = "Playlist Options",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(modifier = Modifier.height(4.dp))

    Card(
      onClick = onCreateClick,
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
      ),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Create Empty Playlist",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
          )
          Text(
            text = "Create a new blank playlist",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun CreatePlaylistContent(
  onBack: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var playlistName by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp)
      .padding(bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
      }
      
      Text(
        text = "Create Playlist",
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
          contentDescription = "Create",
          modifier = Modifier.size(32.dp)
        )
      }
    }

    OutlinedTextField(
      value = playlistName,
      onValueChange = { playlistName = it },
      label = { Text("Playlist Name") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.extraLarge,
    )
  }
}
