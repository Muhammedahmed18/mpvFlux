package app.marlboroadvance.mpvex.ui.browser.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.utils.media.CopyPasteOps

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FileOperationProgressDialog(
  isOpen: Boolean,
  operationType: CopyPasteOps.OperationType,
  progress: CopyPasteOps.FileOperationProgress,
  onCancel: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!isOpen) return

  val isOperationComplete = progress.isComplete || progress.isCancelled || progress.error != null

  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true,
    confirmValueChange = { isOperationComplete }
  )

  val operationName = when (operationType) {
    is CopyPasteOps.OperationType.Copy -> "Copying"
    is CopyPasteOps.OperationType.Move -> "Moving"
  }

  ModalBottomSheet(
    onDismissRequest = { if (isOperationComplete) onDismiss() },
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
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      // Header Section
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "$operationName files",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
          )
          if (!isOperationComplete) {
            Text(
              text = "File ${progress.currentFileIndex} of ${progress.totalFiles}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Action Button: Cancel (during) or Done/Close (after)
        if (!isOperationComplete) {
          IconButton(
            onClick = onCancel,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Cancel",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          val isSuccess = progress.isComplete && progress.error == null && !progress.isCancelled
          
          if (isSuccess) {
            AnimatedVisibility(
              visible = true,
              enter = fadeIn() + scaleIn(animationSpec = tween(300)),
              exit = fadeOut() + scaleOut()
            ) {
              IconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.filledIconButtonColors(
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                  contentColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.size(56.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Done",
                  modifier = Modifier.size(32.dp)
                )
              }
            }
          } else {
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(48.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      // Progress Content
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        when {
          progress.error != null -> {
            StatusCard(
              message = progress.error,
              containerColor = MaterialTheme.colorScheme.errorContainer,
              contentColor = MaterialTheme.colorScheme.onErrorContainer,
              icon = Icons.Default.Error
            )
          }
          progress.isCancelled -> {
            StatusCard(
              message = "Operation cancelled",
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
              icon = Icons.Default.Info
            )
          }
          progress.isComplete -> {
            StatusCard(
              message = "Operation completed successfully!",
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              icon = Icons.Default.CheckCircle
            )
          }
          else -> {
            // Current File Name
            Text(
              text = progress.currentFile,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )

            // Overall Progress Section - Refactored for Draw-phase updates
            ProgressSection(
              label = "Overall Progress",
              progressProvider = { progress.overallProgress },
              detailsProvider = { "${CopyPasteOps.formatBytes(progress.bytesProcessed)} / ${CopyPasteOps.formatBytes(progress.totalBytes)}" }
            )
          }
        }

        if (isOperationComplete) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryRow(
              label = "Total Files",
              value = progress.totalFiles.toString(),
            )
            SummaryRow(
              label = "Total size",
              value = CopyPasteOps.formatBytes(progress.totalBytes),
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingDialog(
  isOpen: Boolean,
  message: String = "Loading...",
  onDismissRequest: () -> Unit = {},
) {
  if (!isOpen) return

  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    dragHandle = { BottomSheetDefaults.DragHandle() },
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp)
        .padding(bottom = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      CircularProgressIndicator(
        modifier = Modifier.size(48.dp),
        color = MaterialTheme.colorScheme.primary,
        strokeWidth = 4.dp
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun StatusCard(
  message: String,
  containerColor: Color,
  contentColor: Color,
  icon: ImageVector? = null
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = containerColor),
    shape = MaterialTheme.shapes.extraLarge,
  ) {
    Row(
      modifier = Modifier.padding(20.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      if (icon != null) {
        Icon(icon, null, tint = contentColor)
      }
      Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = contentColor,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProgressSection(
  label: String,
  progressProvider: () -> Float,
  detailsProvider: () -> String
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
        // Wrapped in remember derived state to avoid frequent label recompositions
        val percentageText by remember { derivedStateOf { "${(progressProvider() * 100).toInt().coerceIn(0, 100)}%" } }
        Text(
          text = percentageText,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
          fontWeight = FontWeight.SemiBold
        )
      }
      val detailsText by remember { derivedStateOf { detailsProvider() } }
      Text(
        text = detailsText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    
    LinearWavyProgressIndicator(
      modifier = Modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.primary,
      trackColor = MaterialTheme.colorScheme.surfaceVariant,
      stroke = WavyProgressIndicatorDefaults.linearIndicatorStroke,
      trackStroke = WavyProgressIndicatorDefaults.linearTrackStroke,
      amplitude = { 0.5f },
      progress = progressProvider,
    )
  }
}

@Composable
private fun SummaryRow(
  label: String,
  value: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.Bold,
    )
  }
}
