package app.marlboroadvance.mpvex.ui.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Centralized state holder for navigation bar visibility states.
 * Uses Compose MutableState for reactive updates without polling.
 */
object NavigationBarState {
  var isInSelectionMode: Boolean by mutableStateOf(false)
    private set

  var shouldHideNavigationBar: Boolean by mutableStateOf(false)
    private set

  /**
   * True once the storage permission has actually been checked. Until then the bar stays
   * hidden, so it never flashes up on the first frame and then disappears.
   */
  var isPermissionKnown: Boolean by mutableStateOf(false)
    private set

  var isPermissionDenied: Boolean by mutableStateOf(false)
    private set

  var isBrowserBottomBarVisible: Boolean by mutableStateOf(false)
    private set

  var onlyVideosSelected: Boolean by mutableStateOf(false)
    private set

  /**
   * Single source of truth for showing the navigation bar: permission checked and granted,
   * and not hidden by selection mode or the floating bottom bar.
   */
  val isNavigationBarVisible: Boolean
    get() = isPermissionKnown && !isPermissionDenied && !shouldHideNavigationBar

  /**
   * Update selection state and navigation bar visibility.
   * Only hides navigation bar when videos are selected AND in selection mode.
   */
  fun updateSelectionState(
    inSelectionMode: Boolean,
    onlyVideos: Boolean
  ) {
    isInSelectionMode = inSelectionMode
    onlyVideosSelected = onlyVideos
    shouldHideNavigationBar = inSelectionMode && onlyVideos
  }

  /**
   * Update permission state to control navigation bar visibility.
   * Also marks the permission as checked.
   */
  fun updatePermissionState(denied: Boolean) {
    isPermissionDenied = denied
    isPermissionKnown = true
  }

  /**
   * Update bottom navigation bar visibility based on floating bottom bar state.
   */
  fun updateBottomBarVisibility(visible: Boolean) {
    shouldHideNavigationBar = !visible
  }
}
