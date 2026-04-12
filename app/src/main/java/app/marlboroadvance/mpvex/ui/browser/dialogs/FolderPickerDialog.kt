package app.marlboroadvance.mpvex.ui.browser.dialogs

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.utils.storage.StorageVolumeUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPickerDialog(
  modifier: Modifier = Modifier,
  isOpen: Boolean,
  currentPath: String = Environment.getExternalStorageDirectory().absolutePath,
  titlePrefix: String = "Move to",
  onDismiss: () -> Unit,
  onFolderSelected: (String) -> Unit,
) {
  if (!isOpen) return

  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  
  // Get all available storage volumes
  val storageVolumes = remember(isOpen) {
    StorageVolumeUtils.getAllStorageVolumes(context)
  }
  
  var selectedPath by remember(isOpen, storageVolumes) {
    val initialPath = if (storageVolumes.size == 1) {
      StorageVolumeUtils.getVolumePath(storageVolumes.first())
    } else {
      null 
    }
    mutableStateOf(initialPath)
  }
  var showCreateFolderDialog by remember { mutableStateOf(false) }

  val showStorageRoot = selectedPath == null
  val currentDir = remember(selectedPath) { selectedPath?.let { File(it) } }
  
  val folders = remember(selectedPath) {
    if (showStorageRoot) {
      emptyList<File>()
    } else {
      currentDir?.listFiles { file -> file.isDirectory && !file.name.startsWith(".") }
        ?.sortedBy { it.name.lowercase() }
        ?: emptyList()
    }
  }

  val isSameAsSource = remember(selectedPath, currentPath) {
    selectedPath != null && selectedPath == currentPath
  }

  val displayName = remember(selectedPath, storageVolumes) {
    val volume = storageVolumes.find { StorageVolumeUtils.getVolumePath(it) == selectedPath }
    if (volume != null) {
      volume.getDescription(context)
    } else {
      selectedPath?.let { File(it).name } ?: "Select a storage location"
    }
  }

  if (showCreateFolderDialog && selectedPath != null) {
    CreateFolderDialog(
      parentPath = selectedPath!!,
      onDismiss = { showCreateFolderDialog = false },
      onFolderCreated = { newFolderPath ->
        selectedPath = newFolderPath
        showCreateFolderDialog = false
      },
    )
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    @Suppress("DEPRECATION")
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      // Modern Header Section
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "$titlePrefix:",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            modifier = Modifier.basicMarquee()
          )
        }

        IconButton(
          onClick = { selectedPath?.let { onFolderSelected(it) } },
          enabled = selectedPath != null && !isSameAsSource,
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
          modifier = Modifier.size(56.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Confirm Selection",
            modifier = Modifier.size(32.dp)
          )
        }
      }

      if (isSameAsSource) {
        Text(
          text = "Cannot select the same folder",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.error,
        )
      }

      // Modern Navigation Icon Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (selectedPath != null) {
          FilledTonalIconButton(
            onClick = { selectedPath = currentDir?.parent },
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
            )
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
          }
        }

        FilledTonalIconButton(
          onClick = { selectedPath = Environment.getExternalStorageDirectory().absolutePath },
          modifier = Modifier.size(48.dp),
          colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
          )
        ) {
          Icon(Icons.Default.Home, "Home")
        }

        FilledTonalIconButton(
          onClick = { showCreateFolderDialog = true },
          enabled = selectedPath != null,
          modifier = Modifier.size(48.dp),
          colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
          )
        ) {
          Icon(Icons.Default.CreateNewFolder, "New Folder")
        }
      }

      // Folder/Volume list
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(300.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (showStorageRoot) {
          items(storageVolumes) { volume ->
            val volumePath = StorageVolumeUtils.getVolumePath(volume)
            if (volumePath != null) {
              StorageVolumeItem(
                context = context,
                volume = volume,
                volumePath = volumePath,
                onClick = { selectedPath = volumePath },
              )
            }
          }
        } else {
          items(folders) { folder ->
            FolderItem(
              folder = folder,
              onClick = { selectedPath = folder.absolutePath },
            )
          }
          if (folders.isEmpty()) {
            item {
              Text(
                text = "No subfolders",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun StorageVolumeItem(
  context: Context,
  volume: android.os.storage.StorageVolume,
  volumePath: String,
  onClick: () -> Unit,
) {
  val description = volume.getDescription(context)
  val isPrimary = volume.isPrimary
  val isRemovable = volume.isRemovable
  
  val icon = when {
    isPrimary -> Icons.Default.Home
    isRemovable && volumePath.contains("usb", ignoreCase = true) -> Icons.Default.Usb
    isRemovable -> Icons.Default.SdCard
    else -> Icons.Default.Folder
  }
  
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(32.dp),
    )
    Column {
      Text(
        text = description,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = volumePath,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun FolderItem(
  folder: File,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = Icons.Default.Folder,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(28.dp),
    )
    Text(
      text = folder.name,
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.Medium,
    )
  }
}

@Composable
private fun CreateFolderDialog(
  parentPath: String,
  onDismiss: () -> Unit,
  onFolderCreated: (String) -> Unit,
) {
  var folderName by remember { mutableStateOf("") }
  var error by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create New Folder", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = folderName,
          onValueChange = {
            folderName = it
            error = null
          },
          label = { Text("Folder name") },
          singleLine = true,
          isError = error != null,
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.extraLarge,
        )
        if (error != null) {
          Text(
            text = error!!,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (folderName.isBlank()) return@Button
          val newFolder = File(parentPath, folderName)
          if (newFolder.exists()) {
            error = "Folder already exists"
            return@Button
          }
          if (newFolder.mkdirs()) onFolderCreated(newFolder.absolutePath)
          else error = "Failed to create folder"
        },
        shape = MaterialTheme.shapes.extraLarge,
      ) {
        Text("Create", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
    shape = MaterialTheme.shapes.extraLarge,
  )
}
