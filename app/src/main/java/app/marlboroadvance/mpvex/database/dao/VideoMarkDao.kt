package app.marlboroadvance.mpvex.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import app.marlboroadvance.mpvex.database.entities.VideoMarkEntity

@Dao
interface VideoMarkDao {
  @Upsert
  suspend fun upsertAll(marks: List<VideoMarkEntity>)

  @Query("SELECT mediaTitle FROM video_marks")
  suspend fun getMarkedTitles(): List<String>

  @Query("SELECT * FROM video_marks WHERE mediaTitle IN (:titles)")
  suspend fun getByTitles(titles: List<String>): List<VideoMarkEntity>

  @Query("DELETE FROM video_marks WHERE mediaTitle IN (:titles)")
  suspend fun deleteByTitles(titles: List<String>)
}
