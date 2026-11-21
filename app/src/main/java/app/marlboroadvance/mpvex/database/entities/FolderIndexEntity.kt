package app.marlboroadvance.mpvex.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistent index entry for one folder that directly contains videos.
 *
 * All counters are DIRECT (immediate children only). Recursive totals for the tree view
 * are rolled up from these rows by FolderIndexRepository.
 */
@Entity(tableName = "folder_index")
data class FolderIndexEntity(
  @PrimaryKey
  val path: String, // Absolute folder path
  val name: String,
  val videoCount: Int, // Direct videos only
  val totalSize: Long, // Bytes, direct videos only
  val totalDuration: Long, // Milliseconds, direct videos only
  val lastModified: Long, // Seconds, newest direct video
  val hasSubfolders: Boolean,
  val signature: String, // Change signature of the direct videos ("" until videos are indexed)
  val videosIndexedAt: Long, // 0 = per-video rows not stored yet
  val lastIndexed: Long, // When this row was last written
)
