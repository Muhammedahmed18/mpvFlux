package app.marlboroadvance.mpvex.ui.browser.selection

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.ui.browser.components.ActionFeedbackState
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Manager for handling item selection and operations in browser screens
 */
@Stable
class SelectionManager<T, ID>(
  private val items: () -> List<T>,
  private val getId: (T) -> ID,
  private val context: Context,
  private val scope: CoroutineScope,
  private val onDeleteItems: suspend (List<T>, Boolean) -> Pair<Int, Int>,
  private val onRenameItem: (suspend (T, String) -> Result<Unit>)?,
  private val onOperationComplete: () -> Unit,
  private val feedback: ActionFeedbackState,
  @PluralsRes private val deletedMessage: Int = R.plurals.delete_result_items,
) {
  var state by mutableStateOf(SelectionState<ID>())
    private set

  val isInSelectionMode: Boolean
    get() = state.isInSelectionMode

  val selectedCount: Int
    get() = state.selectedCount

  val isSingleSelection: Boolean
    get() = state.isSingleSelection

  /**
   * Toggle selection of an item
   */
  fun toggle(item: T) {
    state = state.toggle(getId(item))
  }

  /**
   * Clear all selections
   */
  fun clear() {
    state = state.clear()
  }

  /**
   * Select all items
   */
  fun selectAll() {
    state = state.selectAll(items().map(getId))
  }

  /**
   * Invert selection
   */
  fun invertSelection() {
    state = state.invertSelection(items().map(getId))
  }

  /**
   * Check if an item is selected
   */
  fun isSelected(item: T): Boolean = state.isSelected(getId(item))

  /**
   * Get currently selected items
   */
  fun getSelectedItems(): List<T> = state.getSelected(items(), getId)

  /**
   * Delete selected items directly (using MANAGE_EXTERNAL_STORAGE permission)
   */
  fun deleteSelected(deleteFiles: Boolean = false) {
    val selected = getSelectedItems()
    if (selected.isEmpty()) return

    scope.launch {
      runCatching {
        val (deleted, failed) = onDeleteItems(selected, deleteFiles)
        // One message per batch, e.g. "12 videos deleted" or "10 videos deleted · 2 failed".
        // Deleting is not reversible, so there is no Undo.
        val message =
          when {
            deleted > 0 && failed > 0 ->
              context.getString(
                R.string.delete_result_with_failures,
                context.resources.getQuantityString(deletedMessage, deleted, deleted),
                failed,
              )
            deleted > 0 -> context.resources.getQuantityString(deletedMessage, deleted, deleted)
            failed > 0 -> context.getString(R.string.delete_result_failed)
            else -> null
          }
        message?.let { feedback.show(it, if (deleted > 0) Icons.Rounded.Delete else Icons.Rounded.ErrorOutline, isError = deleted == 0) }
      }.onFailure {
        feedback.show(context.getString(R.string.delete_result_failed), Icons.Rounded.ErrorOutline, isError = true)
      }
      clear()
      onOperationComplete()
    }
  }

  /**
   * Rename the selected item (only works with single selection)
   */
  fun renameSelected(newName: String) {
    if (!isSingleSelection || onRenameItem == null) return

    val item = getSelectedItems().firstOrNull() ?: return

    scope.launch {
      runCatching {
        val result = onRenameItem(item, newName)
        result.onSuccess {
          feedback.show(context.getString(R.string.rename_success), Icons.Rounded.DriveFileRenameOutline)
        }.onFailure {
          feedback.show(context.getString(R.string.rename_failed), Icons.Rounded.ErrorOutline, isError = true)
        }
      }.onFailure {
        feedback.show(context.getString(R.string.rename_failed), Icons.Rounded.ErrorOutline, isError = true)
      }
      clear()
      onOperationComplete()
    }
  }

  /**
   * Share selected items (only for videos)
   */
  fun shareSelected() {
    val selected = getSelectedItems()
    if (selected.isEmpty() || selected.first() !is Video) return

    @Suppress("UNCHECKED_CAST")
    val videos = selected as List<Video>
    MediaUtils.shareVideos(context, videos)
  }

  /**
   * Play selected items as a playlist (only for videos)
   */
  fun playSelected() {
    val selected = getSelectedItems()
    if (selected.isEmpty() || selected.first() !is Video) return

    @Suppress("UNCHECKED_CAST")
    val videos = selected as List<Video>

    playVideosAsQueue(context, videos)

    // Clear selection after starting playback
    clear()
  }
}

/**
 * Composable function to remember a SelectionManager
 *
 * @param items List of items to manage selection for
 * @param getId Function to extract ID from an item
 * @param onDeleteItems Callback to delete items (includes boolean to delete original files)
 * @param onRenameItem Optional callback to rename an item
 * @param onOperationComplete Callback when an operation completes (to refresh list)
 * @param feedback Shared feedback state used for delete/rename results
 * @param deletedMessage Plural for the "N deleted" message, e.g. [R.plurals.delete_result_videos]
 */
@Composable
fun <T, ID> rememberSelectionManager(
  items: List<T>,
  getId: (T) -> ID,
  onDeleteItems: suspend (List<T>, Boolean) -> Pair<Int, Int>,
  onRenameItem: (suspend (T, String) -> Result<Unit>)? = null,
  onOperationComplete: () -> Unit = {},
  feedback: ActionFeedbackState,
  @PluralsRes deletedMessage: Int = R.plurals.delete_result_items,
): SelectionManager<T, ID> {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  return remember(items, getId, onDeleteItems, onRenameItem) {
    SelectionManager(
      items = { items },
      getId = getId,
      context = context,
      scope = scope,
      onDeleteItems = onDeleteItems,
      onRenameItem = onRenameItem,
      onOperationComplete = onOperationComplete,
      feedback = feedback,
      deletedMessage = deletedMessage,
    )
  }
}
