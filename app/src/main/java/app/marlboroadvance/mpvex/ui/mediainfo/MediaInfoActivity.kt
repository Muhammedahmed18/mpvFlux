package app.marlboroadvance.mpvex.ui.mediainfo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.marlboroadvance.mpvex.preferences.AppearancePreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.ui.theme.DarkMode
import app.marlboroadvance.mpvex.ui.theme.MpvexTheme
import app.marlboroadvance.mpvex.utils.media.MediaInfoOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.File

class MediaInfoActivity : ComponentActivity() {
  private val appearancePreferences by inject<AppearancePreferences>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      val dark by appearancePreferences.darkMode.collectAsState()
      val isSystemInDarkTheme = isSystemInDarkTheme()
      val isDarkMode = dark == DarkMode.Dark || (dark == DarkMode.System && isSystemInDarkTheme)

      enableEdgeToEdge(
        SystemBarStyle.auto(
          lightScrim = Color.White.toArgb(),
          darkScrim = Color.Transparent.toArgb(),
        ) { isDarkMode },
      )

      MpvexTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
          MediaInfoScreen(onBack = { finish() })
        }
      }
    }
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  private fun MediaInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var textContent by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf("Media File") }
    var mediaInfo by remember { mutableStateOf<MediaInfoOps.MediaInfoData?>(null) }

    // LargeTopAppBar with exitUntilCollapsed for the collapsing title effect
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
      val uri = when (intent?.action) {
        Intent.ACTION_SEND -> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
          } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
          }
        }
        Intent.ACTION_VIEW -> intent.data
        else -> null
      }

      if (uri == null) {
        error = "No media file provided"
        isLoading = false
        return@LaunchedEffect
      }

      fileName = try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
          val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
          if (nameIndex >= 0 && cursor.moveToFirst()) {
            cursor.getString(nameIndex) ?: uri.lastPathSegment ?: "Unknown"
          } else uri.lastPathSegment ?: "Unknown"
        } ?: uri.lastPathSegment ?: "Unknown"
      } catch (_: Exception) {
        uri.lastPathSegment ?: "Unknown"
      }

      scope.launch {
        try {
          MediaInfoOps.getMediaInfo(context, uri, fileName).onSuccess { info ->
            mediaInfo = info
            MediaInfoOps.generateTextOutput(context, uri, fileName).onSuccess { text ->
              textContent = text
            }
            isLoading = false
          }.onFailure { e ->
            error = e.message ?: "Failed to load media info"
            isLoading = false
          }
        } catch (e: Exception) {
          error = e.message ?: "Unknown error"
          isLoading = false
        }
      }
    }

    Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      containerColor = MaterialTheme.colorScheme.surface,
      topBar = {
        LargeTopAppBar(
          // Strip the extension from the top bar title
          title = {
            Text(
              text = fileName.substringBeforeLast('.'),
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          },
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
            }
          },
          actions = {
            if (!isLoading && error == null && textContent != null) {
              FilledTonalIconButton(onClick = { scope.launch { copyToClipboard(textContent!!, fileName) } }) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
              }
              IconButton(onClick = { scope.launch { shareMediaInfo(textContent!!, fileName) } }) {
                Icon(Icons.Filled.Share, contentDescription = "Share")
              }
            }
          },
          scrollBehavior = scrollBehavior,
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
        )
      },
    ) { padding ->
      Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        when {
          isLoading -> LoadingContent()
          error != null -> ErrorContent(error!!)
          mediaInfo != null -> MediaInfoContent(mediaInfo!!, textContent)
        }
      }
    }
  }

  @Composable
  private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(42.dp))
    }
  }

  @Composable
  private fun ErrorContent(errorMessage: String) {
    Column(
      modifier = Modifier.fillMaxSize().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.size(72.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
      }
      Spacer(modifier = Modifier.height(20.dp))
      Text(
        errorMessage,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }

  @Composable
  private fun MediaInfoContent(
    mediaInfo: MediaInfoOps.MediaInfoData,
    fullMediaInfoText: String?,
  ) {
    val sections = remember(fullMediaInfoText) {
      fullMediaInfoText?.let { parseMediaInfoText(it) } ?: emptyList()
    }

    // Build hero chips: resolution badge, video codec, audio codec(s), file size
    val video = mediaInfo.videoStreams.firstOrNull()
    val heroChips = remember(mediaInfo) {
      buildList {
        video?.let { v ->
          val h = v.height.filter { it.isDigit() }.toIntOrNull() ?: 0
          val resLabel = when {
            h >= 2160 -> "4K UHD"
            h >= 1080 -> "1080p"
            h >= 720  -> "720p"
            h > 0     -> "${h}p"
            else      -> null
          }
          resLabel?.let { add(it) }
          if (v.format.isNotBlank() && v.format != "---") add(v.format)
        }
        mediaInfo.audioStreams.firstOrNull()?.let { a ->
          if (a.format.isNotBlank() && a.format != "---") add(a.format)
        }
        if (mediaInfo.general.fileSize.isNotBlank() && mediaInfo.general.fileSize != "---") {
          add(mediaInfo.general.fileSize)
        }
      }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 32.dp),
    ) {
      // Hero chip row
      if (heroChips.isNotEmpty()) {
        item {
          HeroChipRow(heroChips)
        }
      }

      // ── Architecture section ──────────────────────────────────
      item { SectionDividerHeader("Architecture") }

      // Video stream cards
      mediaInfo.videoStreams.forEachIndexed { i, v ->
        item {
          StreamCard(
            title = if (mediaInfo.videoStreams.size > 1) "Video #${i + 1}" else "Video",
            badge = v.format.takeIf { it.isNotBlank() && it != "---" },
            icon = Icons.Default.Movie,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            onContainerColor = MaterialTheme.colorScheme.onSecondaryContainer,
            properties = listOfNotNull(
              "Resolution" to "${v.width.filter { it.isDigit() }}×${v.height.filter { it.isDigit() }}",
              "Frame rate" to v.frameRate,
              "Bitrate"    to v.bitRate,
              "Profile"    to v.formatProfile,
            ).filter { (_, v2) -> v2.isNotBlank() && v2 != "---" },
          )
        }
      }

      // Audio stream cards
      mediaInfo.audioStreams.forEachIndexed { i, a ->
        item {
          StreamCard(
            title = "Audio #${i + 1}",
            badge = a.format.takeIf { it.isNotBlank() && it != "---" },
            icon = Icons.Default.MusicNote,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            onContainerColor = MaterialTheme.colorScheme.onTertiaryContainer,
            properties = listOfNotNull(
              "Channels" to a.channels,
              "Language" to a.language,
              "Bitrate"  to a.bitRate,
            ).filter { (_, v2) -> v2.isNotBlank() && v2 != "---" },
          )
        }
      }

      // Container card
      item {
        StreamCard(
          title = "Container",
          badge = mediaInfo.general.format.takeIf { it.isNotBlank() && it != "---" },
          icon = Icons.Default.Inventory2,
          containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          onContainerColor = MaterialTheme.colorScheme.onSurface,
          properties = listOfNotNull(
            "Duration"    to mediaInfo.general.duration,
            "File size"   to mediaInfo.general.fileSize,
            "Writing app" to mediaInfo.general.writingApplication,
          ).filter { (_, v2) -> v2.isNotBlank() && v2 != "---" },
        )
      }

      // ── Technical log section ─────────────────────────────────
      if (sections.isNotEmpty()) {
        item { SectionDividerHeader("Technical Log") }
        sections.forEach { section ->
          item {
            Text(
              text = section.name,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          items(section.properties) { (k, v) -> TechnicalPropertyRow(k, v) }
          item {
            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 16.dp),
              thickness = 0.5.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
          }
        }
      }

      item { Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
    }
  }

  // ── Hero chip row ─────────────────────────────────────────────────────────

  @Composable
  private fun HeroChipRow(chips: List<String>) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      chips.forEach { label ->
        SuggestionChip(
          onClick = {},
          label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        )
      }
    }
  }

  // ── Section divider header ────────────────────────────────────────────────

  @Composable
  private fun SectionDividerHeader(title: String) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      HorizontalDivider(
        modifier = Modifier.width(12.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
      )
      Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
        color = MaterialTheme.colorScheme.primary,
      )
      HorizontalDivider(
        modifier = Modifier.weight(1f),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
      )
    }
  }

  // ── Per-stream elevated card ──────────────────────────────────────────────

  @Composable
  private fun StreamCard(
    title: String,
    badge: String?,
    icon: ImageVector,
    containerColor: Color,
    onContainerColor: Color,
    properties: List<Pair<String, String>>,
  ) {
    if (properties.isEmpty()) return
    ElevatedCard(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
      colors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      ),
    ) {
      // Card header — tonal background strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(containerColor)
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          shape = MaterialTheme.shapes.small,
          color = onContainerColor.copy(alpha = 0.15f),
          modifier = Modifier.size(32.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = onContainerColor)
          }
        }
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = onContainerColor,
          modifier = Modifier.weight(1f),
        )
        if (badge != null) {
          Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = onContainerColor.copy(alpha = 0.15f),
          ) {
            Text(
              text = badge,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = onContainerColor,
            )
          }
        }
      }

      // 2-column stat tile grid
      val chunked = properties.chunked(2)
      chunked.forEach { pair ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          pair.forEach { (label, value) ->
            StatTile(
              label = label,
              value = value,
              modifier = Modifier.weight(1f),
            )
          }
          // Fill empty slot if odd number of properties
          if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
    }
  }

  // ── Stat tile (inside stream card) ───────────────────────────────────────

  @Composable
  private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
      modifier = modifier
        .clickable {
          val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          cm.setPrimaryClip(ClipData.newPlainText(label, value))
          Toast.makeText(context, "Copied $label", Toast.LENGTH_SHORT).show()
        },
      shape = MaterialTheme.shapes.small,
      tonalElevation = 2.dp,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
      Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = value,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }

  // ── Technical log property row ────────────────────────────────────────────

  @Composable
  private fun TechnicalPropertyRow(label: String, value: String) {
    if (value.isEmpty() || value == "---") return
    val context = LocalContext.current
    ListItem(
      headlineContent = {
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
      },
      overlineContent = {
        Text(
          label.uppercase(),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
          fontWeight = FontWeight.Bold,
        )
      },
      modifier = Modifier.clickable {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(context, "Copied $label", Toast.LENGTH_SHORT).show()
      },
      colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private fun parseMediaInfoText(text: String): List<InfoSection> {
    val sections = mutableListOf<InfoSection>()
    var currentName: String? = null
    val currentProps = mutableListOf<Pair<String, String>>()
    text.lines().forEach { line ->
      val trimmed = line.trim()
      when {
        trimmed.isEmpty() || trimmed.startsWith("=") || line.contains("MEDIA INFO") -> {}
        !line.startsWith(" ") && !line.contains(":") -> {
          if (currentName != null && currentProps.isNotEmpty()) {
            sections.add(InfoSection(currentName, currentProps.toList()))
          }
          currentName = trimmed
          currentProps.clear()
        }
        line.contains(":") -> {
          val parts = line.split(":", limit = 2)
          if (parts.size == 2 && parts[0].trim().isNotEmpty() && parts[1].trim().isNotEmpty()) {
            currentProps.add(parts[0].trim() to parts[1].trim())
          }
        }
      }
    }
    if (currentName != null && currentProps.isNotEmpty()) {
      sections.add(InfoSection(currentName, currentProps.toList()))
    }
    return sections
  }

  private data class InfoSection(val name: String, val properties: List<Pair<String, String>>)

  private suspend fun copyToClipboard(content: String, fileName: String) {
    withContext(Dispatchers.Main) {
      val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      cm.setPrimaryClip(ClipData.newPlainText("Media Info - $fileName", content))
      Toast.makeText(this@MediaInfoActivity, "Copied full report", Toast.LENGTH_SHORT).show()
    }
  }

  private suspend fun shareMediaInfo(content: String, fileName: String) {
    withContext(Dispatchers.IO) {
      try {
        val file = File(cacheDir, "mediainfo_${fileName.substringBeforeLast('.')}.txt")
        file.writeText(content)
        withContext(Dispatchers.Main) {
          val uri = FileProvider.getUriForFile(this@MediaInfoActivity, "${packageName}.provider", file)
          val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          startActivity(Intent.createChooser(intent, "Share Media Info"))
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          Toast.makeText(this@MediaInfoActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
}
