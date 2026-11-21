package app.marlboroadvance.mpvex.utils.media

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Simple app-wide event bus to notify screens/viewmodels when the media library changes
 * (copy/move/delete/rename, private storage move/restore, rescans, etc.).
 *
 * [changes] is the unscoped "something changed" signal. When the caller knows which
 * folders or files were affected it can pass them to [notifyChanged]; they are published on
 * [changedPaths] so listeners can refresh only those folders instead of the whole library.
 */
object MediaLibraryEvents {
  private val _changes =
    MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  val changes: SharedFlow<Unit> = _changes

  private val _changedPaths =
    MutableSharedFlow<Set<String>>(replay = 0, extraBufferCapacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

  /** Folder or video-file paths affected by a change. Only emitted when the caller supplied paths. */
  val changedPaths: SharedFlow<Set<String>> = _changedPaths

  fun notifyChanged() {
    _changes.tryEmit(Unit)
  }

  /** Same as [notifyChanged], but tells listeners which folders/files were affected. */
  fun notifyChanged(paths: Collection<String>) {
    if (paths.isNotEmpty()) _changedPaths.tryEmit(paths.toSet())
    _changes.tryEmit(Unit)
  }
}
