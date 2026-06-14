package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.PlayerButton
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.SeekbarStyle
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.theme.DarkMode
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import app.marlboroadvance.mpvex.ui.preferences.components.PlayerButtonChip
import org.koin.compose.koinInject

@Serializable
object PlayerControlsPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val appearancePrefs = koinInject<AppearancePreferences>()
    val playerPrefs = koinInject<PlayerPreferences>()

    val topRState by appearancePrefs.topRightControls.collectAsState()
    val bottomRState by appearancePrefs.bottomRightControls.collectAsState()
    val bottomLState by appearancePrefs.bottomLeftControls.collectAsState()
    val portraitBottomState by appearancePrefs.portraitBottomControls.collectAsState()

    val topRightButtons = remember(topRState) { appearancePrefs.parseButtons(topRState, mutableSetOf()) }
    val bottomRightButtons = remember(bottomRState) { appearancePrefs.parseButtons(bottomRState, mutableSetOf()) }
    val bottomLeftButtons = remember(bottomLState) { appearancePrefs.parseButtons(bottomLState, mutableSetOf()) }
    val portraitBottomButtons = remember(portraitBottomState) { appearancePrefs.parseButtons(portraitBottomState, mutableSetOf()) }

    val darkMode by appearancePrefs.darkMode.collectAsState()
    val systemDarkTheme = isSystemInDarkTheme()
    val isDark = when (darkMode) {
      DarkMode.Dark -> true
      DarkMode.Light -> false
      DarkMode.System -> systemDarkTheme
    }
    val backgroundColor = if (isDark) Color.Black else MaterialTheme.colorScheme.surface
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
                text = stringResource(id = R.string.pref_layout_title),
                style = MaterialTheme.typography.titleLarge,
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
            scrollBehavior = scrollBehavior,
            containerColor = backgroundColor,
          )
        },
      ) { padding ->
        ProvidePreferenceLocals {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
                start = 8.dp,
                end = 8.dp
            )
          ) {
            item {
              PreferenceSectionHeader(title = "Landscape Controls")
            }
            
            item {
              PreferenceCard {
                PreferenceCategoryWithEditButton(
                  title = stringResource(id = R.string.pref_layout_top_right_controls),
                  onClick = {
                    backstack.add(ControlLayoutEditorScreen(ControlRegion.TOP_RIGHT))
                  },
                )
                PreferenceIconSummary(buttons = topRightButtons)
                
                PreferenceDivider()
                
                PreferenceCategoryWithEditButton(
                  title = stringResource(id = R.string.pref_layout_bottom_right_controls),
                  onClick = {
                    backstack.add(ControlLayoutEditorScreen(ControlRegion.BOTTOM_RIGHT))
                  },
                )
                PreferenceIconSummary(buttons = bottomRightButtons)
                
                PreferenceDivider()
                
                PreferenceCategoryWithEditButton(
                  title = stringResource(id = R.string.pref_layout_bottom_left_controls),
                  onClick = {
                    backstack.add(ControlLayoutEditorScreen(ControlRegion.BOTTOM_LEFT))
                  },
                )
                PreferenceIconSummary(buttons = bottomLeftButtons)
              }
            }
            
            item {
              PreferenceSectionHeader(title = "Portrait Controls")
            }

            item {
              PreferenceCard {
                PreferenceCategoryWithEditButton(
                  title = stringResource(id = R.string.pref_layout_portrait_bottom_controls),
                  onClick = {
                    backstack.add(ControlLayoutEditorScreen(ControlRegion.PORTRAIT_BOTTOM))
                  },
                )
                PreferenceIconSummary(buttons = portraitBottomButtons)
              }
            }
            
            item {
              PreferenceSectionHeader(title = "Seekbar Style")
            }

            item {
              val seekbarStyle by appearancePrefs.seekbarStyle.collectAsState()
              
              PreferenceCard {
                SeekbarStyle.entries.forEachIndexed { index, style ->
                  ListItem(
                    headlineContent = {
                      Text(text = style.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    },
                    trailingContent = {
                      RadioButton(
                        selected = seekbarStyle == style,
                        onClick = null
                      )
                    },
                    colors = ListItemDefaults.colors(
                      containerColor = Color.Transparent,
                    ),
                    modifier = Modifier
                      .clickable { appearancePrefs.seekbarStyle.set(style) }
                  )
                  if (index < SeekbarStyle.entries.size - 1) {
                    PreferenceDivider()
                  }
                }
              }
            }
            
            item {
              PreferenceSectionHeader(title = "Appearance")
            }
            
            item {
              val hidePlayerButtonsBackground by appearancePrefs.hidePlayerButtonsBackground.collectAsState()
              val playerTimeToDisappear by playerPrefs.playerTimeToDisappear.collectAsState()
              val predefinedTimeValues = listOf(500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000)
              val isCustomTimeValue = !predefinedTimeValues.contains(playerTimeToDisappear)
              
              var showCustomTimeDialog by remember { mutableStateOf(false) }
              var customTimeValue by remember { mutableStateOf("") }
              
              PreferenceCard {
                SwitchPreference(
                  value = hidePlayerButtonsBackground,
                  onValueChange = { appearancePrefs.hidePlayerButtonsBackground.set(it) },
                  title = { Text(text = stringResource(id = R.string.pref_appearance_hide_player_buttons_background_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                  summary = { Text(text = stringResource(id = R.string.pref_appearance_hide_player_buttons_background_summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                )
                
                PreferenceDivider()
                
                val showFileExtension by playerPrefs.showFileExtension.collectAsState()
                SwitchPreference(
                  value = showFileExtension,
                  onValueChange = playerPrefs.showFileExtension::set,
                  title = { Text(stringResource(R.string.pref_player_show_file_extension_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                  summary = { Text(stringResource(R.string.pref_player_show_file_extension_summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                )

                PreferenceDivider()
                
                ListPreference(
                  value = if (isCustomTimeValue) -1 else playerTimeToDisappear,
                  onValueChange = { newValue ->
                    if (newValue == -1) {
                      customTimeValue = playerTimeToDisappear.toString()
                      showCustomTimeDialog = true
                    } else {
                      playerPrefs.playerTimeToDisappear.set(newValue)
                    }
                  },
                  values = predefinedTimeValues + listOf(-1),
                  valueToText = { value ->
                    if (value == -1) AnnotatedString("Custom") else AnnotatedString("$value ms")
                  },
                  title = { Text(text = stringResource(R.string.pref_player_display_hide_player_control_time), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                  summary = {
                    Text(
                      text = if (isCustomTimeValue) "Custom ($playerTimeToDisappear ms)" else "$playerTimeToDisappear ms",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  },
                )
              }
              
              if (showCustomTimeDialog) {
                AlertDialog(
                  onDismissRequest = { showCustomTimeDialog = false },
                  title = { Text(text = stringResource(R.string.pref_player_display_hide_player_control_time), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
                  text = {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                      Text(text = "Enter custom hide time in milliseconds", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))
                      OutlinedTextField(
                        value = customTimeValue,
                        onValueChange = { customTimeValue = it },
                        label = { Text("Milliseconds") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                      )
                    }
                  },
                  confirmButton = {
                    TextButton(
                      onClick = {
                        val value = customTimeValue.toIntOrNull()
                        if (value != null && value >= 100) {
                          playerPrefs.playerTimeToDisappear.set(value)
                          showCustomTimeDialog = false
                        }
                      },
                    ) {
                      Text(stringResource(R.string.generic_ok), fontWeight = FontWeight.Bold)
                    }
                  },
                  dismissButton = {
                    TextButton(onClick = { showCustomTimeDialog = false }) {
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
    }
  }

  @Composable
  private fun PreferenceCategoryWithEditButton(
    title: String,
    onClick: () -> Unit,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f),
      )
      IconButton(onClick = onClick) {
        Icon(
          imageVector = Icons.Rounded.Edit,
          contentDescription = "Edit $title",
          tint = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }

  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  private fun PreferenceIconSummary(buttons: List<PlayerButton>) {
    FlowRow(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
      if (buttons.isEmpty()) {
        Text(
          "None",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.outline,
        )
      } else {
        buttons.forEach { button ->
          PlayerButtonChip(
            button = button,
            enabled = true,
            onClick = null, 
            badgeIcon = null,
            badgeColor = null
          )
        }
      }
    }
  }
}
