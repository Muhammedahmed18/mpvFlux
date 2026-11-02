package app.marlboroadvance.mpvex.presentation.components.pullrefresh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A reusable Box composable that wraps content with pull-to-refresh functionality.
 *
 * Built on Material 3's pull-to-refresh with the M3 Expressive loading indicator (this
 * replaced the old Material 2 `pullrefresh` API, which was the app's last Material 2
 * dependency). The M3 modifier only reacts to over-scroll, so it naturally activates
 * only when the list is already scrolled to the top.
 *
 * @param isRefreshing State that tracks whether refresh is in progress
 * @param onRefresh Lambda to invoke when refresh is triggered
 * @param modifier Modifier to apply to the Box
 * @param enabled Whether pulling to refresh is allowed
 * @param listState Kept for API compatibility; the M3 modifier handles "only at top" itself
 * @param refreshThreshold Distance needed to pull to trigger the refresh
 * @param delayAfterRefresh Delay (in milliseconds) to show indicator after refresh completes
 * @param content Content to display inside the Box
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun PullRefreshBox(
  isRefreshing: MutableState<Boolean>,
  onRefresh: suspend () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  listState: LazyListState? = null,
  refreshThreshold: Dp = PullToRefreshDefaults.PositionalThreshold,
  delayAfterRefresh: Long = 800L,
  content: @Composable BoxScope.() -> Unit,
) {
  val coroutineScope = rememberCoroutineScope()
  val pullToRefreshState = rememberPullToRefreshState()

  Box(
    modifier = modifier.pullToRefresh(
      isRefreshing = isRefreshing.value,
      state = pullToRefreshState,
      enabled = enabled,
      threshold = refreshThreshold,
      onRefresh = {
        if (!isRefreshing.value) {
          isRefreshing.value = true
          coroutineScope.launch {
            try {
              onRefresh()
              delay(delayAfterRefresh)
            } finally {
              isRefreshing.value = false
            }
          }
        }
      },
    ),
  ) {
    content()

    PullToRefreshDefaults.LoadingIndicator(
      state = pullToRefreshState,
      isRefreshing = isRefreshing.value,
      modifier = Modifier.align(Alignment.TopCenter),
    )
  }
}
