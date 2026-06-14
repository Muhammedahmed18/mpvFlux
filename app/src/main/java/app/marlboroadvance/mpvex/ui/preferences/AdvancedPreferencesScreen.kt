package app.marlboroadvance.mpvex.ui.preferences

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.database.MpvExDatabase
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.preferences.SettingsManager
import app.marlboroadvance.mpvex.preferences.SubtitlesPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.ConfirmDialog
import app.marlboroadvance.mpvex.presentation.crash.CrashActivity
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.history.RecentlyPlayedOps
import app.marlboroadvance.mpvex.utils.media.OpenDocumentTreeContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.TwoTargetIconButtonPreference
import org.koin.compose.koinInject

@Serializable
object AdvancedPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backStack = LocalBackStack.current
    val preferences = koinInject<AdvancedPreferences>()
    val subtitlesPreferences = koinInject<SubtitlesPreferences>()
    val settingsManager = koinInject<SettingsManager>()
    val scope = rememberCoroutineScope()
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var importStats by remember { mutableStateOf<SettingsManager.ImportStats?>(null) }
    var exportStats by remember { mutableStateOf<SettingsManager.ExportStats?>(null) }
    
    val clearedHistoryMsg = stringResource(R.string.pref_advanced_cleared_playback_history)

    val backgroundColor = rememberPreferenceBackgroundColor()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/xml")) { uri ->
        uri?.let {
          scope.launch {
            settingsManager.exportSettings(it).fold(
              onSuccess = { stats -> exportStats = stats; showExportDialog = true },
              onFailure = { error -> Toast.makeText(context, "Export failed: ${error.message}", Toast.LENGTH_LONG).show() },
            )
          }
        }
      }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
          scope.launch {
            settingsManager.importSettings(it).fold(
              onSuccess = { stats -> importStats = stats; showImportDialog = true },
              onFailure = { error -> Toast.makeText(context, "Import failed: ${error.message}", Toast.LENGTH_LONG).show() },
            )
          }
        }
      }

    if (showExportDialog && exportStats != null) {
      AlertDialog(
        onDismissRequest = { showExportDialog = false },
        title = { Text("Export Complete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = { Text("Successfully exported ${exportStats?.totalExported} items!", style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = { showExportDialog = false }) { Text("OK", fontWeight = FontWeight.Bold) } },
        shape = MaterialTheme.shapes.extraLarge
      )
    }

    if (showImportDialog && importStats != null) {
      AlertDialog(
        onDismissRequest = { showImportDialog = false },
        title = { Text("Import Complete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
          Text(
            "Successfully imported: ${importStats?.imported}\n" +
              "Failed: ${importStats?.failed}\n" +
              "Version: ${importStats?.version}\n\n" +
              "Please restart the app for all changes to take effect.",
            style = MaterialTheme.typography.bodyMedium
          )
        },
        confirmButton = { TextButton(onClick = { showImportDialog = false }) { Text("OK", fontWeight = FontWeight.Bold) } },
        shape = MaterialTheme.shapes.extraLarge
      )
    }

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
                text = stringResource(R.string.pref_advanced),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
              )
            },
            navigationIcon = {
              IconButton(onClick = backStack::removeLastOrNull) {
                Icon(
                  Icons.AutoMirrored.Rounded.ArrowBack, 
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary
                )
              }
            },
            scrollBehavior = scrollBehavior,
            containerColor = backgroundColor,
          )
        },
      ) { padding ->
        ProvidePreferenceLocals {
          val locationPicker = rememberLauncherForActivityResult(OpenDocumentTreeContract()) { uri ->
              if (uri == null) return@rememberLauncherForActivityResult
              val flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
              context.contentResolver.takePersistableUriPermission(uri, flags)
              preferences.mpvConfStorageUri.set(uri.toString())
              subtitlesPreferences.subtitleSaveFolder.set("")

              scope.launch(Dispatchers.IO) {
                runCatching {
                  val tree = DocumentFile.fromTreeUri(context, uri)
                  if (tree != null && tree.exists() && tree.canWrite()) {
                    listOf("fonts", "subtitles", "script-opts", "scripts", "shaders").forEach { name ->
                      if (tree.listFiles().none { it.isDirectory && it.name?.equals(name, ignoreCase = true) == true }) {
                        tree.createDirectory(name)
                      }
                    }
                    if (tree.listFiles().none { it.isFile && it.name?.equals("mpv.conf", ignoreCase = true) == true }) {
                      tree.createFile("application/octet-stream", "mpv.conf")
                    }
                    withContext(Dispatchers.Main) { Toast.makeText(context, "MPV directory ready ✓", Toast.LENGTH_SHORT).show() }
                  }
                }
              }
            }
          val mpvConfStorageLocation by preferences.mpvConfStorageUri.collectAsState()
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
                start = 8.dp,
                end = 8.dp
            )
          ) {
            item { PreferenceSectionHeader(title = "Backup & Restore") }
            item {
              PreferenceCard {
                PreferenceItem(
                  title = "Export Settings",
                  summary = "Export settings to an XML file",
                  icon = { PreferenceIcon(Icons.Rounded.FileUpload, containerColor = MaterialTheme.colorScheme.tertiaryContainer) },
                  onClick = { exportLauncher.launch(settingsManager.getDefaultExportFilename()) },
                )
                PreferenceDivider()
                PreferenceItem(
                  title = "Import Settings",
                  summary = "Import settings from an XML file",
                  icon = { PreferenceIcon(Icons.Rounded.FileDownload, containerColor = MaterialTheme.colorScheme.tertiaryContainer) },
                  onClick = { importLauncher.launch(arrayOf("text/xml", "application/xml", "*/*")) },
                )
              }
            }
            
            item { PreferenceSectionHeader(title = "MPV Configuration") }
            item {
              PreferenceCard {
                var mpvConf by remember { mutableStateOf(preferences.mpvConf.get()) }
                var inputConf by remember { mutableStateOf(preferences.inputConf.get()) }
                
                TwoTargetIconButtonPreference(
                  title = { Text(text = stringResource(R.string.pref_advanced_mpv_conf_storage_location), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                  summary = {
                    if (mpvConfStorageLocation.isNotBlank()) {
                      Text(text = getSimplifiedPathFromUri(mpvConfStorageLocation), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                  },
                  onClick = { locationPicker.launch(null) },
                  iconButtonIcon = { Icon(Icons.Rounded.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                  onIconButtonClick = { preferences.mpvConfStorageUri.delete() },
                  iconButtonEnabled = mpvConfStorageLocation.isNotBlank(),
                )
                
                PreferenceDivider()
                
                PreferenceItem(
                  title = stringResource(R.string.pref_advanced_mpv_conf),
                  summary = mpvConf.lines().firstOrNull()?.ifBlank { "Tap to edit configuration" } ?: "Tap to edit configuration",
                  onClick = { backStack.add(ConfigEditorScreen(ConfigEditorScreen.ConfigType.MPV_CONF)) },
                )
                
                PreferenceDivider()
                
                PreferenceItem(
                  title = stringResource(R.string.pref_advanced_input_conf),
                  summary = inputConf.lines().firstOrNull()?.ifBlank { "Tap to edit configuration" } ?: "Tap to edit configuration",
                  onClick = { backStack.add(ConfigEditorScreen(ConfigEditorScreen.ConfigType.INPUT_CONF)) },
                )
              }
            }
            
            item { PreferenceSectionHeader(title = "History") }
            item {
              PreferenceCard {
                val mpvexDatabase = koinInject<MpvExDatabase>()
                val enableRecentlyPlayed by preferences.enableRecentlyPlayed.collectAsState()
                var isConfirmDialogShown by remember { mutableStateOf(false) }
                
                SwitchPreference(
                  value = enableRecentlyPlayed,
                  onValueChange = preferences.enableRecentlyPlayed::set,
                  title = { Text(text = stringResource(R.string.pref_advanced_enable_recently_played_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                  summary = { Text(text = stringResource(R.string.pref_advanced_enable_recently_played_summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                )
                
                PreferenceDivider()
                
                PreferenceItem(
                  title = stringResource(R.string.pref_advanced_clear_playback_history),
                  summary = "Remove all playback history and positions",
                  onClick = { isConfirmDialogShown = true },
                )
                
                if (isConfirmDialogShown) {
                  ConfirmDialog(
                    stringResource(R.string.pref_advanced_clear_playback_history_confirm_title),
                    stringResource(R.string.pref_advanced_clear_playback_history_confirm_subtitle),
                    onConfirm = {
                      scope.launch(Dispatchers.IO) {
                        runCatching {
                          mpvexDatabase.videoDataDao().clearAllPlaybackStates()
                          RecentlyPlayedOps.clearAll()
                        }.onSuccess {
                          withContext(Dispatchers.Main) {
                            isConfirmDialogShown = false
                            Toast.makeText(context, clearedHistoryMsg, Toast.LENGTH_SHORT).show()
                          }
                        }
                      }
                    },
                    onCancel = { isConfirmDialogShown = false },
                  )
                }
              }
            }
            
            item { PreferenceSectionHeader(title = "Logging") }
            item {
              PreferenceCard {
                val activity = LocalActivity.current!!
                val clipboard = LocalClipboard.current
                val verboseLogging by preferences.verboseLogging.collectAsState()
                
                SwitchPreference(
                  value = verboseLogging,
                  onValueChange = preferences.verboseLogging::set,
                  title = { Text(text = stringResource(R.string.pref_advanced_verbose_logging_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                  summary = { Text(text = stringResource(R.string.pref_advanced_verbose_logging_summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                )
                
                PreferenceDivider()
                
                PreferenceItem(
                  title = stringResource(R.string.pref_advanced_dump_logs_title),
                  summary = stringResource(R.string.pref_advanced_dump_logs_summary),
                  onClick = {
                    scope.launch(Dispatchers.IO) {
                      val deviceInfo = CrashActivity.collectDeviceInfo()
                      val logcat = CrashActivity.collectLogcat()
                      val logs = CrashActivity.concatLogs(deviceInfo, null, logcat)
                      clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Logs", logs)))
                      CrashActivity.shareLogs(deviceInfo, null, logcat, activity)
                    }
                  },
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun getSimplifiedPathFromUri(uriString: String): String {
    val uri = Uri.parse(uriString)
    return uri.path?.let { path ->
        if (path.contains(":")) {
            path.substringAfterLast(":")
        } else {
            path
        }
    } ?: uriString
}
