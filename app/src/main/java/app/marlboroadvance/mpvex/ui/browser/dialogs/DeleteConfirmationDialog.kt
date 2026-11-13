package app.marlboroadvance.mpvex.ui.browser.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteConfirmationDialog(
  isOpen: Boolean,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
  itemType: String,
  itemCount: Int,
  itemNames: List<String> = emptyList(),
) {
  if (!isOpen) return

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    dragHandle = { BottomSheetDefaults.DragHandle() },
  ) {
    val itemText = if (itemCount == 1) itemType else "${itemType}s"

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Title. The confirm action now lives in a labelled button row at the bottom
      // (a corner icon read as "close" and made permanent delete easy to mis-tap).
      Text(
        text = "Delete $itemCount $itemText?",
        style = MaterialTheme.typography.headlineSmallEmphasized,
      )

      // Warning Card
      Card(
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
          ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = pluralStringResource(R.plurals.delete_dialog_warning, itemCount),
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.padding(16.dp),
        )
      }

      // Item List (if any)
      if (itemNames.isNotEmpty()) {
        Card(
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            ),
          shape = MaterialTheme.shapes.extraLarge,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
          ) {
            val scrollState = rememberScrollState()

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .verticalScroll(scrollState),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              itemNames.forEachIndexed { index, name ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Start,
                ) {
                  Text(
                    text = "${index + 1}. ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                  )
                }
              }
            }
          }
        }
      }

      DeleteActionRow(
        confirmLabel = stringResource(R.string.delete),
        destructive = true,
        onCancel = onDismiss,
        onConfirm = {
          onConfirm()
          onDismiss()
        },
      )
    }
  }
}

/**
 * Cancel / confirm buttons shared by the delete sheets. The confirm button uses the error
 * colors when the action is destructive (permanent delete), primary otherwise.
 */
@Composable
internal fun DeleteActionRow(
  confirmLabel: String,
  destructive: Boolean,
  onCancel: () -> Unit,
  onConfirm: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    OutlinedButton(
      onClick = onCancel,
      modifier = Modifier
        .weight(1f)
        .height(48.dp),
    ) {
      Text(stringResource(R.string.generic_cancel))
    }
    Button(
      onClick = onConfirm,
      modifier = Modifier
        .weight(1f)
        .height(48.dp),
      colors = if (destructive) {
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
          contentColor = MaterialTheme.colorScheme.onError,
        )
      } else {
        ButtonDefaults.buttonColors()
      },
    ) {
      Text(confirmLabel)
    }
  }
}
