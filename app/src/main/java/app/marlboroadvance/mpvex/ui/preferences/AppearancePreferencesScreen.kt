package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import app.marlboroadvance.mpvex.ui.utils.readableContentWidth
import app.marlboroadvance.mpvex.ui.browser.sheets.rememberAudioFolderPicker
import app.marlboroadvance.mpvex.utils.storage.AudioFolderStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.Preference
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

        val isDarkMode = when (darkMode) {
            DarkMode.Dark -> true
            DarkMode.Light -> false
            DarkMode.System -> systemDarkTheme
        }
        val backgroundColor = MaterialTheme.colorScheme.surface
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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
                                text = stringResource(R.string.pref_appearance_title),
                                style = MaterialTheme.typography.titleLargeEmphasized,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = backstack::removeLastOrNull) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        },
                        scrollBehavior = scrollBehavior,
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
                        item {
                            PreferenceSectionHeader(title = stringResource(id = R.string.pref_appearance_category_theme))
                        }

                        item {
                            PreferenceCard {
                                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                                    MultiChoiceSegmentedButton(
                                        choices = DarkMode.entries.map { stringResource(it.titleRes) }.toImmutableList(),
                                        selectedIndices = persistentListOf(DarkMode.entries.indexOf(darkMode)),
                                        onClick = { preferences.darkMode.set(DarkMode.entries[it]) },
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val amoledMode by preferences.amoledMode.collectAsState()

                                ThemePicker(
                                    currentTheme = appTheme,
                                    isDarkMode = isDarkMode,
                                    onThemeSelected = { preferences.appTheme.set(it) },
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )

                                PreferenceDivider()

                                SwitchPreference(
                                    value = amoledMode,
                                    onValueChange = { newValue ->
                                        preferences.amoledMode.set(newValue)
                                    },
                                    title = { 
                                        Text(
                                            text = stringResource(id = R.string.pref_appearance_amoled_mode_title),
                                            style = MaterialTheme.typography.titleMediumEmphasized
                                        ) 
                                    },
                                    summary = {
                                        Text(
                                            text = stringResource(id = R.string.pref_appearance_amoled_mode_summary),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            style = MaterialTheme.typography.titleMediumEmphasized
                                        )
                                    },
                                    summary = {
                                        Text(
                                            text = stringResource(id = R.string.pref_appearance_unlimited_name_lines_summary),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            style = MaterialTheme.typography.titleMediumEmphasized
                                        )
                                    },
                                    summary = {
                                        Text(
                                            text = stringResource(id = R.string.pref_appearance_show_unplayed_old_video_label_summary),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            style = MaterialTheme.typography.titleMediumEmphasized
                                        ) 
                                    },
                                    valueRange = 1f..30f,
                                    summary = {
                                        Text(
                                            text = stringResource(
                                                id = R.string.pref_appearance_unplayed_old_video_days_summary,
                                                unplayedOldVideoDays,
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            style = MaterialTheme.typography.titleMediumEmphasized
                                        )
                                    },
                                    summary = {
                                        Text(
                                            text = stringResource(R.string.pref_appearance_auto_scroll_summary),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            style = MaterialTheme.typography.titleMediumEmphasized
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
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            style = MaterialTheme.typography.titleMediumEmphasized
                                        )
                                    },
                                    summary = {
                                        Text(
                                            text = stringResource(id = R.string.pref_gesture_tap_thumbnail_to_select_summary),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                            style = MaterialTheme.typography.titleMediumEmphasized
                                        )
                                    },
                                    summary = {
                                        Text(
                                            text = stringResource(id = R.string.pref_appearance_show_network_thumbnails_summary),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }

                        item {
                            PreferenceSectionHeader(title = stringResource(id = R.string.pref_audio_category))
                        }

                        item {
                            PreferenceCard {
                                val context = LocalContext.current
                                val audioTreeValue by browserPreferences.audioOutputTreeUri.collectAsState()
                                var audioFolderName by remember { mutableStateOf<String?>(null) }
                                LaunchedEffect(audioTreeValue) {
                                    val treeUri = AudioFolderStore.parse(audioTreeValue)
                                    audioFolderName = withContext(Dispatchers.IO) {
                                        if (AudioFolderStore.hasAccess(context, treeUri)) {
                                            AudioFolderStore.displayName(context, treeUri!!)
                                        } else {
                                            null
                                        }
                                    }
                                }
                                val pickAudioFolder = rememberAudioFolderPicker()
                                Preference(
                                    title = {
                                        Text(
                                            text = stringResource(R.string.pref_audio_folder_title),
                                            style = MaterialTheme.typography.titleMediumEmphasized
                                        )
                                    },
                                    summary = {
                                        Text(
                                            text = audioFolderName ?: stringResource(R.string.extract_audio_no_folder),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = pickAudioFolder,
                                )

                                PreferenceDivider()

                                val showAudioTab by browserPreferences.showAudioTab.collectAsState()
                                SwitchPreference(
                                    value = showAudioTab,
                                    onValueChange = { browserPreferences.showAudioTab.set(it) },
                                    title = {
                                        Text(
                                            text = stringResource(R.string.pref_audio_show_tab_title),
                                            style = MaterialTheme.typography.titleMediumEmphasized
                                        )
                                    },
                                    summary = {
                                        Text(
                                            text = stringResource(R.string.pref_audio_show_tab_summary),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
