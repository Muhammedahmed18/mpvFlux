package app.marlboroadvance.mpvex.utils.media

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * App-wide signal that saved playback progress (position, watched state) has changed.
 *
 * This is deliberately separate from [MediaLibraryEvents]. A progress save doesn't add,
 * remove or rename any file, so listeners only need to refresh progress bars and watched
 * badges from the database. Sending these through [MediaLibraryEvents] made every pause
 * during playback clear the folder caches and rescan storage in the background.
 */
object PlaybackProgressEvents {
  private val _changes =
    MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
  val changes: SharedFlow<Unit> = _changes

  fun notifyChanged() {
    _changes.tryEmit(Unit)
  }
}
