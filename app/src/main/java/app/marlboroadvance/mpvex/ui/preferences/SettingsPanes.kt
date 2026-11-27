package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.presentation.Screen

/**
 * Settings screens that open as the detail pane next to the settings list on wide windows.
 * Editors (config, control layout) are left out so they keep the full window.
 */
private val SettingsDetailScreens: Set<Screen> = setOf(
    AppearancePreferencesScreen,
    PlayerControlsPreferencesScreen,
    PlayerPreferencesScreen,
    GesturePreferencesScreen,
    DecoderPreferencesScreen,
    DeviceCodecSupportScreen,
    AudioPreferencesScreen,
    SubtitlesPreferencesScreen,
    FoldersPreferencesScreen,
    StorageScreen,
    AdvancedPreferencesScreen,
    UpdatesScreen,
    AboutScreen,
)

/**
 * Nav3 entry metadata for the two-pane settings layout: the settings home is the list pane,
 * the screens in [SettingsDetailScreens] are detail panes, anything else gets no metadata and
 * stays single-pane.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun settingsPaneMetadata(route: Screen): Map<String, Any> = when {
    route == PreferencesScreen -> ListDetailSceneStrategy.listPane(
        detailPlaceholder = { SettingsDetailPlaceholder() },
    )
    route in SettingsDetailScreens -> ListDetailSceneStrategy.detailPane()
    else -> emptyMap()
}

/**
 * Opens a settings screen from the settings list. If a detail screen is already showing (the
 * two-pane layout), it is replaced instead of stacked, so back does not step through every
 * screen the user has tapped. On a phone the list is on top, so this is a plain push.
 */
fun NavBackStack<Screen>.openSetting(screen: Screen) {
    if (lastOrNull() in SettingsDetailScreens) removeLastOrNull()
    add(screen)
}

@Composable
private fun SettingsDetailPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.settings_detail_placeholder),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
