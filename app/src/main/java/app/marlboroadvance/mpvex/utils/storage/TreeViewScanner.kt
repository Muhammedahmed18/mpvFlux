package app.marlboroadvance.mpvex.utils.storage

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import app.marlboroadvance.mpvex.domain.media.model.Video
import app.marlboroadvance.mpvex.utils.perf.PerfTrace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Tree View Scanner - Optimized for tree/browser view
 * 
 * Includes parent folders and recursive counts
 * Used for directory navigation with hierarchical structure
 */
object TreeViewScanner {
    private const val TAG = "TreeViewScanner"
    
    // Smart cache with short TTL (30 seconds)
    private var cachedTreeViewData: Map<String, FolderData>? = null
    private var cacheTimestamp: Long = 0
    private const val CACHE_TTL_MS = 300_000L // 5 minutes for fewer rescans
    
    /**
     * Clear cache (call when media library changes)
     */
    fun clearCache() {
        cachedTreeViewData = null
        cacheTimestamp = 0
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
     * Helper data class for folder stats during scanning
     */
    private data class FolderStats(
        var count: Int = 0,
        var size: Long = 0L,
        var duration: Long = 0L,
        var lastModified: Long = 0L
    )
    
    /**
     * Get direct child folders of a parent directory for tree view
     * Uses recursive counts with parent hierarchy
     */
    suspend fun getFoldersInDirectory(
        context: Context,
        parentPath: String
    ): List<FolderData> = withContext(Dispatchers.IO) {
        val allFolders = getOrBuildTreeViewData(context)
        
        // Filter for direct children only
        allFolders.values.filter { folder ->
            val parent = File(folder.path).parent
            parent == parentPath
        }.sortedBy { it.name.lowercase(Locale.getDefault()) }
    }
    
    /**
     * Get folder data with recursive counts for a specific path
     * Used for storage roots to show total video counts
     */
    suspend fun getFolderDataRecursive(
        context: Context,
        folderPath: String
    ): FolderData? = withContext(Dispatchers.IO) {
        val allFolders = getOrBuildTreeViewData(context)
        
        // First try exact match
        allFolders[folderPath]?.let { return@withContext it }
        
        // If no exact match, aggregate from ALL descendants (not just immediate children)
        // This is needed for storage roots that don't have direct videos
        var totalCount = 0
        var totalSize = 0L
        var totalDuration = 0L
        var lastModified = 0L
        var hasSubfolders = false
        
        // Find all descendants (any folder that starts with this path)
        for ((path, data) in allFolders) {
            if (path.startsWith("$folderPath${File.separator}")) {
                // Check if this is a direct child (immediate subdirectory)
                val relativePath = path.substring(folderPath.length + 1)
                val isDirectChild = !relativePath.contains(File.separator)
                
                if (isDirectChild) {
                    // Direct child - its counts are already recursive, so just add them
                    totalCount += data.videoCount
                    totalSize += data.totalSize
                    totalDuration += data.totalDuration
                    if (data.lastModified > lastModified) {
                        lastModified = data.lastModified
                    }
                    hasSubfolders = true
                }
            }
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
    
    /**
     * Get or build tree view data with smart caching
     */
    private suspend fun getOrBuildTreeViewData(
        context: Context
    ): Map<String, FolderData> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        
        // Return cached data if still valid
        cachedTreeViewData?.let { cached ->
            if (now - cacheTimestamp < CACHE_TTL_MS) {
                return@withContext cached
            }
        }
        
        // Build fresh data
        val data = buildTreeViewData(context)
        
        // Update cache
        cachedTreeViewData = data
        cacheTimestamp = now
        
        data
    }
    
    /**
     * Build tree view data (no caching)
     */
    private suspend fun buildTreeViewData(
        context: Context
    ): Map<String, FolderData> = withContext(Dispatchers.IO) {
        PerfTrace.section("TreeViewScanner.buildTreeViewData") {
            PerfTrace.time(TAG, "buildTreeViewData", thresholdMs = 50) {
                val allFolders = mutableMapOf<String, FolderData>()

                PerfTrace.time(TAG, "scanMediaStoreDirect", thresholdMs = 50) {
                    scanMediaStoreDirect(context, allFolders)
                }

                PerfTrace.time(TAG, "scanExternalVolumes", thresholdMs = 50) {
                    scanExternalVolumes(context, allFolders)
                }

                PerfTrace.time(TAG, "buildParentHierarchy", thresholdMs = 50) {
                    buildParentHierarchy(allFolders)
                }

                allFolders
            }
        }
    }
    
    /**
     * Scan MediaStore for all videos and build folder map (direct counts only)
     */
    private fun scanMediaStoreDirect(
        context: Context,
        folders: MutableMap<String, FolderData>
    ) {
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

                while (cursor.moveToNext()) {
                    val videoPath = cursor.getString(dataColumn)
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
                }
                
                for ((folderPath, stats) in statsByFolder) {
                    folders[folderPath] = FolderData(
                        path = folderPath,
                        name = File(folderPath).name,
                        videoCount = stats.count,
                        totalSize = stats.size,
                        totalDuration = stats.duration,
                        lastModified = stats.lastModified,
                        hasSubfolders = false
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore scan error", e)
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
    
    /**
     * Build parent folder hierarchy
     * Ensures intermediate folders (without direct videos) are included with correct recursive counts
     */
    private fun buildParentHierarchy(folders: MutableMap<String, FolderData>) {
        if (folders.isEmpty()) return

        val direct = HashMap(folders)
        val allPaths = mutableSetOf<String>()
        allPaths.addAll(direct.keys)
        val sep = File.separatorChar

        for (path in direct.keys) {
            var currentPath = File(path).parent
            while (currentPath != null) {
                if (currentPath == "/" || currentPath.length <= 1) break
                allPaths.add(currentPath)
                currentPath = File(currentPath).parent
            }
        }

        val childrenByParent = HashMap<String, MutableList<String>>()
        for (path in allPaths) {
            val parentSep = path.lastIndexOf(sep)
            if (parentSep > 0) {
                val parent = path.substring(0, parentSep)
                if (allPaths.contains(parent)) {
                    childrenByParent.getOrPut(parent) { mutableListOf() }.add(path)
                }
            }
        }

        val totalCount = HashMap<String, Int>(allPaths.size)
        val totalSize = HashMap<String, Long>(allPaths.size)
        val totalDuration = HashMap<String, Long>(allPaths.size)
        val lastModified = HashMap<String, Long>(allPaths.size)

        for (path in allPaths) {
            val data = direct[path]
            totalCount[path] = data?.videoCount ?: 0
            totalSize[path] = data?.totalSize ?: 0L
            totalDuration[path] = data?.totalDuration ?: 0L
            lastModified[path] = data?.lastModified ?: 0L
        }

        val sorted = allPaths.sortedByDescending { it.count { c -> c == sep } }
        for (path in sorted) {
            val parentSep = path.lastIndexOf(sep)
            if (parentSep <= 0) continue
            val parent = path.substring(0, parentSep)
            if (!allPaths.contains(parent)) continue

            totalCount[parent] = (totalCount[parent] ?: 0) + (totalCount[path] ?: 0)
            totalSize[parent] = (totalSize[parent] ?: 0L) + (totalSize[path] ?: 0L)
            totalDuration[parent] = (totalDuration[parent] ?: 0L) + (totalDuration[path] ?: 0L)

            val childLast = lastModified[path] ?: 0L
            val parentLast = lastModified[parent] ?: 0L
            if (childLast > parentLast) {
                lastModified[parent] = childLast
            }
        }

        folders.clear()
        for (path in allPaths) {
            val count = totalCount[path] ?: 0
            if (count <= 0) continue

            val hasSubfolders =
                childrenByParent[path]?.any { child ->
                    (totalCount[child] ?: 0) > 0
                } == true

            folders[path] = FolderData(
                path = path,
                name = File(path).name,
                videoCount = count,
                totalSize = totalSize[path] ?: 0L,
                totalDuration = totalDuration[path] ?: 0L,
                lastModified = lastModified[path] ?: 0L,
                hasSubfolders = hasSubfolders
            )
        }
    }
}
