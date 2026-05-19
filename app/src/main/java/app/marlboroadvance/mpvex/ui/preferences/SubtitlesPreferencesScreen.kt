package app.marlboroadvance.mpvex.ui.preferences

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.SubtitlesPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.repository.wyzie.WyzieEncodings
import app.marlboroadvance.mpvex.repository.wyzie.WyzieFormats
import app.marlboroadvance.mpvex.repository.wyzie.WyzieLanguages
import app.marlboroadvance.mpvex.repository.wyzie.WyzieSources
import app.marlboroadvance.mpvex.ui.theme.DarkMode
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.media.CustomFontEntry
import app.marlboroadvance.mpvex.utils.media.OpenDocumentTreeContract
import app.marlboroadvance.mpvex.utils.media.copyFontsFromDirectory
import app.marlboroadvance.mpvex.utils.media.loadCustomFontEntries
import com.github.k1rakishou.fsaf.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.TextFieldPreference
import org.koin.compose.koinInject

@Serializable
object SubtitlesPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val preferences = koinInject<SubtitlesPreferences>()
    val appPreferences = koinInject<AppearancePreferences>()
    val fileManager = koinInject<FileManager>()

    val darkMode by appPreferences.darkMode.collectAsState()
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
          TopAppBar(
            title = {
              Text(
                text = stringResource(R.string.pref_subtitles),
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                titleContentColor = MaterialTheme.colorScheme.primary
            )
          )
        },
      ) { padding ->
        ProvidePreferenceLocals {
          val fontsFolder by preferences.fontsFolder.collectAsState()
          var availableFonts by remember { mutableStateOf<List<String>>(emptyList()) }
          var customFontEntries by remember { mutableStateOf<List<CustomFontEntry>>(emptyList()) }
          var fontLoadTrigger by remember { mutableIntStateOf(0) }
          var isLoadingFonts by remember { mutableStateOf(false) }

          val locationPicker = rememberLauncherForActivityResult(OpenDocumentTreeContract()) { uri ->
              if (uri == null) return@rememberLauncherForActivityResult
              val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
              context.contentResolver.takePersistableUriPermission(uri, flags)
              preferences.fontsFolder.set(uri.toString())
              kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                isLoadingFonts = true
                copyFontsFromDirectory(context, fileManager, uri.toString())
                withContext(Dispatchers.Main) {
                  fontLoadTrigger++
                  isLoadingFonts = false
                }
              }
            }

          LaunchedEffect(fontsFolder, fontLoadTrigger) {
            customFontEntries = loadCustomFontEntries(context)
            availableFonts = listOf("Default") + customFontEntries.map { it.familyName }
          }

          LaunchedEffect(Unit) {
            if (fontsFolder.isNotBlank()) {
              isLoadingFonts = true
              withContext(Dispatchers.IO) {
                copyFontsFromDirectory(context, fileManager, fontsFolder)
              }
              fontLoadTrigger++
              isLoadingFonts = false
            }
          }

          val subtitleSaveFolder by preferences.subtitleSaveFolder.collectAsState()
          val wyzieHearingImpaired by preferences.wyzieHearingImpaired.collectAsState()
          val wyzieSources by preferences.wyzieSources.collectAsState()
          val wyzieFormats by preferences.wyzieFormats.collectAsState()
          val wyzieEncodings by preferences.wyzieEncodings.collectAsState()

          val saveLocationPicker = rememberLauncherForActivityResult(OpenDocumentTreeContract()) { uri ->
              if (uri == null) return@rememberLauncherForActivityResult
              val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
              context.contentResolver.takePersistableUriPermission(uri, flags)
              preferences.subtitleSaveFolder.set(uri.toString())
            }

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
              PreferenceSectionHeader(title = stringResource(R.string.general))
            }

            item {
              PreferenceCard {
                val preferredLanguages by preferences.preferredLanguages.collectAsState()
                TextFieldPreference(
                  value = preferredLanguages,
                  onValueChange = preferences.preferredLanguages::set,
                  textToValue = { it },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_preferred_languages),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = if (preferredLanguages.isNotBlank()) preferredLanguages else stringResource(R.string.not_set_video_default),
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  },
                  textField = { value, onValueChange, _ ->
                    Column {
                      Text(stringResource(R.string.enter_language_codes), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                      TextField(
                        value,
                        onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.language_codes_placeholder)) },
                      )
                    }
                  },
                )
                
                PreferenceDivider()

                val autoload by preferences.autoloadMatchingSubtitles.collectAsState()
                SwitchPreference(
                  value = autoload,
                  onValueChange = { preferences.autoloadMatchingSubtitles.set(it) },
                  title = { 
                    Text(
                      text = stringResource(R.string.pref_subtitles_autoload_title),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = stringResource(R.string.pref_subtitles_autoload_summary),
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  },
                )

                PreferenceDivider()

                val overrideAss by preferences.overrideAssSubs.collectAsState()
                SwitchPreference(
                  value = overrideAss,
                  onValueChange = { preferences.overrideAssSubs.set(it) },
                  title = { 
                    Text(
                      text = stringResource(R.string.player_sheets_sub_override_ass),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    ) 
                  },
                  summary = {
                    Text(
                      text = stringResource(R.string.player_sheets_sub_override_ass_subtitle),
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  },
                )

                PreferenceDivider()

                val scaleByWindow by preferences.scaleByWindow.collectAsState()
                SwitchPreference(
                    value = scaleByWindow,
                    onValueChange = { preferences.scaleByWindow.set(it) },
                    title = {
                        Text(
                            text = "Scale by window",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    summary = {
                        Text(
                            text = "Scale subtitles based on window size (useful for some types of subtitles)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                )

                PreferenceDivider()

                Surface(
                  onClick = { locationPicker.launch(null) },
                  color = Color.Transparent
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        stringResource(R.string.pref_subtitles_fonts_dir),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                      )
                      Text(
                        text = if (fontsFolder.isBlank()) stringResource(R.string.not_set_system_fonts) else stringResource(R.string.fonts_loaded, availableFonts.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }

                    if (fontsFolder.isNotBlank()) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLoadingFonts) {
                            CircularProgressIndicator(
                              modifier = Modifier.size(24.dp).padding(4.dp),
                              strokeWidth = 2.dp,
                              color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                          IconButton(
                            onClick = {
                              kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                isLoadingFonts = true
                                copyFontsFromDirectory(context, fileManager, fontsFolder)
                                withContext(Dispatchers.Main) {
                                  fontLoadTrigger++
                                  isLoadingFonts = false
                                }
                              }
                            },
                          ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                          }
                        }

                        IconButton(onClick = { preferences.fontsFolder.set(""); fontLoadTrigger++ }) {
                          Icon(Icons.Rounded.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                      }
                    }
                  }
                }
              }
            }

            item {
              PreferenceSectionHeader(title = "Subtitle Search")
            }

            item {
              PreferenceCard {
                Surface(
                  onClick = { saveLocationPicker.launch(null) },
                  color = Color.Transparent
                ) {
                  Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                      stringResource(R.string.pref_subtitles_save_location),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    )
                    val folderPath = if (subtitleSaveFolder.isBlank()) stringResource(R.string.not_set_video_default) else Uri.parse(subtitleSaveFolder).path ?: subtitleSaveFolder
                    Text(
                      text = folderPath,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                PreferenceDivider()

                var showClearDialog by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                MultiChoicePreference(
                  title = { Text(text = "Subtitle Sources", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                  summary = {
                    val summaryText = if (wyzieSources.isEmpty() || wyzieSources.contains("all")) "All" else wyzieSources.mapNotNull { WyzieSources.ALL[it] }.joinToString(", ")
                    Text(text = summaryText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  },
                  values = WyzieSources.ALL,
                  selectedValues = wyzieSources,
                  onValuesChange = { preferences.wyzieSources.set(it) },
                  hasAllOption = true
                )

                PreferenceDivider()

                val subdlLanguages by preferences.subdlLanguages.collectAsState()
                MultiChoicePreference(
                  title = { Text(text = stringResource(R.string.pref_subtitles_subdl_languages), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                  summary = {
                    val summaryText = if (subdlLanguages.isEmpty() || subdlLanguages.contains("all")) stringResource(R.string.all_languages) else subdlLanguages.mapNotNull { WyzieLanguages.ALL[it] }.joinToString(", ")
                    Text(text = summaryText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  },
                  values = WyzieLanguages.SORTED,
                  selectedValues = subdlLanguages,
                  onValuesChange = { preferences.subdlLanguages.set(it) },
                  hasAllOption = true
                )

                PreferenceDivider()

                var showAdvanced by remember { mutableStateOf(false) }
                Column(modifier = Modifier.fillMaxWidth()) {
                  Surface(
                    onClick = { showAdvanced = !showAdvanced },
                    color = Color.Transparent
                  ) {
                      Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                      ) {
                        Text(
                          text = "Advanced Search Filters",
                          style = MaterialTheme.typography.labelLarge,
                          color = MaterialTheme.colorScheme.primary,
                          fontWeight = FontWeight.Bold
                        )
                        Icon(
                          imageVector = if (showAdvanced) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.primary
                        )
                      }
                  }
                  
                  if (showAdvanced) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                      SwitchPreference(
                        value = wyzieHearingImpaired,
                        onValueChange = { preferences.wyzieHearingImpaired.set(it) },
                        title = { Text(text = "Hearing-impaired friendly", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                        summary = { Text(text = "Only show subtitles optimized for hearing impaired", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                      )

                      PreferenceDivider()

                      MultiChoicePreference(
                        title = { Text(text = "Preferred Formats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                        summary = {
                          val summaryText = if (wyzieFormats.isEmpty() || wyzieFormats.contains("all")) "All" else wyzieFormats.mapNotNull { WyzieFormats.ALL[it] }.joinToString(", ")
                          Text(text = summaryText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        values = WyzieFormats.ALL,
                        selectedValues = wyzieFormats,
                        onValuesChange = { preferences.wyzieFormats.set(it) },
                        hasAllOption = true
                      )

                      PreferenceDivider()

                      MultiChoicePreference(
                        title = { Text(text = "Preferred Encodings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                        summary = {
                          val summaryText = if (wyzieEncodings.isEmpty() || wyzieEncodings.contains("all")) "All" else wyzieEncodings.mapNotNull { WyzieEncodings.ALL[it] }.joinToString(", ")
                          Text(text = summaryText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        values = WyzieEncodings.ALL,
                        selectedValues = wyzieEncodings,
                        onValuesChange = { preferences.wyzieEncodings.set(it) },
                        hasAllOption = true
                      )
                      
                      Spacer(modifier = Modifier.size(16.dp))
                    }
                  }
                }

                PreferenceDivider()

                PreferenceItem(
                  title = stringResource(R.string.pref_subtitles_clear_downloads), 
                  summary = stringResource(R.string.pref_subtitles_clear_downloads_summary),
                  onClick = { showClearDialog = true },
                  modifier = Modifier.padding(vertical = 4.dp)
                )

                if (showClearDialog) {
                  AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text(stringResource(R.string.pref_subtitles_clear_downloads), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
                    text = { Text(stringResource(R.string.pref_subtitles_clear_downloads_confirmation), style = MaterialTheme.typography.bodyMedium) },
                    confirmButton = {
                      TextButton(
                        onClick = {
                          showClearDialog = false
                          scope.launch(Dispatchers.IO) {
                            runCatching {
                              val uri = Uri.parse(subtitleSaveFolder)
                              val folder = DocumentFile.fromTreeUri(context, uri)
                              folder?.listFiles()?.forEach { it.delete() }
                              withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(context, R.string.toast_subtitles_cleared, android.widget.Toast.LENGTH_SHORT).show()
                              }
                            }.onFailure { e ->
                              withContext(Dispatchers.Main) {
                                android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                              }
                            }
                          }
                        }
                      ) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                      }
                    },
                    dismissButton = {
                      TextButton(onClick = { showClearDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                      }
                    },
                    shape = MaterialTheme.shapes.extraLarge
                  )
                }

                PreferenceDivider()
                
                Surface(
                    onClick = {
                      val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sub.wyzie.ru"))
                      context.startActivity(intent)
                    },
                    color = Color.Transparent
                ) {
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(16.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = "Subtitle Search provided by",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                      Text(
                        text = "sub.wyzie.ru",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                      )
                    }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun MultiChoicePreference(
  title: @Composable () -> Unit,
  summary: @Composable () -> Unit,
  values: Map<String, String>,
  selectedValues: Set<String>,
  onValuesChange: (Set<String>) -> Unit,
  hasAllOption: Boolean = false
) {
  var showDialog by remember { mutableStateOf(false) }

  Surface(onClick = { showDialog = true }, color = Color.Transparent) {
      Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
          title()
          summary()
      }
  }

  if (showDialog) {
    AlertDialog(
      onDismissRequest = { showDialog = false },
      title = title,
      text = {
        LazyColumn {
          items(values.toList().size) { index ->
            val entry = values.toList()[index]
            val key = entry.first
            val checked = if (hasAllOption && (selectedValues.isEmpty() || selectedValues.contains("all"))) {
              key == "all"
            } else {
              selectedValues.contains(key)
            }
            
            Surface(
                onClick = {
                  val newSet = selectedValues.toMutableSet()
                  if (hasAllOption) {
                    if (key == "all") {
                      newSet.clear()
                      newSet.add("all")
                    } else {
                      newSet.remove("all")
                      if (checked) newSet.remove(key) else newSet.add(key)
                      if (newSet.isEmpty()) newSet.add("all")
                    }
                  } else {
                    if (checked) newSet.remove(key) else newSet.add(key)
                  }
                  onValuesChange(newSet)
                },
                color = Color.Transparent
            ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Checkbox(
                    checked = checked,
                    onCheckedChange = null
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(text = entry.second, style = MaterialTheme.typography.bodyLarge)
                }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showDialog = false }) {
          Text(stringResource(android.R.string.ok), fontWeight = FontWeight.Bold)
        }
      },
      shape = MaterialTheme.shapes.extraLarge
    )
  }
}
