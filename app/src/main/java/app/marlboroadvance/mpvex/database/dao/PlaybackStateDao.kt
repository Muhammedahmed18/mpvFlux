package app.marlboroadvance.mpvex.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.marlboroadvance.mpvex.database.entities.PlaybackStateEntity
import kotlinx.coroutines.flow.Flow

/** The few playback-state columns the watched/progress/NEW badges need. */
data class PlaybackBadgeRow(
  val mediaTitle: String,
  val timeRemaining: Int,
  val hasBeenWatched: Boolean,
)

@Dao
interface PlaybackStateDao {
  /** All saved progress in one query; re-emits whenever the table changes. */
  @Query("SELECT mediaTitle, timeRemaining, hasBeenWatched FROM PlaybackStateEntity")
  fun observePlaybackBadgeRows(): Flow<List<PlaybackBadgeRow>>

  @Upsert
  suspend fun upsert(playbackStateEntity: PlaybackStateEntity)

  @Query("SELECT * FROM PlaybackStateEntity WHERE mediaTitle = :mediaTitle LIMIT 1")
  suspend fun getVideoDataByTitle(mediaTitle: String): PlaybackStateEntity?

  @Query("DELETE FROM PlaybackStateEntity")
  suspend fun clearAllPlaybackStates()

  @Query("DELETE FROM PlaybackStateEntity WHERE mediaTitle = :mediaTitle")
  suspend fun deleteByTitle(mediaTitle: String)

  @Query(
    """
    UPDATE PlaybackStateEntity 
    SET mediaTitle = :newTitle 
    WHERE mediaTitle = :oldTitle
  """,
  )
  suspend fun updateMediaTitle(
    oldTitle: String,
    newTitle: String,
  )

  @Query("SELECT * FROM PlaybackStateEntity")
  suspend fun getAllPlaybackStates(): List<PlaybackStateEntity>

  @Upsert
  suspend fun upsertAll(playbackStates: List<PlaybackStateEntity>)
}
