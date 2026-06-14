package app.marlboroadvance.mpvex.ui.preferences

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import java.io.File
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream
import kotlin.io.path.readLines

@Serializable
data class ConfigEditorScreen(
  val configType: ConfigType
) : Screen {

  enum class ConfigType {
    MPV_CONF,
    INPUT_CONF
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backStack = LocalBackStack.current
    val preferences = koinInject<AdvancedPreferences>()
    val scope = rememberCoroutineScope()

    val (fileName, initialValue) = when (configType) {
      ConfigType.MPV_CONF   -> "mpv.conf"   to preferences.mpvConf.get()
      ConfigType.INPUT_CONF -> "input.conf" to preferences.inputConf.get()
    }
    val screenTitle = when (configType) {
      ConfigType.MPV_CONF   -> "Edit mpv.conf"
      ConfigType.INPUT_CONF -> "Edit input.conf"
    }

    var configText by remember { mutableStateOf(initialValue) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    val mpvConfStorageLocation by preferences.mpvConfStorageUri.collectAsState()

    val backgroundColor = rememberPreferenceBackgroundColor()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(mpvConfStorageLocation) {
      if (mpvConfStorageLocation.isBlank()) return@LaunchedEffect
      withContext(Dispatchers.IO) {
        val tempFile = createTempFile()
        runCatching {
          val tree = DocumentFile.fromTreeUri(context, mpvConfStorageLocation.toUri())
          val configFile = tree?.findFile(fileName)
          if (configFile != null && configFile.exists()) {
            context.contentResolver.openInputStream(configFile.uri)?.copyTo(tempFile.outputStream())
            val content = tempFile.readLines().joinToString("\n")
            withContext(Dispatchers.Main) { configText = content }
          }
        }
        tempFile.deleteIfExists()
      }
    }

    fun saveConfig() {
      scope.launch(Dispatchers.IO) {
        try {
          when (configType) {
            ConfigType.MPV_CONF   -> preferences.mpvConf.set(configText)
            ConfigType.INPUT_CONF -> preferences.inputConf.set(configText)
          }
          File(context.filesDir, fileName).writeText(configText)

          if (mpvConfStorageLocation.isNotBlank()) {
            val tree = DocumentFile.fromTreeUri(context, mpvConfStorageLocation.toUri())
            if (tree != null) {
                val existing = tree.findFile(fileName)
                val confFile = existing ?: tree.createFile("text/plain", fileName)?.also { it.renameTo(fileName) }
                confFile?.uri?.let { uri ->
                    context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                      out.write(configText.toByteArray())
                      out.flush()
                    }
                }
            }
          }

          withContext(Dispatchers.Main) {
            hasUnsavedChanges = false
            Toast.makeText(context, "$fileName saved", Toast.LENGTH_SHORT).show()
            backStack.removeLastOrNull()
          }
        } catch (e: Exception) {
          withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
          }
        }
      }
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
            scrollBehavior = scrollBehavior,
            containerColor = backgroundColor,
            title = {
              Column {
                Text(
                  text = screenTitle,
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
                if (hasUnsavedChanges) {
                  Text(
                    text = "Unsaved changes",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                  )
                }
              }
            },
            navigationIcon = {
              IconButton(onClick = { backStack.removeLastOrNull() }) {
                Icon(
                  Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = "Back",
                  tint = MaterialTheme.colorScheme.secondary,
                )
              }
            },
            actions = {
              if (hasUnsavedChanges) {
                  val saveScale by animateFloatAsState(
                      targetValue = 1.1f,
                      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                      label = "save_scale"
                  )
                  IconButton(
                    onClick = { saveConfig() },
                    modifier = Modifier.scale(saveScale).padding(end = 8.dp)
                  ) {
                    Icon(
                        Icons.Rounded.Check, 
                        contentDescription = "Save",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                  }
              }
            },
          )
        },
      ) { padding ->
        val scrollState = rememberScrollState()
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .imePadding()
        ) {
          Surface(
              modifier = Modifier
                  .fillMaxSize()
                  .padding(12.dp),
              shape = MaterialTheme.shapes.large,
              color = MaterialTheme.colorScheme.surfaceContainerLow,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
          ) {
              BasicTextField(
                value = configText,
                onValueChange = { configText = it; hasUnsavedChanges = true },
                modifier = Modifier
                  .fillMaxSize()
                  .verticalScroll(scrollState)
                  .padding(16.dp),
                textStyle = TextStyle(
                  fontSize = 14.sp,
                  fontFamily = FontFamily.Monospace,
                  color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
              )
          }
        }
      }
    }
  }
}
