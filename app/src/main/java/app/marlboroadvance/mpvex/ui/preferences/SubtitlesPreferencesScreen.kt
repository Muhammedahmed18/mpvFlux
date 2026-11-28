package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.material3.LoadingIndicator
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.documentfile.provider.DocumentFile
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import app.marlboroadvance.mpvex.repository.wyzie.WyzieEncodings
import app.marlboroadvance.mpvex.repository.wyzie.WyzieFormats
import app.marlboroadvance.mpvex.repository.wyzie.WyzieSources
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.preferences.PreferredLanguage
import app.marlboroadvance.mpvex.preferences.SubtitlesPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.preferences.components.LanguagePickerPreference
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.ui.utils.readableContentWidth
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
import app.marlboroadvance.mpvex.ui.preferences.components.SwitchPreference
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
    val screenScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      containerColor = MaterialTheme.colorScheme.surface,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        PreferenceTopBar(
          title = {
            Text(
              text = stringResource(R.string.pref_subtitles),
              color = MaterialTheme.colorScheme.primary,
            )
          },
          navigationIcon = {
            IconButton(
              onClick = backstack::removeLastOrNull,
            ) {
              Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.secondary,
              )
            }
          },
          scrollBehavior = scrollBehavior,
          containerColor = MaterialTheme.colorScheme.surface,
          large = true,
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        var customFontEntries by remember { mutableStateOf<List<CustomFontEntry>>(emptyList()) }
        var fontLoadTrigger by remember { mutableIntStateOf(0) }
        var showFontViewerSheet by remember { mutableStateOf(false) }
        var fontViewerEntries by remember { mutableStateOf<List<Pair<CustomFontEntry, FontFamily?>>>(emptyList()) }
        var isFontViewerLoading by remember { mutableStateOf(false) }
        // A font removed from the list but not yet deleted from disk, so the user can still undo.
        var pendingFontDelete by remember { mutableStateOf<CustomFontEntry?>(null) }
        val fontSnackbarHostState = remember { SnackbarHostState() }

        val commitPendingFontDelete: () -> Unit = {
          pendingFontDelete?.let { entry ->
            pendingFontDelete = null
            screenScope.launch(Dispatchers.IO) {
              runCatching { entry.file.delete() }
              withContext(Dispatchers.Main) { fontLoadTrigger++ }
            }
          }
        }

        DisposableEffect(Unit) {
          onDispose { pendingFontDelete?.let { runCatching { it.file.delete() } } }
        }

        val fontFilePicker =
          rememberLauncherForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments(),
          ) { uris ->
            if (uris.isEmpty()) return@rememberLauncherForActivityResult
            screenScope.launch(Dispatchers.IO) {
              uris.forEach { uri -> copyFontFromUri(context, uri) }
              withContext(Dispatchers.Main) { fontLoadTrigger++ }
            }
          }

        LaunchedEffect(fontLoadTrigger) {
          customFontEntries = loadCustomFontEntries(context).filterNot { it == pendingFontDelete }
        }

        // Load typefaces for font viewer sheet when it opens or fonts are added
        LaunchedEffect(showFontViewerSheet, fontLoadTrigger) {
          if (!showFontViewerSheet) return@LaunchedEffect
          isFontViewerLoading = true
          val pending = pendingFontDelete
          val entries = withContext(Dispatchers.IO) {
            loadCustomFontEntries(context).filterNot { it == pending }.map { entry ->
              val fontFamily = runCatching {
                FontFamily(Typeface.createFromFile(entry.file))
              }.getOrNull()
              Pair(entry, fontFamily)
            }
          }
          fontViewerEntries = entries
          isFontViewerLoading = false
        }

        val removeFont: (CustomFontEntry) -> Unit = { entry ->
          commitPendingFontDelete()
          pendingFontDelete = entry
          fontViewerEntries = fontViewerEntries.filterNot { it.first == entry }
          screenScope.launch {
            fontSnackbarHostState.currentSnackbarData?.dismiss()
            val result = fontSnackbarHostState.showSnackbar(
              message = context.getString(R.string.font_removed),
              actionLabel = context.getString(R.string.mark_as_undo),
              duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
              if (pendingFontDelete == entry) {
                pendingFontDelete = null
                fontLoadTrigger++
              }
            } else if (pendingFontDelete == entry) {
              commitPendingFontDelete()
            }
          }
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
              .padding(padding)
              .readableContentWidth(),
        ) {
          // === LANGUAGE & LOADING ===
          item {
            PreferenceSectionHeader(title = stringResource(R.string.pref_subtitles_section_language_loading))
          }

          item {
            PreferenceCard {
              val preferredLanguages by preferences.preferredLanguages.collectAsState()
              LanguagePickerPreference(
                title = stringResource(R.string.pref_preferred_language),
                value = preferredLanguages,
                onValueChange = { code ->
                  preferences.preferredLanguages.set(code)
                  // Online subtitle search follows the preferred language until the user
                  // picks search languages themselves.
                  if (code.isNotEmpty() && code != PreferredLanguage.OFF && !preferences.subdlLanguages.isSet()) {
                    preferences.subdlLanguages.set(setOf(code))
                  }
                },
                showOffOption = true,
              )


              val autoload by preferences.autoloadMatchingSubtitles.collectAsState()
              SwitchPreference(
                value = autoload,
                onValueChange = { preferences.autoloadMatchingSubtitles.set(it) },
                title = { Text(stringResource(R.string.pref_subtitles_autoload_title)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_subtitles_autoload_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )
            }
          }

          // === RENDERING & FONTS ===
          item {
            PreferenceSectionHeader(title = stringResource(R.string.pref_subtitles_section_rendering_fonts))
          }

          item {
            PreferenceCard {
              val usePhoneFont by preferences.usePhoneFontForTextSubs.collectAsState()
              SwitchPreference(
                value = usePhoneFont,
                onValueChange = { preferences.usePhoneFontForTextSubs.set(it) },
                title = { Text(stringResource(R.string.pref_sub_phone_font_title)) },
                summary = {
                  Text(
                    stringResource(R.string.pref_sub_phone_font_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )


              val overrideAss by preferences.overrideAssSubs.collectAsState()
              SwitchPreference(
                value = overrideAss,
                onValueChange = { preferences.overrideAssSubs.set(it) },
                title = { Text(stringResource(R.string.player_sheets_sub_override_ass)) },
                summary = {
                  Text(
                    stringResource(R.string.player_sheets_sub_override_ass_subtitle),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )


              val scaleByWindow by preferences.scaleByWindow.collectAsState()
              SwitchPreference(
                value = scaleByWindow,
                onValueChange = { preferences.scaleByWindow.set(it) },
                title = { Text(stringResource(R.string.player_sheets_sub_scale_by_window)) },
                summary = {
                  Text(
                    stringResource(R.string.player_sheets_sub_scale_by_window_summary),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
              )


              Preference(
                title = {
                  Text(
                    stringResource(R.string.pref_subtitles_fonts_dir),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                  )
                },
                summary = {
                  Text(
                    text =
                      if (customFontEntries.isEmpty()) {
                        stringResource(R.string.not_set_system_fonts)
                      } else {
                        stringResource(R.string.fonts_loaded, customFontEntries.size)
                      },
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                onClick = { showFontViewerSheet = true },
              )
            }
          }

          // === SUBTITLE SEARCH ===
          item {
            PreferenceSectionHeader(title = stringResource(R.string.pref_subtitles_section_search))
          }

          item {
            PreferenceCard {
              val allLabel = stringResource(R.string.all)

              // Wyzie Sources
              MultiChoicePreference(
                title = { Text(stringResource(R.string.pref_subtitles_sources), style = MaterialTheme.typography.titleMediumEmphasized) },
                summary = {
                  Text(
                    selectionSummary(wyzieSources, WyzieSources.ALL, allLabel),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                values = WyzieSources.ALL,
                selectedValues = wyzieSources,
                onValuesChange = { preferences.wyzieSources.set(it) },
                hasAllOption = true
              )


              // Languages
              val subdlLanguages by preferences.subdlLanguages.collectAsState()
              val allLanguagesLabel = stringResource(R.string.all_languages)
              MultiChoicePreference(
                title = { Text(stringResource(R.string.pref_subtitles_subdl_languages), style = MaterialTheme.typography.titleMediumEmphasized) },
                summary = {
                  Text(
                    selectionSummary(subdlLanguages, WyzieLanguages.ALL, allLanguagesLabel),
                    color = MaterialTheme.colorScheme.outline,
                  )
                },
                values = WyzieLanguages.SORTED,
                selectedValues = subdlLanguages,
                onValuesChange = { preferences.subdlLanguages.set(it) },
                hasAllOption = true
              )


              // Advanced Filters (Toggleable)
              var showAdvanced by remember { mutableStateOf(false) }
              val chevronRotation by animateFloatAsState(
                targetValue = if (showAdvanced) 180f else 0f,
                label = "advancedChevron",
              )
              Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) { showAdvanced = !showAdvanced }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = stringResource(R.string.pref_subtitles_advanced_filters),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                  )
                  Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(
                      if (showAdvanced) R.string.cd_expanded else R.string.cd_collapsed,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevronRotation),
                  )
                }

                AnimatedVisibility(visible = showAdvanced) {
                  Column(modifier = Modifier.fillMaxWidth()) {
                    SwitchPreference(
                      value = wyzieHearingImpaired,
                      onValueChange = { preferences.wyzieHearingImpaired.set(it) },
                      title = { Text(stringResource(R.string.pref_subtitles_hearing_impaired)) },
                      summary = {
                        Text(
                          stringResource(R.string.pref_subtitles_hearing_impaired_summary),
                          color = MaterialTheme.colorScheme.outline,
                        )
                      }
                    )


                    MultiChoicePreference(
                      title = { Text(stringResource(R.string.pref_subtitles_formats), style = MaterialTheme.typography.titleMediumEmphasized) },
                      summary = {
                        Text(
                          selectionSummary(wyzieFormats, WyzieFormats.ALL, allLabel),
                          color = MaterialTheme.colorScheme.outline,
                        )
                      },
                      values = WyzieFormats.ALL,
                      selectedValues = wyzieFormats,
                      onValuesChange = { preferences.wyzieFormats.set(it) },
                      hasAllOption = true
                    )


                    MultiChoicePreference(
                      title = { Text(stringResource(R.string.pref_subtitles_encodings), style = MaterialTheme.typography.titleMediumEmphasized) },
                      summary = {
                        Text(
                          selectionSummary(wyzieEncodings, WyzieEncodings.ALL, allLabel),
                          color = MaterialTheme.colorScheme.outline,
                        )
                      },
                      values = WyzieEncodings.ALL,
                      selectedValues = wyzieEncodings,
                      onValuesChange = { preferences.wyzieEncodings.set(it) },
                      hasAllOption = true
                    )
                  }
                }
              }
            }
          }

          // Wyzie attribution (footer, outside the card)
          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = stringResource(R.string.subtitle_search_provided_by),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = stringResource(R.string.wyzie_host),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(role = Role.Button) {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.wyzie_url)))
                  runCatching { context.startActivity(intent) }.onFailure {
                    screenScope.launch {
                      snackbarHostState.showSnackbar(context.getString(R.string.about_no_browser))
                    }
                  }
                },
              )
            }
          }

          // === DOWNLOADS ===
          item {
            PreferenceSectionHeader(title = stringResource(R.string.pref_subtitles_section_downloads))
          }

          item {
            PreferenceCard {
              var showClearDialog by remember { mutableStateOf(false) }
              // Number of subtitle files in the save folder; -1 while counting.
              var clearCount by remember { mutableIntStateOf(-1) }

              LaunchedEffect(showClearDialog) {
                if (showClearDialog) {
                  clearCount = -1
                  clearCount = withContext(Dispatchers.IO) {
                    runCatching { listSubtitleFiles(context, subtitleSaveFolder).size }.getOrDefault(0)
                  }
                }
              }

              // Location display
              val folderPath = if (subtitleSaveFolder.isBlank()) {
                stringResource(R.string.not_set_video_default)
              } else {
                val raw = Uri.parse(subtitleSaveFolder).path ?: subtitleSaveFolder
                if (raw.contains(":")) raw.substringAfterLast(":") else raw
              }
              Preference(
                title = {
                  Text(
                    stringResource(R.string.pref_subtitles_save_location),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                  )
                },
                summary = { Text(folderPath, color = MaterialTheme.colorScheme.outline) },
                onClick = { saveLocationPicker.launch(null) },
              )


              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 16.dp),
              ) {
                Text(
                  stringResource(R.string.pref_subtitles_clear_downloads),
                  style = MaterialTheme.typography.titleMediumEmphasized,
                )
                Text(
                  stringResource(R.string.pref_subtitles_clear_downloads_summary),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                  onClick = { showClearDialog = true },
                  enabled = subtitleSaveFolder.isNotBlank(),
                  colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                  ),
                ) {
                  Text(stringResource(R.string.pref_storage_clear))
                }
              }

              if (showClearDialog) {
                AlertDialog(
                  onDismissRequest = { showClearDialog = false },
                  icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                  title = { Text(stringResource(R.string.pref_subtitles_clear_downloads)) },
                  text = {
                    Text(
                      when {
                        clearCount < 0 -> stringResource(R.string.pref_subtitles_clear_downloads_confirmation)
                        clearCount == 0 -> stringResource(R.string.pref_subtitles_clear_downloads_none)
                        else -> pluralStringResource(
                          R.plurals.pref_subtitles_clear_downloads_confirmation_count,
                          clearCount,
                          clearCount,
                        )
                      },
                    )
                  },
                  confirmButton = {
                    TextButton(
                      enabled = clearCount > 0,
                      onClick = {
                        showClearDialog = false
                        screenScope.launch {
                          val result = withContext(Dispatchers.IO) {
                            runCatching { listSubtitleFiles(context, subtitleSaveFolder).forEach { it.delete() } }
                          }
                          val message = result.fold(
                            onSuccess = { context.getString(R.string.toast_subtitles_cleared) },
                            onFailure = {
                              context.getString(
                                R.string.pref_subtitles_clear_downloads_failed,
                                it.localizedMessage ?: it.javaClass.simpleName,
                              )
                            },
                          )
                          snackbarHostState.showSnackbar(message)
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
            }
          }
        }

        // Font viewer bottom sheet
        if (showFontViewerSheet) {
          val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
          ModalBottomSheet(
            onDismissRequest = {
              commitPendingFontDelete()
              showFontViewerSheet = false
              fontViewerEntries = emptyList()
              isFontViewerLoading = false
            },
            sheetState = sheetState,
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = stringResource(R.string.installed_fonts),
                style = MaterialTheme.typography.titleLargeEmphasized,
              )
              FilledTonalButton(
                onClick = {
                  fontFilePicker.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf", "*/*"))
                },
              ) {
                Icon(
                  Icons.Default.Add,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_font))
              }
            }

            Box(modifier = Modifier.fillMaxWidth()) {
              if (isFontViewerLoading) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  LoadingIndicator()
                }
              } else if (fontViewerEntries.isEmpty()) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = stringResource(R.string.no_fonts_installed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              } else {
                LazyColumn {
                  items(fontViewerEntries, key = { it.first.familyName }) { (fontEntry, fontFamily) ->
                    val dismissState = rememberSwipeToDismissBoxState(
                      confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                          removeFont(fontEntry)
                          true
                        } else {
                          false
                        }
                      },
                    )
                    SwipeToDismissBox(
                      state = dismissState,
                      enableDismissFromStartToEnd = false,
                      backgroundContent = {
                        Box(
                          modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(end = 24.dp),
                          contentAlignment = Alignment.CenterEnd,
                        ) {
                          Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                          )
                        }
                      },
                    ) {
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                            text = stringResource(R.string.font_preview_sample),
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = fontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                        }
                        IconButton(onClick = { removeFont(fontEntry) }) {
                          Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_font),
                            tint = MaterialTheme.colorScheme.error,
                          )
                        }
                      }
                    }
                    HorizontalDivider()
                  }
                  item { Spacer(Modifier.height(32.dp)) }
                }
              }

              SnackbarHost(
                hostState = fontSnackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
              )
            }
          }
        }
      }
    }
  }
}

/** Short summary for a multi-select value: "All", "English, Arabic" or "English, Arabic +3". */
@Composable
private fun selectionSummary(
  selected: Set<String>,
  values: Map<String, String>,
  allLabel: String,
): String {
  if (selected.isEmpty() || selected.contains("all")) return allLabel
  val names = selected.mapNotNull { values[it] }
  if (names.isEmpty()) return selected.joinToString(", ")
  val shown = names.take(2).joinToString(", ")
  val remaining = names.size - 2
  return if (remaining > 0) {
    stringResource(R.string.pref_subtitles_selection_summary, shown, remaining)
  } else {
    shown
  }
}

/** Extensions treated as subtitle files: the Wyzie formats plus the common ones. */
private fun subtitleExtensions(): Set<String> =
  WyzieFormats.ALL.keys.filter { it != "all" }.map { it.lowercase() }.toSet() +
    setOf("srt", "vtt", "ass", "ssa", "sub")

/** Subtitle files directly inside the chosen save folder. Other files are never touched. */
private fun listSubtitleFiles(context: Context, folderUri: String): List<DocumentFile> {
  val folder = DocumentFile.fromTreeUri(context, Uri.parse(folderUri)) ?: return emptyList()
  val extensions = subtitleExtensions()
  return folder.listFiles().filter {
    it.isFile && it.name.orEmpty().substringAfterLast('.', "").lowercase() in extensions
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiChoicePreference(
  title: @Composable () -> Unit,
  summary: @Composable () -> Unit,
  values: Map<String, String>,
  selectedValues: Set<String>,
  onValuesChange: (Set<String>) -> Unit,
  hasAllOption: Boolean = false
) {
  var showSheet by remember { mutableStateOf(false) }

  Preference(
    title = title,
    summary = summary,
    onClick = { showSheet = true }
  )

  if (showSheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }

    val closeSheet: () -> Unit = {
      scope.launch { sheetState.hide() }.invokeOnCompletion {
        if (!sheetState.isVisible) showSheet = false
      }
    }

    val isAll = hasAllOption && (selectedValues.isEmpty() || selectedValues.contains("all"))
    val options = remember(values, hasAllOption) {
      values.entries.filter { !(hasAllOption && it.key == "all") }
    }
    val filtered = remember(options, query) {
      val needle = query.trim()
      if (needle.isEmpty()) options else options.filter { it.value.contains(needle, ignoreCase = true) }
    }

    val toggle: (String) -> Unit = { key ->
      val newSet = selectedValues.toMutableSet()
      if (hasAllOption) newSet.remove("all")
      if (!newSet.add(key)) newSet.remove(key)
      if (hasAllOption && newSet.isEmpty()) newSet.add("all")
      onValuesChange(newSet)
    }

    ModalBottomSheet(
      onDismissRequest = { showSheet = false },
      sheetState = sheetState,
      shape = MaterialTheme.shapes.extraLarge,
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(modifier = Modifier.weight(1f)) { title() }
        TextButton(onClick = closeSheet) {
          Text(stringResource(R.string.done))
        }
      }

      OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(R.string.search)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { query = "" }) {
              Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_clear_search))
            }
          }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
      )

      val selectedEntries = if (isAll) {
        emptyList()
      } else {
        selectedValues.mapNotNull { key -> values[key]?.let { key to it } }
      }
      if (selectedEntries.isNotEmpty()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          selectedEntries.forEach { (key, label) ->
            InputChip(
              selected = true,
              onClick = { toggle(key) },
              label = { Text(label) },
              trailingIcon = {
                Icon(
                  Icons.Default.Close,
                  contentDescription = stringResource(R.string.remove),
                  modifier = Modifier.size(InputChipDefaults.IconSize),
                )
              },
            )
          }
        }
      }

      if (hasAllOption) {
        SelectionRow(
          label = stringResource(R.string.all),
          checked = isAll,
          onToggle = { onValuesChange(setOf("all")) },
        )
        HorizontalDivider()
      }

      if (filtered.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = stringResource(R.string.no_results_found),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn {
          items(filtered, key = { it.key }) { entry ->
            SelectionRow(
              label = entry.value,
              checked = !isAll && selectedValues.contains(entry.key),
              onToggle = { toggle(entry.key) },
            )
          }
          item { Spacer(Modifier.height(32.dp)) }
        }
      }
    }
  }
}

@Composable
private fun SelectionRow(
  label: String,
  checked: Boolean,
  onToggle: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Checkbox(checked = checked, onCheckedChange = null)
    Spacer(modifier = Modifier.width(16.dp))
    Text(text = label, style = MaterialTheme.typography.bodyLarge)
  }
}
