package app.marlboroadvance.mpvex.ui.update

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.update.UpdateError
import app.marlboroadvance.mpvex.update.UpdateState

/** Short, user-facing explanation of an updater error. */
@Composable
fun updateErrorText(error: UpdateError): String =
  stringResource(
    when (error) {
      UpdateError.Network -> R.string.update_error_network
      UpdateError.RateLimited -> R.string.update_error_rate_limited
      UpdateError.NoRelease -> R.string.update_error_no_release
      UpdateError.NoApkForDevice -> R.string.update_error_no_apk
      UpdateError.DownloadFailed -> R.string.update_error_download
      UpdateError.ChecksumMismatch -> R.string.update_error_checksum
      UpdateError.SignatureMismatch -> R.string.update_error_signature
      UpdateError.InstallFailed -> R.string.update_error_install
      UpdateError.InstallCancelled -> R.string.update_error_install_cancelled
      UpdateError.PermissionMissing -> R.string.update_error_permission
      UpdateError.Unknown -> R.string.update_error_unknown
    },
  )

/** "24.6 MB" in the phone's locale. */
@Composable
fun formatBytes(bytes: Long): String = Formatter.formatShortFileSize(LocalContext.current, bytes)

/** True when there is something the user should act on (shown as a dot / highlighted card). */
fun UpdateState.needsAttention(): Boolean =
  when (this) {
    is UpdateState.Available -> !skipped
    is UpdateState.Downloading, is UpdateState.ReadyToInstall, is UpdateState.Installing -> true
    is UpdateState.Failed -> release != null
    else -> false
  }
