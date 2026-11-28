package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.ui.preferences.components.SwitchPreference
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.player.CustomKeyCodes
import app.marlboroadvance.mpvex.ui.player.SingleActionGesture
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.ui.utils.readableContentWidth
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.FooterPreference
import app.marlboroadvance.mpvex.ui.preferences.components.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import org.koin.compose.koinInject

@Serializable
object GesturePreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<GesturePreferences>()
    val backstack = LocalBackStack.current
    val useSingleTapForCenter by preferences.useSingleTapForCenter.collectAsState()

    var showCustomSeekDialog by remember { mutableStateOf(false) }
    var customSeekValue by remember { mutableStateOf("") }
    
    val gestureNames = SingleActionGesture.entries.associateWith { stringResource(it.titleRes) }

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
                text = stringResource(R.string.pref_gesture),
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
              PreferenceSectionHeader(title = stringResource(R.string.pref_gesture_double_tap_title))
            }
            
            item {
              PreferenceCard {
                val customLabel = stringResource(R.string.pref_gesture_double_tap_custom)
                val doubleTapSeekDuration by preferences.doubleTapToSeekDuration.collectAsState()
                val predefinedValues = listOf(3, 5, 10, 15, 20, 25, 30)
                val isCustomValue = !predefinedValues.contains(doubleTapSeekDuration)

                ListPreference(
                  value = if (isCustomValue) -1 else doubleTapSeekDuration,
                  onValueChange = { newValue ->
                    if (newValue == -1) {
                      customSeekValue = doubleTapSeekDuration.toString()
                      showCustomSeekDialog = true
                    } else {
                      preferences.doubleTapToSeekDuration.set(newValue)
                    }
                  },
                  values = predefinedValues + listOf(-1),
                  valueToText = { value ->
                    if (value == -1) {
                      AnnotatedString(customLabel)
                    } else {
                      AnnotatedString("${value}s")
                    }
                  },
                  title = { 
                    Text(
                      text = stringResource(id = R.string.pref_player_double_tap_seek_duration),
                    ) 
                  },
                  summary = {
                    Text(
                      text = if (isCustomValue) stringResource(R.string.custom_seconds_value, doubleTapSeekDuration) else "${doubleTapSeekDuration}s",
                    )
                  },
                )
                

                val doubleTapSeekAreaWidth by preferences.doubleTapSeekAreaWidth.collectAsState()
                val seekAreaValues = listOf(20, 25, 30, 35, 40, 45)

                ListPreference(
                  value = doubleTapSeekAreaWidth,
                  onValueChange = { preferences.doubleTapSeekAreaWidth.set(it) },
                  values = seekAreaValues,
                  valueToText = { AnnotatedString("${it}%") },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_double_tap_seek_area_width_title),
                    ) 
                  },
                  summary = {
                    Text(
                      text = stringResource(R.string.pref_double_tap_seek_area_width_summary, doubleTapSeekAreaWidth),
                    )
                  },
                )
                

                val leftDoubleTap by preferences.leftSingleActionGesture.collectAsState()
                ListPreference(
                  value = leftDoubleTap,
                  onValueChange = { preferences.leftSingleActionGesture.set(it) },
                  values = SingleActionGesture.entries,
                  valueToText = { AnnotatedString(gestureNames[it] ?: "") },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_gesture_double_tap_left_title),
                    ) 
                  },
                  summary = { 
                    Text(
                      text = gestureNames[leftDoubleTap] ?: "",
                    ) 
                  },
                )
                

                val centerDoubleTap by preferences.centerSingleActionGesture.collectAsState()
                val centerValues = listOf(
                      SingleActionGesture.None,
                      SingleActionGesture.PlayPause,
                      SingleActionGesture.Custom,
                    )
                ListPreference(
                  value = centerDoubleTap,
                  onValueChange = { preferences.centerSingleActionGesture.set(it) },
                  values = centerValues,
                  valueToText = { AnnotatedString(gestureNames[it] ?: "") },
                  title = {
                    Text(
                      text = stringResource(
                        if (useSingleTapForCenter) R.string.pref_gesture_single_tap_center_title else R.string.pref_gesture_double_tap_center_title,
                      ),
                    )
                  },
                  summary = { 
                    Text(
                      text = gestureNames[centerDoubleTap] ?: "",
                    ) 
                  },
                )
                

                val rightDoubleTap by preferences.rightSingleActionGesture.collectAsState()
                ListPreference(
                  value = rightDoubleTap,
                  onValueChange = { preferences.rightSingleActionGesture.set(it) },
                  values = SingleActionGesture.entries,
                  valueToText = { AnnotatedString(gestureNames[it] ?: "") },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_gesture_double_tap_right_title),
                    ) 
                  },
                  summary = { 
                    Text(
                      text = gestureNames[rightDoubleTap] ?: "",
                    ) 
                  },
                )
                

                val useSingleTapForCenterVal by preferences.useSingleTapForCenter.collectAsState()
                SwitchPreference(
                  value = useSingleTapForCenterVal,
                  onValueChange = { preferences.useSingleTapForCenter.set(it) },
                  title = {
                    Text(
                      text = stringResource(id = R.string.pref_gesture_use_single_tap_for_center_title),
                    )
                  },
                  summary = {
                    Text(
                      text = stringResource(id = R.string.pref_gesture_use_single_tap_for_center_summary),
                    )
                  },
                )

                val doubleTapKeyCodes = listOf(
                    CustomKeyCodes.DoubleTapLeft,
                    CustomKeyCodes.DoubleTapCenter,
                    CustomKeyCodes.DoubleTapRight,
                  ).map { it.keyCode }.toImmutableList()
                
                FooterPreference(
                  summary = {
                    var annotatedString = buildAnnotatedString {
                        append(stringResource(R.string.pref_gesture_double_tap_custom_info))
                    }

                    doubleTapKeyCodes.forEach { keyCode ->
                      val startIndex = annotatedString.indexOf(keyCode)
                      if (startIndex != -1) {
                          val endIndex = startIndex + keyCode.length
                          val currentString = annotatedString
                          annotatedString = buildAnnotatedString {
                              append(currentString)
                              addStyle(
                                style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                                start = startIndex,
                                end = endIndex,
                              )
                          }
                      }
                    }

                    Text(
                      text = annotatedString,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                  },
                )
              }
            }
            
            item {
              PreferenceSectionHeader(title = stringResource(R.string.pref_gesture_media_title))
            }
            
            item {
              PreferenceCard {
                val mediaButtonsEnabled by preferences.mediaButtonsEnabled.collectAsState()
                SwitchPreference(
                  value = mediaButtonsEnabled,
                  onValueChange = { preferences.mediaButtonsEnabled.set(it) },
                  title = {
                    Text(
                      text = stringResource(R.string.pref_gesture_media_buttons_enabled_title),
                    )
                  },
                  summary = {
                    Text(
                      text = stringResource(
                        if (mediaButtonsEnabled) {
                          R.string.pref_gesture_media_buttons_enabled_summary_on
                        } else {
                          R.string.pref_gesture_media_buttons_enabled_summary_off
                        },
                      ),
                    )
                  },
                )


                val mediaPrevious by preferences.mediaPreviousGesture.collectAsState()
                ListPreference(
                  value = mediaPrevious,
                  onValueChange = { preferences.mediaPreviousGesture.set(it) },
                  enabled = mediaButtonsEnabled,
                  values = SingleActionGesture.entries,
                  valueToText = { AnnotatedString(gestureNames[it] ?: "") },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_gesture_media_previous),
                    ) 
                  },
                  summary = { 
                    Text(
                      text = gestureNames[mediaPrevious] ?: "",
                    ) 
                  },
                )


                val mediaPlay by preferences.mediaPlayGesture.collectAsState()
                ListPreference(
                  value = mediaPlay,
                  onValueChange = { preferences.mediaPlayGesture.set(it) },
                  enabled = mediaButtonsEnabled,
                  values = SingleActionGesture.entries,
                  valueToText = { AnnotatedString(gestureNames[it] ?: "") },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_gesture_media_play),
                    ) 
                  },
                  summary = { 
                    Text(
                      text = gestureNames[mediaPlay] ?: "",
                    ) 
                  },
                )


                val mediaNext by preferences.mediaNextGesture.collectAsState()
                ListPreference(
                  value = mediaNext,
                  onValueChange = { preferences.mediaNextGesture.set(it) },
                  enabled = mediaButtonsEnabled,
                  values = SingleActionGesture.entries,
                  valueToText = { AnnotatedString(gestureNames[it] ?: "") },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_gesture_media_next),
                    ) 
                  },
                  summary = { 
                    Text(
                      text = gestureNames[mediaNext] ?: "",
                    )
                  },
                )

                val mediaKeyCodes = listOf(
                    CustomKeyCodes.MediaPrevious,
                    CustomKeyCodes.MediaPlay,
                    CustomKeyCodes.MediaNext,
                  ).map { it.keyCode }
                val mediaInfoText = stringResource(R.string.pref_gesture_media_custom_info)
                val keyCodeStyle = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                FooterPreference(
                  summary = {
                    val annotated = buildAnnotatedString {
                      append(mediaInfoText)
                      // Highlight each key code where it appears after its "Label: " prefix.
                      mediaKeyCodes.forEach { keyCode ->
                        val start = mediaInfoText.indexOf(": $keyCode")
                        if (start != -1) {
                          addStyle(keyCodeStyle, start + 2, start + 2 + keyCode.length)
                        }
                      }
                    }
                    Text(
                      text = annotated,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                  },
                )
              }
            }
          }
        }
      }
    }

    if (showCustomSeekDialog) {
      AlertDialog(
        onDismissRequest = { showCustomSeekDialog = false },
        title = { Text(text = stringResource(id = R.string.pref_player_double_tap_seek_duration), style = MaterialTheme.typography.headlineSmallEmphasized) },
        text = {
          Column {
            Text(text = stringResource(R.string.dialog_custom_seek_duration),
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(bottom = 16.dp),
            )
            OutlinedTextField(
              value = customSeekValue,
              onValueChange = { customSeekValue = it },
              label = { Text(stringResource(R.string.seconds)) },
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
              val value = customSeekValue.toIntOrNull()
              if (value != null && value in 1..120) {
                preferences.doubleTapToSeekDuration.set(value)
                showCustomSeekDialog = false
              }
            },
          ) {
            Text(stringResource(R.string.generic_ok), fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { showCustomSeekDialog = false }) {
            Text(stringResource(R.string.generic_cancel))
          }
        },
        shape = MaterialTheme.shapes.extraLarge
      )
    }
  }
}
