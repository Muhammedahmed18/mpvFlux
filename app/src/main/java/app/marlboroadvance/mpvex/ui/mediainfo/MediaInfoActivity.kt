package app.marlboroadvance.mpvex.ui.mediainfo

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.theme.DarkMode
import app.marlboroadvance.mpvex.ui.theme.MpvexTheme
import org.koin.android.ext.android.inject

/**
 * Media info for one video: opened from the library ("Info") or from other apps
 * (ACTION_VIEW / ACTION_SEND). The UI lives in [MediaInfoScreen]; loading and formatting are
 * in [MediaInfoViewModel], so rotating the phone doesn't re-read the file.
 */
class MediaInfoActivity : ComponentActivity() {
  private val appearancePreferences by inject<AppearancePreferences>()
  private val viewModel: MediaInfoViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Ignored after rotation: the ViewModel already has (or is loading) this file.
    viewModel.load(mediaUri(intent))

    setContent {
      val dark by appearancePreferences.darkMode.collectAsState()
      val isSystemInDarkTheme = isSystemInDarkTheme()
      val isDarkMode = dark == DarkMode.Dark || (dark == DarkMode.System && isSystemInDarkTheme)

      LaunchedEffect(isDarkMode) {
        enableEdgeToEdge(
          statusBarStyle =
            SystemBarStyle.auto(
              lightScrim = Color.White.toArgb(),
              darkScrim = Color.Transparent.toArgb(),
            ) { isDarkMode },
          navigationBarStyle =
            SystemBarStyle.auto(
              lightScrim = Color.Transparent.toArgb(),
              darkScrim = Color.Transparent.toArgb(),
            ) { isDarkMode },
        )
      }

      MpvexTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
          MediaInfoScreen(viewModel = viewModel, onBack = { finish() })
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    viewModel.load(mediaUri(intent))
  }

  private fun mediaUri(intent: Intent?): Uri? =
    when (intent?.action) {
      Intent.ACTION_SEND ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
          @Suppress("DEPRECATION")
          intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
      Intent.ACTION_VIEW -> intent.data
      else -> intent?.data
    }
}
