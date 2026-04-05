package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.preferenceTheme

@Serializable
object PreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val customPreferenceTheme = preferenceTheme(
      padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      iconContainerMinWidth = 72.dp,
    )
    Scaffold(
      topBar = {
        TopAppBar(
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
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals(theme = customPreferenceTheme) {
        LazyColumn(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(padding),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          // Search bar - full width, prominent placement
          item {
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                .clickable { backstack.add(SettingsSearchScreen) },
              shape = RoundedCornerShape(28.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLowest,
              tonalElevation = 0.dp,
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Rounded.Search,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                  text = stringResource(R.string.settings_search_hint),
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
          
          // UI & Appearance Section
          item {
            PreferenceSectionHeader(title = "UI & Appearance", modifier = Modifier.padding(top = 12.dp))
          }
          
          item {
            PreferenceCard {
              Preference(
                title = { Text(text = stringResource(id = R.string.pref_appearance_title)) },
                summary = { 
                  Text(
                    text = stringResource(id = R.string.pref_appearance_summary),
                    color = MaterialTheme.colorScheme.outline
                  ) 
                },
                icon = { 
                  PreferenceIcon(
                    Icons.Rounded.Palette, 
                    contentDescription = null,
                  ) 
                },
                onClick = { backstack.add(AppearancePreferencesScreen) },
              )
              
              PreferenceDivider()
              
              Preference(

                title = { Text(text = stringResource(id = R.string.pref_layout_title)) },
                summary = { 
                  Text(
                    text = stringResource(id = R.string.pref_layout_summary),
                    color = MaterialTheme.colorScheme.outline
                  ) 
                },
                icon = { 
                  PreferenceIcon(
                    Icons.AutoMirrored.Rounded.ViewQuilt, 
                    contentDescription = null,
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

                title = { Text(text = stringResource(id = R.string.pref_player)) },
                summary = { 
                  Text(
                    text = stringResource(id = R.string.pref_player_summary),
                    color = MaterialTheme.colorScheme.outline
                  ) 
                },
                icon = { 
                  PreferenceIcon(
                    Icons.Rounded.PlayCircle, 
                    contentDescription = null,
                  ) 
                },
                onClick = { backstack.add(PlayerPreferencesScreen) },
              )
              
              PreferenceDivider()
              
              Preference(

                title = { Text(text = stringResource(id = R.string.pref_gesture)) },
                summary = { 
                  Text(
                    text = stringResource(id = R.string.pref_gesture_summary),
                    color = MaterialTheme.colorScheme.outline
                  ) 
                },
                icon = { 
                  PreferenceIcon(
                    Icons.Rounded.Gesture, 
                    contentDescription = null,
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

                title = { Text(text = stringResource(id = R.string.pref_folders_title)) },
                summary = { 
                  Text(
                    text = stringResource(id = R.string.pref_folders_summary),
                    color = MaterialTheme.colorScheme.outline
                  ) 
                },
                icon = { 
                  PreferenceIcon(
                    Icons.Rounded.Folder, 
                    contentDescription = null,
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

                title = { Text(text = stringResource(id = R.string.pref_decoder)) },
                summary = { 
                  Text(
                    text = stringResource(id = R.string.pref_decoder_summary),
                    color = MaterialTheme.colorScheme.outline
                  ) 
                },
                icon = { 
                  PreferenceIcon(
                    Icons.Rounded.Memory, 
                    contentDescription = null,
                  ) 
                },
                onClick = { backstack.add(DecoderPreferencesScreen) },
              )
              
              PreferenceDivider()
              
              Preference(

                title = { Text(text = stringResource(id = R.string.pref_subtitles)) },
                summary = { 
                  Text(
                    text = stringResource(id = R.string.pref_subtitles_summary),
                    color = MaterialTheme.colorScheme.outline
                  ) 
                },
                icon = { 
                  PreferenceIcon(
                    Icons.Rounded.Subtitles, 
                    contentDescription = null,
                  ) 
                },
                onClick = { backstack.add(SubtitlesPreferencesScreen) },
              )
              
              PreferenceDivider()
              
              Preference(

                title = { Text(text = stringResource(id = R.string.pref_audio)) },
                summary = { 
                  Text(
                    text = stringResource(id = R.string.pref_audio_summary),
                    color = MaterialTheme.colorScheme.outline
                  ) 
                },
                icon = { 
                  PreferenceIcon(
                    Icons.Rounded.Audiotrack, 
                    contentDescription = null,
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

                title = { Text(text = stringResource(R.string.pref_advanced)) },
                summary = { 
                  Text(
                    text = stringResource(id = R.string.pref_advanced_summary),
                    color = MaterialTheme.colorScheme.outline
                  ) 
                },
                icon = { 
                  PreferenceIcon(
                    Icons.Rounded.Code, 
                    contentDescription = null,
                  ) 
                },
                onClick = { backstack.add(AdvancedPreferencesScreen) },
              )
              
              PreferenceDivider()
              
              Preference(

                title = { Text(text = stringResource(id = R.string.pref_about_title)) },
                summary = { 
                  Text(
                    text = stringResource(id = R.string.pref_about_summary),
                    color = MaterialTheme.colorScheme.outline
                  ) 
                },
                icon = { 
                  PreferenceIcon(
                    Icons.Rounded.Info, 
                    contentDescription = null,
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
