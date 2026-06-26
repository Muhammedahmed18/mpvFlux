package app.marlboroadvance.mpvex.ui.preferences

import android.content.Intent
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.documentfile.provider.DocumentFile
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import app.marlboroadvance.mpvex.repository.wyzie.WyzieEncodings
import app.marlboroadvance.mpvex.repository.wyzie.WyzieFormats
import app.marlboroadvance.mpvex.repository.wyzie.WyzieSources
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.SubtitlesPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.media.CustomFontEntry
import app.marlboroadvance.mpvex.utils.media.OpenDocumentTreeContract
import app.marlboroadvance.mpvex.utils.media.copyFontFromUri
import app.marlboroadvance.mpvex.utils.media.loadCustomFontEntries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.TextFieldPreference
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import android.net.Uri
import androidx.compose.ui.text.font.FontFamily
import app.marlboroadvance.mpvex.repository.wyzie.WyzieLanguages
import org.koin.compose.koinInject

@Serializable
object SubtitlesPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val preferences = koinInject<SubtitlesPreferences>()

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = stringResource(R.string.pref_subtitles),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            )
          },
          navigationIcon = {
            IconButton(
              onClick = backstack::removeLastOrNull,
            ) {
              Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
              )
            }
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        var customFontEntries by remember { mutableStateOf<List<CustomFontEntry>>(emptyList()) }
        var fontLoadTrigger by remember { mutableIntStateOf(0) }
        var showFontViewerSheet by remember { mutableStateOf(false) }
        var fontViewerEntries by remember { mutableStateOf<List<Pair<CustomFontEntry, FontFamily?>>>(emptyList()) }
        var isFontViewerLoading by remember { mutableStateOf(false) }

        val fontFilePicker =
          rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments(),
          ) { uris ->
            if (uris.isEmpty()) return@rememberLauncherForActivityResult
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
              uris.forEach { uri -> copyFontFromUri(context, uri) }
              withContext(Dispatchers.Main) { fontLoadTrigger++ }
            }
          }

        LaunchedEffect(fontLoadTrigger) {
          customFontEntries = loadCustomFontEntries(context)
        }

        // Load typefaces for font viewer sheet when it opens or fonts are added
        LaunchedEffect(showFontViewerSheet, fontLoadTrigger) {
          if (!showFontViewerSheet) return@LaunchedEffect
          isFontViewerLoading = true
          val entries = withContext(Dispatchers.IO) {
            loadCustomFontEntries(context).map { entry ->
              val fontFamily = runCatching {
                FontFamily(Typeface.createFromFile(entry.file))
              }.getOrNull()
              Pair(entry, fontFamily)
            }
          }
          fontViewerEntries = entries
          isFontViewerLoading = false
        }

        val subtitleSaveFolder by preferences.subtitleSaveFolder.collectAsState()
        val wyzieHearingImpaired by preferences.wyzieHearingImpaired.collectAsState()
        val wyzieSources by preferences.wyzieSources.collectAsState()
        val wyzieFormats by preferences.wyzieFormats.collectAsState()
        val wyzieEncodings by preferences.wyzieEncodings.collectAsState()

        val saveLocationPicker =
          rememberLauncherForActivityResult(
            OpenDocumentTreeContract(),
          ) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult

            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            preferences.subtitleSaveFolder.set(uri.toString())
          }

        LazyColumn(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(padding),
        ) {
          // === GENERAL SECTION ===
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
                title = { Text(stringResource(R.string.pref_preferred_languages), fontWeight = FontWeight.Bold) },
                summary = {
                  if (preferredLanguages.isNotBlank()) {
                    Text(
                      preferredLanguages,
                      color = MaterialTheme.colorScheme.outline,
                    )
                  } else {
                    Text(
                      stringResource(R.string.not_set_video_default),
                      color = MaterialTheme.colorScheme.outline,
                    )
                  }
                },
                textField = { value, onValueChange, _ ->
                  Column {
                    Text(stringResource(R.string.enter_language_codes))
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
                title = { Text(stringResource(R.string.pref_subtitles_autoload_title), fontWeight = FontWeight.Bold) },
                summary = {
                  Text(
                    stringResource(R.string.pref_subtitles_autoload_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val overrideAss by preferences.overrideAssSubs.collectAsState()
              SwitchPreference(
                value = overrideAss,
                onValueChange = { preferences.overrideAssSubs.set(it) },
                title = { Text(stringResource(R.string.player_sheets_sub_override_ass), fontWeight = FontWeight.Bold) },
                summary = {
                  Text(
                    stringResource(R.string.player_sheets_sub_override_ass_subtitle),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              val scaleByWindow by preferences.scaleByWindow.collectAsState()
              SwitchPreference(
                value = scaleByWindow,
                onValueChange = { preferences.scaleByWindow.set(it) },
                title = { Text(stringResource(R.string.player_sheets_sub_scale_by_window), fontWeight = FontWeight.Bold) },
                summary = {
                  Text(
                    stringResource(R.string.player_sheets_sub_scale_by_window_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )

              PreferenceDivider()

              Box(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .clickable { showFontViewerSheet = true }
                    .padding(vertical = 16.dp, horizontal = 16.dp),
              ) {
                Column {
                  Text(
                    stringResource(R.string.pref_subtitles_fonts_dir),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  if (customFontEntries.isEmpty()) {
                    Text(
                      stringResource(R.string.not_set_system_fonts),
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  } else {
                    Text(
                      stringResource(R.string.fonts_loaded, customFontEntries.size),
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                  }
                }
              }
            }
          }

          // === ONLINE SUBTITLE SECTION ===
          item {
            PreferenceSectionHeader(title = "Subtitle Search")
          }

          item {
            PreferenceCard {
              // Location display
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { saveLocationPicker.launch(null) }
                  .padding(vertical = 16.dp, horizontal = 16.dp),
              ) {
                Column {
                  Text(
                    stringResource(R.string.pref_subtitles_save_location),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  val folderPath = if (subtitleSaveFolder.isBlank()) {
                    stringResource(R.string.not_set_video_default)
                  } else {
                    val raw = Uri.parse(subtitleSaveFolder).path ?: subtitleSaveFolder
                    if (raw.contains(":")) raw.substringAfterLast(":") else raw
                  }
                  Text(
                    text = folderPath,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              PreferenceDivider()

              var showClearDialog by remember { mutableStateOf(false) }
              val scope = androidx.compose.runtime.rememberCoroutineScope()

              PreferenceDivider()

              // Wyzie Sources
              MultiChoicePreference(
                title = { Text("Subtitle Sources", fontWeight = FontWeight.Bold) },
                summary = {
                  val summaryText = if (wyzieSources.isEmpty() || wyzieSources.contains("all")) {
                    "All"
                  } else {
                    wyzieSources.mapNotNull { WyzieSources.ALL[it] }.joinToString(", ")
                  }
                  Text(summaryText, color = MaterialTheme.colorScheme.outline)
                },
                values = WyzieSources.ALL,
                selectedValues = wyzieSources,
                onValuesChange = { preferences.wyzieSources.set(it) },
                hasAllOption = true
              )

              PreferenceDivider()

              // Languages
              val subdlLanguages by preferences.subdlLanguages.collectAsState()
              MultiChoicePreference(
                title = { Text(stringResource(R.string.pref_subtitles_subdl_languages), fontWeight = FontWeight.Bold) },
                summary = {
                  val summaryText = if (subdlLanguages.isEmpty() || subdlLanguages.contains("all")) {
                    stringResource(R.string.all_languages)
                  } else {
                    subdlLanguages.mapNotNull { WyzieLanguages.ALL[it] }.joinToString(", ")
                  }
                  Text(summaryText, color = MaterialTheme.colorScheme.outline)
                },
                values = WyzieLanguages.SORTED,
                selectedValues = subdlLanguages,
                onValuesChange = { preferences.subdlLanguages.set(it) },
                hasAllOption = true
              )

              PreferenceDivider()

              // Advanced Filters (Toggleable)
              var showAdvanced by remember { mutableStateOf(false) }
              Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvanced = !showAdvanced }
                    .padding(16.dp),
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
                    imageVector = if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                  )
                }

                if (showAdvanced) {
                  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SwitchPreference(
                      value = wyzieHearingImpaired,
                      onValueChange = { preferences.wyzieHearingImpaired.set(it) },
                      title = { Text("Hearing-impaired friendly", fontWeight = FontWeight.Bold) },
                      summary = { Text("Only show subtitles optimized for hearing impaired") }
                    )

                    PreferenceDivider()

                    MultiChoicePreference(
                      title = { Text("Preferred Formats", fontWeight = FontWeight.Bold) },
                      summary = {
                        val summaryText = if (wyzieFormats.isEmpty() || wyzieFormats.contains("all")) {
                          "All"
                        } else {
                          wyzieFormats.mapNotNull { WyzieFormats.ALL[it] }.joinToString(", ")
                        }
                        Text(summaryText, color = MaterialTheme.colorScheme.outline)
                      },
                      values = WyzieFormats.ALL,
                      selectedValues = wyzieFormats,
                      onValuesChange = { preferences.wyzieFormats.set(it) },
                      hasAllOption = true
                    )

                    PreferenceDivider()

                    MultiChoicePreference(
                      title = { Text("Preferred Encodings", fontWeight = FontWeight.Bold) },
                      summary = {
                        val summaryText = if (wyzieEncodings.isEmpty() || wyzieEncodings.contains("all")) {
                          "All"
                        } else {
                          wyzieEncodings.mapNotNull { WyzieEncodings.ALL[it] }.joinToString(", ")
                        }
                        Text(summaryText, color = MaterialTheme.colorScheme.outline)
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

              Preference(
                title = { Text(stringResource(R.string.pref_subtitles_clear_downloads), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                summary = { Text(stringResource(R.string.pref_subtitles_clear_downloads_summary)) },
                onClick = { showClearDialog = true },
                enabled = subtitleSaveFolder.isNotBlank()
              )

              if (showClearDialog) {
                AlertDialog(
                  onDismissRequest = { showClearDialog = false },
                  title = { Text(stringResource(R.string.pref_subtitles_clear_downloads)) },
                  text = { Text(stringResource(R.string.pref_subtitles_clear_downloads_confirmation)) },
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
                      Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                  },
                  dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                      Text(stringResource(android.R.string.cancel))
                    }
                  }
                )
              }

              PreferenceDivider()

              // Wyzie Tag
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Subtitle Search provided by",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "sub.wyzie.io",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sub.wyzie.io"))
                    context.startActivity(intent)
                  }
                )
              }
            }
          }
        }

        // Font viewer bottom sheet
        if (showFontViewerSheet) {
          val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
          ModalBottomSheet(
            onDismissRequest = {
              showFontViewerSheet = false
              fontViewerEntries = emptyList()
              isFontViewerLoading = false
            },
            sheetState = sheetState,
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Installed Fonts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
              )
              IconButton(
                onClick = {
                  fontFilePicker.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf", "*/*"))
                },
              ) {
                Icon(
                  Icons.Default.Add,
                  contentDescription = "Add font",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }

            val sheetScope = androidx.compose.runtime.rememberCoroutineScope()
            if (isFontViewerLoading) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(160.dp),
                contentAlignment = Alignment.Center,
              ) {
                CircularProgressIndicator()
              }
            } else if (fontViewerEntries.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(160.dp),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = "No fonts installed",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            } else {
              LazyColumn {
                items(fontViewerEntries, key = { it.first.familyName }) { (fontEntry, fontFamily) ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = fontEntry.familyName,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Medium,
                      )
                      Text(
                        text = "The quick brown fox jumps over the lazy dog",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = fontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    IconButton(
                      onClick = {
                        sheetScope.launch(Dispatchers.IO) {
                          runCatching { fontEntry.file.delete() }
                          withContext(Dispatchers.Main) {
                            fontViewerEntries = fontViewerEntries.filterNot { it.first == fontEntry }
                            fontLoadTrigger++
                          }
                        }
                      },
                    ) {
                      Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete font",
                        tint = MaterialTheme.colorScheme.error,
                      )
                    }
                  }
                  HorizontalDivider()
                }
                item { Spacer(Modifier.height(32.dp)) }
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

  Preference(
    title = title,
    summary = summary,
    onClick = { showDialog = true }
  )

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

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
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
                }
                .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Checkbox(
                checked = checked,
                onCheckedChange = null
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = entry.second)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showDialog = false }) {
          Text(stringResource(android.R.string.ok))
        }
      }
    )
  }
}
