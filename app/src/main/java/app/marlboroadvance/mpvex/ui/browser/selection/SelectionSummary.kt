package app.marlboroadvance.mpvex.ui.browser.selection

import app.marlboroadvance.mpvex.domain.browser.FileSystemItem
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

/**
 * Builds the "3.2 GB · 4h 12m" line shown under the selection count in the browser top bar.
 *
 * Uses only values the library already has (video size/duration and the folder totals computed
 * during scanning); it never reads files. When some selected items have no known size or
 * duration, the known part is shown with a "≥" prefix so the total doesn't look exact.
 */
object SelectionSummary {
  /** One selected item: its size in bytes and duration in ms, or null when unknown. */
  class Entry(
    val sizeBytes: Long?,
    val durationMs: Long?,
  )

  /** An item with no size/duration information at all (e.g. a playlist). */
  val Unknown = Entry(null, null)

  fun entry(video: Video): Entry =
    Entry(
      sizeBytes = video.size.takeIf { it > 0 },
      durationMs = video.duration.takeIf { it > 0 },
    )

  fun entry(folder: VideoFolder): Entry = folderEntry(folder.videoCount, folder.totalSize, folder.totalDuration)

  fun entry(folder: FileSystemItem.Folder): Entry = folderEntry(folder.videoCount, folder.totalSize, folder.totalDuration)

  fun ofVideos(videos: List<Video>): String? = format(videos.map(::entry))

  /** Returns the summary text, or null when nothing useful is known. */
  fun format(entries: List<Entry>): String? {
    if (entries.isEmpty()) return null

    val sizes = entries.mapNotNull { it.sizeBytes }
    val durations = entries.mapNotNull { it.durationMs }

    val sizePart =
      sizes.takeIf { it.isNotEmpty() }?.let {
        prefix(incomplete = sizes.size < entries.size) + formatSize(it.sum())
      }
    val durationPart =
      durations.takeIf { list -> list.any { it > 0 } }?.let {
        prefix(incomplete = durations.size < entries.size) + formatDuration(it.sum())
      }

    return listOfNotNull(sizePart, durationPart).joinToString(" · ").ifBlank { null }
  }

  /** An empty folder genuinely totals zero; a non-empty one with zero totals wasn't measured. */
  private fun folderEntry(
    videoCount: Int,
    totalSize: Long,
    totalDuration: Long,
  ): Entry =
    if (videoCount == 0) {
      Entry(0L, 0L)
    } else {
      Entry(totalSize.takeIf { it > 0 }, totalDuration.takeIf { it > 0 })
    }

  private fun prefix(incomplete: Boolean) = if (incomplete) "≥ " else ""

  private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val group = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
    return String.format(Locale.getDefault(), "%.1f %s", bytes / 1024.0.pow(group.toDouble()), units[group])
  }

  private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
      hours > 0 -> "${hours}h ${minutes}m"
      minutes > 0 -> "${minutes}m ${seconds}s"
      else -> "${seconds}s"
    }
  }
}
