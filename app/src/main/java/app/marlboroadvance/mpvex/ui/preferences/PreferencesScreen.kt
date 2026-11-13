package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewQuilt
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.ui.utils.readableContentWidth
import kotlinx.serialization.Serializable

@Serializable
object PreferencesScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val backstack = LocalBackStack.current

        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                PreferenceTopBar(
                    title = {
                        Text(
                            text = stringResource(R.string.pref_preferences),
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = backstack::removeLastOrNull) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack, 
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            }
        ) { padding ->
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
                    app.marlboroadvance.mpvex.ui.update.UpdateSettingsCard(
                        onClick = { backstack.add(UpdatesScreen) },
                    )
                }

                item {
                    SearchPill(onClick = { backstack.add(SettingsSearchScreen) })
                }

                item { PreferenceSectionHeader(title = stringResource(R.string.pref_section_ui_appearance)) }
                item {
                    PreferenceCard {
                        PreferenceItem(
                            title = stringResource(R.string.pref_appearance_title),
                            summary = stringResource(R.string.pref_appearance_summary),
                            icon = { PreferenceIcon(Icons.Rounded.Palette, containerColor = MaterialTheme.colorScheme.primaryContainer) },
                            onClick = { backstack.add(AppearancePreferencesScreen) }
                        )
                        PreferenceItem(
                            title = stringResource(R.string.pref_layout_title),
                            summary = stringResource(R.string.pref_layout_summary),
                            icon = { PreferenceIcon(Icons.AutoMirrored.Rounded.ViewQuilt, containerColor = MaterialTheme.colorScheme.primaryContainer) },
                            onClick = { backstack.add(PlayerControlsPreferencesScreen) }
                        )
                    }
                }

                item { PreferenceSectionHeader(title = stringResource(R.string.pref_section_playback_controls)) }
                item {
                    PreferenceCard {
                        PreferenceItem(
                            title = stringResource(R.string.pref_player),
                            summary = stringResource(R.string.pref_player_summary),
                            icon = { PreferenceIcon(Icons.Rounded.PlayCircle, containerColor = MaterialTheme.colorScheme.tertiaryContainer) },
                            onClick = { backstack.add(PlayerPreferencesScreen) }
                        )
                        PreferenceItem(
                            title = stringResource(R.string.pref_gesture),
                            summary = stringResource(R.string.pref_gesture_summary),
                            icon = { PreferenceIcon(Icons.Rounded.Gesture, containerColor = MaterialTheme.colorScheme.tertiaryContainer) },
                            onClick = { backstack.add(GesturePreferencesScreen) }
                        )
                    }
                }

                item { PreferenceSectionHeader(title = stringResource(R.string.pref_section_media_files)) }
                item {
                    PreferenceCard {
                        PreferenceItem(
                            title = stringResource(R.string.pref_folders_title),
                            summary = stringResource(R.string.pref_folders_summary),
                            icon = { PreferenceIcon(Icons.Rounded.Folder) },
                            onClick = { backstack.add(FoldersPreferencesScreen) }
                        )
                        PreferenceItem(
                            title = stringResource(R.string.pref_decoder),
                            summary = stringResource(R.string.pref_decoder_summary),
                            icon = { PreferenceIcon(Icons.Rounded.Memory) },
                            onClick = { backstack.add(DecoderPreferencesScreen) }
                        )
                    }
                }

                item { PreferenceSectionHeader(title = stringResource(R.string.pref_section_audio_subtitles)) }
                item {
                    PreferenceCard {
                        PreferenceItem(
                            title = stringResource(R.string.pref_subtitles),
                            summary = stringResource(R.string.pref_subtitles_summary),
                            icon = { PreferenceIcon(Icons.Rounded.Subtitles) },
                            onClick = { backstack.add(SubtitlesPreferencesScreen) }
                        )
                        PreferenceItem(
                            title = stringResource(R.string.pref_audio),
                            summary = stringResource(R.string.pref_audio_summary),
                            icon = { PreferenceIcon(Icons.Rounded.Audiotrack) },
                            onClick = { backstack.add(AudioPreferencesScreen) }
                        )
                    }
                }

                item { PreferenceSectionHeader(title = stringResource(R.string.pref_section_system_info)) }
                item {
                    PreferenceCard {
                        PreferenceItem(
                            title = stringResource(R.string.pref_storage_title),
                            summary = stringResource(R.string.pref_storage_summary),
                            icon = { PreferenceIcon(Icons.Rounded.Storage, containerColor = MaterialTheme.colorScheme.surfaceContainerHighest) },
                            onClick = { backstack.add(StorageScreen) }
                        )
                        PreferenceItem(
                            title = stringResource(R.string.pref_advanced),
                            summary = stringResource(R.string.pref_advanced_summary),
                            icon = { PreferenceIcon(Icons.Rounded.Code, containerColor = MaterialTheme.colorScheme.surfaceContainerHighest) },
                            onClick = { backstack.add(AdvancedPreferencesScreen) }
                        )
                        PreferenceItem(
                            title = stringResource(R.string.pref_about_title),
                            summary = stringResource(R.string.pref_about_summary),
                            icon = { PreferenceIcon(Icons.Rounded.Info, containerColor = MaterialTheme.colorScheme.surfaceContainerHighest) },
                            onClick = { backstack.add(AboutScreen) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchPill(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "search_pill_scale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .scale(scale),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.settings_search_hint),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
