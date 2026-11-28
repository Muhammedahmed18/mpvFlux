package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.player.PlayerOrientation
import app.marlboroadvance.mpvex.ui.player.controls.components.sheets.toFixed
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.ui.utils.readableContentWidth
import kotlinx.serialization.Serializable
import app.marlboroadvance.mpvex.ui.preferences.components.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import app.marlboroadvance.mpvex.ui.preferences.components.SwitchPreference
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Serializable
object PlayerPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val preferences = koinInject<PlayerPreferences>()

    val orientationNames = PlayerOrientation.entries.associateWith { stringResource(it.titleRes) }

    val backgroundColor = rememberPreferenceBackgroundColor()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Surface(
      modifier = Modifier.fillMaxSize(),
      color = backgroundColor
    ) {
      Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
          PreferenceTopBar(
            title = {
              Text(
                text = stringResource(id = R.string.pref_player),
                color = MaterialTheme.colorScheme.primary,
              )
            },
            navigationIcon = {
              IconButton(onClick = backstack::removeLastOrNull) {
                Icon(
                  Icons.AutoMirrored.Outlined.ArrowBack,
                  contentDescription = stringResource(R.string.back),
                  tint = MaterialTheme.colorScheme.secondary,
                )
              }
            },
            scrollBehavior = scrollBehavior,
            large = true,
            containerColor = backgroundColor,
          )
        },
      ) { padding ->
        ProvidePreferenceLocals {
          LazyColumn(
            modifier = Modifier.fillMaxSize().readableContentWidth(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
                start = 8.dp,
                end = 8.dp
            )
          ) {
            // ---------------------------------------------------------------
            // Orientation & Display
            // ---------------------------------------------------------------
            item {
              PreferenceSectionHeader(title = stringResource(R.string.pref_section_orientation_display))
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
                    )
                  },
                  summary = {
                    Text(
                      text = orientationNames[orientation] ?: "",
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  },
                )

                val rememberBrightness by preferences.rememberBrightness.collectAsState()
                SwitchPreference(
                  value = rememberBrightness,
                  onValueChange = preferences.rememberBrightness::set,
                  title = {
                    Text(
                      text = stringResource(R.string.pref_player_remember_brightness),
                    )
                  },
                )

                val keepScreenOnWhenPaused by preferences.keepScreenOnWhenPaused.collectAsState()
                SwitchPreference(
                  value = keepScreenOnWhenPaused,
                  onValueChange = preferences.keepScreenOnWhenPaused::set,
                  title = {
                    Text(
                      text = stringResource(R.string.pref_player_keep_screen_on_paused_title),
                    )
                  },
                  summary = {
                    Text(
                      text = if (keepScreenOnWhenPaused)
                        stringResource(R.string.pref_player_keep_screen_on_paused_summary_on)
                      else
                        stringResource(R.string.pref_player_keep_screen_on_paused_summary_off),
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  },
                )

                val reduceMotion by preferences.reduceMotion.collectAsState()
                SwitchPreference(
                  value = reduceMotion,
                  onValueChange = preferences.reduceMotion::set,
                  title = {
                    Text(
                      text = stringResource(R.string.pref_player_display_reduce_player_animation),
                    )
                  },
                )
              }
            }

            // ---------------------------------------------------------------
            // Playback & Resume
            // ---------------------------------------------------------------
            item {
              PreferenceSectionHeader(title = stringResource(R.string.pref_section_playback_resume))
            }

            item {
              PreferenceCard {
                val savePositionOnQuit by preferences.savePositionOnQuit.collectAsState()
                SwitchPreference(
                  value = savePositionOnQuit,
                  onValueChange = preferences.savePositionOnQuit::set,
                  title = {
                    Text(
                      text = stringResource(R.string.pref_player_save_position_on_quit),
                    )
                  },
                )

                val closeAfterEndOfVideo by preferences.closeAfterReachingEndOfVideo.collectAsState()
                SwitchPreference(
                  value = closeAfterEndOfVideo,
                  onValueChange = preferences.closeAfterReachingEndOfVideo::set,
                  title = {
                    Text(
                      text = stringResource(id = R.string.pref_player_close_after_eof),
                    )
                  },
                )
              }
            }

            // ---------------------------------------------------------------
            // Playlist & Autoplay
            // ---------------------------------------------------------------
            item {
              PreferenceSectionHeader(title = stringResource(R.string.pref_section_playlist_autoplay))
            }

            item {
              PreferenceCard {
                val autoplayNextVideo by preferences.autoplayNextVideo.collectAsState()
                SwitchPreference(
                  value = autoplayNextVideo,
                  onValueChange = preferences.autoplayNextVideo::set,
                  title = {
                    Text(
                      text = stringResource(R.string.pref_autoplay_next_video_title),
                    )
                  },
                  summary = {
                    Text(
                      text = if (autoplayNextVideo)
                        stringResource(R.string.pref_autoplay_next_video_summary)
                      else
                        stringResource(R.string.pref_autoplay_next_video_summary_off),
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  },
                )

                val playlistMode by preferences.playlistMode.collectAsState()
                SwitchPreference(
                  value = playlistMode,
                  onValueChange = preferences.playlistMode::set,
                  title = {
                    Text(
                      text = stringResource(R.string.pref_autoplay_title),
                    )
                  },
                  summary = {
                    Text(
                      text = if (playlistMode)
                        stringResource(R.string.pref_autoplay_summary)
                      else
                        stringResource(R.string.pref_autoplay_summary_off),
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  },
                )
              }
            }

            // ---------------------------------------------------------------
            // Seeking
            // ---------------------------------------------------------------
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
                    )
                  },
                )

                val showSeekTimeWhileSeeking by preferences.showSeekTimeWhileSeeking.collectAsState()
                SwitchPreference(
                  value = showSeekTimeWhileSeeking,
                  onValueChange = preferences.showSeekTimeWhileSeeking::set,
                  title = {
                    Text(
                      text = stringResource(R.string.show_time_on_double_tap_to_seek),
                    )
                  },
                )

                val usePreciseSeeking by preferences.usePreciseSeeking.collectAsState()
                SwitchPreference(
                  value = usePreciseSeeking,
                  onValueChange = preferences.usePreciseSeeking::set,
                  title = {
                    Text(
                      text = stringResource(R.string.pref_player_use_precise_seeking),
                    )
                  },
                )

                val customSkipDuration by preferences.customSkipDuration.collectAsState()
                SliderPreference(
                  value = customSkipDuration.toFloat(),
                  onValueChange = { preferences.customSkipDuration.set(it.roundToInt()) },
                  title = {
                    Text(
                      text = stringResource(R.string.pref_player_custom_skip_duration_title),
                    )
                  },
                  valueRange = 5f..180f,
                  summary = {
                    Text(
                      text = stringResource(R.string.pref_player_custom_skip_duration_summary) +
                        " (" + pluralStringResource(R.plurals.seconds, customSkipDuration, customSkipDuration) + ")",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  },
                  onSliderValueChange = { preferences.customSkipDuration.set(it.roundToInt()) },
                  sliderValue = customSkipDuration.toFloat(),
                )
              }
            }

            // ---------------------------------------------------------------
            // Gestures (touch + seek/speed merged into one section)
            // ---------------------------------------------------------------
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
                    )
                  },
                )

                val volumeGesture by preferences.volumeGesture.collectAsState()
                SwitchPreference(
                  value = volumeGesture,
                  onValueChange = preferences.volumeGesture::set,
                  title = {
                    Text(
                      text = stringResource(R.string.pref_player_gestures_volume),
                    )
                  },
                )

                // Only relevant while at least one of the two slider gestures is on.
                val swapVolumeAndBrightness by preferences.swapVolumeAndBrightness.collectAsState()
                AnimatedVisibility(visible = brightnessGesture || volumeGesture) {
                  SwitchPreference(
                    value = swapVolumeAndBrightness,
                    onValueChange = preferences.swapVolumeAndBrightness::set,
                    title = {
                      Text(
                        text = stringResource(R.string.swap_the_volume_and_brightness_slider),
                      )
                    },
                  )
                }

                val pinchToZoomGesture by preferences.pinchToZoomGesture.collectAsState()
                SwitchPreference(
                  value = pinchToZoomGesture,
                  onValueChange = preferences.pinchToZoomGesture::set,
                  title = {
                    Text(
                      text = stringResource(R.string.pref_player_gestures_pinch_to_zoom),
                    )
                  },
                )

                val horizontalSwipeToSeek by preferences.horizontalSwipeToSeek.collectAsState()
                SwitchPreference(
                  value = horizontalSwipeToSeek,
                  onValueChange = preferences.horizontalSwipeToSeek::set,
                  title = {
                    Text(
                      text = stringResource(R.string.pref_player_gestures_horizontal_swipe_to_seek),
                    )
                  },
                  summary = {
                    Text(
                      text = stringResource(R.string.pref_player_gestures_horizontal_swipe_to_seek_summary),
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  },
                )

                // Sensitivity only matters while swipe-to-seek is on.
                val horizontalSwipeSensitivity by preferences.horizontalSwipeSensitivity.collectAsState()
                AnimatedVisibility(visible = horizontalSwipeToSeek) {
                  SliderPreference(
                    value = horizontalSwipeSensitivity,
                    onValueChange = { preferences.horizontalSwipeSensitivity.set(it.toFixed(3)) },
                    title = {
                      Text(
                        text = stringResource(R.string.pref_player_gestures_horizontal_swipe_sensitivity),
                      )
                    },
                    valueRange = 0.020f..0.1f,
                    summary = {
                      // Scale is x/100, matching the "Current: %1$d/100" string.
                      val sensitivityValue = (horizontalSwipeSensitivity * 1000).toInt()
                      Text(
                        text = stringResource(
                          R.string.pref_player_gestures_horizontal_swipe_sensitivity_current_summary,
                          sensitivityValue,
                          stringResource(
                            when {
                              sensitivityValue < 30 -> R.string.pref_player_gestures_horizontal_swipe_sensitivity_low
                              sensitivityValue < 55 -> R.string.pref_player_gestures_horizontal_swipe_sensitivity_medium
                              else -> R.string.pref_player_gestures_horizontal_swipe_sensitivity_high
                            },
                          ),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    },
                    onSliderValueChange = { preferences.horizontalSwipeSensitivity.set(it.toFixed(3)) },
                    sliderValue = horizontalSwipeSensitivity,
                  )
                }

                val holdForMultipleSpeed by preferences.holdForMultipleSpeed.collectAsState()
                SliderPreference(
                  value = holdForMultipleSpeed,
                  onValueChange = { preferences.holdForMultipleSpeed.set(it.toFixed(2)) },
                  title = {
                    Text(
                      text = stringResource(R.string.pref_player_gestures_hold_for_multiple_speed),
                    )
                  },
                  valueRange = 0f..6f,
                  summary = {
                    Text(
                      text = if (holdForMultipleSpeed == 0F) {
                        stringResource(R.string.generic_disabled)
                      } else {
                        stringResource(R.string.pref_player_speed_value, holdForMultipleSpeed)
                      },
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  },
                  onSliderValueChange = { preferences.holdForMultipleSpeed.set(it.toFixed(2)) },
                  sliderValue = holdForMultipleSpeed,
                )

                val showDynamicSpeedOverlay by preferences.showDynamicSpeedOverlay.collectAsState()
                SwitchPreference(
                  value = showDynamicSpeedOverlay,
                  onValueChange = preferences.showDynamicSpeedOverlay::set,
                  title = {
                    Text(
                      text = stringResource(R.string.pref_dynamic_speed_overlay_title),
                    )
                  },
                  summary = {
                    Text(
                      text = stringResource(R.string.pref_dynamic_speed_overlay_summary),
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  },
                )
              }
            }

            // ---------------------------------------------------------------
            // Picture-in-Picture
            // ---------------------------------------------------------------
            item {
              PreferenceSectionHeader(title = stringResource(R.string.pref_section_pip))
            }

            item {
              PreferenceCard {
                val autoPiPOnNavigation by preferences.autoPiPOnNavigation.collectAsState()
                SwitchPreference(
                  value = autoPiPOnNavigation,
                  onValueChange = preferences.autoPiPOnNavigation::set,
                  title = {
                    Text(
                      text = stringResource(R.string.pref_auto_pip_title),
                    )
                  },
                  summary = {
                    Text(
                      text = stringResource(R.string.pref_auto_pip_summary),
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  },
                )
              }
            }
          }
        }
      }
    }
  }
}
