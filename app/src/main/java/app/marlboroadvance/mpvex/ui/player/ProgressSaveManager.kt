package app.marlboroadvance.mpvex.ui.player

import android.util.Log
import app.marlboroadvance.mpvex.database.entities.PlaybackStateEntity
import app.marlboroadvance.mpvex.domain.playbackstate.repository.PlaybackStateRepository
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.utils.media.PlaybackProgressEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Data snapshot representing the playback state at a specific point in time.
 */
data class PlaybackStateSnapshot(
    val mediaIdentifier: String,
    val position: Int,
    val duration: Int,
    val playbackSpeed: Double,
    val videoZoom: Float,
    val sid: Int,
    val secondarySid: Int,
    val subDelay: Int,
    val subSpeed: Double,
    val aid: Int,
    val audioDelay: Int,
    val externalSubtitles: String,
    val savePositionOnQuit: Boolean
)

/**
 * Centralized manager for video playback progress saving.
 */
class ProgressSaveManager : KoinComponent {
    private val playbackStateRepository: PlaybackStateRepository by inject()
    private val browserPreferences: BrowserPreferences by inject()
    
    // Single long-lived scope shared by all saves, instead of allocating a fresh
    // CoroutineScope per call. SupervisorJob so one failed save can't cancel the scope
    // (each save also catches its own exceptions). Individual saves are still cancelled
    // via currentSaveJob; immediate saves run under NonCancellable to survive teardown.
    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentSaveJob: Job? = null
    private var lastSavedPosition: Int? = null
    private var lastSavedMediaIdentifier: String? = null

    companion object {
        private const val TAG = "ProgressSaveManager"
        private const val SAVE_DEBOUNCE_MS = 500L
        private const val BLOCKING_SAVE_TIMEOUT_MS = 1500L
    }
    
    /**
     * Saves playback progress using a pre-captured snapshot of the state.
     *
     * With [lookupOldState] the previous database row is read inside the save job itself, on
     * this manager's own scope. Callers no longer need the Activity's lifecycleScope for that
     * lookup, so a save requested while the Activity is being destroyed still runs.
     */
    fun saveProgress(
        snapshot: PlaybackStateSnapshot,
        oldState: PlaybackStateEntity? = null,
        isImmediate: Boolean = false,
        lookupOldState: Boolean = false,
    ) {
        val mediaIdentifier = snapshot.mediaIdentifier
        
        // Cancel pending save only if it's the same media or if this is a regular (non-immediate) save.
        // We allow multiple "Immediate" saves for different videos (e.g., during rapid playlist switching)
        // to ensure the closing video's progress is written even if the next video already requested a save.
        if (!isImmediate || lastSavedMediaIdentifier == mediaIdentifier) {
            currentSaveJob?.cancel()
        }
        
        currentSaveJob = saveScope.launch {
            try {
                // Debounce to prevent rapid saves unless immediate
                if (!isImmediate) {
                    delay(SAVE_DEBOUNCE_MS)
                }
                
                if (!isActive && !isImmediate) return@launch

                val previousState = if (lookupOldState) loadOldState(mediaIdentifier) else oldState
                
                // Use NonCancellable for immediate saves to ensure database write completes 
                // even if the activity or scope is being destroyed.
                withContext(if (isImmediate) NonCancellable else Dispatchers.IO) {
                    writeProgress(snapshot, previousState, isImmediate)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving progress for $mediaIdentifier", e)
            }
        }
    }

    /**
     * Writes progress synchronously, waiting at most [timeoutMs]. Only for moments when the
     * process is about to be killed (task removed from Recents during background playback),
     * where an asynchronous save would never get to run.
     */
    fun saveProgressBlocking(snapshot: PlaybackStateSnapshot, timeoutMs: Long = BLOCKING_SAVE_TIMEOUT_MS) {
        runCatching {
            runBlocking(Dispatchers.IO) {
                withTimeoutOrNull(timeoutMs) {
                    writeProgress(snapshot, loadOldState(snapshot.mediaIdentifier), isImmediate = true)
                }
            }
        }.onFailure { e ->
            Log.e(TAG, "Error in blocking progress save for ${snapshot.mediaIdentifier}", e)
        }
    }

    private suspend fun loadOldState(mediaIdentifier: String): PlaybackStateEntity? =
        runCatching { playbackStateRepository.getVideoDataByTitle(mediaIdentifier) }.getOrNull()

    private suspend fun writeProgress(
        snapshot: PlaybackStateSnapshot,
        oldState: PlaybackStateEntity?,
        isImmediate: Boolean,
    ) {
        val mediaIdentifier = snapshot.mediaIdentifier
        val currentPos = snapshot.position
        val duration = snapshot.duration
        
        // Skip save if position hasn't changed significantly (more than 1 second)
        // Unless it's an immediate save (like reaching end of video or closing player)
        if (!isImmediate && 
            lastSavedMediaIdentifier == mediaIdentifier && 
            lastSavedPosition != null && 
            kotlin.math.abs(lastSavedPosition!! - currentPos) <= 1) {
            return
        }

        Log.d(TAG, "Saving progress for: $mediaIdentifier at position: $currentPos (immediate: $isImmediate)")

        val positionToSave = calculateSavePosition(currentPos, duration, snapshot.savePositionOnQuit, oldState)
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
                playbackSpeed = snapshot.playbackSpeed,
                videoZoom = snapshot.videoZoom,
                sid = snapshot.sid,
                secondarySid = snapshot.secondarySid,
                subDelay = snapshot.subDelay,
                subSpeed = snapshot.subSpeed,
                aid = snapshot.aid,
                audioDelay = snapshot.audioDelay,
                timeRemaining = timeRemaining,
                externalSubtitles = snapshot.externalSubtitles,
                hasBeenWatched = isWatched
            ),
        )
        
        lastSavedPosition = currentPos
        lastSavedMediaIdentifier = mediaIdentifier
        
        Log.d(TAG, "Progress saved successfully for $mediaIdentifier")

        // Let the lists refresh their progress bars and watched badges.
        // Not MediaLibraryEvents: no file changed, so there's nothing to rescan.
        PlaybackProgressEvents.notifyChanged()
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
