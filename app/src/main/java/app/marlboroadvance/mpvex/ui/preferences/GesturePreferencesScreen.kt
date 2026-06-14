package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import app.marlboroadvance.mpvex.preferences.GesturePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.player.CustomKeyCodes
import app.marlboroadvance.mpvex.ui.player.SingleActionGesture
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.FooterPreference
import me.zhanghai.compose.preference.ListPreference
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
                text = stringResource(R.string.pref_gesture),
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
              PreferenceSectionHeader(title = stringResource(R.string.pref_gesture_double_tap_title))
            }
            
            item {
              PreferenceCard {
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
                      AnnotatedString("Custom")
                    } else {
                      AnnotatedString("${value}s")
                    }
                  },
                  title = { 
                    Text(
                      text = stringResource(id = R.string.pref_player_double_tap_seek_duration),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = if (isCustomValue) "Custom (${doubleTapSeekDuration}s)" else "${doubleTapSeekDuration}s",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  },
                )
                
                PreferenceDivider()

                val doubleTapSeekAreaWidth by preferences.doubleTapSeekAreaWidth.collectAsState()
                val seekAreaValues = listOf(20, 25, 30, 35, 40, 45)

                ListPreference(
                  value = doubleTapSeekAreaWidth,
                  onValueChange = { preferences.doubleTapSeekAreaWidth.set(it) },
                  values = seekAreaValues,
                  valueToText = { AnnotatedString("${it}%") },
                  title = { 
                    Text(
                      text = "Double Tap Seek Area Width",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = "Current: ${doubleTapSeekAreaWidth}%",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  },
                )
                
                PreferenceDivider()

                val leftDoubleTap by preferences.leftSingleActionGesture.collectAsState()
                ListPreference(
                  value = leftDoubleTap,
                  onValueChange = { preferences.leftSingleActionGesture.set(it) },
                  values = SingleActionGesture.entries,
                  valueToText = { AnnotatedString(gestureNames[it] ?: "") },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_gesture_double_tap_left_title),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = { 
                    Text(
                      text = gestureNames[leftDoubleTap] ?: "",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                  },
                )
                
                PreferenceDivider()

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
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    )
                  },
                  summary = { 
                    Text(
                      text = gestureNames[centerDoubleTap] ?: "",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                  },
                )
                
                PreferenceDivider()

                val rightDoubleTap by preferences.rightSingleActionGesture.collectAsState()
                ListPreference(
                  value = rightDoubleTap,
                  onValueChange = { preferences.rightSingleActionGesture.set(it) },
                  values = SingleActionGesture.entries,
                  valueToText = { AnnotatedString(gestureNames[it] ?: "") },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_gesture_double_tap_right_title),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = { 
                    Text(
                      text = gestureNames[rightDoubleTap] ?: "",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                  },
                )
                
                PreferenceDivider()

                val useSingleTapForCenterVal by preferences.useSingleTapForCenter.collectAsState()
                me.zhanghai.compose.preference.SwitchPreference(
                  value = useSingleTapForCenterVal,
                  onValueChange = { preferences.useSingleTapForCenter.set(it) },
                  title = {
                    Text(
                      text = stringResource(id = R.string.pref_gesture_use_single_tap_for_center_title),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    )
                  },
                  summary = {
                    Text(
                      text = stringResource(id = R.string.pref_gesture_use_single_tap_for_center_summary),
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
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
                val mediaPrevious by preferences.mediaPreviousGesture.collectAsState()
                ListPreference(
                  value = mediaPrevious,
                  onValueChange = { preferences.mediaPreviousGesture.set(it) },
                  values = SingleActionGesture.entries,
                  valueToText = { AnnotatedString(gestureNames[it] ?: "") },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_gesture_media_previous),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = { 
                    Text(
                      text = gestureNames[mediaPrevious] ?: "",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                  },
                )

                PreferenceDivider()

                val mediaPlay by preferences.mediaPlayGesture.collectAsState()
                ListPreference(
                  value = mediaPlay,
                  onValueChange = { preferences.mediaPlayGesture.set(it) },
                  values = SingleActionGesture.entries,
                  valueToText = { AnnotatedString(gestureNames[it] ?: "") },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_gesture_media_play),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = { 
                    Text(
                      text = gestureNames[mediaPlay] ?: "",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    ) 
                  },
                )

                PreferenceDivider()

                val mediaNext by preferences.mediaNextGesture.collectAsState()
                ListPreference(
                  value = mediaNext,
                  onValueChange = { preferences.mediaNextGesture.set(it) },
                  values = SingleActionGesture.entries,
                  valueToText = { AnnotatedString(gestureNames[it] ?: "") },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_gesture_media_next),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = { 
                    Text(
                      text = gestureNames[mediaNext] ?: "",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
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
        title = { Text(text = stringResource(id = R.string.pref_player_double_tap_seek_duration), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
          Column {
            Text(
              text = "Enter custom seek duration in seconds (1-120)",
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(bottom = 16.dp),
            )
            OutlinedTextField(
              value = customSeekValue,
              onValueChange = { customSeekValue = it },
              label = { Text("Seconds") },
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
