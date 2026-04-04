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

  var isPermissionDenied: Boolean by mutableStateOf(false)
    private set

  var isBrowserBottomBarVisible: Boolean by mutableStateOf(false)
    private set

  var onlyVideosSelected: Boolean by mutableStateOf(false)
    private set

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
   */
  fun updatePermissionState(denied: Boolean) {
    isPermissionDenied = denied
  }

  /**
   * Update bottom navigation bar visibility based on floating bottom bar state.
   */
  fun updateBottomBarVisibility(visible: Boolean) {
    shouldHideNavigationBar = !visible
  }
}
