package app.marlboroadvance.mpvex.utils.storage

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import app.marlboroadvance.mpvex.domain.media.model.VideoFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent
import java.io.File
import java.util.Locale

/**
 * Folder View Scanner - library-wide scan used to SEED / RECONCILE the persistent folder index.
 *
 * Opening a folder never calls this; only [FolderIndexRepository.seedIfEmpty] and
 * [FolderIndexRepository.reconcileAll] do. Results are not cached here any more: the
 * persistent index in Room is the cache.
 */
object FolderViewScanner {
    private const val TAG = "FolderViewScanner"

    /**
     * Kept for source compatibility. There is no in-memory cache to clear any more;
     * use [FolderIndexRepository] to invalidate folders.
     */
    @Suppress("unused")
    fun clearCache() {
    }

    /**
     * Folder metadata (direct children only)
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
     * Helper data class for folder stats during scanning
     */
    private data class FolderStats(
        var count: Int = 0,
        var size: Long = 0L,
        var duration: Long = 0L,
        var lastModified: Long = 0L
    )

    /**
     * Get all video folders for folder list view. Served from the persistent index
     * (seeded on first use). Kept for source compatibility.
     */
    suspend fun getAllVideoFolders(context: Context): List<VideoFolder> =
        KoinJavaComponent.get<FolderIndexRepository>(FolderIndexRepository::class.java).getVideoFolders()

    /**
     * Library-wide scan: MediaStore plus a filesystem walk of external volumes.
     * Returns null when MediaStore could not be queried, so callers never treat a
     * failed scan as "the library is empty".
     */
    suspend fun scanAllFolders(context: Context): Map<String, FolderData>? = withContext(Dispatchers.IO) {
        val allFolders = mutableMapOf<String, FolderData>()
        val mediaStoreOk = scanMediaStoreImmediateChildren(context, allFolders)
        if (!mediaStoreOk) return@withContext null
        scanExternalVolumes(context, allFolders)
        allFolders
    }

    /**
     * Scan MediaStore for all videos and build folder map (immediate children only).
     * Returns false if the query failed.
     */
    private fun scanMediaStoreImmediateChildren(
        context: Context,
        folders: MutableMap<String, FolderData>
    ): Boolean {
        val projection = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.DATE_MODIFIED
        )
        
        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                
                val statsByFolder = mutableMapOf<String, FolderStats>()
                val childCountByFolder = mutableMapOf<String, Int>()
                val sep = File.separatorChar

                while (cursor.moveToNext()) {
                    val videoPath = cursor.getString(dataColumn) ?: continue
                    val file = File(videoPath)
                    
                    if (!file.exists()) continue
                    
                    val folderPath = file.parent ?: continue
                    val size = cursor.getLong(sizeColumn)
                    val duration = cursor.getLong(durationColumn)
                    val dateModified = cursor.getLong(dateColumn)

                    val stats = statsByFolder.getOrPut(folderPath) { FolderStats() }
                    stats.count += 1
                    stats.size += size
                    stats.duration += duration
                    if (dateModified > stats.lastModified) {
                        stats.lastModified = dateModified
                    }

                    val parentSep = folderPath.lastIndexOf(sep)
                    if (parentSep > 0) {
                        val parent = folderPath.substring(0, parentSep)
                        childCountByFolder[parent] = (childCountByFolder[parent] ?: 0) + 1
                    }
                }

                for ((folderPath, stats) in statsByFolder) {
                    val hasSubfolders = (childCountByFolder[folderPath] ?: 0) > 0

                    folders[folderPath] = FolderData(
                        path = folderPath,
                        name = File(folderPath).name,
                        videoCount = stats.count,
                        totalSize = stats.size,
                        totalDuration = stats.duration,
                        lastModified = stats.lastModified,
                        hasSubfolders = hasSubfolders
                    )
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore scan error", e)
            return false
        }
    }
    
    /**
     * Scan external volumes (USB OTG, SD cards) via filesystem
     */
    private fun scanExternalVolumes(
        context: Context,
        folders: MutableMap<String, FolderData>
    ) {
        try {
            val externalVolumes = StorageVolumeUtils.getExternalStorageVolumes(context)
            
            if (externalVolumes.isEmpty()) {
                return
            }
            
            for (volume in externalVolumes) {
                val volumePath = StorageVolumeUtils.getVolumePath(volume)
                if (volumePath == null) {
                    continue
                }
                
                val volumeDir = File(volumePath)
                if (!volumeDir.exists() || !volumeDir.canRead()) {
                    continue
                }
                
                scanDirectoryRecursive(volumeDir, folders, maxDepth = 20)
            }
        } catch (e: Exception) {
            Log.e(TAG, "External volume scan error", e)
        }
    }
    
    /**
     * Recursively scan directory for videos
     */
    private fun scanDirectoryRecursive(
        directory: File,
        folders: MutableMap<String, FolderData>,
        maxDepth: Int,
        currentDepth: Int = 0
    ) {
        if (currentDepth >= maxDepth) return
        if (!directory.exists() || !directory.canRead() || !directory.isDirectory) return
        
        try {
            val files = directory.listFiles() ?: return
            
            val videoFiles = mutableListOf<File>()
            val subdirectories = mutableListOf<File>()
            
            for (file in files) {
                try {
                    when {
                        file.isDirectory -> {
                            if (!FileFilterUtils.shouldSkipFolder(file)) {
                                subdirectories.add(file)
                            }
                        }
                        file.isFile -> {
                            val extension = file.extension.lowercase(Locale.getDefault())
                            if (FileTypeUtils.VIDEO_EXTENSIONS.contains(extension)) {
                                videoFiles.add(file)
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    continue
                }
            }
            
            // Add folder if it has videos
            if (videoFiles.isNotEmpty()) {
                val folderPath = directory.absolutePath
                
                // Skip if already from MediaStore
                if (!folders.containsKey(folderPath)) {
                    var totalSize = 0L
                    var lastModified = 0L
                    
                    for (video in videoFiles) {
                        totalSize += video.length()
                        val modified = video.lastModified()
                        if (modified > lastModified) {
                            lastModified = modified
                        }
                    }
                    
                    folders[folderPath] = FolderData(
                        path = folderPath,
                        name = directory.name,
                        videoCount = videoFiles.size,
                        totalSize = totalSize,
                        totalDuration = 0L, // Duration not available from filesystem
                        lastModified = lastModified / 1000,
                        hasSubfolders = subdirectories.isNotEmpty()
                    )
                }
            }
            
            // Recurse into subdirectories
            for (subdir in subdirectories) {
                scanDirectoryRecursive(subdir, folders, maxDepth, currentDepth + 1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning: ${directory.absolutePath}", e)
        }
    }
}
