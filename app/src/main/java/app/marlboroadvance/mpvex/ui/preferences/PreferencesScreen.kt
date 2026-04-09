package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewQuilt
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Subtitles
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.theme.DarkMode
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceTheme
import org.koin.compose.koinInject

@Serializable
object PreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val preferences = koinInject<AppearancePreferences>()
    
    // OLED Optimization: Pure black background in dark mode to save battery
    val darkMode by preferences.darkMode.collectAsState()
    val systemDarkTheme = isSystemInDarkTheme()
    val isDark = when (darkMode) {
      DarkMode.Dark -> true
      DarkMode.Light -> false
      DarkMode.System -> systemDarkTheme
    }
    val backgroundColor = if (isDark) Color.Black else MaterialTheme.colorScheme.background

    // Modern Android 17 style: more horizontal padding, thinner visual weight
    val customPreferenceTheme = preferenceTheme(
      padding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
      iconContainerMinWidth = 64.dp,
    )

    Surface(
      modifier = Modifier.fillMaxSize(),
      color = backgroundColor
    ) {
      Scaffold(
        containerColor = Color.Transparent,
        topBar = {
          Column(modifier = Modifier.statusBarsPadding()) {
            TopAppBar(
              colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
              ),
              title = {
                Text(
                  text = stringResource(R.string.pref_preferences),
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
              }
            )

            // Integrated Search Bar Pill (Modern Header Evolution)
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null, // Efficiency: No complex ripple animation
                  onClick = { backstack.add(SettingsSearchScreen) }
                ),
              shape = RoundedCornerShape(28.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  Icons.Rounded.Search,
                  contentDescription = null,
                  modifier = Modifier.size(20.dp),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = stringResource(R.string.settings_search_hint),
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
              }
            }
          }
        },
      ) { padding ->
        ProvidePreferenceLocals(theme = customPreferenceTheme) {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp) // Anti-Overdraw: Flat list hierarchy
          ) {
            // UI & Appearance Section
            item {
              PreferenceSectionHeader(title = "UI & Appearance")
            }

            item {
              PreferenceCard {
                Preference(
                  title = { 
                    Text(
                      text = stringResource(id = R.string.pref_appearance_title),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold // Hierarchy via weight
                    ) 
                  },
                  summary = {
                    Text(
                      text = stringResource(id = R.string.pref_appearance_summary),
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light, // Clean, thin summary
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                  icon = {
                    PreferenceIcon(
                      Icons.Rounded.Palette,
                      containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                  },
                  onClick = { backstack.add(AppearancePreferencesScreen) },
                )

                PreferenceDivider()

                Preference(
                  title = { 
                    Text(
                      text = stringResource(id = R.string.pref_layout_title),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = stringResource(id = R.string.pref_layout_summary),
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                  icon = {
                    PreferenceIcon(
                      Icons.AutoMirrored.Rounded.ViewQuilt,
                      containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                  },
                  onClick = { backstack.add(PlayerControlsPreferencesScreen) },
                )
              }
            }

            // Playback & Controls Section
            item {
              PreferenceSectionHeader(title = "Playback & Controls")
            }

            item {
              PreferenceCard {
                Preference(
                  title = { 
                    Text(
                      text = stringResource(id = R.string.pref_player),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = stringResource(id = R.string.pref_player_summary),
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                  icon = {
                    PreferenceIcon(
                      Icons.Rounded.PlayCircle,
                      containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    )
                  },
                  onClick = { backstack.add(PlayerPreferencesScreen) },
                )

                PreferenceDivider()

                Preference(
                  title = { 
                    Text(
                      text = stringResource(id = R.string.pref_gesture),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = stringResource(id = R.string.pref_gesture_summary),
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                  icon = {
                    PreferenceIcon(
                      Icons.Rounded.Gesture,
                      containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    )
                  },
                  onClick = { backstack.add(GesturePreferencesScreen) },
                )
              }
            }

            // File Management Section
            item {
              PreferenceSectionHeader(title = "File Management")
            }

            item {
              PreferenceCard {
                Preference(
                  title = { 
                    Text(
                      text = stringResource(id = R.string.pref_folders_title),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = stringResource(id = R.string.pref_folders_summary),
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                  icon = {
                    PreferenceIcon(
                      Icons.Rounded.Folder,
                      containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                  },
                  onClick = { backstack.add(FoldersPreferencesScreen) },
                )
              }
            }

            // Media Settings Section
            item {
              PreferenceSectionHeader(title = "Media Settings")
            }

            item {
              PreferenceCard {
                Preference(
                  title = { 
                    Text(
                      text = stringResource(id = R.string.pref_decoder),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = stringResource(id = R.string.pref_decoder_summary),
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                  icon = {
                    PreferenceIcon(
                      Icons.Rounded.Memory,
                      containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                  },
                  onClick = { backstack.add(DecoderPreferencesScreen) },
                )

                PreferenceDivider()

                Preference(
                  title = { 
                    Text(
                      text = stringResource(id = R.string.pref_subtitles),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = stringResource(id = R.string.pref_subtitles_summary),
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                  icon = {
                    PreferenceIcon(
                      Icons.Rounded.Subtitles,
                      containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                  },
                  onClick = { backstack.add(SubtitlesPreferencesScreen) },
                )

                PreferenceDivider()

                Preference(
                  title = { 
                    Text(
                      text = stringResource(id = R.string.pref_audio),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = stringResource(id = R.string.pref_audio_summary),
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                  icon = {
                    PreferenceIcon(
                      Icons.Rounded.Audiotrack,
                      containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                  },
                  onClick = { backstack.add(AudioPreferencesScreen) },
                )
              }
            }

            // Advanced & About Section
            item {
              PreferenceSectionHeader(title = "Advanced & About")
            }

            item {
              PreferenceCard {
                Preference(
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_advanced),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = stringResource(id = R.string.pref_advanced_summary),
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                  icon = {
                    PreferenceIcon(
                      Icons.Rounded.Code,
                      containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                  },
                  onClick = { backstack.add(AdvancedPreferencesScreen) },
                )

                PreferenceDivider()

                Preference(
                  title = { 
                    Text(
                      text = stringResource(id = R.string.pref_about_title),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = stringResource(id = R.string.pref_about_summary),
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                  icon = {
                    PreferenceIcon(
                      Icons.Rounded.Info,
                      containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    )
                  },
                  onClick = { backstack.add(AboutScreen) },
                )
              }
            }
          }
        }
      }
    }
  }
}
