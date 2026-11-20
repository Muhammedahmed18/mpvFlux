package app.marlboroadvance.mpvex.ui.update

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.update.UpdateError
import app.marlboroadvance.mpvex.update.UpdateState

/** Short, user-facing explanation of a failed update check. */
@Composable
fun updateErrorText(error: UpdateError): String =
  stringResource(
    when (error) {
      UpdateError.Network -> R.string.update_error_network
      UpdateError.RateLimited -> R.string.update_error_rate_limited
      UpdateError.NoRelease -> R.string.update_error_no_release
      UpdateError.Unknown -> R.string.update_error_unknown
    },
  )

/** True when a newer version is available (shown as a dot / highlighted card). */
fun UpdateState.needsAttention(): Boolean = this is UpdateState.Available
