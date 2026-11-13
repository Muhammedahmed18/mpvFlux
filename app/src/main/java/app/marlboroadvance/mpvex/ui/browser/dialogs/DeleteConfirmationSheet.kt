package app.marlboroadvance.mpvex.ui.browser.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteConfirmationSheet(
    isOpen: Boolean,
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (deleteFiles: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var deleteFiles by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (!isOpen) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
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
                        text = stringResource(if (deleteFiles) R.string.delete_sheet_title_files else R.string.delete_sheet_title_history),
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                    )
                    Text(
                        text = pluralStringResource(R.plurals.delete_sheet_selected_count, selectedCount, selectedCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Warning Text
            Text(
                text = if (deleteFiles) {
                    stringResource(R.string.delete_sheet_warning_files)
                } else {
                    stringResource(R.string.delete_sheet_warning_history)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Checkbox Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = deleteFiles,
                    onCheckedChange = { deleteFiles = it },
                )
                Text(
                    text = stringResource(R.string.delete_sheet_also_delete_files),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            DeleteActionRow(
                confirmLabel = stringResource(if (deleteFiles) R.string.delete else R.string.remove),
                destructive = deleteFiles,
                onCancel = onDismiss,
                onConfirm = {
                    onConfirm(deleteFiles)
                    onDismiss()
                },
            )
        }
    }
}
