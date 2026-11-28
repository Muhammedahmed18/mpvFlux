package app.marlboroadvance.mpvex.ui.preferences

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.DecoderPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.player.Debanding
import app.marlboroadvance.mpvex.ui.player.MPVProfile
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.ui.utils.readableContentWidth
import kotlinx.serialization.Serializable
import app.marlboroadvance.mpvex.ui.preferences.components.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import app.marlboroadvance.mpvex.ui.preferences.components.SwitchPreference
import org.koin.compose.koinInject

@Serializable
object DecoderPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<DecoderPreferences>()
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    val isVulkanSupported = remember { VulkanUtils.isVulkanSupported(context) }
    var showGpuNextWarning by remember { mutableStateOf(false) }

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
                text = stringResource(R.string.pref_decoder),
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
            item {
              PreferenceSectionHeader(title = stringResource(R.string.pref_decoder_section_decoding))
            }

            item {
              PreferenceCard {
                val profile by preferences.profile.collectAsState()
                val currentProfile = MPVProfile.fromValue(profile)
                ListPreference(
                  value = currentProfile,
                  onValueChange = { preferences.profile.set(it.value) },
                  values = MPVProfile.entries,
                  title = {
                    Text(
                      text = stringResource(R.string.pref_decoder_profile_title),
                    )
                  },
                  summary = {
                    Text(
                      text = currentProfile.displayName,
                    )
                  },
                )

                val tryHWDecoding by preferences.tryHWDecoding.collectAsState()
                SwitchPreference(
                  value = tryHWDecoding,
                  onValueChange = {
                    preferences.tryHWDecoding.set(it)
                  },
                  title = {
                    Text(
                      text = stringResource(R.string.pref_decoder_try_hw_dec_title),
                    )
                  },
                )
              }
            }

            item {
              PreferenceSectionHeader(title = stringResource(R.string.pref_decoder_section_rendering))
            }

            item {
              PreferenceCard {
                val gpuNext by preferences.gpuNext.collectAsState()
                val useVulkan by preferences.useVulkan.collectAsState()
                SwitchPreference(
                  value = gpuNext,
                  onValueChange = { enabled ->
                    if (enabled && !gpuNext && !useVulkan) {
                      showGpuNextWarning = true
                    } else {
                      preferences.gpuNext.set(enabled)
                    }
                  },
                  title = {
                    Text(
                      text = stringResource(R.string.pref_decoder_gpu_next_title),
                    )
                  },
                  summary = {
                    Text(
                      text = stringResource(R.string.pref_decoder_gpu_next_summary),
                    )
                  },
                )

                SwitchPreference(
                  value = useVulkan,
                  onValueChange = { enabled ->
                    preferences.useVulkan.set(enabled)
                  },
                  enabled = isVulkanSupported,
                  title = {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Text(text = stringResource(R.string.pref_decoder_vulkan_title))
                      ExperimentalBadge()
                    }
                  },
                  summary = {
                    Text(
                      text = stringResource(
                        if (isVulkanSupported) R.string.pref_decoder_vulkan_summary
                        else R.string.pref_decoder_vulkan_not_supported
                      ),
                    )
                  },
                )

                val debanding by preferences.debanding.collectAsState()
                ListPreference(
                  value = debanding,
                  onValueChange = { preferences.debanding.set(it) },
                  values = Debanding.entries,
                  title = {
                    Text(
                      text = stringResource(R.string.pref_decoder_debanding_title),
                    )
                  },
                  summary = {
                    Text(
                      text = stringResource(debanding.titleRes),
                    )
                  },
                )

                val useYUV420p by preferences.useYUV420P.collectAsState()
                SwitchPreference(
                  value = useYUV420p,
                  onValueChange = {
                    preferences.useYUV420P.set(it)
                  },
                  title = {
                    Text(
                      text = stringResource(R.string.pref_decoder_yuv420p_title),
                    )
                  },
                  summary = {
                    Text(
                      text = stringResource(R.string.pref_decoder_yuv420p_summary),
                    )
                  },
                )
              }
            }

            // Which formats this phone decodes in hardware: opens its own screen.
            item {
              PreferenceSectionHeader(title = stringResource(R.string.codec_support_section))
            }
            item {
              PreferenceCard {
                app.marlboroadvance.mpvex.ui.preferences.components.DeviceCodecSupportRow(
                  onClick = { backstack.add(DeviceCodecSupportScreen) },
                )
              }
            }
          }
        }

        // Kept outside the preference cards: a dialog inside PreferenceCard would be
        // counted as an extra (empty) segment and change the card's corner shapes.
        if (showGpuNextWarning) {
          AlertDialog(
            onDismissRequest = { showGpuNextWarning = false },
            title = { Text(stringResource(R.string.pref_decoder_gpu_next_enable_title), style = MaterialTheme.typography.headlineSmallEmphasized) },
            text = {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.pref_decoder_gpu_next_warning), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.pref_decoder_gpu_next_purple_screen_fix), style = MaterialTheme.typography.bodyMedium)
              }
            },
            confirmButton = {
              Button(onClick = {
                preferences.gpuNext.set(true)
                showGpuNextWarning = false
              }) {
                Text(stringResource(R.string.pref_decoder_gpu_next_enable_anyway), fontWeight = FontWeight.Bold)
              }
            },
            dismissButton = {
              TextButton(onClick = { showGpuNextWarning = false }) {
                Text(stringResource(R.string.generic_cancel))
              }
            },
            shape = MaterialTheme.shapes.extraLarge
          )
        }
      }
    }
  }
}

/** Small tertiary pill shown next to the title of experimental options. */
@Composable
private fun ExperimentalBadge() {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
  ) {
    Text(
      text = stringResource(R.string.pref_experimental_badge),
      style = MaterialTheme.typography.labelSmall,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    )
  }
}

object VulkanUtils {
  private const val TAG = "VulkanUtils"

  fun isVulkanSupported(context: Context): Boolean {
    try {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Log.d(TAG, "Vulkan not supported: Android version ${Build.VERSION.SDK_INT} < 33 (Tiramisu)")
        return false
      }

      val packageManager = context.packageManager
      return packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
    } catch (e: Exception) {
      Log.e(TAG, "Error checking Vulkan support", e)
      return false
    }
  }
}
