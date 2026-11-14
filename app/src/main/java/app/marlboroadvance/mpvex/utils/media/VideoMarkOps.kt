package app.marlboroadvance.mpvex.utils.media

import android.util.Log
import androidx.room.withTransaction
import app.marlboroadvance.mpvex.database.MpvExDatabase
import app.marlboroadvance.mpvex.database.entities.PlaybackStateEntity
import app.marlboroadvance.mpvex.database.entities.RecentlyPlayedEntity
import app.marlboroadvance.mpvex.database.entities.VideoMarkEntity
import app.marlboroadvance.mpvex.domain.media.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

/** The four choices of the "Mark as" sheet (GitHub issue #3). */
enum class VideoMark { New, LastPlayed, Finished, Unwatched }

/**
 * Applies "Mark as" to a set of videos and can undo it.
 *
 * Saved progress is keyed by file name and Recently played by file path, the same keys the
 * player uses. Every change runs as one database transaction, so marking a whole folder is
 * instant, and the state before the change is returned so it can be restored.
 */
object VideoMarkOps {
  private const val TAG = "VideoMarkOps"
  private val database: MpvExDatabase by inject(MpvExDatabase::class.java)

  /** Everything "Mark as" can change for a set of videos, captured before the change. */
  class Snapshot internal constructor(
    internal val titles: List<String>,
    internal val paths: List<String>,
    internal val playbackStates: List<PlaybackStateEntity>,
    internal val recentlyPlayed: List<RecentlyPlayedEntity>,
    internal val marks: List<VideoMarkEntity>,
  )

  /** File names currently marked as New (only shown while the video has no progress). */
  suspend fun getMarkedNewTitles(): Set<String> =
    withContext(Dispatchers.IO) {
      runCatching { database.videoMarkDao().getMarkedTitles().toSet() }.getOrDefault(emptySet())
    }

  /** Applies [mark] to [videos]; returns the previous state for [undo], or null on failure. */
  suspend fun apply(videos: List<Video>, mark: VideoMark): Snapshot? =
    withContext(Dispatchers.IO) {
      if (videos.isEmpty()) return@withContext null
      val titles = videos.map { it.displayName }.distinct()
      val paths = videos.map { it.path }.filter { it.isNotBlank() }.distinct()
      val playbackDao = database.videoDataDao()
      val recentDao = database.recentlyPlayedDao()
      val markDao = database.videoMarkDao()

      try {
        val snapshot = Snapshot(
          titles = titles,
          paths = paths,
          playbackStates = titles.mapNotNull { playbackDao.getVideoDataByTitle(it) },
          recentlyPlayed = paths.mapNotNull { recentDao.getByFilePath(it) },
          marks = markDao.getByTitles(titles),
        )

        database.withTransaction {
            when (mark) {
              VideoMark.New -> {
                clearHistory(titles, paths)
                val now = System.currentTimeMillis()
                markDao.upsertAll(titles.map { VideoMarkEntity(mediaTitle = it, markedNewAt = now) })
              }

              VideoMark.LastPlayed -> {
                // Newest first: the first selected video ends up at the top.
                val now = System.currentTimeMillis()
                videos.distinctBy { it.path }.forEachIndexed { index, video ->
                  if (video.path.isBlank()) return@forEachIndexed
                  val existing = recentDao.getByFilePath(video.path)
                  recentDao.insert(
                    existing?.copy(timestamp = now - index) ?: RecentlyPlayedEntity(
                      filePath = video.path,
                      fileName = video.displayName,
                      duration = video.duration,
                      fileSize = video.size,
                      width = video.width,
                      height = video.height,
                      timestamp = now - index,
                    ),
                  )
                }
              }

              VideoMark.Finished -> {
                markDao.deleteByTitles(titles)
                val finished = titles.map { title ->
                  val existing = snapshot.playbackStates.firstOrNull { it.mediaTitle == title }
                  existing?.copy(lastPosition = 0, timeRemaining = 0, hasBeenWatched = true)
                    ?: PlaybackStateEntity(
                      mediaTitle = title,
                      lastPosition = 0,
                      playbackSpeed = 1.0,
                      sid = -1,
                      subDelay = 0,
                      subSpeed = 1.0,
                      aid = -1,
                      audioDelay = 0,
                      timeRemaining = 0,
                      hasBeenWatched = true,
                    )
                }
                playbackDao.upsertAll(finished)
              }

              VideoMark.Unwatched -> {
                clearHistory(titles, paths)
                markDao.deleteByTitles(titles)
              }
            }
        }
        PlaybackProgressEvents.notifyChanged()
        snapshot
      } catch (e: Exception) {
        Log.e(TAG, "Mark as $mark failed", e)
        null
      }
    }

  /** Puts back exactly what [snapshot] captured. */
  suspend fun undo(snapshot: Snapshot) =
    withContext(Dispatchers.IO) {
      val playbackDao = database.videoDataDao()
      val recentDao = database.recentlyPlayedDao()
      val markDao = database.videoMarkDao()
      try {
        database.withTransaction {
          clearHistory(snapshot.titles, snapshot.paths)
          markDao.deleteByTitles(snapshot.titles)
          playbackDao.upsertAll(snapshot.playbackStates)
          snapshot.recentlyPlayed.forEach { recentDao.insert(it) }
          if (snapshot.marks.isNotEmpty()) markDao.upsertAll(snapshot.marks)
        }
        PlaybackProgressEvents.notifyChanged()
      } catch (e: Exception) {
        Log.e(TAG, "Undo failed", e)
      }
    }

  private suspend fun clearHistory(titles: List<String>, paths: List<String>) {
    val playbackDao = database.videoDataDao()
    val recentDao = database.recentlyPlayedDao()
    titles.forEach { playbackDao.deleteByTitle(it) }
    paths.forEach { recentDao.deleteByFilePath(it) }
  }
}
