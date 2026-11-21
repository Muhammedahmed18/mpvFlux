package app.marlboroadvance.mpvex.utils.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Tree View Scanner - tree/browser view data.
 *
 * Serves folders with recursive counts from the persistent [FolderIndexRepository]. Opening a
 * directory no longer builds a library-wide map: the index is read (and seeded once if empty)
 * and totals are rolled up in memory.
 */
object TreeViewScanner {
    /**
     * Kept for source compatibility. There is no in-memory cache to clear any more;
     * use [FolderIndexRepository] to invalidate folders.
     */
    @Suppress("unused")
    fun clearCache() {
    }

    /**
     * Folder metadata with recursive counts
     */
    data class FolderData(
        val path: String,
        val name: String,
        val videoCount: Int,
        val totalSize: Long,
        val totalDuration: Long,
        val lastModified: Long,
        val hasSubfolders: Boolean = false
    )

    /**
     * Get direct child folders of a parent directory for tree view
     * Uses recursive counts with parent hierarchy
     */
    suspend fun getFoldersInDirectory(
        context: Context,
        parentPath: String
    ): List<FolderData> = withContext(Dispatchers.IO) {
        FolderIndexRepository.get().rolledUp().values
            .filter { folder -> File(folder.path).parent == parentPath }
            .map { it.toFolderData() }
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
    }

    /**
     * Get folder data with recursive counts for a specific path
     * Used for storage roots to show total video counts
     */
    suspend fun getFolderDataRecursive(
        context: Context,
        folderPath: String
    ): FolderData? = withContext(Dispatchers.IO) {
        val allFolders = FolderIndexRepository.get().rolledUp()

        // Ancestors are part of the rollup, so storage roots normally match exactly.
        allFolders[folderPath]?.let { return@withContext it.toFolderData() }

        // Fallback: aggregate the direct children (their counts are already recursive).
        var totalCount = 0
        var totalSize = 0L
        var totalDuration = 0L
        var lastModified = 0L
        var hasSubfolders = false

        val prefix = "$folderPath${File.separator}"
        for ((path, data) in allFolders) {
            if (!path.startsWith(prefix)) continue
            val relativePath = path.substring(prefix.length)
            if (relativePath.contains(File.separator)) continue

            totalCount += data.videoCount
            totalSize += data.totalSize
            totalDuration += data.totalDuration
            if (data.lastModified > lastModified) lastModified = data.lastModified
            hasSubfolders = true
        }

        if (totalCount > 0) {
            FolderData(
                path = folderPath,
                name = File(folderPath).name,
                videoCount = totalCount,
                totalSize = totalSize,
                totalDuration = totalDuration,
                lastModified = lastModified,
                hasSubfolders = hasSubfolders
            )
        } else {
            null
        }
    }

    private fun FolderIndexRepository.RolledFolder.toFolderData() =
        FolderData(
            path = path,
            name = name,
            videoCount = videoCount,
            totalSize = totalSize,
            totalDuration = totalDuration,
            lastModified = lastModified,
            hasSubfolders = hasSubfolders
        )
}
