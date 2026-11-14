package app.marlboroadvance.mpvex.ui.browser.audio

import android.content.ClipData
import android.content.Intent
import android.text.format.Formatter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.ui.browser.LocalNavigationBarHeight
import app.marlboroadvance.mpvex.ui.browser.components.BrowserTopBar
import app.marlboroadvance.mpvex.ui.browser.dialogs.DeleteConfirmationDialog
import app.marlboroadvance.mpvex.ui.browser.dialogs.RenameDialog
import app.marlboroadvance.mpvex.ui.browser.sheets.rememberAudioFolderPicker
import app.marlboroadvance.mpvex.ui.browser.states.EmptyState
import app.marlboroadvance.mpvex.ui.utils.readableContentWidth
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import app.marlboroadvance.mpvex.utils.storage.AudioFolderStore
import org.koin.compose.koinInject
import java.text.DateFormat
import java.util.Date

/** Audio tab: the files in the folder chosen for Extract audio. */
object ExtractedAudioScreen {
  @Composable
  fun Content() {
    val context = LocalContext.current
    val preferences = koinInject<BrowserPreferences>()
    val viewModel: ExtractedAudioViewModel = viewModel(
      factory = ExtractedAudioViewModel.factory(context.applicationContext as android.app.Application, preferences),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickFolder = rememberAudioFolderPicker()
    val navBarHeight = LocalNavigationBarHeight.current

    // Files can change outside the app (file manager, other apps).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
      val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var renameTarget by remember { mutableStateOf<AudioFolderStore.AudioFile?>(null) }
    var deleteTarget by remember { mutableStateOf<AudioFolderStore.AudioFile?>(null) }

    Scaffold(
      topBar = {
        BrowserTopBar(
          title = stringResource(R.string.audio_tab_title),
          subtitle = state.folderName,
          isInSelectionMode = false,
          selectedCount = 0,
          totalCount = state.files.size,
          onCancelSelection = {},
          additionalActions = {
            IconButton(onClick = pickFolder) {
              Icon(Icons.Rounded.FolderOpen, contentDescription = stringResource(R.string.pref_audio_folder_title))
            }
          },
        )
      },
    ) { padding ->
      Box(modifier = Modifier.padding(padding).fillMaxSize()) {
        when {
          state.isLoading && state.files.isEmpty() -> Unit

          state.folderUri == null || !state.hasAccess -> EmptyState(
            icon = Icons.Rounded.FolderOpen,
            title = stringResource(
              if (state.folderUri == null) R.string.audio_no_folder_title else R.string.extract_audio_folder_access_lost,
            ),
            message = stringResource(R.string.audio_no_folder_message),
            modifier = Modifier.fillMaxSize(),
            action = {
              Button(onClick = pickFolder) { Text(stringResource(R.string.extract_audio_choose_folder)) }
            },
          )

          state.files.isEmpty() -> EmptyState(
            icon = Icons.Rounded.LibraryMusic,
            title = stringResource(R.string.audio_empty_title),
            message = stringResource(R.string.audio_empty_message),
            modifier = Modifier.fillMaxSize(),
          )

          else -> LazyColumn(
            modifier = Modifier.fillMaxSize().readableContentWidth(),
            contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = navBarHeight + 32.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            items(state.files, key = { it.uri.toString() }) { file ->
              AudioFileRow(
                file = file,
                onPlay = { MediaUtils.playFile(file.uri, context, "audio_tab") },
                onShare = {
                  val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/*"
                    putExtra(Intent.EXTRA_STREAM, file.uri)
                    clipData = ClipData.newRawUri(file.name, file.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                  }
                  context.startActivity(Intent.createChooser(intent, file.name))
                },
                onRename = { renameTarget = file },
                onDelete = { deleteTarget = file },
              )
            }
          }
        }
      }
    }

    renameTarget?.let { file ->
      RenameDialog(
        isOpen = true,
        onDismiss = { renameTarget = null },
        onConfirm = { newName ->
          viewModel.rename(file, newName)
          renameTarget = null
        },
        currentName = file.baseName,
        itemType = "file",
        extension = file.extension.takeIf { it.isNotEmpty() }?.let { ".$it" },
      )
    }

    deleteTarget?.let { file ->
      DeleteConfirmationDialog(
        isOpen = true,
        onDismiss = { deleteTarget = null },
        onConfirm = {
          viewModel.delete(file)
          deleteTarget = null
        },
        itemType = "file",
        itemCount = 1,
        itemNames = listOf(file.name),
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AudioFileRow(
  file: AudioFolderStore.AudioFile,
  onPlay: () -> Unit,
  onShare: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit,
) {
  val context = LocalContext.current
  var showMenu by remember { mutableStateOf(false) }
  val supporting = remember(file) {
    listOfNotNull(
      file.extension.uppercase().takeIf { it.isNotEmpty() },
      Formatter.formatShortFileSize(context, file.size),
      file.lastModified.takeIf { it > 0 }?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) },
    ).joinToString(" · ")
  }

  ListItem(
    modifier = Modifier
      .clip(RoundedCornerShape(16.dp))
      .combinedClickable(onClick = onPlay, onLongClick = { showMenu = true }),
    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    leadingContent = {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
      }
    },
    headlineContent = {
      Text(file.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
    },
    supportingContent = { Text(supporting, maxLines = 1) },
    trailingContent = {
      Box {
        IconButton(onClick = { showMenu = true }) {
          Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
          DropdownMenuItem(
            text = { Text(stringResource(R.string.generic_share)) },
            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
            onClick = { showMenu = false; onShare() },
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.rename)) },
            leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) },
            onClick = { showMenu = false; onRename() },
          )
          DropdownMenuItem(
            text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = { showMenu = false; onDelete() },
          )
        }
      }
    },
  )
}
