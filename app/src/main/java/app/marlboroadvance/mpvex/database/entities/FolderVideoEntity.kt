package app.marlboroadvance.mpvex.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One direct video of an indexed folder. Holds everything needed to rebuild a Video
 * without touching MediaStore or the filesystem.
 */
@Entity(
  tableName = "folder_video",
  indices = [Index(value = ["folderPath"])],
)
data class FolderVideoEntity(
  @PrimaryKey
  val path: String,
  val folderPath: String,
  val mediaId: Long,
  val uri: String,
  val displayName: String,
  val size: Long,
  val duration: Long,
  val dateModified: Long,
  val dateAdded: Long,
  val mimeType: String,
  val width: Int,
  val height: Int,
  val fps: Float,
  val hasEmbeddedSubtitles: Boolean,
  val subtitleCodec: String,
)
