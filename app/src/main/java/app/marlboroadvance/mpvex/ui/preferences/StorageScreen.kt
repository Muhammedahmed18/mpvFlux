package app.marlboroadvance.mpvex.ui.preferences

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.thumbnail.ThumbnailRepository
import app.marlboroadvance.mpvex.preferences.AdvancedPreferences
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.presentation.components.ConfirmDialog
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.utils.storage.CacheSizeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import java.io.File

@Serializable
object StorageScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val thumbnailRepository = koinInject<ThumbnailRepository>()
    val advancedPreferences = koinInject<AdvancedPreferences>()

    val fontsDir = remember { File(context.filesDir, "fonts") }
    val configFile = remember { File(context.filesDir, "mpv.conf") }

    // null = still computing -> rows render a placeholder dash.
    var thumbnailBytes by remember { mutableStateOf<Long?>(null) }
    var fontsBytes by remember { mutableStateOf<Long?>(null) }
    var configBytes by remember { mutableStateOf<Long?>(null) }
    var tempBytes by remember { mutableStateOf<Long?>(null) }
    val maxThumbnailBytes = remember { thumbnailRepository.getMaxDiskCacheBytes() }

    fun refresh() {
      scope.launch(Dispatchers.IO) {
        val thumbs = thumbnailRepository.getDiskCacheBytes()
        val fonts = CacheSizeUtils.dirSizeBytes(fontsDir)
        val config = CacheSizeUtils.fileSizeBytes(configFile)
        val temp = CacheSizeUtils.dirSizeBytes(context.cacheDir)
        withContext(Dispatchers.Main) {
          thumbnailBytes = thumbs
          fontsBytes = fonts
          configBytes = config
          tempBytes = temp
        }
      }
    }

    LaunchedEffect(Unit) { refresh() }

    var confirmClearThumbnails by remember { mutableStateOf(false) }
    var confirmClearFonts by remember { mutableStateOf(false) }
    var confirmClearConfig by remember { mutableStateOf(false) }
    var confirmClearTemp by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }

    // Captured up here because the toasts fire from Dispatchers.IO, where stringResource is unavailable.
    val thumbnailsClearedMsg = stringResource(R.string.pref_storage_clear_thumbnails_done)
    val fontsClearedMsg = stringResource(R.string.pref_storage_clear_fonts_done)
    val configClearedMsg = stringResource(R.string.pref_storage_clear_config_done)
    val tempClearedMsg = stringResource(R.string.pref_storage_clear_temp_done)
    val allClearedMsg = stringResource(R.string.pref_storage_clear_all_done)

    fun toast(message: String) {
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val totalBytes: Long? = run {
      val parts = listOf(thumbnailBytes, fontsBytes, configBytes, tempBytes)
      if (parts.any { it == null }) null else parts.filterNotNull().sum()
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      topBar = {
        PreferenceTopBar(
          title = {
            Text(
              text = stringResource(R.string.pref_storage_title),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            )
          },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.secondary,
              )
            }
          },
          scrollBehavior = scrollBehavior,
          containerColor = MaterialTheme.colorScheme.background,
        )
      },
    ) { padding ->
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
          top = padding.calculateTopPadding(),
          bottom = padding.calculateBottomPadding() + 24.dp,
          start = 8.dp,
          end = 8.dp,
        ),
      ) {
        // Hero gauge — the one cache with a real budget.
        item {
          ThumbnailStorageCard(
            usedBytes = thumbnailBytes,
            maxBytes = maxThumbnailBytes,
            onClear = { confirmClearThumbnails = true },
          )
        }

        item { PreferenceSectionHeader(title = stringResource(R.string.pref_storage_section_other)) }
        item {
          PreferenceCard {
            CacheSizeRow(
              title = stringResource(R.string.pref_storage_fonts_title),
              summary = stringResource(R.string.pref_storage_fonts_summary),
              icon = Icons.Rounded.FontDownload,
              bytes = fontsBytes,
              onClick = { confirmClearFonts = true },
            )
            PreferenceDivider()
            CacheSizeRow(
              title = stringResource(R.string.pref_storage_config_title),
              summary = stringResource(R.string.pref_storage_config_summary),
              icon = Icons.Rounded.Settings,
              bytes = configBytes,
              onClick = { confirmClearConfig = true },
            )
            PreferenceDivider()
            CacheSizeRow(
              title = stringResource(R.string.pref_storage_temp_title),
              summary = stringResource(R.string.pref_storage_temp_summary),
              icon = Icons.Rounded.DeleteSweep,
              bytes = tempBytes,
              onClick = { confirmClearTemp = true },
            )
          }
        }

        item {
          PreferenceCard {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = stringResource(R.string.pref_storage_total_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
              )
              Text(
                text = totalBytes?.let { CacheSizeUtils.formatBytes(it) }
                  ?: stringResource(R.string.pref_storage_size_unknown),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
              TextButton(
                onClick = { confirmClearAll = true },
                colors = ButtonDefaults.textButtonColors(
                  contentColor = MaterialTheme.colorScheme.error,
                ),
              ) {
                Text(stringResource(R.string.pref_storage_clear_all), fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    if (confirmClearThumbnails) {
      ConfirmDialog(
        title = stringResource(R.string.pref_storage_clear_thumbnails_confirm_title),
        subtitle = stringResource(R.string.pref_storage_clear_thumbnails_confirm_subtitle),
        onConfirm = {
          confirmClearThumbnails = false
          scope.launch(Dispatchers.IO) {
            runCatching { thumbnailRepository.clearThumbnailCache() }
            withContext(Dispatchers.Main) { toast(thumbnailsClearedMsg) }
            refresh()
          }
        },
        onCancel = { confirmClearThumbnails = false },
      )
    }

    if (confirmClearFonts) {
      ConfirmDialog(
        title = stringResource(R.string.pref_storage_clear_fonts_confirm_title),
        subtitle = stringResource(R.string.pref_storage_clear_fonts_confirm_subtitle),
        onConfirm = {
          confirmClearFonts = false
          scope.launch(Dispatchers.IO) {
            clearFonts(fontsDir)
            withContext(Dispatchers.Main) { toast(fontsClearedMsg) }
            refresh()
          }
        },
        onCancel = { confirmClearFonts = false },
      )
    }

    if (confirmClearConfig) {
      ConfirmDialog(
        title = stringResource(R.string.pref_storage_clear_config_confirm_title),
        subtitle = stringResource(R.string.pref_storage_clear_config_confirm_subtitle),
        onConfirm = {
          confirmClearConfig = false
          scope.launch(Dispatchers.IO) {
            runCatching {
              configFile.delete()
              advancedPreferences.mpvConf.delete()
            }
            withContext(Dispatchers.Main) { toast(configClearedMsg) }
            refresh()
          }
        },
        onCancel = { confirmClearConfig = false },
      )
    }

    if (confirmClearTemp) {
      ConfirmDialog(
        title = stringResource(R.string.pref_storage_clear_temp_confirm_title),
        subtitle = stringResource(R.string.pref_storage_clear_temp_confirm_subtitle),
        onConfirm = {
          confirmClearTemp = false
          scope.launch(Dispatchers.IO) {
            runCatching { context.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
            withContext(Dispatchers.Main) { toast(tempClearedMsg) }
            refresh()
          }
        },
        onCancel = { confirmClearTemp = false },
      )
    }

    if (confirmClearAll) {
      ConfirmDialog(
        title = stringResource(R.string.pref_storage_clear_all_confirm_title),
        subtitle = stringResource(R.string.pref_storage_clear_all_confirm_subtitle),
        onConfirm = {
          confirmClearAll = false
          scope.launch(Dispatchers.IO) {
            runCatching { thumbnailRepository.clearThumbnailCache() }
            clearFonts(fontsDir)
            runCatching {
              configFile.delete()
              advancedPreferences.mpvConf.delete()
            }
            runCatching { context.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
            withContext(Dispatchers.Main) { toast(allClearedMsg) }
            refresh()
          }
        },
        onCancel = { confirmClearAll = false },
      )
    }
  }

  /** Deletes cached font files, mirroring the legacy Advanced screen behavior (ttf/otf only). */
  private fun clearFonts(fontsDir: File) {
    runCatching {
      if (fontsDir.exists()) {
        fontsDir.listFiles()?.forEach { f ->
          if (f.isFile && f.name.lowercase().matches(".*\\.[ot]tf$".toRegex())) f.delete()
        }
      }
    }
  }
}

@Composable
private fun ThumbnailStorageCard(
  usedBytes: Long?,
  maxBytes: Long,
  onClear: () -> Unit,
) {
  val fraction = if (usedBytes != null && maxBytes > 0L) {
    (usedBytes.toFloat() / maxBytes.toFloat()).coerceIn(0f, 1f)
  } else {
    0f
  }
  val animatedFraction by animateFloatAsState(targetValue = fraction, label = "thumb_fraction")
  val percent = (fraction * 100f).toInt()

  PreferenceCard {
    Column(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        PreferenceIcon(
          imageVector = Icons.Rounded.Image,
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.pref_storage_thumbnails_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = if (usedBytes != null) {
              stringResource(
                R.string.pref_storage_thumbnails_used,
                CacheSizeUtils.formatBytes(usedBytes),
                CacheSizeUtils.formatBytes(maxBytes),
              )
            } else {
              stringResource(R.string.pref_storage_calculating)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = if (usedBytes != null) "$percent%" else stringResource(R.string.pref_storage_size_unknown),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      LinearProgressIndicator(
        progress = { animatedFraction },
        modifier = Modifier
          .fillMaxWidth()
          .height(8.dp),
        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = stringResource(R.string.pref_storage_thumbnails_caption),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClear) {
          Text(stringResource(R.string.pref_storage_clear), fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun CacheSizeRow(
  title: String,
  summary: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  bytes: Long?,
  onClick: () -> Unit,
) {
  Preference(
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
    },
    summary = {
      Text(
        text = summary,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    icon = { PreferenceIcon(imageVector = icon) },
    trailingContent = {
      Text(
        text = bytes?.let { CacheSizeUtils.formatBytes(it) }
          ?: stringResource(R.string.pref_storage_size_unknown),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    onClick = onClick,
  )
}
