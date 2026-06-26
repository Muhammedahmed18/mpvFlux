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
    }
  }
}
