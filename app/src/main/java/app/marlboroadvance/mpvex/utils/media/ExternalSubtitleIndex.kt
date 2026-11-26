package app.marlboroadvance.mpvex.utils.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Finds external subtitle files that sit next to a video, for the browser card badge.
 *
 * Each folder is listed once and cached in memory, so cards never rescan on their own.
 * The cache is cleared on [MediaLibraryEvents.changes]; [version] changes at the same time
 * so visible cards look their subtitles up again.
 *
 * A subtitle matches a video when its name is the video's base name followed by the
 * extension, or by a dot-suffix and the extension (`Movie.srt`, `Movie.en.srt`,
 * `Movie.eng.forced.srt`). `Movie 2.srt` does not match `Movie.mkv`.
 */
object ExternalSubtitleIndex {
  // Display order of the formats shown on the card.
  private val formats = listOf("srt", "ass", "ssa", "vtt")

  private class SubtitleFile(val baseName: String, val extension: String)

  private val folders = ConcurrentHashMap<String, List<SubtitleFile>>()
  private val loadLock = Mutex()

  // Bumped on every cache clear, so a listing that started before it is never stored.
  @Volatile
  private var generation = 0

  private val _version = MutableStateFlow(0)

  /** Changes whenever the cache is cleared. Key a lookup on it to refresh after library changes. */
  val version: StateFlow<Int> = _version

  init {
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      MediaLibraryEvents.changes.collect {
        generation++
        folders.clear()
        _version.value++
      }
    }
  }

  /** Cache-only lookup with no disk access. Empty when the folder has not been listed yet. */
  fun peek(videoPath: String): List<String> {
    val folder = File(videoPath).parent ?: return emptyList()
    val files = folders[folder] ?: return emptyList()
    return match(files, videoPath)
  }

  /** Upper-case formats of the external subtitles for [videoPath], e.g. `["SRT", "VTT"]`. */
  suspend fun formatsFor(videoPath: String): List<String> {
    if (videoPath.contains("://")) return emptyList()
    val folder = File(videoPath).parent ?: return emptyList()
    return match(filesIn(folder), videoPath)
  }

  private suspend fun filesIn(folder: String): List<SubtitleFile> {
    folders[folder]?.let { return it }
    return withContext(Dispatchers.IO) {
      loadLock.withLock {
        folders[folder]?.let { return@withLock it }
        val startedAt = generation
        val files =
          File(folder).list()?.mapNotNull { name ->
            val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
            if (extension !in formats) {
              null
            } else {
              SubtitleFile(name.substringBeforeLast('.').lowercase(Locale.ROOT), extension)
            }
          } ?: emptyList()
        if (startedAt == generation) folders[folder] = files
        files
      }
    }
  }

  private fun match(files: List<SubtitleFile>, videoPath: String): List<String> {
    if (files.isEmpty()) return emptyList()
    val base = File(videoPath).name.substringBeforeLast('.').lowercase(Locale.ROOT)
    val found = files.filter { it.baseName == base || it.baseName.startsWith("$base.") }
    if (found.isEmpty()) return emptyList()
    val extensions = found.mapTo(HashSet()) { it.extension }
    return formats.filter { it in extensions }.map { it.uppercase(Locale.ROOT) }
  }
}
