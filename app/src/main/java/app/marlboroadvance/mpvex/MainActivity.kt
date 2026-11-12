package app.marlboroadvance.mpvex

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.browser.MainScreen
import app.marlboroadvance.mpvex.ui.theme.DarkMode
import app.marlboroadvance.mpvex.ui.theme.MpvexTheme
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.permission.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

// Added correct import for Navigation3 Scene
import androidx.navigation3.scene.Scene

/**
 * Main entry point for the application
 */
class MainActivity : ComponentActivity() {
  private val appearancePreferences by inject<AppearancePreferences>()
  private val thumbnailRepository by inject<app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository>()
  private val updateManager by inject<app.marlboroadvance.mpvex.update.UpdateManager>()

  // Incremented when the "update ready" notification asks to open Settings → Updates.
  private val openUpdatesRequests = kotlinx.coroutines.flow.MutableStateFlow(0)

  // Release notes to show once after an in-app update ("What's new"), if any.
  private var whatsNew: Pair<String, String>? = null

  // Create a coroutine scope tied to the activity lifecycle
  private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  // Register the ActivityResultLauncher at class level
  private val mediaAccessLauncher = registerForActivityResult(
    ActivityResultContracts.StartIntentSenderForResult()
  ) { result ->
    PermissionUtils.handleMediaAccessResult(result.resultCode)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    PermissionUtils.setMediaAccessLauncher(mediaAccessLauncher)

    if (savedInstanceState == null) {
      whatsNew = updateManager.consumeWhatsNew()
      handleUpdateIntent(intent)
    }

    // Storage-wide media scan (rate-limited to once every 12 h). Started here rather than in
    // App.onCreate so that opening a video straight from another app doesn't scan storage.
    (application as? App)?.runLaunchMediaScanIfDue()

    setContent {
      // Set up theme and edge-to-edge display
      val dark by appearancePreferences.darkMode.collectAsState()
      val systemInDarkTheme = isSystemInDarkTheme()
      
      // MEMOIZED: Stabilize isDarkMode calculation
      val isDarkMode = remember(dark, systemInDarkTheme) {
        dark == DarkMode.Dark || (dark == DarkMode.System && systemInDarkTheme)
      }

      // LAUNCHED EFFECT: Only trigger edge-to-edge update when theme changes.
      // This stops SurfaceFlinger spam caused by redundant window transactions.
      LaunchedEffect(isDarkMode) {
        enableEdgeToEdge(
          statusBarStyle = SystemBarStyle.auto(
            lightScrim = Color.White.toArgb(),
            darkScrim = Color.Transparent.toArgb(),
          ) { isDarkMode },
          navigationBarStyle = SystemBarStyle.auto(
            lightScrim = Color.White.toArgb(),
            darkScrim = Color.Transparent.toArgb(),
          ) { isDarkMode }
        )
      }

      MpvexTheme {
        Surface {
          Navigator()
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    app.marlboroadvance.mpvex.utils.media.MediaStoreWatcher.start(this)
    // Continue folder thumbnail batches paused while a video was playing.
    thumbnailRepository.resumeFolderGeneration()
    // At most one small request to GitHub a day (only if auto-check is on).
    updateManager.checkIfDue()
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleUpdateIntent(intent)
  }

  private fun handleUpdateIntent(intent: android.content.Intent?) {
    if (intent?.getBooleanExtra(app.marlboroadvance.mpvex.update.UpdateManager.EXTRA_OPEN_UPDATES, false) == true) {
      intent.removeExtra(app.marlboroadvance.mpvex.update.UpdateManager.EXTRA_OPEN_UPDATES)
      openUpdatesRequests.value += 1
    }
  }

  override fun onStop() {
    app.marlboroadvance.mpvex.utils.media.MediaStoreWatcher.stop(this)
    // Player opened or app in background: don't decode thumbnails alongside playback.
    thumbnailRepository.pauseFolderGeneration()
    super.onStop()
  }

  override fun onDestroy() {
    try {
      super.onDestroy()
    } catch (e: Exception) {
      Log.e("MainActivity", "Error during onDestroy", e)
    }
  }

  /**
   * Navigator that handles screen transitions and provides shared states
   */
  @Composable
  fun Navigator() {
    val backstack = rememberNavBackStack(MainScreen)

    @Suppress("UNCHECKED_CAST")
    val typedBackstack = backstack as NavBackStack<Screen>

    // MEMOIZED: Navigation Transitions to keep UI transactions stable.
    val popTransitionSpec = remember {
      { scope: AnimatedContentTransitionScope<Scene<Screen>> ->
        (
          fadeIn(animationSpec = tween(220)) +
            slideIn(animationSpec = tween(220)) { size -> IntOffset(-size.width / 2, 0) }
        ) togetherWith (
            fadeOut(animationSpec = tween(220)) +
              slideOut(animationSpec = tween(220)) { size -> IntOffset(size.width / 2, 0) }
        )
      }
    }

    val transitionSpec = remember {
      { scope: AnimatedContentTransitionScope<Scene<Screen>> ->
        (
          fadeIn(animationSpec = tween(220)) +
            slideIn(animationSpec = tween(220)) { size -> IntOffset(size.width / 2, 0) }
        ) togetherWith (
            fadeOut(animationSpec = tween(220)) +
              slideOut(animationSpec = tween(220)) { size -> IntOffset(-size.width / 2, 0) }
        )
      }
    }

    val predictivePopTransitionSpec = remember {
      { scope: AnimatedContentTransitionScope<Scene<Screen>>, _: Int ->
        (
          fadeIn(animationSpec = tween(220)) +
            scaleIn(
              animationSpec = tween(220, delayMillis = 30),
              initialScale = .9f,
              TransformOrigin(-1f, .5f),
            )
        ) togetherWith (
            fadeOut(animationSpec = tween(220)) +
              scaleOut(
                animationSpec = tween(220, delayMillis = 30),
                targetScale = .9f,
                TransformOrigin(-1f, .5f),
              )
        )
      }
    }

    // Provide LocalBackStack to all screens
    CompositionLocalProvider(
      LocalBackStack provides typedBackstack
    ) {
      NavDisplay(
        backStack = typedBackstack,
        onBack = { typedBackstack.removeLastOrNull() },
        entryProvider = { route -> NavEntry(route) { route.Content() } },
        popTransitionSpec = popTransitionSpec,
        transitionSpec = transitionSpec,
        predictivePopTransitionSpec = predictivePopTransitionSpec,
      )
      UpdatePrompts(typedBackstack)
    }
  }

  /**
   * Update popups over the app: "What's new" once after an in-app update, the one-time
   * "update available" sheet, and opening Settings → Updates from the notification.
   */
  @Composable
  private fun UpdatePrompts(backstack: NavBackStack<Screen>) {
    val updatesScreen = app.marlboroadvance.mpvex.ui.preferences.UpdatesScreen

    val openRequests by openUpdatesRequests.collectAsState()
    LaunchedEffect(openRequests) {
      if (openRequests > 0 && backstack.lastOrNull() != updatesScreen) backstack.add(updatesScreen)
    }

    var whatsNewState by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(whatsNew) }
    whatsNewState?.let { (version, notes) ->
      app.marlboroadvance.mpvex.ui.update.WhatsNewSheet(version, notes) { whatsNewState = null }
    }

    val state by updateManager.state.collectAsState()
    val available = state as? app.marlboroadvance.mpvex.update.UpdateState.Available
    var popupFor by androidx.compose.runtime.remember {
      androidx.compose.runtime.mutableStateOf<app.marlboroadvance.mpvex.update.ReleaseInfo?>(null)
    }
    LaunchedEffect(available?.release?.version) {
      val release = available?.release ?: return@LaunchedEffect
      if (available.skipped || updateManager.wasNotified(release)) return@LaunchedEffect
      if (backstack.lastOrNull() == updatesScreen) {
        // Already looking at it (manual check): no popup needed.
        updateManager.markNotified(release)
      } else if (whatsNewState == null) {
        popupFor = release
      }
    }
    popupFor?.let { release ->
      app.marlboroadvance.mpvex.ui.update.UpdateAvailableSheet(
        release = release,
        onLater = {
          updateManager.markNotified(release)
          popupFor = null
        },
        onView = {
          updateManager.markNotified(release)
          popupFor = null
          backstack.add(updatesScreen)
        },
      )
    }
  }
}
