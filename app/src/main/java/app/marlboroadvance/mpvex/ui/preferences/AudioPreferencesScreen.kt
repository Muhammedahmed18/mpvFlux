package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AudioChannels
import app.marlboroadvance.mpvex.preferences.AudioPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.preferences.components.LanguagePickerPreference
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.ui.utils.readableContentWidth
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.SwitchPreference
import org.koin.compose.koinInject

@Serializable
object AudioPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val preferences = koinInject<AudioPreferences>()

    val channelNames = AudioChannels.entries.associateWith { stringResource(it.title) }

    val backgroundColor = rememberPreferenceBackgroundColor()
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
                text = stringResource(R.string.pref_audio),
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
              PreferenceSectionHeader(title = stringResource(R.string.pref_audio))
            }
            
            item {
              PreferenceCard {
                val preferredLanguages by preferences.preferredLanguages.collectAsState()
                LanguagePickerPreference(
                  title = stringResource(R.string.pref_preferred_language),
                  value = preferredLanguages,
                  onValueChange = { preferences.preferredLanguages.set(it) },
                )

                PreferenceDivider()

                val audioPitchCorrection by preferences.audioPitchCorrection.collectAsState()
                SwitchPreference(
                  value = audioPitchCorrection,
                  onValueChange = { preferences.audioPitchCorrection.set(it) },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_audio_pitch_correction_title),
                      style = MaterialTheme.typography.titleMediumEmphasized
                    ) 
                  },
                  summary = { 
                    Text(
                      text = stringResource(R.string.pref_audio_pitch_correction_summary),
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
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
                      style = MaterialTheme.typography.titleMediumEmphasized
                    ) 
                  },
                  summary = { 
                    Text(
                      text = stringResource(R.string.pref_audio_volume_normalization_summary),
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
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
                      style = MaterialTheme.typography.titleMediumEmphasized
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
                      style = MaterialTheme.typography.titleMediumEmphasized
                    ) 
                  },
                  summary = { 
                    Text(
                      text = channelNames[audioChannel] ?: "",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
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
                      style = MaterialTheme.typography.titleMediumEmphasized
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
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
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
