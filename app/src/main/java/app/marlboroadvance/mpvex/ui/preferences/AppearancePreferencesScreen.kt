package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.MultiChoiceSegmentedButton
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.preferences.components.ThemePicker
import app.marlboroadvance.mpvex.ui.theme.DarkMode
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.SwitchPreference
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Serializable
object AppearancePreferencesScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val preferences = koinInject<AppearancePreferences>()
        val browserPreferences = koinInject<BrowserPreferences>()
        val gesturePreferences = koinInject<GesturePreferences>()
        val backstack = LocalBackStack.current
        val systemDarkTheme = isSystemInDarkTheme()

        val darkMode by preferences.darkMode.collectAsState()
        val appTheme by preferences.appTheme.collectAsState()

        // OLED Optimization: Pure black background in dark mode
        val isDarkMode = when (darkMode) {
            DarkMode.Dark -> true
            DarkMode.Light -> false
            DarkMode.System -> systemDarkTheme
        }
        val backgroundColor = if (isDarkMode) Color.Black else MaterialTheme.colorScheme.background

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
                                text = stringResource(R.string.pref_appearance_title),
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
                            PreferenceSectionHeader(title = stringResource(id = R.string.pref_appearance_category_theme))
                        }

                        item {
                            PreferenceCard {
                                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                                    // Dark mode selector (Modern Segmented Button)
                                    MultiChoiceSegmentedButton(
                                        choices = DarkMode.entries.map { stringResource(it.titleRes) }.toImmutableList(),
                                        selectedIndices = persistentListOf(DarkMode.entries.indexOf(darkMode)),
                                        onClick = { preferences.darkMode.set(DarkMode.entries[it]) },
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // AMOLED mode state - need it before theme picker
                                val amoledMode by preferences.amoledMode.collectAsState()

                                // Theme picker
                                ThemePicker(
                                    currentTheme = appTheme,
                                    isDarkMode = isDarkMode,
                                    onThemeSelected = { preferences.appTheme.set(it) },
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )

                                PreferenceDivider()

                                // AMOLED mode toggle
                                SwitchPreference(
                                    value = amoledMode,
                                    onValueChange = { newValue ->
                                        preferences.amoledMode.set(newValue)
                                    },
                                    title = { 
                                        Text(
                                            text = stringResource(id = R.string.pref_appearance_amoled_mode_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        ) 
                                    },
                                    summary = {
                                        Text(
                                            text = stringResource(id = R.string.pref_appearance_amoled_mode_summary),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Light,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    },
                                    enabled = darkMode != DarkMode.Light
                                )
                            }
                        }

                        item {
                            PreferenceSectionHeader(title = stringResource(id = R.string.pref_appearance_category_file_browser))
                        }

                        item {
                            PreferenceCard {
                                val unlimitedNameLines by preferences.unlimitedNameLines.collectAsState()
                                SwitchPreference(
                                    value = unlimitedNameLines,
                                    onValueChange = { preferences.unlimitedNameLines.set(it) },
                                    title = {
                                        Text(
                                            text = stringResource(id = R.string.pref_appearance_unlimited_name_lines_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    summary = {
                                        Text(
                                            text = stringResource(id = R.string.pref_appearance_unlimited_name_lines_summary),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Light,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                )

                                PreferenceDivider()

                                val showUnplayedOldVideoLabel by preferences.showUnplayedOldVideoLabel.collectAsState()
                                SwitchPreference(
                                    value = showUnplayedOldVideoLabel,
                                    onValueChange = { preferences.showUnplayedOldVideoLabel.set(it) },
                                    title = {
                                        Text(
                                            text = stringResource(id = R.string.pref_appearance_show_unplayed_old_video_label_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    summary = {
                                        Text(
                                            text = stringResource(id = R.string.pref_appearance_show_unplayed_old_video_label_summary),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Light,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                )

                                PreferenceDivider()

                                val unplayedOldVideoDays by preferences.unplayedOldVideoDays.collectAsState()
                                SliderPreference(
                                    value = unplayedOldVideoDays.toFloat(),
                                    onValueChange = { preferences.unplayedOldVideoDays.set(it.roundToInt()) },
                                    title = { 
                                        Text(
                                            text = stringResource(id = R.string.pref_appearance_unplayed_old_video_days_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        ) 
                                    },
                                    valueRange = 1f..30f,
                                    summary = {
                                        Text(
                                            text = stringResource(
                                                id = R.string.pref_appearance_unplayed_old_video_days_summary,
                                                unplayedOldVideoDays,
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Light,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    },
                                    onSliderValueChange = { preferences.unplayedOldVideoDays.set(it.roundToInt()) },
                                    sliderValue = unplayedOldVideoDays.toFloat(),
                                    enabled = showUnplayedOldVideoLabel
                                )

                                PreferenceDivider()

                                val autoScrollToLastPlayed by browserPreferences.autoScrollToLastPlayed.collectAsState()
                                SwitchPreference(
                                    value = autoScrollToLastPlayed,
                                    onValueChange = { browserPreferences.autoScrollToLastPlayed.set(it) },
                                    title = {
                                        Text(
                                            text = stringResource(R.string.pref_appearance_auto_scroll_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    summary = {
                                        Text(
                                            text = stringResource(R.string.pref_appearance_auto_scroll_summary),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Light,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                )

                                PreferenceDivider()

                                val watchedThreshold by browserPreferences.watchedThreshold.collectAsState()
                                SliderPreference(
                                    value = watchedThreshold.toFloat(),
                                    onValueChange = { browserPreferences.watchedThreshold.set(it.roundToInt()) },
                                    sliderValue = watchedThreshold.toFloat(),
                                    onSliderValueChange = { browserPreferences.watchedThreshold.set(it.roundToInt()) },
                                    title = { 
                                        Text(
                                            text = stringResource(id = R.string.pref_appearance_watched_threshold_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        ) 
                                    },
                                    valueRange = 50f..100f,
                                    valueSteps = 9,
                                    summary = {
                                        Text(
                                            text = stringResource(
                                                id = R.string.pref_appearance_watched_threshold_summary,
                                                watchedThreshold,
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Light,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    },
                                )

                                PreferenceDivider()

                                val tapThumbnailToSelect by gesturePreferences.tapThumbnailToSelect.collectAsState()
                                SwitchPreference(
                                    value = tapThumbnailToSelect,
                                    onValueChange = { gesturePreferences.tapThumbnailToSelect.set(it) },
                                    title = {
                                        Text(
                                            text = stringResource(id = R.string.pref_gesture_tap_thumbnail_to_select_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    summary = {
                                        Text(
                                            text = stringResource(id = R.string.pref_gesture_tap_thumbnail_to_select_summary),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Light,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                )

                                PreferenceDivider()

                                val showNetworkThumbnails by preferences.showNetworkThumbnails.collectAsState()
                                SwitchPreference(
                                    value = showNetworkThumbnails,
                                    onValueChange = { preferences.showNetworkThumbnails.set(it) },
                                    title = {
                                        Text(
                                            text = stringResource(id = R.string.pref_appearance_show_network_thumbnails_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    summary = {
                                        Text(
                                            text = stringResource(id = R.string.pref_appearance_show_network_thumbnails_summary),
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
