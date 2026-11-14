package app.marlboroadvance.mpvex.utils.media

import android.content.Context
import android.net.Uri
import android.util.Log
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.utils.storage.AudioFolderStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs one audio extraction at a time in an app-wide scope, so it keeps going when the
 * screen rotates or the user navigates. UI observes [state].
 */
object AudioExtractionManager {
  private const val TAG = "AudioExtraction"

  sealed interface State {
    data object Idle : State

    /** [progress] is 0..1, or -1 when the length is unknown. */
    data class Running(val videoName: String, val progress: Float) : State

    data class Done(val fileUri: Uri, val fileName: String, val folderName: String) : State

    data class Error(val message: String) : State
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var job: Job? = null

  private val _state = MutableStateFlow<State>(State.Idle)
  val state: StateFlow<State> = _state.asStateFlow()

  /** Emits after every successful extraction (the Audio tab refreshes on it). */
  private val _finished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
  val finished: SharedFlow<Unit> = _finished.asSharedFlow()

  val isRunning: Boolean get() = _state.value is State.Running

  fun start(
    context: Context,
    videoUri: Uri,
    videoName: String,
    track: AudioExtractor.AudioTrack,
    outputBaseName: String,
    treeUri: Uri,
    onSuccess: () -> Unit = {},
  ) {
    if (isRunning) return
    val output = track.output ?: return
    val appContext = context.applicationContext
    _state.value = State.Running(videoName, if (track.durationUs > 0) 0f else -1f)

    job = scope.launch {
      var outputUri: Uri? = null
      try {
        val folderName = withContext(Dispatchers.IO) { AudioFolderStore.displayName(appContext, treeUri) }
        outputUri = withContext(Dispatchers.IO) {
          AudioFolderStore.createFile(appContext, treeUri, AudioFolderStore.sanitize(outputBaseName), output.extension)
        } ?: throw CreateFileException()

        var lastPercent = -1
        AudioExtractor.extract(appContext, videoUri, track, outputUri) { progress ->
          val percent = (progress * 100).toInt()
          if (percent != lastPercent) {
            lastPercent = percent
            _state.value = State.Running(videoName, progress)
          }
        }

        val fileName = withContext(Dispatchers.IO) {
          AudioFolderStore.list(appContext, treeUri).firstOrNull { it.uri == outputUri }?.name
        } ?: "$outputBaseName.${output.extension}"
        _state.value = State.Done(outputUri, fileName, folderName)
        _finished.tryEmit(Unit)
        onSuccess()
      } catch (e: CancellationException) {
        deletePartial(appContext, outputUri)
        _state.value = State.Idle
        throw e
      } catch (e: Throwable) {
        Log.e(TAG, "Extraction failed", e)
        deletePartial(appContext, outputUri)
        val message = if (e is CreateFileException) R.string.audio_extract_create_failed else R.string.audio_extract_failed
        _state.value = State.Error(appContext.getString(message))
      }
    }
  }

  fun cancel() {
    job?.cancel()
  }

  /** Clears a finished/failed result. Does nothing while an extraction runs. */
  fun reset() {
    if (!isRunning) _state.value = State.Idle
  }

  private class CreateFileException : Exception()

  private suspend fun deletePartial(context: Context, uri: Uri?) {
    if (uri == null) return
    withContext(NonCancellable + Dispatchers.IO) { AudioFolderStore.delete(context, uri) }
  }
}
