package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SouthEast
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.ControlsPosition
import app.marlboroadvance.mpvex.preferences.PlayerButton
import app.marlboroadvance.mpvex.preferences.PlayerPreferences
import app.marlboroadvance.mpvex.preferences.SeekbarStyle
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.ui.utils.readableContentWidth
import kotlinx.serialization.Serializable
import app.marlboroadvance.mpvex.ui.preferences.components.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import app.marlboroadvance.mpvex.ui.preferences.components.SwitchPreference
import app.marlboroadvance.mpvex.ui.preferences.components.PlayerButtonChip
import app.marlboroadvance.mpvex.ui.preferences.components.PreferenceChoice
import app.marlboroadvance.mpvex.ui.preferences.components.PreferenceSlider
import app.marlboroadvance.mpvex.ui.player.controls.components.SeekbarStylePreview
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

    // Settings follow the theme surface; the AMOLED option already makes it pure black.
    val backgroundColor = MaterialTheme.colorScheme.surface
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
                text = stringResource(id = R.string.pref_layout_title),
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
              PreferenceSectionHeader(title = stringResource(R.string.pref_section_landscape_controls))
            }
            
            item {
              PreferenceCard {
                LayoutRegionPreference(
                  title = stringResource(R.string.pref_layout_top_right_controls),
                  icon = Icons.Outlined.NorthEast,
                  buttons = topRightButtons,
                  onClick = { backstack.add(ControlLayoutEditorScreen(ControlRegion.TOP_RIGHT)) },
                )
                LayoutRegionPreference(
                  title = stringResource(R.string.pref_layout_bottom_right_controls),
                  icon = Icons.Outlined.SouthEast,
                  buttons = bottomRightButtons,
                  onClick = { backstack.add(ControlLayoutEditorScreen(ControlRegion.BOTTOM_RIGHT)) },
                )
                LayoutRegionPreference(
                  title = stringResource(R.string.pref_layout_bottom_left_controls),
                  icon = Icons.Outlined.SouthWest,
                  buttons = bottomLeftButtons,
                  onClick = { backstack.add(ControlLayoutEditorScreen(ControlRegion.BOTTOM_LEFT)) },
                )
              }
            }

            item {
              PreferenceSectionHeader(title = stringResource(R.string.pref_section_portrait_controls))
            }

            item {
              PreferenceCard {
                LayoutRegionPreference(
                  title = stringResource(R.string.pref_layout_portrait_bottom_controls),
                  icon = Icons.Outlined.Smartphone,
                  buttons = portraitBottomButtons,
                  onClick = { backstack.add(ControlLayoutEditorScreen(ControlRegion.PORTRAIT_BOTTOM)) },
                )
              }
            }

            item {
              PreferenceSectionHeader(title = stringResource(R.string.pref_section_seekbar_style))
            }

            item {
              val seekbarStyle by appearancePrefs.seekbarStyle.collectAsState()
              val showSeekbarChapters by appearancePrefs.showSeekbarChapters.collectAsState()
              val controlsPosition by appearancePrefs.controlsPosition.collectAsState()
              val aboveText = stringResource(R.string.pref_controls_position_above)
              val belowText = stringResource(R.string.pref_controls_position_below)

              // One card for everything seekbar-related, so a change is visible in the preview
              // right above it.
              PreferenceCard {
                // The three styles as connected buttons, with a live preview of the selected
                // one on a tonal surface. The preview crossfades on style change and drops its
                // chapter gaps when "show chapters" is off.
                PreferenceChoice(
                  options = SeekbarStyle.entries.toList(),
                  selected = seekbarStyle,
                  onSelect = { appearancePrefs.seekbarStyle.set(it) },
                  label = { seekbarStyleLabel(it) },
                  footer = {
                    Crossfade(
                      targetState = seekbarStyle,
                      modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                      label = "seekbar_style_preview",
                    ) { style ->
                      Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                      ) {
                        SeekbarStylePreview(
                          style = style,
                          showChapters = showSeekbarChapters,
                          modifier = Modifier.scale(1.5f),
                        )
                      }
                    }
                  },
                )
                SwitchPreference(
                  value = showSeekbarChapters,
                  onValueChange = { appearancePrefs.showSeekbarChapters.set(it) },
                  title = {
                    Text(
                      text = stringResource(R.string.pref_seekbar_show_chapters_title),
                    )
                  },
                  summary = {
                    Text(
                      text = stringResource(R.string.pref_seekbar_show_chapters_summary),
                    )
                  },
                )
                PreferenceChoice(
                  options = ControlsPosition.entries.toList(),
                  selected = controlsPosition,
                  onSelect = { appearancePrefs.controlsPosition.set(it) },
                  label = { if (it == ControlsPosition.Below) belowText else aboveText },
                  icon = {
                    if (it == ControlsPosition.Below) Icons.Outlined.VerticalAlignBottom else Icons.Outlined.VerticalAlignTop
                  },
                  title = { Text(text = stringResource(R.string.pref_controls_position_title)) },
                )
              }
            }

            item {
              PreferenceSectionHeader(title = stringResource(R.string.pref_appearance_title))
            }

            item {
              val hidePlayerButtonsBackground by appearancePrefs.hidePlayerButtonsBackground.collectAsState()
              PreferenceCard {
                SwitchPreference(
                  value = hidePlayerButtonsBackground,
                  onValueChange = { appearancePrefs.hidePlayerButtonsBackground.set(it) },
                  title = { Text(text = stringResource(id = R.string.pref_appearance_hide_player_buttons_background_title)) },
                  summary = { Text(text = stringResource(id = R.string.pref_appearance_hide_player_buttons_background_summary)) },
                )
              }
            }

            item {
              PreferenceSectionHeader(title = stringResource(R.string.pref_section_behavior))
            }

            item {
              val playerTimeToDisappear by playerPrefs.playerTimeToDisappear.collectAsState()
              val predefinedTimeValues = listOf(500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000)
              val isCustomTimeValue = !predefinedTimeValues.contains(playerTimeToDisappear)

              var showCustomTimeDialog by remember { mutableStateOf(false) }
              var customTimeValue by remember { mutableStateOf("") }

              PreferenceCard {
                val showFileExtension by playerPrefs.showFileExtension.collectAsState()
                SwitchPreference(
                  value = showFileExtension,
                  onValueChange = playerPrefs.showFileExtension::set,
                  title = { Text(stringResource(R.string.pref_player_show_file_extension_title)) },
                  summary = { Text(stringResource(R.string.pref_player_show_file_extension_summary)) },
                )

                PreferenceSlider(
                  value = playerTimeToDisappear,
                  onValueChangeFinished = { playerPrefs.playerTimeToDisappear.set(it) },
                  min = 500,
                  max = 5000,
                  step = 500,
                  title = { Text(text = stringResource(R.string.pref_player_display_hide_player_control_time)) },
                  valueLabel = {
                    if (isCustomTimeValue && it == playerTimeToDisappear) {
                      stringResource(R.string.custom_ms_value, it)
                    } else {
                      stringResource(R.string.value_ms, it)
                    }
                  },
                  trailing = {
                    FilledTonalIconButton(
                      onClick = {
                        customTimeValue = playerTimeToDisappear.toString()
                        showCustomTimeDialog = true
                      },
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.pref_exact_value),
                      )
                    }
                  },
                )
              }

              if (showCustomTimeDialog) {
                AlertDialog(
                  onDismissRequest = { showCustomTimeDialog = false },
                  title = { Text(text = stringResource(R.string.pref_player_display_hide_player_control_time), style = MaterialTheme.typography.headlineSmallEmphasized) },
                  text = {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                      Text(text = stringResource(R.string.dialog_custom_hide_time), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))
                      val customTimeInvalid = (customTimeValue.toIntOrNull() ?: 0) < 100
                      OutlinedTextField(
                        value = customTimeValue,
                        onValueChange = { customTimeValue = it },
                        label = { Text(stringResource(R.string.milliseconds)) },
                        isError = customTimeInvalid,
                        supportingText = if (customTimeInvalid) {
                          { Text(stringResource(R.string.pref_hide_time_error)) }
                        } else {
                          null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                      )
                    }
                  },
                  confirmButton = {
                    TextButton(
                      enabled = (customTimeValue.toIntOrNull() ?: 0) >= 100,
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

  /** One tappable row per control region: icon, name, the buttons placed there, and a chevron. */
  @OptIn(ExperimentalLayoutApi::class)
  @Composable
  private fun LayoutRegionPreference(
    title: String,
    icon: ImageVector,
    buttons: List<PlayerButton>,
    onClick: () -> Unit,
  ) {
    Preference(
      title = { Text(text = title) },
      icon = {
        Surface(
          modifier = Modifier.size(40.dp),
          shape = CircleShape,
          color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(22.dp),
            )
          }
        }
      },
      summary = {
        if (buttons.isEmpty()) {
          Text(text = stringResource(R.string.pref_layout_region_empty))
        } else {
          // The placed buttons sit directly on the card, with no tile behind them.
          FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
          ) {
            buttons.forEach { button ->
              PlayerButtonChip(
                button = button,
                enabled = true,
                onClick = null,
                badgeIcon = null,
                badgeColor = null,
                compact = true,
              )
            }
          }
        }
      },
      trailingContent = {
        Icon(
          imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      },
      onClick = onClick,
    )
  }

  @Composable
  private fun seekbarStyleLabel(style: SeekbarStyle): String = stringResource(
    when (style) {
      SeekbarStyle.Standard -> R.string.seekbar_style_standard
      SeekbarStyle.Thick -> R.string.seekbar_style_thick
      SeekbarStyle.Expressive -> R.string.seekbar_style_expressive
    },
  )
}
