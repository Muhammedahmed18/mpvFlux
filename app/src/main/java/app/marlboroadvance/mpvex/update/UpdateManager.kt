package app.marlboroadvance.mpvex.update

import android.content.Context
import android.util.Log
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
import java.util.concurrent.TimeUnit

/** Why something in the update flow failed. */
enum class UpdateError {
  Network,
  RateLimited,
  NoRelease,
  Unknown,
}

/** What the updater is doing; observed by the Settings card and the Updates screen. */
sealed interface UpdateState {
  data object Idle : UpdateState
  data object Checking : UpdateState
  data object UpToDate : UpdateState
  data class Available(val release: ReleaseInfo) : UpdateState
  data class Failed(val error: UpdateError) : UpdateState
}

/**
 * Lightweight GitHub release checker (stable only).
 *
 * - Checks at most once a day when the app opens ([checkIfDue]) or on demand ([check]).
 *   No background job, so it costs nothing while the app is closed.
 * - Does not download or install APKs; users are directed to the GitHub release page.
 * - Only active in release builds: debug and preview builds use other app IDs.
 */
class UpdateManager(
  private val context: Context,
  private val preferences: UpdatePreferences,
  baseClient: OkHttpClient,
) {
  private val client = baseClient.newBuilder().readTimeout(30, TimeUnit.SECONDS).build()
  private val releases = GitHubReleases(
    client,
    object : ReleaseResponseCache {
      override fun get(url: String): Pair<String, String>? {
        if (preferences.releasesUrl.get() != url) return null
        val etag = preferences.releasesEtag.get()
        val body = preferences.releasesBody.get()
        return if (etag.isNotEmpty() && body.isNotEmpty()) etag to body else null
      }

      override fun put(url: String, etag: String, body: String) {
        preferences.releasesUrl.set(url)
        preferences.releasesEtag.set(etag)
        preferences.releasesBody.set(body)
      }
    },
  )
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
  val state: StateFlow<UpdateState> = _state.asStateFlow()

  private var checkJob: Job? = null

  val currentVersion: String = BuildConfig.VERSION_NAME

  /** Checker only works for the GitHub release build (same app ID). */
  val isSupported: Boolean = BuildConfig.BUILD_TYPE == "release"

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
        val newer = latest != null && (installed == null || latest.version > installed)
        preferences.availableVersion.set(if (newer) latest!!.version.toString() else "")
        when {
          !newer -> UpdateState.UpToDate
          else -> UpdateState.Available(release = latest!!)
        }
      } catch (e: UpdateCheckException) {
        Log.w(TAG, "Update check failed: ${e.reason}", e.cause)
        if (e.reason == UpdateError.NoRelease) UpdateState.UpToDate else UpdateState.Failed(e.reason)
      }
    }
  }

  companion object {
    private const val TAG = "UpdateManager"
  }
}
