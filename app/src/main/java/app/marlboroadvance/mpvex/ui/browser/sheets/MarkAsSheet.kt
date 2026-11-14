package app.marlboroadvance.mpvex.ui.browser.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.utils.media.VideoMark
import app.marlboroadvance.mpvex.utils.media.VideoMarkOps
import kotlinx.coroutines.launch

/**
 * "Mark as" sheet for the selected videos: New, Last played, Finished or Unwatched.
 * Same header as the Delete sheet; options use the M3 Expressive segmented list style
 * (separate rounded tiles with a small gap, larger corners at the ends).
 *
 * [allFinished] greys out "Finished" when every selected video is already finished.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MarkAsSheet(
  selectedCount: Int,
  allFinished: Boolean,
  onDismiss: () -> Unit,
  onMark: (VideoMark) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    dragHandle = { BottomSheetDefaults.DragHandle() },
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 32.dp),
    ) {
      Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp)) {
        Text(
          text = stringResource(R.string.mark_as_title),
          style = MaterialTheme.typography.headlineSmallEmphasized,
        )
        Text(
          text = pluralStringResource(R.plurals.mark_as_selected_count, selectedCount, selectedCount),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      val options = listOf(
        MarkOption(VideoMark.New, R.drawable.ic_sym_new_releases, R.string.mark_as_new, R.string.mark_as_new_summary),
        MarkOption(VideoMark.LastPlayed, R.drawable.ic_sym_history, R.string.mark_as_last_played, R.string.mark_as_last_played_summary),
        MarkOption(
          VideoMark.Finished,
          R.drawable.ic_sym_check_circle_fill,
          R.string.mark_as_finished,
          if (allFinished) R.string.mark_as_finished_already else R.string.mark_as_finished_summary,
          enabled = !allFinished,
        ),
        MarkOption(VideoMark.Unwatched, R.drawable.ic_sym_remove_done, R.string.mark_as_unwatched, R.string.mark_as_unwatched_summary),
      )

      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEachIndexed { index, option ->
          MarkOptionRow(
            option = option,
            shape = segmentShape(index, options.size),
            onClick = { onMark(option.mark) },
          )
        }
      }
    }
  }
}

/**
 * Shows [MarkAsSheet] while [open] is true and runs the chosen mark on [selectedVideos].
 * Afterwards [onApplied] runs (to leave selection mode) and a snackbar offers Undo.
 */
@Composable
fun MarkAsHost(
  open: MutableState<Boolean>,
  selectedVideos: () -> List<Video>,
  allFinished: Boolean,
  snackbarHostState: SnackbarHostState,
  onApplied: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  if (!open.value) return
  val videos = selectedVideos()
  MarkAsSheet(
    selectedCount = videos.size,
    allFinished = allFinished,
    onDismiss = { open.value = false },
    onMark = { mark ->
      open.value = false
      scope.launch {
        val snapshot = VideoMarkOps.apply(videos, mark)
        onApplied()
        val message = if (snapshot == null) {
          context.getString(R.string.mark_as_failed)
        } else {
          val plural = when (mark) {
            VideoMark.New -> R.plurals.mark_as_done_new
            VideoMark.LastPlayed -> R.plurals.mark_as_done_last_played
            VideoMark.Finished -> R.plurals.mark_as_done_finished
            VideoMark.Unwatched -> R.plurals.mark_as_done_unwatched
          }
          context.resources.getQuantityString(plural, videos.size, videos.size)
        }
        snackbarHostState.currentSnackbarData?.dismiss()
        val result = snackbarHostState.showSnackbar(
          message = message,
          actionLabel = if (snapshot != null) context.getString(R.string.mark_as_undo) else null,
          duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed && snapshot != null) {
          VideoMarkOps.undo(snapshot)
        }
      }
    },
  )
}

private class MarkOption(
  val mark: VideoMark,
  val icon: Int,
  val title: Int,
  val summary: Int,
  val enabled: Boolean = true,
)

/** Larger outer corners on the first and last tile, small corners between tiles. */
private fun segmentShape(index: Int, count: Int): RoundedCornerShape {
  val outer = 20.dp
  val inner = 4.dp
  return RoundedCornerShape(
    topStart = if (index == 0) outer else inner,
    topEnd = if (index == 0) outer else inner,
    bottomStart = if (index == count - 1) outer else inner,
    bottomEnd = if (index == count - 1) outer else inner,
  )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MarkOptionRow(
  option: MarkOption,
  shape: RoundedCornerShape,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    enabled = option.enabled,
    shape = shape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .padding(horizontal = 16.dp, vertical = 14.dp)
        .alpha(if (option.enabled) 1f else 0.38f),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = ImageVector.vectorResource(option.icon),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
          )
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(option.title),
          style = MaterialTheme.typography.titleMediumEmphasized,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = stringResource(option.summary),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
