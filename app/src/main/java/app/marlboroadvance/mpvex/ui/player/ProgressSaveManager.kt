package app.marlboroadvance.mpvex.ui.player

import android.util.Log
import app.marlboroadvance.mpvex.database.entities.PlaybackStateEntity
import app.marlboroadvance.mpvex.domain.playbackstate.repository.PlaybackStateRepository
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.utils.media.MediaLibraryEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Centralized manager for video playback progress saving.
 */
class ProgressSaveManager : KoinComponent {
    private val playbackStateRepository: PlaybackStateRepository by inject()
    private val browserPreferences: BrowserPreferences by inject()
    
    private var currentSaveJob: Job? = null
    private var lastSavedPosition: Int? = null
    private var lastSavedMediaIdentifier: String? = null
    
    companion object {
        private const val TAG = "ProgressSaveManager"
        private const val SAVE_DEBOUNCE_MS = 500L
    }
    
    fun saveProgress(
        mediaIdentifier: String,
        getPosition: () -> Int?,
        getDuration: () -> Int?,
        getPlaybackSpeed: () -> Double?,
        getVideoZoom: () -> Float?,
        getSid: () -> Int?,
        getSecondarySid: () -> Int?,
        getSubDelay: () -> Int?,
        getSubSpeed: () -> Double?,
        getAid: () -> Int?,
        getAudioDelay: () -> Int?,
        getExternalSubtitles: () -> String,
        savePositionOnQuit: Boolean,
        oldState: PlaybackStateEntity? = null,
        isImmediate: Boolean = false
    ) {
        // Cancel any pending save operation
        currentSaveJob?.cancel()
        
        currentSaveJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Debounce to prevent rapid saves unless immediate
                if (!isImmediate) {
                    delay(SAVE_DEBOUNCE_MS)
                }
                
                if (!isActive) return@launch
                
                val currentPos = getPosition() ?: 0
                val duration = getDuration() ?: 0
                
                // Skip save if position hasn't changed significantly (more than 1 second)
                // Unless it's an immediate save (like reaching end of video)
                if (!isImmediate && 
                    lastSavedMediaIdentifier == mediaIdentifier && 
                    lastSavedPosition != null && 
                    kotlin.math.abs(lastSavedPosition!! - currentPos) <= 1) {
                    Log.d(TAG, "Skipping save - position unchanged: $currentPos")
                    return@launch
                }
        
                Log.d(TAG, "Saving progress for: $mediaIdentifier at position: $currentPos (immediate: $isImmediate)")
                
                val positionToSave = calculateSavePosition(currentPos, duration, savePositionOnQuit, oldState)
                val timeRemaining = if (duration > positionToSave) duration - positionToSave else 0
                
                // Get watched threshold from preferences (default 95%)
                val thresholdPercent = browserPreferences.watchedThreshold.get()
                val isWatched = if (duration > 0) {
                    currentPos >= (duration * (thresholdPercent / 100f))
                } else {
                    false
                }

                playbackStateRepository.upsert(
                    PlaybackStateEntity(
                        mediaTitle = mediaIdentifier,
                        lastPosition = positionToSave,
                        playbackSpeed = getPlaybackSpeed() ?: 1.0,
                        videoZoom = getVideoZoom() ?: 0f,
                        sid = getSid() ?: 0,
                        secondarySid = getSecondarySid() ?: -1,
                        subDelay = getSubDelay() ?: 0,
                        subSpeed = getSubSpeed() ?: 1.0,
                        aid = getAid() ?: 0,
                        audioDelay = getAudioDelay() ?: 0,
                        timeRemaining = timeRemaining,
                        externalSubtitles = getExternalSubtitles(),
                        hasBeenWatched = isWatched
                    ),
                )
                
                lastSavedPosition = currentPos
                lastSavedMediaIdentifier = mediaIdentifier
                
                Log.d(TAG, "Progress saved successfully")

                // Notify UI to refresh in real-time
                MediaLibraryEvents.notifyChanged()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving progress", e)
            }
        }
    }
    
    private fun calculateSavePosition(
        currentPos: Int,
        duration: Int,
        savePositionOnQuit: Boolean,
        oldState: PlaybackStateEntity?
    ): Int {
        if (!savePositionOnQuit) {
            return oldState?.lastPosition ?: 0
        }
        
        return if (currentPos >= duration - 1) 0 else currentPos
    }
    
    fun cancelPendingSave() {
        currentSaveJob?.cancel()
        currentSaveJob = null
        Log.d(TAG, "Pending save cancelled")
    }
    
    fun resetTracking() {
        lastSavedPosition = null
        lastSavedMediaIdentifier = null
        Log.d(TAG, "Save tracking reset")
    }
}
