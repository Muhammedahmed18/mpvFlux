package app.marlboroadvance.mpvex.ui.browser.dialogs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Live progress of an in-flight folder deletion. [total] is every regular file across all the
 * selected folders (the true unit of work, so the bar reaches 100% exactly when the last file
 * is gone); [completed] ticks up per file; [currentName] is the file currently being removed.
 */
data class DeleteProgress(
  val total: Int,
  val completed: Int,
  val currentName: String = "",
)

/**
 * Non-dismissable Material 3 dialog shown while a folder is being deleted file-by-file. It
 * reports "X / N deleted" with a determinate bar and the current filename, and closes
 * automatically when [progress] becomes null. The Cancel button stops the deletion in place
 * (already-removed files stay removed; their thumbnails are still purged).
 */
@Composable
fun DeleteProgressDialog(
  progress: DeleteProgress?,
  onCancel: () -> Unit,
) {
  if (progress == null) return

  val fraction = if (progress.total > 0) {
    progress.completed.toFloat() / progress.total
  } else {
    0f
  }
  val animatedFraction by animateFloatAsState(targetValue = fraction, label = "deleteProgress")

  AlertDialog(
    onDismissRequest = { /* non-dismissable: deletion must finish or be cancelled */ },
    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    icon = {
      Icon(
        imageVector = Icons.Rounded.DeleteSweep,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Text(text = "Deleting folder")
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(verticalAlignment = Alignment.Bottom) {
          Text(
            text = progress.completed.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = " / ${progress.total} deleted",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        LinearProgressIndicator(
          progress = { animatedFraction },
          modifier = Modifier.fillMaxWidth(),
        )

        if (progress.currentName.isNotBlank()) {
          Text(
            text = progress.currentName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onCancel) {
        Text(text = "Cancel")
      }
    },
  )
}
