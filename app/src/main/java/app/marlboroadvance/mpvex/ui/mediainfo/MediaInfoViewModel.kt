package app.marlboroadvance.mpvex.ui.mediainfo

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Size
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.utils.media.MediaInfoOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads the media info once and keeps it across rotation (the old screen re-read the whole
 * file on every configuration change).
 */
internal class MediaInfoViewModel(application: Application) : AndroidViewModel(application) {

  private val _state = MutableStateFlow<MediaInfoUiState>(MediaInfoUiState.Loading)
  val state: StateFlow<MediaInfoUiState> = _state.asStateFlow()

  private val _thumbnail = MutableStateFlow<Bitmap?>(null)
  val thumbnail: StateFlow<Bitmap?> = _thumbnail.asStateFlow()

  private var uri: Uri? = null
  private var requested = false
  private var loadJob: Job? = null

  /** Starts loading [uri]. Repeated calls for the same file (e.g. after rotation) are ignored. */
  fun load(uri: Uri?) {
    if (requested && uri == this.uri) return
    requested = true
    this.uri = uri
    _thumbnail.value = null
    reload()
  }

  fun retry() = reload()

  private fun reload() {
    val app = getApplication<Application>()
    val target = uri
    loadJob?.cancel()

    if (target == null) {
      _state.value = MediaInfoUiState.Error(app.getString(R.string.media_info_error_no_file))
      return
    }

    _state.value = MediaInfoUiState.Loading
    loadJob =
      viewModelScope.launch {
        val fileName = withContext(Dispatchers.IO) { displayName(app, target) }

        if (_thumbnail.value == null) {
          launch { _thumbnail.value = withContext(Dispatchers.IO) { loadThumbnail(app, target) } }
        }

        val (info, inform) =
          MediaInfoOps.getMediaInfoWithReport(app, target, fileName).getOrElse { e ->
            _state.value = MediaInfoUiState.Error(e.message)
            return@launch
          }

        val details =
          withContext(Dispatchers.Default) {
            MediaInfoFormatter.build(app, fileName, info, inform)
          }
        _state.value = MediaInfoUiState.Loaded(details)
      }
  }

  private fun displayName(context: Context, uri: Uri): String {
    val fallback = uri.lastPathSegment?.substringAfterLast('/') ?: "Unknown"
    return runCatching {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
      }
    }.getOrNull() ?: fallback
  }

  /**
   * A frame for the header. MediaStore's own thumbnail is used when available (cheap and
   * cached by the system); otherwise one frame is decoded at 10% of the duration.
   */
  private fun loadThumbnail(context: Context, uri: Uri): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.scheme == "content") {
      runCatching {
        context.contentResolver.loadThumbnail(uri, Size(THUMB_WIDTH, THUMB_HEIGHT), null)
      }.getOrNull()?.let { return it }
    }

    val retriever = MediaMetadataRetriever()
    return try {
      retriever.setDataSource(context, uri)
      val durationMs =
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
      val timeUs = durationMs * 1000L / 10L
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        retriever.getScaledFrameAtTime(
          timeUs,
          MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
          THUMB_WIDTH,
          THUMB_HEIGHT,
        )
      } else {
        retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
      }
    } catch (_: Exception) {
      null
    } finally {
      runCatching { retriever.release() }
    }
  }

  private companion object {
    const val THUMB_WIDTH = 960
    const val THUMB_HEIGHT = 540
  }
}
