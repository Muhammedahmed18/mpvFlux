package app.marlboroadvance.mpvex.utils.media

import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log

/**
 * Live "new video" detection that replaces the old manifest MediaScanReceiver.
 *
 * Android 8+ no longer delivers MEDIA_SCANNER_* broadcasts to manifest receivers, so that
 * receiver never fired (and, being exported with no permission, any app could send it a
 * fake event). Instead this watches MediaStore's video table while the library screens
 * are visible ([start] in MainActivity.onStart, [stop] in onStop):
 *
 * - Changes are coalesced for [DEBOUNCE_MS], so a download that updates its MediaStore row
 *   many times triggers one refresh, not dozens.
 * - Nothing is registered while the app is in the background or the player is open, so
 *   downloads finishing during playback don't make hidden lists reload.
 * - On return, MediaStore's generation counter (Android 11+) tells us whether anything
 *   changed while we were away, so lists only refresh when needed.
 */
object MediaStoreWatcher {
  private const val TAG = "MediaStoreWatcher"
  private const val DEBOUNCE_MS = 1_000L

  private val handler = Handler(Looper.getMainLooper())
  private val notifyRunnable = Runnable { MediaLibraryEvents.notifyChanged() }

  private var observer: ContentObserver? = null
  private var stoppedOnce = false
  private var generationAtStop = -1L

  fun start(context: Context) {
    if (observer != null) return
    val appContext = context.applicationContext

    if (stoppedOnce && changedWhileStopped(appContext)) {
      scheduleNotify()
    }

    val newObserver = object : ContentObserver(handler) {
      override fun onChange(selfChange: Boolean) {
        scheduleNotify()
      }
    }
    runCatching {
      appContext.contentResolver.registerContentObserver(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        true,
        newObserver,
      )
    }.onSuccess {
      observer = newObserver
    }.onFailure { e ->
      Log.w(TAG, "Could not watch MediaStore", e)
    }
  }

  fun stop(context: Context) {
    val current = observer ?: return
    observer = null
    handler.removeCallbacks(notifyRunnable)
    runCatching { context.applicationContext.contentResolver.unregisterContentObserver(current) }
    stoppedOnce = true
    generationAtStop = currentGeneration(context.applicationContext)
  }

  private fun scheduleNotify() {
    handler.removeCallbacks(notifyRunnable)
    handler.postDelayed(notifyRunnable, DEBOUNCE_MS)
  }

  private fun changedWhileStopped(context: Context): Boolean {
    // Below Android 11 there is no generation counter, so assume something changed.
    if (generationAtStop < 0) return true
    val now = currentGeneration(context)
    return now < 0 || now != generationAtStop
  }

  /** Sum of the generation counters of all external volumes, or -1 if unavailable. */
  private fun currentGeneration(context: Context): Long {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return -1L
    return runCatching {
      MediaStore.getExternalVolumeNames(context).sumOf { volume ->
        MediaStore.getGeneration(context, volume)
      }
    }.getOrDefault(-1L)
  }
}
