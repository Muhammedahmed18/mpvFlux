package app.marlboroadvance.mpvex.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.IntentCompat
import org.koin.core.context.GlobalContext
import java.io.File
import java.security.MessageDigest

/**
 * Installs a downloaded update with Android's PackageInstaller. Android always shows its own
 * confirmation screen; the result arrives in [InstallResultReceiver].
 */
object ApkInstaller {
  private const val TAG = "ApkInstaller"
  const val ACTION_INSTALL_RESULT = "app.marlboroadvance.mpvex.update.INSTALL_RESULT"

  /** True if the user has allowed MpvFlux to install apps ("Install unknown apps"). */
  fun canInstallPackages(context: Context): Boolean =
    context.packageManager.canRequestPackageInstalls()

  /** Opens Android's "Install unknown apps" page for MpvFlux. */
  fun unknownAppSourcesIntent(context: Context): Intent =
    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

  /** Starts the install session; throws if the APK can't be handed to the installer. */
  fun install(context: Context, apk: File) {
    val installer = context.packageManager.packageInstaller
    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
      setAppPackageName(context.packageName)
      setSize(apk.length())
    }
    val sessionId = installer.createSession(params)
    try {
      installer.openSession(sessionId).use { session ->
        apk.inputStream().use { input ->
          session.openWrite("base.apk", 0, apk.length()).use { output ->
            input.copyTo(output, bufferSize = 64 * 1024)
            session.fsync(output)
          }
        }
        val intent = Intent(context, InstallResultReceiver::class.java).setAction(ACTION_INSTALL_RESULT)
        // The installer adds the result to this intent, so it must be mutable on Android 12+.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
          (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        val pending = PendingIntent.getBroadcast(context, sessionId, intent, flags)
        session.commit(pending.intentSender)
      }
    } catch (e: Exception) {
      runCatching { installer.abandonSession(sessionId) }
      throw e
    }
  }

  /**
   * Whether [apk] is MpvFlux signed with the same key as the installed app.
   * Returns null when Android can't tell (old versions sometimes don't report an archive's
   * signatures); the installer then makes the final decision.
   */
  fun matchesInstalledSignature(context: Context, apk: File): Boolean? =
    runCatching {
      val pm = context.packageManager
      val archive = archiveInfo(pm, apk) ?: return false
      if (archive.packageName != context.packageName) return false
      val installed = installedInfo(pm, context.packageName)

      val installedCurrent = currentSigners(installed)
      val archiveAll = currentSigners(archive) + pastSigners(archive)
      if (installedCurrent.isEmpty() || archiveAll.isEmpty()) return null
      // The new APK must be signed by the installed key, or by a key rotated from it.
      archiveAll.containsAll(installedCurrent)
    }.onFailure { Log.w(TAG, "Signature check failed", it) }.getOrNull()

  @Suppress("DEPRECATION")
  private fun archiveInfo(pm: PackageManager, apk: File): PackageInfo? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      pm.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNING_CERTIFICATES)
    } else {
      pm.getPackageArchiveInfo(apk.path, PackageManager.GET_SIGNATURES)
    }

  @Suppress("DEPRECATION")
  private fun installedInfo(pm: PackageManager, packageName: String): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
    } else {
      pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
    }

  @Suppress("DEPRECATION")
  private fun currentSigners(info: PackageInfo): Set<String> {
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      // The certificate(s) the APK is signed with right now.
      info.signingInfo?.apkContentsSigners
    } else {
      info.signatures
    }
    return signatures.orEmpty().map { sha256(it.toByteArray()) }.toSet()
  }

  private fun pastSigners(info: PackageInfo): Set<String> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return emptySet()
    val signing = info.signingInfo ?: return emptySet()
    if (signing.hasMultipleSigners() || !signing.hasPastSigningCertificates()) return emptySet()
    return signing.signingCertificateHistory.orEmpty().map { sha256(it.toByteArray()) }.toSet()
  }

  private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

/** Receives the installer's result and forwards it to [UpdateManager]. */
class InstallResultReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val manager = runCatching { GlobalContext.get().get<UpdateManager>() }.getOrNull()
    when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
      PackageInstaller.STATUS_PENDING_USER_ACTION -> {
        // Show Android's "Do you want to update this app?" screen.
        val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
        if (confirm != null) {
          confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          runCatching { context.startActivity(confirm) }
            .onFailure { manager?.onInstallFinished(InstallOutcome.Failed(it.message)) }
        }
      }
      PackageInstaller.STATUS_SUCCESS -> manager?.onInstallFinished(InstallOutcome.Success)
      PackageInstaller.STATUS_FAILURE_ABORTED -> manager?.onInstallFinished(InstallOutcome.Cancelled)
      PackageInstaller.STATUS_FAILURE_CONFLICT,
      PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
      -> manager?.onInstallFinished(InstallOutcome.SignatureMismatch)
      else -> {
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        Log.w("InstallResultReceiver", "Install failed: status=$status $message")
        manager?.onInstallFinished(InstallOutcome.Failed(message))
      }
    }
  }
}

sealed interface InstallOutcome {
  data object Success : InstallOutcome
  data object Cancelled : InstallOutcome
  data object SignatureMismatch : InstallOutcome
  data class Failed(val message: String?) : InstallOutcome
}
