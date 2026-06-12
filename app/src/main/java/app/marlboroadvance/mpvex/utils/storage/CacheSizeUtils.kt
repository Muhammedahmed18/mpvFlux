package app.marlboroadvance.mpvex.utils.storage

import java.io.File
import java.util.Locale

/**
 * Small filesystem-size helpers used by the Storage settings screen.
 *
 * All functions touch the filesystem and must be called off the main thread
 * (the screen computes them in [kotlinx.coroutines.Dispatchers.IO]).
 */
object CacheSizeUtils {

  /** Recursive on-disk size of [dir] in bytes. Returns 0 if it doesn't exist or on error. */
  fun dirSizeBytes(dir: File): Long =
    runCatching {
      if (!dir.exists()) return 0L
      dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }.getOrDefault(0L)

  /** Size of a single [file] in bytes, or 0 if missing/error. */
  fun fileSizeBytes(file: File): Long =
    runCatching { if (file.isFile) file.length() else 0L }.getOrDefault(0L)

  /**
   * Human-readable byte count, e.g. "0.1 MB", "12.3 MB", "1.05 GB".
   * Always renders in fixed MB below 1 GB so small caches read consistently.
   */
  fun formatBytes(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
      String.format(Locale.US, "%.2f GB", mb / 1024.0)
    } else {
      String.format(Locale.US, "%.1f MB", mb)
    }
  }
}
