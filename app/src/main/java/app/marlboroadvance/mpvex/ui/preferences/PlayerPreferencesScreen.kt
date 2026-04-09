package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.player.PlayerOrientation
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.toFixed
import app.marlboroadvance.mpvex.ui.theme.DarkMode
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.SwitchPreference
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Serializable
object PlayerPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val preferences = koinInject<PlayerPreferences>()
    val appPreferences = koinInject<AppearancePreferences>()
    
    val orientationNames = PlayerOrientation.entries.associateWith { stringResource(it.titleRes) }
    
    // OLED Optimization: Pure black background in dark mode
    val darkMode by appPreferences.darkMode.collectAsState()
    val systemDarkTheme = isSystemInDarkTheme()
    val isDark = when (darkMode) {
      DarkMode.Dark -> true
      DarkMode.Light -> false
      DarkMode.System -> systemDarkTheme
    }
    val backgroundColor = if (isDark) Color.Black else MaterialTheme.colorScheme.background

    Surface(
      modifier = Modifier.fillMaxSize(),
      color = backgroundColor
    ) {
      Scaffold(
        containerColor = Color.Transparent,
        topBar = {
          TopAppBar(
            modifier = Modifier.statusBarsPadding(),
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = Color.Transparent,
              scrolledContainerColor = Color.Transparent
            ),
            title = { 
              Text(
                text = stringResource(id = R.string.pref_player),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
              ) 
            },
            navigationIcon = {
              IconButton(onClick = backstack::removeLastOrNull) {
                Icon(
                  Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                )
              }
            },
          )
        },
      ) { padding ->
        ProvidePreferenceLocals {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(padding),
          ) {
            item {
              PreferenceSectionHeader(title = "General")
            }
            
            item {
              PreferenceCard {
                val orientation by preferences.orientation.collectAsState()
                ListPreference(
                  value = orientation,
                  onValueChange = preferences.orientation::set,
                  values = PlayerOrientation.entries,
                  valueToText = { AnnotatedString(orientationNames[it] ?: "") },
                  title = { 
                    Text(
                      text = stringResource(id = R.string.pref_player_orientation),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = { 
                    Text(
                      text = orientationNames[orientation] ?: "",
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ) 
                  },
                )
                
                PreferenceDivider()
                
                val savePositionOnQuit by preferences.savePositionOnQuit.collectAsState()
                SwitchPreference(
                  value = savePositionOnQuit,
                  onValueChange = preferences.savePositionOnQuit::set,
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_player_save_position_on_quit),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                )
                
                PreferenceDivider()
                
                val closeAfterEndOfVideo by preferences.closeAfterReachingEndOfVideo.collectAsState()
                SwitchPreference(
                  value = closeAfterEndOfVideo,
                  onValueChange = preferences.closeAfterReachingEndOfVideo::set,
                  title = { 
                    Text(
                      text = stringResource(id = R.string.pref_player_close_after_eof),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                )
                
                PreferenceDivider()
                
                val autoplayNextVideo by preferences.autoplayNextVideo.collectAsState()
                SwitchPreference(
                  value = autoplayNextVideo,
                  onValueChange = preferences.autoplayNextVideo::set,
                  title = { 
                    Text(
                      text = "Autoplay next video",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = if (autoplayNextVideo)
                        "Automatically play next video when current ends"
                      else
                        "Stay on current video when it ends",
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                )
                
                PreferenceDivider()
                
                val playlistMode by preferences.playlistMode.collectAsState()
                SwitchPreference(
                  value = playlistMode,
                  onValueChange = preferences.playlistMode::set,
                  title = { 
                    Text(
                      text = "Enable next/previous navigation",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = if (playlistMode)
                        "Show next/previous buttons for all videos in folder"
                      else
                        "Play videos individually (select multiple for playlist)",
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                )
                
                PreferenceDivider()

                val rememberBrightness by preferences.rememberBrightness.collectAsState()
                SwitchPreference(
                  value = rememberBrightness,
                  onValueChange = preferences.rememberBrightness::set,
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_player_remember_brightness),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                )

                PreferenceDivider()

                val autoPiPOnNavigation by preferences.autoPiPOnNavigation.collectAsState()
                SwitchPreference(
                  value = autoPiPOnNavigation,
                  onValueChange = preferences.autoPiPOnNavigation::set,
                  title = { 
                    Text(
                      text = "Auto Picture-in-Picture",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = "Automatically enter PIP mode when pressing home or back",
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                )

                PreferenceDivider()

                val keepScreenOnWhenPaused by preferences.keepScreenOnWhenPaused.collectAsState()
                SwitchPreference(
                  value = keepScreenOnWhenPaused,
                  onValueChange = preferences.keepScreenOnWhenPaused::set,
                  title = { 
                    Text(
                      text = "Keep screen on when paused",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = if (keepScreenOnWhenPaused)
                        "Screen stays awake while video is paused"
                      else
                        "Screen can turn off while video is paused",
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                )
              }
            }

            item {
              PreferenceSectionHeader(title = stringResource(R.string.pref_player_seeking_title))
            }
            
            item {
              PreferenceCard {
                val showDoubleTapOvals by preferences.showDoubleTapOvals.collectAsState()
                SwitchPreference(
                  value = showDoubleTapOvals,
                  onValueChange = preferences.showDoubleTapOvals::set,
                  title = { 
                    Text(
                      text = stringResource(R.string.show_splash_ovals_on_double_tap_to_seek),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                )
                
                PreferenceDivider()
                
                val showSeekTimeWhileSeeking by preferences.showSeekTimeWhileSeeking.collectAsState()
                SwitchPreference(
                  value = showSeekTimeWhileSeeking,
                  onValueChange = preferences.showSeekTimeWhileSeeking::set,
                  title = { 
                    Text(
                      text = stringResource(R.string.show_time_on_double_tap_to_seek),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                )
                
                PreferenceDivider()
                
                val usePreciseSeeking by preferences.usePreciseSeeking.collectAsState()
                SwitchPreference(
                  value = usePreciseSeeking,
                  onValueChange = preferences.usePreciseSeeking::set,
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_player_use_precise_seeking),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                )
                
                PreferenceDivider()
                
                val customSkipDuration by preferences.customSkipDuration.collectAsState()
                SliderPreference(
                  value = customSkipDuration.toFloat(),
                  onValueChange = { preferences.customSkipDuration.set(it.roundToInt()) },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_player_custom_skip_duration_title),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  valueRange = 5f..180f,
                  summary = {
                     val summaryText = stringResource(R.string.pref_player_custom_skip_duration_summary)
                     Text(
                       text = "$summaryText ($customSkipDuration s)",
                       style = MaterialTheme.typography.bodySmall,
                       fontWeight = FontWeight.Light,
                       color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                     )
                  },
                  onSliderValueChange = { preferences.customSkipDuration.set(it.roundToInt()) },
                  sliderValue = customSkipDuration.toFloat(),
                )
              }
            }

            item {
              PreferenceSectionHeader(title = stringResource(R.string.pref_player_gestures))
            }
            
            item {
              PreferenceCard {
                val brightnessGesture by preferences.brightnessGesture.collectAsState()
                SwitchPreference(
                  value = brightnessGesture,
                  onValueChange = preferences.brightnessGesture::set,
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_player_gestures_brightness),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                )
                
                PreferenceDivider()
                
                val volumeGesture by preferences.volumeGesture.collectAsState()
                SwitchPreference(
                  value = volumeGesture,
                  onValueChange = preferences.volumeGesture::set,
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_player_gestures_volume),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                )
                
                PreferenceDivider()
                
                val pinchToZoomGesture by preferences.pinchToZoomGesture.collectAsState()
                SwitchPreference(
                  value = pinchToZoomGesture,
                  onValueChange = preferences.pinchToZoomGesture::set,
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_player_gestures_pinch_to_zoom),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                )
                
                PreferenceDivider()
                
                val horizontalSwipeToSeek by preferences.horizontalSwipeToSeek.collectAsState()
                SwitchPreference(
                  value = horizontalSwipeToSeek,
                  onValueChange = preferences.horizontalSwipeToSeek::set,
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_player_gestures_horizontal_swipe_to_seek),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                )
                
                PreferenceDivider()
                
                val horizontalSwipeSensitivity by preferences.horizontalSwipeSensitivity.collectAsState()
                SliderPreference(
                  value = horizontalSwipeSensitivity,
                  onValueChange = { preferences.horizontalSwipeSensitivity.set(it.toFixed(3)) },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_player_gestures_horizontal_swipe_sensitivity),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  valueRange = 0.020f..0.1f,
                  summary = {
                    val sensitivityPercent = (horizontalSwipeSensitivity * 1000).toInt()
                    Text(
                      text = "Current: ${sensitivityPercent}/100 (${if (sensitivityPercent < 30) "Low" else if (sensitivityPercent < 55) "Medium" else "High"})",
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                  onSliderValueChange = { preferences.horizontalSwipeSensitivity.set(it.toFixed(3)) },
                  sliderValue = horizontalSwipeSensitivity,
                )
                
                PreferenceDivider()
                
                val holdForMultipleSpeed by preferences.holdForMultipleSpeed.collectAsState()
                SliderPreference(
                  value = holdForMultipleSpeed,
                  onValueChange = { preferences.holdForMultipleSpeed.set(it.toFixed(2)) },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_player_gestures_hold_for_multiple_speed),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  valueRange = 0f..6f,
                  summary = {
                    Text(
                      text = if (holdForMultipleSpeed == 0F) {
                        stringResource(R.string.generic_disabled)
                      } else {
                        "%.2fx".format(holdForMultipleSpeed)
                      },
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                  onSliderValueChange = { preferences.holdForMultipleSpeed.set(it.toFixed(2)) },
                  sliderValue = holdForMultipleSpeed,
                )
                
                PreferenceDivider()
                
                val showDynamicSpeedOverlay by preferences.showDynamicSpeedOverlay.collectAsState()
                SwitchPreference(
                  value = showDynamicSpeedOverlay,
                  onValueChange = preferences.showDynamicSpeedOverlay::set,
                  title = { 
                    Text(
                      text = "Dynamic Speed Overlay",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = { 
                    Text(
                      text = "Show advance overlay for speed control during long press and swipe",
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ) 
                  }
                )
              }
            }
          }
        }
      }
    }
  }
}
