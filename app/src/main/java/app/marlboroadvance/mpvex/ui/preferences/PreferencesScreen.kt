package app.marlboroadvance.mpvex.ui.preferences

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewQuilt
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.ui.utils.readableContentWidth
import app.marlboroadvance.mpvex.update.UpdateManager
import app.marlboroadvance.mpvex.update.UpdateState
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
object PreferencesScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val backstack = LocalBackStack.current

        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        val context = LocalContext.current
        val keyboardController = LocalSoftwareKeyboardController.current
        var searchQuery by rememberSaveable { mutableStateOf("") }
        var debouncedSearchQuery by rememberSaveable { mutableStateOf("") }

        // Debounce to reduce work while typing
        LaunchedEffect(searchQuery) {
            if (searchQuery.isBlank()) {
                debouncedSearchQuery = ""
                return@LaunchedEffect
            }
            delay(300)
            debouncedSearchQuery = searchQuery
        }

        val searchResults by remember(debouncedSearchQuery) {
            derivedStateOf {
                SearchablePreferences.search(debouncedSearchQuery) { resId ->
                    context.resources.getString(resId)
                }
            }
        }

        // First back press clears the search, the second leaves the screen
        BackHandler(enabled = searchQuery.isNotEmpty()) {
            searchQuery = ""
            keyboardController?.hide()
        }

        val updateManager = koinInject<UpdateManager>()
        val updateState by updateManager.state.collectAsState()
        val availableUpdate = (updateState as? UpdateState.Available)?.takeIf { updateManager.isSupported }
        val updateAvailable = availableUpdate != null
        val updateSummary = when {
            !updateManager.isSupported -> stringResource(R.string.update_not_supported_title)
            availableUpdate != null -> stringResource(
                R.string.update_available_title,
                availableUpdate.release.version.toString(),
            )
            else -> stringResource(R.string.update_card_up_to_date, updateManager.currentVersion)
        }

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                PreferenceTopBar(
                    title = {
                        Text(
                            text = stringResource(R.string.pref_settings_title),
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
                    large = true,
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
                    SearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = { keyboardController?.hide() },
                    )
                }

                if (searchQuery.isBlank()) {
                    item { PreferenceSectionHeader(title = stringResource(R.string.pref_section_interface)) }
                    item {
                        PreferenceCard {
                            PreferenceItem(
                                title = stringResource(R.string.pref_appearance_title),
                                summary = stringResource(R.string.pref_appearance_summary),
                                icon = { PreferenceIcon(Icons.Rounded.Palette) },
                                showChevron = true,
                                onClick = { backstack.openSetting(AppearancePreferencesScreen) }
                            )
                            PreferenceItem(
                                title = stringResource(R.string.pref_layout_title),
                                summary = stringResource(R.string.pref_layout_summary),
                                icon = { PreferenceIcon(Icons.AutoMirrored.Rounded.ViewQuilt) },
                                showChevron = true,
                                onClick = { backstack.openSetting(PlayerControlsPreferencesScreen) }
                            )
                        }
                    }

                    item { PreferenceSectionHeader(title = stringResource(R.string.pref_section_playback)) }
                    item {
                        PreferenceCard {
                            PreferenceItem(
                                title = stringResource(R.string.pref_player),
                                summary = stringResource(R.string.pref_player_summary),
                                icon = { PreferenceIcon(Icons.Rounded.PlayCircle) },
                                showChevron = true,
                                onClick = { backstack.openSetting(PlayerPreferencesScreen) }
                            )
                            PreferenceItem(
                                title = stringResource(R.string.pref_gesture),
                                summary = stringResource(R.string.pref_gesture_summary),
                                icon = { PreferenceIcon(Icons.Rounded.Gesture) },
                                showChevron = true,
                                onClick = { backstack.openSetting(GesturePreferencesScreen) }
                            )
                            PreferenceItem(
                                title = stringResource(R.string.pref_decoder),
                                summary = stringResource(R.string.pref_decoder_summary),
                                icon = { PreferenceIcon(Icons.Rounded.Memory) },
                                showChevron = true,
                                onClick = { backstack.openSetting(DecoderPreferencesScreen) }
                            )
                            PreferenceItem(
                                title = stringResource(R.string.pref_audio),
                                summary = stringResource(R.string.pref_audio_summary),
                                icon = { PreferenceIcon(Icons.Rounded.Audiotrack) },
                                showChevron = true,
                                onClick = { backstack.openSetting(AudioPreferencesScreen) }
                            )
                            PreferenceItem(
                                title = stringResource(R.string.pref_subtitles),
                                summary = stringResource(R.string.pref_subtitles_summary),
                                icon = { PreferenceIcon(Icons.Rounded.Subtitles) },
                                showChevron = true,
                                onClick = { backstack.openSetting(SubtitlesPreferencesScreen) }
                            )
                        }
                    }

                    item { PreferenceSectionHeader(title = stringResource(R.string.pref_section_library)) }
                    item {
                        PreferenceCard {
                            PreferenceItem(
                                title = stringResource(R.string.pref_folders_title),
                                summary = stringResource(R.string.pref_folders_summary),
                                icon = { PreferenceIcon(Icons.Rounded.Folder) },
                                showChevron = true,
                                onClick = { backstack.openSetting(FoldersPreferencesScreen) }
                            )
                        }
                    }

                    item { PreferenceSectionHeader(title = stringResource(R.string.pref_section_system)) }
                    item {
                        PreferenceCard {
                            PreferenceItem(
                                title = stringResource(R.string.pref_storage_title),
                                summary = stringResource(R.string.pref_storage_summary),
                                icon = { PreferenceIcon(Icons.Rounded.Storage) },
                                showChevron = true,
                                onClick = { backstack.openSetting(StorageScreen) }
                            )
                            PreferenceItem(
                                title = stringResource(R.string.pref_advanced),
                                summary = stringResource(R.string.pref_advanced_summary),
                                icon = { PreferenceIcon(Icons.Rounded.Code) },
                                showChevron = true,
                                onClick = { backstack.openSetting(AdvancedPreferencesScreen) }
                            )
                            PreferenceItem(
                                title = stringResource(R.string.update_title),
                                summary = updateSummary,
                                icon = {
                                    PreferenceIcon(
                                        Icons.Rounded.SystemUpdate,
                                        iconColor = if (updateAvailable) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Unspecified
                                        },
                                    )
                                },
                                showChevron = true,
                                onClick = { backstack.openSetting(UpdatesScreen) }
                            )
                            PreferenceItem(
                                title = stringResource(R.string.pref_about_title),
                                summary = stringResource(R.string.pref_about_summary),
                                icon = { PreferenceIcon(Icons.Rounded.Info) },
                                showChevron = true,
                                onClick = { backstack.openSetting(AboutScreen) }
                            )
                        }
                    }
                } else if (debouncedSearchQuery.isNotEmpty() && searchResults.isEmpty()) {
                    item { SearchEmptyState() }
                } else {
                    itemsIndexed(
                        items = searchResults,
                        key = { index, pref -> "${pref.titleRes}_${pref.category}_${pref.screen}_$index".hashCode() },
                    ) { _, preference ->
                        SearchResultItem(
                            preference = preference,
                            query = debouncedSearchQuery,
                            onClick = {
                                keyboardController?.hide()
                                backstack.openSetting(preference.screen)
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * M3 search bar used as the settings search field. Results are shown inline in the list below
 * it, so the bar itself never expands into the full-screen search view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { onSearch() },
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text(text = stringResource(R.string.settings_search_hint)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = stringResource(R.string.pref_storage_clear),
                            )
                        }
                    }
                },
            )
        },
        expanded = false,
        onExpandedChange = {},
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        content = {},
    )
}

@Composable
private fun SearchEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.settings_search_no_results),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

/** Highlights every case-insensitive occurrence of [query] in [text]. */
@Composable
private fun highlightedText(text: String, query: String): AnnotatedString {
    val needle = query.trim()
    if (needle.isEmpty()) return AnnotatedString(text)
    val highlight = SpanStyle(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        background = MaterialTheme.colorScheme.primaryContainer,
    )
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val match = text.indexOf(needle, cursor, ignoreCase = true)
            if (match < 0) break
            append(text.substring(cursor, match))
            withStyle(highlight) { append(text.substring(match, match + needle.length)) }
            cursor = match + needle.length
        }
        append(text.substring(cursor))
    }
}

@Composable
private fun SearchResultItem(
    preference: SearchablePreference,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleText = if (preference.titleRes != null) {
        stringResource(preference.titleRes)
    } else {
        preference.title ?: ""
    }

    val summaryText = if (preference.summaryRes != null) {
        stringResource(preference.summaryRes)
    } else {
        preference.summary
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "item_scale",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .scale(scale),
        onClick = onClick,
        interactionSource = interactionSource,
        shape = MaterialTheme.shapes.large,
        color = if (isPressed) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = searchCategoryIcon(preference.category),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlightedText(titleText, query),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                summaryText?.let {
                    Text(
                        text = highlightedText(it, query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = searchCategoryTitleRes(preference.category)?.let { stringResource(it) } ?: preference.category,
                    style = MaterialTheme.typography.labelSmallEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** Same icons as the settings home rows, so a search result shows the section it lives in. */
private fun searchCategoryIcon(category: String): ImageVector = when (category) {
    "About" -> Icons.Rounded.Info
    "Advanced" -> Icons.Rounded.Code
    "Appearance" -> Icons.Rounded.Palette
    "Audio" -> Icons.Rounded.Audiotrack
    "Decoder" -> Icons.Rounded.Memory
    "Folders" -> Icons.Rounded.Folder
    "Gestures" -> Icons.Rounded.Gesture
    "Player" -> Icons.Rounded.PlayCircle
    "Storage" -> Icons.Rounded.Storage
    "Subtitles" -> Icons.Rounded.Subtitles
    "Updates" -> Icons.Rounded.SystemUpdate
    else -> Icons.Rounded.Settings
}
