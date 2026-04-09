package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.TextField
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
import app.marlboroadvance.mpvex.preferences.AudioChannels
import app.marlboroadvance.mpvex.preferences.AudioPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.theme.DarkMode
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.TextFieldPreference
import org.koin.compose.koinInject

@Serializable
object AudioPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val preferences = koinInject<AudioPreferences>()
    val appPreferences = koinInject<AppearancePreferences>()
    
    val channelNames = AudioChannels.entries.associateWith { stringResource(it.title) }

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
                text = stringResource(R.string.pref_audio),
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
              PreferenceSectionHeader(title = stringResource(R.string.pref_audio))
            }
            
            item {
              PreferenceCard {
                val preferredLanguages by preferences.preferredLanguages.collectAsState()
                TextFieldPreference(
                  value = preferredLanguages,
                  onValueChange = { preferences.preferredLanguages.set(it) },
                  textToValue = { it },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_preferred_languages),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    if (preferredLanguages.isNotBlank()) {
                      Text(
                        text = preferredLanguages,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                      )
                    } else {
                      Text(
                        text = stringResource(R.string.not_set_video_default),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                      )
                    }
                  },
                  textField = { value, onValueChange, _ ->
                    Column {
                      Text(stringResource(R.string.pref_audio_preferred_language))
                      TextField(
                        value,
                        onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                      )
                    }
                  },
                )
                
                PreferenceDivider()

                val audioPitchCorrection by preferences.audioPitchCorrection.collectAsState()
                SwitchPreference(
                  value = audioPitchCorrection,
                  onValueChange = { preferences.audioPitchCorrection.set(it) },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_audio_pitch_correction_title),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = { 
                    Text(
                      text = stringResource(R.string.pref_audio_pitch_correction_summary),
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ) 
                  },
                )
                
                PreferenceDivider()

                val volumeNormalization by preferences.volumeNormalization.collectAsState()
                SwitchPreference(
                  value = volumeNormalization,
                  onValueChange = { preferences.volumeNormalization.set(it) },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_audio_volume_normalization_title),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = { 
                    Text(
                      text = stringResource(R.string.pref_audio_volume_normalization_summary),
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ) 
                  },
                )
                
                PreferenceDivider()

                val automaticBackgroundPlayback by preferences.automaticBackgroundPlayback.collectAsState()
                SwitchPreference(
                  value = automaticBackgroundPlayback,
                  onValueChange = { preferences.automaticBackgroundPlayback.set(it) },
                  title = { 
                    Text(
                      text = stringResource(R.string.background_playback_title),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                )
                
                PreferenceDivider()

                val audioChannel by preferences.audioChannels.collectAsState()
                ListPreference(
                  value = audioChannel,
                  onValueChange = { preferences.audioChannels.set(it) },
                  values = AudioChannels.entries,
                  valueToText = { AnnotatedString(channelNames[it] ?: "") },
                  title = { 
                    Text(
                      text = stringResource(id = R.string.pref_audio_channels),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = { 
                    Text(
                      text = channelNames[audioChannel] ?: "",
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ) 
                  },
                )
                
                PreferenceDivider()

                val volumeBoostCap by preferences.volumeBoostCap.collectAsState()
                SliderPreference(
                  value = volumeBoostCap.toFloat(),
                  onValueChange = { preferences.volumeBoostCap.set(it.toInt()) },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_audio_volume_boost_cap),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  valueRange = 0f..200f,
                  summary = {
                    Text(
                      text = if (volumeBoostCap == 0) {
                        stringResource(R.string.generic_disabled)
                      } else {
                        volumeBoostCap.toString()
                      },
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Light,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  },
                  onSliderValueChange = { preferences.volumeBoostCap.set(it.toInt()) },
                  sliderValue = volumeBoostCap.toFloat(),
                )
              }
            }
          }
        }
      }
    }
  }
}
