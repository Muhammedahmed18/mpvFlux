package app.marlboroadvance.mpvex.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import app.marlboroadvance.mpvex.database.entities.FolderIndexEntity
import app.marlboroadvance.mpvex.database.entities.FolderVideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FolderIndexDao {
  @Query("SELECT * FROM folder_index")
  abstract suspend fun getAllFolders(): List<FolderIndexEntity>

  @Query("SELECT * FROM folder_index")
  abstract fun observeAllFolders(): Flow<List<FolderIndexEntity>>

  @Query("SELECT COUNT(*) FROM folder_index")
  abstract suspend fun folderCount(): Int

  @Query("SELECT * FROM folder_index WHERE path = :path LIMIT 1")
  abstract suspend fun getFolder(path: String): FolderIndexEntity?

  @Query("SELECT * FROM folder_video WHERE folderPath = :folderPath")
  abstract suspend fun getVideos(folderPath: String): List<FolderVideoEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract suspend fun upsertFolders(folders: List<FolderIndexEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract suspend fun upsertFolder(folder: FolderIndexEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract suspend fun insertVideos(videos: List<FolderVideoEntity>)

  @Query("DELETE FROM folder_video WHERE folderPath = :folderPath")
  abstract suspend fun deleteVideosInFolder(folderPath: String)

  @Query("DELETE FROM folder_video WHERE folderPath IN (:folderPaths)")
  abstract suspend fun deleteVideosInFolders(folderPaths: List<String>)

  @Query("DELETE FROM folder_index WHERE path IN (:paths)")
  abstract suspend fun deleteFolders(paths: List<String>)

  @Query("DELETE FROM folder_index")
  abstract suspend fun clearFolders()

  @Query("DELETE FROM folder_video")
  abstract suspend fun clearVideos()

  /** Replaces the folder row and all of its video rows atomically. */
  @Transaction
  open suspend fun replaceFolderWithVideos(
    folder: FolderIndexEntity,
    videos: List<FolderVideoEntity>,
  ) {
    deleteVideosInFolder(folder.path)
    if (videos.isNotEmpty()) videos.chunked(SQLITE_CHUNK).forEach { insertVideos(it) }
    upsertFolder(folder)
  }

  /** Applies a reconcile result (changed rows + removed folders) atomically. */
  @Transaction
  open suspend fun applyChanges(
    upserts: List<FolderIndexEntity>,
    removedPaths: List<String>,
  ) {
    removedPaths.chunked(SQLITE_CHUNK).forEach { chunk ->
      deleteVideosInFolders(chunk)
      deleteFolders(chunk)
    }
    upserts.chunked(SQLITE_CHUNK).forEach { upsertFolders(it) }
  }

  @Transaction
  open suspend fun clearAll() {
    clearVideos()
    clearFolders()
  }

  companion object {
    private const val SQLITE_CHUNK = 500
  }
}
