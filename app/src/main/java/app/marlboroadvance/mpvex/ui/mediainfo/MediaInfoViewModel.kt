package app.marlboroadvance.mpvex.ui.mediainfo

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
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

  private var uri: Uri? = null
  private var knownPath: String? = null
  private var requested = false
  private var loadJob: Job? = null

  /**
   * Starts loading [uri]. [knownPath] is the file's absolute path when the caller has it
   * (library screens); otherwise it is looked up from the uri where possible.
   * Repeated calls for the same file (e.g. after rotation) are ignored.
   */
  fun load(
    uri: Uri?,
    knownPath: String? = null,
  ) {
    if (requested && uri == this.uri) return
    requested = true
    this.uri = uri
    this.knownPath = knownPath
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
        val (fileName, filePath) =
          withContext(Dispatchers.IO) {
            displayName(app, target) to resolvePath(app, target, knownPath)
          }

        val (info, inform) =
          MediaInfoOps.getMediaInfoWithReport(app, target, fileName).getOrElse { e ->
            _state.value = MediaInfoUiState.Error(e.message)
            return@launch
          }

        val details =
          withContext(Dispatchers.Default) {
            MediaInfoFormatter.build(app, fileName, filePath, info, inform)
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
   * The file's absolute path on the device, or null when it can't be known (e.g. a file shared
   * from a cloud app). Only real filesystem paths are returned: never a content:// uri or a
   * network address, which may contain credentials.
   */
  private fun resolvePath(
    context: Context,
    uri: Uri,
    knownPath: String?,
  ): String? {
    knownPath?.trim()?.takeIf { it.startsWith("/") }?.let { return it }

    return when (uri.scheme) {
      ContentResolver.SCHEME_FILE -> uri.path?.takeIf { it.startsWith("/") }
      ContentResolver.SCHEME_CONTENT ->
        runCatching {
          // MediaStore (and many file managers) still report the path in the DATA column.
          // Providers that don't have it either return null or throw; both mean "unknown".
          @Suppress("DEPRECATION")
          val column = MediaStore.MediaColumns.DATA
          context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(column)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
          }
        }.getOrNull()?.takeIf { it.startsWith("/") }
      else -> null
    }
  }

}
