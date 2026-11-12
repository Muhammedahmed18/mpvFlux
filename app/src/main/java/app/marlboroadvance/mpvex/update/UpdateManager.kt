package app.marlboroadvance.mpvex.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.marlboroadvance.mpvex.BuildConfig
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.UpdatePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Why something in the update flow failed. */
enum class UpdateError {
  Network,
  RateLimited,
  NoRelease,
  NoApkForDevice,
  DownloadFailed,
  ChecksumMismatch,
  SignatureMismatch,
  InstallFailed,
  InstallCancelled,
  PermissionMissing,
  Unknown,
}

/** What the updater is doing; observed by the Settings card and the Updates screen. */
sealed interface UpdateState {
  data object Idle : UpdateState
  data object Checking : UpdateState
  data object UpToDate : UpdateState
  data class Available(val release: ReleaseInfo, val skipped: Boolean) : UpdateState
  data class Downloading(val release: ReleaseInfo, val downloaded: Long, val total: Long) : UpdateState {
    val progress: Float get() = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else 0f
  }
  data class ReadyToInstall(val release: ReleaseInfo, val apk: File) : UpdateState
  data class Installing(val release: ReleaseInfo, val apk: File) : UpdateState
  data class Failed(val error: UpdateError, val release: ReleaseInfo?) : UpdateState
}

/**
 * In-app updater for GitHub releases.
 *
 * - Checks at most once a day when the app opens ([checkIfDue]) or on demand ([check]).
 *   No background job, so it costs nothing while the app is closed.
 * - Downloads the APK for this phone's ABI into the app's private cache with progress
 *   (also shown as a notification), then verifies size, SHA-256 (from GitHub) and that it
 *   is signed with the same key as the installed app.
 * - Installs with PackageInstaller; Android always asks the user to confirm.
 * - Only active in release builds: debug and preview builds use other app IDs.
 */
class UpdateManager(
  private val context: Context,
  private val preferences: UpdatePreferences,
  baseClient: OkHttpClient,
) {
  // Downloads can take a while on slow connections, so no read deadline per call.
  private val client = baseClient.newBuilder().readTimeout(60, TimeUnit.SECONDS).build()
  private val releases = GitHubReleases(client)
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val updatesDir = File(context.cacheDir, "updates")

  private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
  val state: StateFlow<UpdateState> = _state.asStateFlow()

  private var checkJob: Job? = null
  private var downloadJob: Job? = null

  val currentVersion: String = BuildConfig.VERSION_NAME

  /** Updater only works for the GitHub release build (same app ID and signing key). */
  val isSupported: Boolean = BuildConfig.BUILD_TYPE == "release"

  init {
    // Leftover APKs from an earlier session are of no use (or already installed).
    scope.launch { runCatching { updatesDir.deleteRecursively() } }
  }

  /** Called when the app opens: checks GitHub if auto-check is on and a day has passed. */
  fun checkIfDue() {
    if (!isSupported || !preferences.autoCheck.get()) return
    if (_state.value !is UpdateState.Idle) return
    val elapsed = System.currentTimeMillis() - preferences.lastCheckTime.get()
    // Also re-check when an earlier check found an update that isn't installed yet, so the
    // Settings card still shows it after the app was closed.
    val pending = AppVersion.parse(preferences.availableVersion.get())
    val installed = AppVersion.parse(currentVersion)
    val updatePending = pending != null && installed != null && pending > installed
    if (elapsed >= CHECK_INTERVAL_MS || updatePending) check()
  }

  /** Asks GitHub for the newest release now. */
  fun check() {
    if (!isSupported) return
    if (checkJob?.isActive == true || downloadJob?.isActive == true) return
    if (_state.value is UpdateState.ReadyToInstall || _state.value is UpdateState.Installing) return
    checkJob = scope.launch {
      _state.value = UpdateState.Checking
      _state.value = try {
        val latest = releases.fetchLatest(preferences.includePrereleases.get())
        preferences.lastCheckTime.set(System.currentTimeMillis())
        val installed = AppVersion.parse(currentVersion)
        val newer = latest != null && (installed == null || latest.version > installed)
        preferences.availableVersion.set(if (newer) latest!!.version.toString() else "")
        when {
          !newer -> UpdateState.UpToDate
          else -> UpdateState.Available(
            release = latest!!,
            skipped = preferences.skippedVersion.get() == latest.version.toString(),
          )
        }
      } catch (e: UpdateCheckException) {
        Log.w(TAG, "Update check failed: ${e.reason}", e.cause)
        if (e.reason == UpdateError.NoRelease) UpdateState.UpToDate else UpdateState.Failed(e.reason, null)
      }
    }
  }

  /** Don't remind about this version again (a newer one will be offered). */
  fun skip(release: ReleaseInfo) {
    preferences.skippedVersion.set(release.version.toString())
    _state.value = UpdateState.Available(release, skipped = true)
  }

  /** Remember that the one-time "update available" popup was shown for this version. */
  fun markNotified(release: ReleaseInfo) {
    preferences.notifiedVersion.set(release.version.toString())
  }

  fun wasNotified(release: ReleaseInfo): Boolean =
    preferences.notifiedVersion.get() == release.version.toString()

  /** Downloads and verifies the APK of an available release. */
  fun download() {
    val release = when (val s = _state.value) {
      is UpdateState.Available -> s.release
      is UpdateState.Failed -> s.release ?: return
      else -> return
    }
    if (downloadJob?.isActive == true) return
    downloadJob = scope.launch { downloadAndVerify(release) }
  }

  fun cancelDownload() {
    val release = (_state.value as? UpdateState.Downloading)?.release ?: return
    downloadJob?.cancel()
    downloadJob = null
    notifications().cancel(NOTIFICATION_ID)
    updatesDir.listFiles()?.forEach { it.delete() }
    _state.value = UpdateState.Available(release, skipped = false)
  }

  /**
   * Hands the verified APK to Android's installer. If "Install unknown apps" isn't allowed
   * yet, reports [UpdateError.PermissionMissing] so the UI can send the user to Settings.
   */
  fun install() {
    val ready = _state.value as? UpdateState.ReadyToInstall ?: return
    if (!ApkInstaller.canInstallPackages(context)) {
      _state.value = UpdateState.Failed(UpdateError.PermissionMissing, ready.release)
      return
    }
    _state.value = UpdateState.Installing(ready.release, ready.apk)
    // Saved so the new version can show "What's new" once it starts.
    preferences.pendingNotesVersion.set(ready.release.version.toString())
    preferences.pendingNotes.set(ready.release.notes)
    scope.launch {
      runCatching { ApkInstaller.install(context, ready.apk) }
        .onFailure {
          Log.e(TAG, "Could not start install", it)
          _state.value = UpdateState.Failed(UpdateError.InstallFailed, ready.release)
        }
    }
  }

  /** After the user allowed "Install unknown apps", continue where we stopped. */
  fun retryAfterPermission() {
    val failed = _state.value as? UpdateState.Failed ?: return
    if (failed.error != UpdateError.PermissionMissing || failed.release == null) return
    val apk = File(updatesDir, failed.release.apkName)
    _state.value = if (apk.exists()) {
      UpdateState.ReadyToInstall(failed.release, apk)
    } else {
      UpdateState.Available(failed.release, skipped = false)
    }
    if (_state.value is UpdateState.ReadyToInstall) install()
  }

  internal fun onInstallFinished(outcome: InstallOutcome) {
    val installing = _state.value as? UpdateState.Installing
    val release = installing?.release
    when (outcome) {
      // Normally the app is restarted by the update before this arrives.
      InstallOutcome.Success -> _state.value = UpdateState.UpToDate
      InstallOutcome.Cancelled ->
        _state.value = if (installing != null) {
          UpdateState.ReadyToInstall(installing.release, installing.apk)
        } else {
          UpdateState.Failed(UpdateError.InstallCancelled, null)
        }
      InstallOutcome.SignatureMismatch -> _state.value = UpdateState.Failed(UpdateError.SignatureMismatch, release)
      is InstallOutcome.Failed -> _state.value = UpdateState.Failed(UpdateError.InstallFailed, release)
    }
  }

  /**
   * Called once when the app starts. Returns the release notes to show as "What's new" if
   * this is the first run after an in-app update, otherwise null.
   */
  fun consumeWhatsNew(): Pair<String, String>? {
    val last = preferences.lastRunVersion.get()
    preferences.lastRunVersion.set(currentVersion)
    if (last.isEmpty() || last == currentVersion) return null
    val notesVersion = preferences.pendingNotesVersion.get()
    val notes = preferences.pendingNotes.get()
    preferences.pendingNotesVersion.delete()
    preferences.pendingNotes.delete()
    val updatedTo = AppVersion.parse(currentVersion)?.toString()
    return if (notes.isNotBlank() && notesVersion == updatedTo) notesVersion to notes else null
  }

  // ---------------------------------------------------------------------------
  // Download + verification
  // ---------------------------------------------------------------------------

  private suspend fun downloadAndVerify(release: ReleaseInfo) {
    _state.value = UpdateState.Downloading(release, 0, release.apkSize)
    updatesDir.mkdirs()
    updatesDir.listFiles()?.forEach { it.delete() }
    val target = File(updatesDir, release.apkName)
    val partial = File(updatesDir, release.apkName + ".part")

    try {
      val sha256 = withContext(Dispatchers.IO) { downloadTo(release, partial) }

      if (release.apkSize > 0 && partial.length() != release.apkSize) {
        throw VerificationException(UpdateError.DownloadFailed)
      }
      if (release.apkSha256 != null && release.apkSha256 != sha256) {
        throw VerificationException(UpdateError.ChecksumMismatch)
      }
      if (!partial.renameTo(target)) throw VerificationException(UpdateError.DownloadFailed)
      if (ApkInstaller.matchesInstalledSignature(context, target) == false) {
        throw VerificationException(UpdateError.SignatureMismatch)
      }

      _state.value = UpdateState.ReadyToInstall(release, target)
      showReadyNotification(release)
    } catch (e: kotlinx.coroutines.CancellationException) {
      partial.delete()
      throw e
    } catch (e: VerificationException) {
      Log.w(TAG, "Update verification failed: ${e.reason}")
      partial.delete()
      target.delete()
      notifications().cancel(NOTIFICATION_ID)
      _state.value = UpdateState.Failed(e.reason, release)
    } catch (e: Exception) {
      Log.w(TAG, "Update download failed", e)
      partial.delete()
      notifications().cancel(NOTIFICATION_ID)
      _state.value = UpdateState.Failed(
        if (e is IOException) UpdateError.Network else UpdateError.DownloadFailed,
        release,
      )
    }
  }

  /** Streams the APK to [file], reporting progress; returns its SHA-256 (lower-case hex). */
  private suspend fun downloadTo(release: ReleaseInfo, file: File): String {
    val request = Request.Builder()
      .url(release.apkUrl)
      .header("User-Agent", "MpvFlux-Updater")
      .build()
    val digest = MessageDigest.getInstance("SHA-256")
    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
      val body = response.body
      val total = body.contentLength().takeIf { it > 0 } ?: release.apkSize
      var downloaded = 0L
      var lastReported = -1
      body.byteStream().use { input ->
        file.outputStream().use { output ->
          val buffer = ByteArray(64 * 1024)
          while (true) {
            kotlin.coroutines.coroutineContext.ensureActive()
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            digest.update(buffer, 0, read)
            downloaded += read
            // Report at most once per percent to keep UI and notification updates cheap.
            val percent = if (total > 0) (downloaded * 100 / total).toInt() else 0
            if (percent != lastReported) {
              lastReported = percent
              _state.value = UpdateState.Downloading(release, downloaded, total)
              showProgressNotification(release, percent)
            }
          }
        }
      }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  private class VerificationException(val reason: UpdateError) : Exception(reason.name)

  // ---------------------------------------------------------------------------
  // Notifications
  // ---------------------------------------------------------------------------

  private fun notifications() = NotificationManagerCompat.from(context)

  private fun canNotify(): Boolean =
    (
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
      ) && notifications().areNotificationsEnabled()

  private fun ensureChannel() {
    val channel = NotificationChannel(
      CHANNEL_ID,
      context.getString(R.string.update_channel_name),
      NotificationManager.IMPORTANCE_LOW,
    ).apply { setShowBadge(false) }
    context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
  }

  private fun openAppIntent(): PendingIntent? {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
      ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
      ?.putExtra(EXTRA_OPEN_UPDATES, true)
      ?: return null
    return PendingIntent.getActivity(
      context,
      NOTIFICATION_ID,
      launch,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  @Suppress("MissingPermission")
  private fun showProgressNotification(release: ReleaseInfo, percent: Int) {
    if (!canNotify()) return
    ensureChannel()
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_sym_download)
      .setContentTitle(context.getString(R.string.update_notification_downloading, release.version.toString()))
      .setProgress(100, percent, false)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setContentIntent(openAppIntent())
      .build()
    runCatching { notifications().notify(NOTIFICATION_ID, notification) }
  }

  @Suppress("MissingPermission")
  private fun showReadyNotification(release: ReleaseInfo) {
    if (!canNotify()) return
    ensureChannel()
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_sym_download)
      .setContentTitle(context.getString(R.string.update_notification_ready, release.version.toString()))
      .setContentText(context.getString(R.string.update_notification_ready_text))
      .setAutoCancel(true)
      .setContentIntent(openAppIntent())
      .build()
    runCatching { notifications().notify(NOTIFICATION_ID, notification) }
  }

  companion object {
    private const val TAG = "UpdateManager"
    private const val CHANNEL_ID = "app_updates"
    private const val NOTIFICATION_ID = 4210
    private const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000

    /** Set on the notification's intent so the app can open the Updates screen. */
    const val EXTRA_OPEN_UPDATES = "open_updates"
  }
}
