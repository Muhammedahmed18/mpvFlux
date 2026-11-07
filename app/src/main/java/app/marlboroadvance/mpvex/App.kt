package app.marlboroadvance.mpvex

import android.app.Application
import app.marlboroadvance.mpvex.database.repository.VideoMetadataCacheRepository
import app.marlboroadvance.mpvex.di.DatabaseModule
import app.marlboroadvance.mpvex.di.FileManagerModule
import app.marlboroadvance.mpvex.di.PreferencesModule
import app.marlboroadvance.mpvex.presentation.crash.CrashActivity
import app.marlboroadvance.mpvex.presentation.crash.GlobalExceptionHandler
import app.marlboroadvance.mpvex.utils.media.MediaLibraryEvents
import app.marlboroadvance.mpvex.utils.storage.FolderViewScanner
import app.marlboroadvance.mpvex.utils.storage.TreeViewScanner
import `is`.xyz.mpv.FastThumbnails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.annotation.KoinExperimentalAPI

@OptIn(KoinExperimentalAPI::class)
class App : Application() {
  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private val metadataCache: VideoMetadataCacheRepository by inject()
  private val launchScanRequested = java.util.concurrent.atomic.AtomicBoolean(false)

  override fun onCreate() {
    super.onCreate()

    // Initialize Koin
    startKoin {
      androidContext(this@App)
      modules(
        PreferencesModule,
        DatabaseModule,
        FileManagerModule,
        app.marlboroadvance.mpvex.di.domainModule,
      )
    }

    Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(applicationContext, CrashActivity::class.java))

    removeOrphanedPreferences()

    FastThumbnails.initialize(this)

    observeMediaLibraryChanges()

    // Perform cache maintenance on app startup (non-blocking)
    applicationScope.launch {
      runCatching {
        metadataCache.performMaintenance()
      }
    }
    
    // The launch media scan is no longer started here: App.onCreate also runs when the
    // process starts just to play a video opened from another app, where a scan of the
    // whole storage competed with playback start-up. MainActivity starts it instead
    // (see runLaunchMediaScanIfDue).
  }

  /**
   * Starts the launch media scan (still at most once every [LAUNCH_SCAN_INTERVAL_MS]) the
   * first time the library is shown in this process. Called from MainActivity.
   */
  fun runLaunchMediaScanIfDue() {
    if (!launchScanRequested.compareAndSet(false, true)) return
    applicationScope.launch {
      runCatching {
        triggerMediaScanOnLaunch()
      }
    }
  }

  @OptIn(FlowPreview::class)
  private fun observeMediaLibraryChanges() {
    MediaLibraryEvents.changes
      .debounce(500L)
      .onEach {
        TreeViewScanner.clearCache()
        FolderViewScanner.clearCache()
      }
      .launchIn(applicationScope)
  }
  
  /**
   * Remove preference keys whose features have been deleted, so their stored
   * values don't linger in SharedPreferences on already-installed devices.
   */
  private fun removeOrphanedPreferences() {
    try {
      val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
      if (prefs.contains("show_next_up_prompt")) {
        prefs.edit().remove("show_next_up_prompt").apply()
      }
    } catch (e: Exception) {
      android.util.Log.e("App", "Failed to remove orphaned preferences", e)
    }
  }

  /**
   * Trigger a media scan on app launch to ensure MediaStore is up-to-date.
   * This helps detect videos added by external apps while the app was closed.
   *
   * A recursive scan of the whole storage is expensive (disk + CPU), and Android
   * already indexes most new files on its own, so it runs at most once every
   * [LAUNCH_SCAN_INTERVAL_MS] instead of on every launch.
   */
  private fun triggerMediaScanOnLaunch() {
    try {
      val prefs = getSharedPreferences(LAUNCH_SCAN_PREFS, MODE_PRIVATE)
      val now = System.currentTimeMillis()
      val lastScan = prefs.getLong(KEY_LAST_LAUNCH_SCAN, 0L)
      if (now - lastScan in 0L until LAUNCH_SCAN_INTERVAL_MS) {
        android.util.Log.d("App", "Skipping launch media scan (last scan ${(now - lastScan) / 60_000} min ago)")
        return
      }
      prefs.edit().putLong(KEY_LAST_LAUNCH_SCAN, now).apply()

      val externalStorage = android.os.Environment.getExternalStorageDirectory()

      android.media.MediaScannerConnection.scanFile(
        this,
        arrayOf(externalStorage.absolutePath),
        null, // Let MediaScanner detect all media types
      ) { path, uri ->
        android.util.Log.d("App", "Launch media scan completed for: $path")
        // Notify the app that media library may have changed
        MediaLibraryEvents.notifyChanged()
      }
      
      android.util.Log.d("App", "Triggered media scan on app launch")
    } catch (e: Exception) {
      android.util.Log.e("App", "Failed to trigger media scan on launch", e)
    }
  }

  private companion object {
    const val LAUNCH_SCAN_PREFS = "media_scan"
    const val KEY_LAST_LAUNCH_SCAN = "last_launch_scan"
    const val LAUNCH_SCAN_INTERVAL_MS = 12L * 60 * 60 * 1000 // 12 hours
  }
}
