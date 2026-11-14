package app.marlboroadvance.mpvex.ui.browser.audio

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.utils.media.AudioExtractionManager
import app.marlboroadvance.mpvex.utils.storage.AudioFolderStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExtractedAudioViewModel(
  application: Application,
  private val preferences: BrowserPreferences,
) : AndroidViewModel(application) {

  data class UiState(
    val isLoading: Boolean = true,
    /** null when no folder is chosen. */
    val folderUri: Uri? = null,
    val folderName: String? = null,
    /** False when a folder is set but the app lost access to it. */
    val hasAccess: Boolean = false,
    val files: List<AudioFolderStore.AudioFile> = emptyList(),
  )

  private val _state = MutableStateFlow(UiState())
  val state: StateFlow<UiState> = _state.asStateFlow()

  private var loadJob: Job? = null

  init {
    viewModelScope.launch {
      preferences.audioOutputTreeUri.changes().distinctUntilChanged().collect { refresh() }
    }
    viewModelScope.launch {
      AudioExtractionManager.finished.collect { refresh() }
    }
  }

  fun refresh() {
    loadJob?.cancel()
    loadJob = viewModelScope.launch {
      val context = getApplication<Application>()
      val treeUri = AudioFolderStore.parse(preferences.audioOutputTreeUri.get())
      _state.value = _state.value.copy(isLoading = true, folderUri = treeUri)
      val newState = withContext(Dispatchers.IO) {
        if (treeUri == null) return@withContext UiState(isLoading = false)
        val access = AudioFolderStore.hasAccess(context, treeUri)
        UiState(
          isLoading = false,
          folderUri = treeUri,
          folderName = AudioFolderStore.displayName(context, treeUri),
          hasAccess = access,
          files = if (access) runCatching { AudioFolderStore.list(context, treeUri) }.getOrDefault(emptyList()) else emptyList(),
        )
      }
      _state.value = newState
    }
  }

  fun delete(file: AudioFolderStore.AudioFile) {
    viewModelScope.launch {
      withContext(Dispatchers.IO) { AudioFolderStore.delete(getApplication(), file.uri) }
      refresh()
    }
  }

  fun rename(file: AudioFolderStore.AudioFile, newName: String) {
    if (newName.isBlank() || newName == file.name) return
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        AudioFolderStore.rename(getApplication(), file.uri, AudioFolderStore.sanitize(newName))
      }
      refresh()
    }
  }

  companion object {
    fun factory(application: Application, preferences: BrowserPreferences) =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          ExtractedAudioViewModel(application, preferences) as T
      }
  }
}
