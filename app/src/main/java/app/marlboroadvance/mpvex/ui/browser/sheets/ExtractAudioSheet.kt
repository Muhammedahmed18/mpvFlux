package app.marlboroadvance.mpvex.ui.browser.sheets

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.preferences.BrowserPreferences
import app.marlboroadvance.mpvex.preferences.preference.collectAsState
import app.marlboroadvance.mpvex.utils.media.AudioExtractionManager
import app.marlboroadvance.mpvex.utils.media.AudioExtractionManager.State
import app.marlboroadvance.mpvex.utils.media.AudioExtractor
import app.marlboroadvance.mpvex.utils.media.MediaUtils
import app.marlboroadvance.mpvex.utils.storage.AudioFolderStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

/**
 * Opens the system folder picker (starting at Music) and stores the chosen folder as the
 * Extract audio output folder. Returns a function that launches the picker.
 */
@Composable
fun rememberAudioFolderPicker(onPicked: (Uri) -> Unit = {}): () -> Unit {
  val context = LocalContext.current
  val preferences = koinInject<BrowserPreferences>()
  val latestOnPicked by rememberUpdatedState(onPicked)
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
    if (uri != null) {
      val previous = AudioFolderStore.parse(preferences.audioOutputTreeUri.get())
      AudioFolderStore.persist(context, uri, previous)
      preferences.audioOutputTreeUri.set(uri.toString())
      latestOnPicked(uri)
    }
  }
  return remember(launcher) { { launcher.launch(AudioFolderStore.musicFolderUri) } }
}

/** State a browser screen keeps for Extract audio. */
@Stable
class ExtractAudioController internal constructor(
  private val managerState: androidx.compose.runtime.State<State>,
) {
  /** Video whose sheet is open, or null. */
  var video by mutableStateOf<Video?>(null)

  /** True while an extraction runs anywhere in the app (the button is disabled then). */
  val isRunning: Boolean get() = managerState.value is State.Running

  /** Opens the sheet for [video] when it is a local file; returns true when opened. */
  fun open(video: Video?): Boolean {
    if (video == null || !video.canExtractAudio()) return false
    this.video = video
    return true
  }
}

@Composable
fun rememberExtractAudioController(): ExtractAudioController {
  val state = AudioExtractionManager.state.collectAsStateWithLifecycle()
  return remember(state) { ExtractAudioController(state) }
}

/** Only local files (MediaStore/file paths) can be extracted, not network streams. */
fun Video.canExtractAudio(): Boolean = uri.scheme == null || uri.scheme == "content" || uri.scheme == "file"

@Composable
fun ExtractAudioHost(controller: ExtractAudioController) {
  ExtractAudioHost(video = controller.video, onDismiss = { controller.video = null })
}

/**
 * Shows the Extract audio sheet for [video], and the progress/result of a running extraction
 * (also after rotation, when [video] is gone). Place once per screen.
 */
@Composable
fun ExtractAudioHost(
  video: Video?,
  onDismiss: () -> Unit,
) {
  val state by AudioExtractionManager.state.collectAsStateWithLifecycle()
  if (video == null && state is State.Idle) return
  ExtractAudioSheet(
    video = video,
    state = state,
    onDismiss = {
      AudioExtractionManager.reset()
      onDismiss()
    },
  )
}

private sealed interface ProbeResult {
  data object Loading : ProbeResult
  data class Ready(val tracks: List<AudioExtractor.AudioTrack>) : ProbeResult
  data object Failed : ProbeResult
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtractAudioSheet(
  video: Video?,
  state: State,
  onDismiss: () -> Unit,
) {
  val running = state is State.Running
  val latestRunning by rememberUpdatedState(running)
  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true,
    // While extracting, the sheet can only be closed with Cancel.
    confirmValueChange = { it != SheetValue.Hidden || !latestRunning },
  )

  ModalBottomSheet(
    onDismissRequest = { if (!running) onDismiss() },
    sheetState = sheetState,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
    ) {
      when (state) {
        is State.Running -> RunningContent(state)
        is State.Done -> DoneContent(state, onDismiss)
        is State.Error -> ErrorContent(state, onDismiss)
        State.Idle -> if (video != null) PickContent(video, onDismiss)
      }
    }
  }
}

@Composable
private fun PickContent(
  video: Video,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val preferences = koinInject<BrowserPreferences>()
  val treeValue by preferences.audioOutputTreeUri.collectAsState()
  val treeUri = remember(treeValue) { AudioFolderStore.parse(treeValue) }

  // Folder name + access check (off the main thread; re-run when the folder changes).
  var folderName by remember { mutableStateOf<String?>(null) }
  var folderOk by remember { mutableStateOf(false) }
  LaunchedEffect(treeUri) {
    val (ok, name) = withContext(Dispatchers.IO) {
      val ok = AudioFolderStore.hasAccess(context, treeUri)
      ok to if (ok && treeUri != null) AudioFolderStore.displayName(context, treeUri) else null
    }
    folderOk = ok
    folderName = name
  }
  val pickFolder = rememberAudioFolderPicker()

  var probe by remember(video.uri) { mutableStateOf<ProbeResult>(ProbeResult.Loading) }
  var selected by remember(video.uri) { mutableStateOf<AudioExtractor.AudioTrack?>(null) }
  LaunchedEffect(video.uri) {
    probe = withContext(Dispatchers.IO) {
      runCatching { ProbeResult.Ready(AudioExtractor.probe(context, video.uri)) }.getOrDefault(ProbeResult.Failed)
    }
    selected = (probe as? ProbeResult.Ready)?.tracks?.firstOrNull { it.isSupported }
  }

  val baseName = remember(video.displayName) { video.displayName.substringBeforeLast('.', video.displayName) }
  val tracks = (probe as? ProbeResult.Ready)?.tracks.orEmpty()
  val outputBaseName = selected?.let { outputBaseName(baseName, it, tracks.size) }

  Text(
    text = stringResource(R.string.extract_audio),
    style = MaterialTheme.typography.headlineSmall,
  )
  Text(
    text = video.displayName,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
    modifier = Modifier.padding(top = 4.dp),
  )

  // Save to
  SectionLabel(stringResource(R.string.extract_audio_save_to))
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
    Icon(
      Icons.Rounded.Folder,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
      Text(
        text = folderName ?: stringResource(R.string.extract_audio_no_folder),
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (!folderOk && treeUri != null) {
        Text(
          text = stringResource(R.string.extract_audio_folder_access_lost),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }
    }
    TextButton(onClick = pickFolder) {
      Text(stringResource(if (folderOk) R.string.change else R.string.extract_audio_choose_folder))
    }
  }

  // Audio track
  when (val p = probe) {
    ProbeResult.Loading -> Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(top = 20.dp),
    ) {
      CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
      Spacer(Modifier.width(12.dp))
      Text(stringResource(R.string.extract_audio_reading_tracks), style = MaterialTheme.typography.bodyMedium)
    }

    ProbeResult.Failed -> ErrorLine(stringResource(R.string.extract_audio_read_failed))

    is ProbeResult.Ready -> when {
      p.tracks.isEmpty() -> ErrorLine(stringResource(R.string.extract_audio_no_tracks))
      p.tracks.size == 1 -> {
        val track = p.tracks.first()
        SectionLabel(stringResource(R.string.extract_audio_track))
        TrackText(track, context)
      }
      else -> {
        SectionLabel(stringResource(R.string.extract_audio_track))
        Column(Modifier.selectableGroup()) {
          p.tracks.forEach { track ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .selectable(
                  selected = selected == track,
                  enabled = track.isSupported,
                  role = Role.RadioButton,
                  onClick = { selected = track },
                )
                .padding(vertical = 4.dp),
            ) {
              RadioButton(selected = selected == track, onClick = null, enabled = track.isSupported)
              Spacer(Modifier.width(12.dp))
              TrackText(track, context, Modifier.weight(1f))
            }
          }
        }
      }
    }
  }

  // Output file
  if (selected != null && outputBaseName != null) {
    SectionLabel(stringResource(R.string.extract_audio_output))
    Text(
      text = "${AudioFolderStore.sanitize(outputBaseName)}.${selected!!.output!!.extension}",
      style = MaterialTheme.typography.bodyLarge,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
  }

  Spacer(Modifier.height(24.dp))
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
  ) {
    TextButton(onClick = onDismiss) { Text(stringResource(R.string.generic_cancel)) }
    Button(
      enabled = folderOk && treeUri != null && selected?.isSupported == true,
      onClick = {
        val track = selected ?: return@Button
        val tree = treeUri ?: return@Button
        AudioExtractionManager.start(
          context = context,
          videoUri = video.uri,
          videoName = video.displayName,
          track = track,
          outputBaseName = outputBaseName ?: baseName,
          treeUri = tree,
          onSuccess = { preferences.showAudioTab.set(true) },
        )
      },
    ) {
      Text(stringResource(R.string.extract_audio_action))
    }
  }
}

@Composable
private fun RunningContent(state: State.Running) {
  Text(stringResource(R.string.extract_audio_extracting), style = MaterialTheme.typography.headlineSmall)
  Text(
    text = state.videoName,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
  )
  if (state.progress >= 0f) {
    LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
    Text(
      text = "${(state.progress * 100).toInt()}%",
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(top = 8.dp),
    )
  } else {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
  }
  Spacer(Modifier.height(16.dp))
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
    TextButton(onClick = { AudioExtractionManager.cancel() }) { Text(stringResource(R.string.generic_cancel)) }
  }
}

@Composable
private fun DoneContent(state: State.Done, onDismiss: () -> Unit) {
  val context = LocalContext.current
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.width(12.dp))
    Text(stringResource(R.string.extract_audio_done), style = MaterialTheme.typography.headlineSmall)
  }
  Text(
    text = stringResource(R.string.extract_audio_saved_to, state.folderName),
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 12.dp),
  )
  Text(
    text = state.fileName,
    style = MaterialTheme.typography.bodyLarge,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
    modifier = Modifier.padding(top = 4.dp),
  )
  Spacer(Modifier.height(24.dp))
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
  ) {
    TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
    Button(onClick = {
      MediaUtils.playFile(state.fileUri, context, "extract_audio")
      onDismiss()
    }) { Text(stringResource(R.string.generic_open)) }
  }
}

@Composable
private fun ErrorContent(state: State.Error, onDismiss: () -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
    Spacer(Modifier.width(12.dp))
    Text(stringResource(R.string.extract_audio), style = MaterialTheme.typography.headlineSmall)
  }
  Text(
    text = state.message,
    style = MaterialTheme.typography.bodyMedium,
    modifier = Modifier.padding(top = 12.dp),
  )
  Spacer(Modifier.height(24.dp))
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
    TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
  }
}

@Composable
private fun SectionLabel(text: String) {
  HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))
  Text(
    text = text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(bottom = 8.dp),
  )
}

@Composable
private fun ErrorLine(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.error,
    modifier = Modifier.padding(top = 20.dp),
  )
}

@Composable
private fun TrackText(track: AudioExtractor.AudioTrack, context: Context, modifier: Modifier = Modifier) {
  Column(modifier) {
    Text(
      text = trackTitle(track, context),
      style = MaterialTheme.typography.bodyLarge,
      color = if (track.isSupported) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    track.unsupportedReason?.let { reason ->
      Text(
        text = when (reason) {
          AudioExtractor.UnsupportedReason.NEEDS_ANDROID_10 -> stringResource(R.string.extract_audio_needs_android_10, track.codecLabel)
          AudioExtractor.UnsupportedReason.FORMAT -> stringResource(R.string.extract_audio_format_unsupported, track.codecLabel)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
      )
    }
  }
}

private fun trackTitle(track: AudioExtractor.AudioTrack, context: Context): String =
  listOfNotNull(
    context.getString(R.string.extract_audio_track_number, track.number),
    track.languageLabel,
    track.codecLabel,
    track.channelsLabel.takeIf { it.isNotEmpty() },
  ).joinToString(" · ")

/** "Video" for a single audio track, "Video (Japanese)" or "Video (track 2)" otherwise. */
private fun outputBaseName(baseName: String, track: AudioExtractor.AudioTrack, trackCount: Int): String {
  if (trackCount <= 1) return baseName
  val suffix = track.languageLabel ?: "track ${track.number}"
  return "$baseName ($suffix)"
}
