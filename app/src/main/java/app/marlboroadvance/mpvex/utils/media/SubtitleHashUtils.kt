package app.marlboroadvance.mpvex.utils.media

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Implementation of the OpenSubtitles (OSDb) hashing algorithm.
 * Used to uniquely identify video files for subtitle matching.
 *
 * The hash is calculated as: file_size + 64-bit checksum of the first 64KB + 64-bit checksum of the last 64KB.
 */
object SubtitleHashUtils {
    private const val TAG = "SubtitleHashUtils"
    private const val HASH_CHUNK_SIZE = 65536 // 64 KB

    fun computeHash(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileSize = getFileSize(context, uri)
                if (fileSize < HASH_CHUNK_SIZE) return null

                var headHash: Long = 0
                var tailHash: Long = 0

                // Read first 64KB
                val headBuffer = ByteArray(HASH_CHUNK_SIZE)
                var bytesRead = inputStream.read(headBuffer)
                if (bytesRead < HASH_CHUNK_SIZE) return null
                headHash = computeChunkHash(headBuffer)

                // Skip to last 64KB
                // We need a new stream or seek if possible, but InputStream doesn't support seek well.
                // For content URIs, we reopen or use a seekable source if available.
                context.contentResolver.openInputStream(uri)?.use { tailStream ->
                    tailStream.skip(maxOf(0, fileSize - HASH_CHUNK_SIZE))
                    val tailBuffer = ByteArray(HASH_CHUNK_SIZE)
                    bytesRead = tailStream.read(tailBuffer)
                    if (bytesRead < HASH_CHUNK_SIZE) return null
                    tailHash = computeChunkHash(tailBuffer)
                }

                val finalHash = fileSize + headHash + tailHash
                String.format("%016x", finalHash)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error computing hash for $uri", e)
            null
        }
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun computeChunkHash(buffer: ByteArray): Long {
        val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
        var hash: Long = 0
        while (byteBuffer.hasRemaining()) {
            hash += byteBuffer.long
        }
        return hash
    }
}
