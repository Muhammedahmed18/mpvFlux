package app.marlboroadvance.mpvex.ui.browser.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R

/**
 * Asks whether the selection should be copied or moved before the destination is picked.
 * This replaces the two look-alike Copy / Move buttons the old floating bar had.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyOrMoveSheet(
  isOpen: Boolean,
  itemCount: Int,
  onDismiss: () -> Unit,
  onCopy: () -> Unit,
  onMove: () -> Unit,
) {
  if (!isOpen) return
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
      Text(
        text = pluralStringResource(R.plurals.selection_send_to_folder_title, itemCount, itemCount),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
      )
      ListItem(
        headlineContent = { Text(stringResource(R.string.copy_to)) },
        supportingContent = { Text(stringResource(R.string.selection_copy_summary)) },
        leadingContent = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.clickable { onDismiss(); onCopy() },
      )
      ListItem(
        headlineContent = { Text(stringResource(R.string.move_to)) },
        supportingContent = { Text(stringResource(R.string.selection_move_summary)) },
        leadingContent = { Icon(Icons.AutoMirrored.Rounded.DriveFileMove, contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.clickable { onDismiss(); onMove() },
      )
    }
  }
}
