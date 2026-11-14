package app.marlboroadvance.mpvex.utils.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log

/**
 * Access to the user-chosen "Extract audio" folder through the Storage Access Framework.
 *
 * The folder is picked with ACTION_OPEN_DOCUMENT_TREE and kept with a persisted read/write
 * permission, so no storage or READ_MEDIA_AUDIO permission is needed on any Android version.
 */
object AudioFolderStore {
  private const val TAG = "AudioFolderStore"
  private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

  private val AUDIO_EXTENSIONS = setOf("m4a", "mp3", "ogg", "opus", "aac", "flac", "wav", "mka")

  data class AudioFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val lastModified: Long,
  ) {
    val baseName: String get() = name.substringBeforeLast('.', name)
    val extension: String get() = name.substringAfterLast('.', "").lowercase()
  }

  /** Initial location for the folder picker: the shared Music folder. */
  val musicFolderUri: Uri
    get() = DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, "primary:Music")

  fun parse(value: String): Uri? = value.takeIf { it.isNotBlank() }?.let { runCatching { Uri.parse(it) }.getOrNull() }

  /** Keep access to [treeUri] across restarts and drop the permission of the previous folder. */
  fun persist(context: Context, treeUri: Uri, previous: Uri?) {
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    val resolver = context.contentResolver
    runCatching { resolver.takePersistableUriPermission(treeUri, flags) }
      .onFailure { Log.w(TAG, "takePersistableUriPermission failed", it) }
    if (previous != null && previous != treeUri) {
      runCatching { resolver.releasePersistableUriPermission(previous, flags) }
    }
  }

  /** True when the app still holds read/write access to the folder and it still exists. */
  fun hasAccess(context: Context, treeUri: Uri?): Boolean {
    if (treeUri == null) return false
    val granted = context.contentResolver.persistedUriPermissions.any {
      it.uri == treeUri && it.isReadPermission && it.isWritePermission
    }
    if (!granted) return false
    return runCatching {
      context.contentResolver.query(rootDocumentUri(treeUri), arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)
        ?.use { it.moveToFirst() } ?: false
    }.getOrDefault(false)
  }

  /** Readable folder name such as "Music/mpvFlux". */
  fun displayName(context: Context, treeUri: Uri): String {
    val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
    if (treeUri.authority == EXTERNAL_STORAGE_AUTHORITY && docId != null && docId.contains(':')) {
      val volume = docId.substringBefore(':')
      val path = docId.substringAfter(':').trimEnd('/')
      return when {
        volume == "primary" -> path.ifEmpty { "Internal storage" }
        path.isEmpty() -> volume
        else -> "$volume/$path"
      }
    }
    return runCatching {
      context.contentResolver.query(
        rootDocumentUri(treeUri),
        arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
        null, null, null,
      )?.use { if (it.moveToFirst()) it.getString(0) else null }
    }.getOrNull() ?: treeUri.lastPathSegment.orEmpty()
  }

  /** Audio files directly inside the folder, newest first. */
  fun list(context: Context, treeUri: Uri): List<AudioFile> {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
      treeUri,
      DocumentsContract.getTreeDocumentId(treeUri),
    )
    val projection = arrayOf(
      DocumentsContract.Document.COLUMN_DOCUMENT_ID,
      DocumentsContract.Document.COLUMN_DISPLAY_NAME,
      DocumentsContract.Document.COLUMN_MIME_TYPE,
      DocumentsContract.Document.COLUMN_SIZE,
      DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )
    val result = mutableListOf<AudioFile>()
    context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
      while (c.moveToNext()) {
        val id = c.getString(0) ?: continue
        val name = c.getString(1) ?: continue
        val mime = c.getString(2).orEmpty()
        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue
        val ext = name.substringAfterLast('.', "").lowercase()
        if (!mime.startsWith("audio/") && ext !in AUDIO_EXTENSIONS) continue
        result += AudioFile(
          uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id),
          name = name,
          size = if (c.isNull(3)) 0L else c.getLong(3),
          lastModified = if (c.isNull(4)) 0L else c.getLong(4),
        )
      }
    }
    return result.sortedByDescending { it.lastModified }
  }

  /**
   * Create an empty file named "[baseName].[extension]", adding " (1)", " (2)"… when the name
   * is taken. Returns the new document URI, or null when the provider refuses.
   */
  fun createFile(context: Context, treeUri: Uri, baseName: String, extension: String): Uri? {
    val existing = runCatching { list(context, treeUri).map { it.name.lowercase() }.toSet() }.getOrDefault(emptySet())
    var name = "$baseName.$extension"
    var n = 1
    while (name.lowercase() in existing) {
      name = "$baseName ($n).$extension"
      n++
    }
    // application/octet-stream keeps our extension as-is (a typed MIME can make the
    // provider append its own extension, e.g. "x.m4a.mp4").
    return runCatching {
      DocumentsContract.createDocument(context.contentResolver, rootDocumentUri(treeUri), "application/octet-stream", name)
    }.onFailure { Log.w(TAG, "createDocument failed", it) }.getOrNull()
  }

  fun delete(context: Context, documentUri: Uri): Boolean =
    runCatching { DocumentsContract.deleteDocument(context.contentResolver, documentUri) }.getOrDefault(false)

  fun rename(context: Context, documentUri: Uri, newName: String): Uri? =
    runCatching { DocumentsContract.renameDocument(context.contentResolver, documentUri, newName) }
      .onFailure { Log.w(TAG, "renameDocument failed", it) }
      .getOrNull()

  /** Strip characters that are not allowed in file names. */
  fun sanitize(name: String): String =
    name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().trimEnd('.').ifEmpty { "audio" }

  private fun rootDocumentUri(treeUri: Uri): Uri =
    DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
}
