package app.marlboroadvance.mpvex.update

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import app.marlboroadvance.mpvex.BuildConfig
import app.marlboroadvance.mpvex.preferences.UpdatePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/** Why a check failed. */
enum class UpdateError {
  Network,
  RateLimited,
  NoRelease,
  Unknown,
}

/** What the update check is doing; observed by the Settings card and the Updates screen. */
sealed interface UpdateState {
  data object Idle : UpdateState
  data object Checking : UpdateState
  data object UpToDate : UpdateState
  data class Available(val release: ReleaseInfo) : UpdateState
  data class Failed(val error: UpdateError) : UpdateState
}

/**
 * Checks GitHub for a newer stable release of MpvFlux. It never downloads or installs anything:
 * when a newer version exists, the user is pointed at the release page on GitHub.
 *
 * - Checks at most once per app launch when the app opens ([checkIfDue]) or on demand
 *   ([check]). No background job, so it costs nothing while the app is closed.
 * - Only active in release builds: debug builds use other app IDs and versions.
 */
class UpdateManager(
  context: Context,
  private val preferences: UpdatePreferences,
  baseClient: OkHttpClient,
) {
  private val client = baseClient.newBuilder().readTimeout(30, TimeUnit.SECONDS).build()
  private val releases = GitHubReleases(
    client,
    object : ReleaseResponseCache {
      override fun get(): Pair<String, String>? {
        val etag = preferences.releaseEtag.get()
        val body = preferences.releaseBody.get()
        return if (etag.isNotEmpty() && body.isNotEmpty()) etag to body else null
      }

      override fun put(etag: String, body: String) {
        preferences.releaseEtag.set(etag)
        preferences.releaseBody.set(body)
      }
    },
  )
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
  val state: StateFlow<UpdateState> = _state.asStateFlow()

  private var checkJob: Job? = null

  val currentVersion: String = BuildConfig.VERSION_NAME

  /** Update checks only run for the GitHub release build. */
  val isSupported: Boolean = BuildConfig.BUILD_TYPE == "release"

  init {
    // Earlier versions downloaded APKs and showed update notifications; clear what they left.
    val appContext = context.applicationContext
    scope.launch {
      runCatching { File(appContext.cacheDir, "updates").deleteRecursively() }
      runCatching {
        NotificationManagerCompat.from(appContext).apply {
          cancel(LEGACY_NOTIFICATION_ID)
          deleteNotificationChannel(LEGACY_CHANNEL_ID)
        }
      }
    }
  }

  /** Called when the app opens: checks GitHub in the background if auto-check is on. */
  fun checkIfDue() {
    if (!isSupported || !preferences.autoCheck.get()) return
    // Once per app launch: the state is Idle only until the first check of this process,
    // so coming back from the player or switching apps doesn't check again.
    if (_state.value !is UpdateState.Idle) return
    check()
  }

  /** Asks GitHub for the newest stable release now. */
  fun check() {
    if (!isSupported) return
    if (checkJob?.isActive == true) return
    checkJob = scope.launch {
      _state.value = UpdateState.Checking
      _state.value = try {
        val latest = releases.fetchLatest()
        preferences.lastCheckTime.set(System.currentTimeMillis())
        val installed = AppVersion.parse(currentVersion)
        val newer = latest?.takeIf { installed == null || it.version > installed }
        preferences.availableVersion.set(newer?.version?.toString().orEmpty())
        if (newer != null) UpdateState.Available(newer) else UpdateState.UpToDate
      } catch (e: UpdateCheckException) {
        Log.w(TAG, "Update check failed: ${e.reason}", e.cause)
        if (e.reason == UpdateError.NoRelease) UpdateState.UpToDate else UpdateState.Failed(e.reason)
      }
    }
  }

  private companion object {
    const val TAG = "UpdateManager"

    // Used by the removed download notifications; only referenced to clean them up.
    const val LEGACY_CHANNEL_ID = "app_updates"
    const val LEGACY_NOTIFICATION_ID = 4210
  }
}
