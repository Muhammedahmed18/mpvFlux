package app.marlboroadvance.mpvex.utils.media

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Implementation of the OpenSubtitles (OSDb) hashing algorithm.
 * Optimized with FileChannel for instant random access to large files.
 */
object SubtitleHashUtils {
    private const val TAG = "SubtitleHashUtils"
    private const val HASH_CHUNK_SIZE = 65536 // 64 KB

    fun computeHash(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val fileSize = pfd.statSize
                if (fileSize < HASH_CHUNK_SIZE) return null

                FileInputStream(pfd.fileDescriptor).use { fis ->
                    val channel = fis.channel
                    
                    // Read first 64KB
                    val headBuffer = ByteBuffer.allocate(HASH_CHUNK_SIZE).order(ByteOrder.LITTLE_ENDIAN)
                    channel.read(headBuffer, 0)
                    headBuffer.flip()
                    val headHash = computeByteBufferHash(headBuffer)

                    // Read last 64KB (instantly jump to end)
                    val tailBuffer = ByteBuffer.allocate(HASH_CHUNK_SIZE).order(ByteOrder.LITTLE_ENDIAN)
                    channel.read(tailBuffer, maxOf(0, fileSize - HASH_CHUNK_SIZE))
                    tailBuffer.flip()
                    val tailHash = computeByteBufferHash(tailBuffer)

                    val finalHash = fileSize + headHash + tailHash
                    String.format("%016x", finalHash)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error computing hash for $uri", e)
            null
        }
    }

    private fun computeByteBufferHash(buffer: ByteBuffer): Long {
        var hash: Long = 0
        while (buffer.hasRemaining()) {
            hash += buffer.long
        }
        return hash
    }
}
