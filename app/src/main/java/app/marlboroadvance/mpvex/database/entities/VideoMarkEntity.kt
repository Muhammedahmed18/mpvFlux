package app.marlboroadvance.mpvex.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A video the user marked as "New" from the "Mark as" sheet.
 *
 * Keyed by file name, the same key the saved playback progress uses. The mark only counts
 * while the video has no playback progress, so playing it clears the NEW chip by itself.
 */
@Entity(tableName = "video_marks")
data class VideoMarkEntity(
  @PrimaryKey val mediaTitle: String,
  val markedNewAt: Long,
)
