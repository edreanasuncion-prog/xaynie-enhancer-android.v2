package com.xstudio.app.core.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Single, shared way to copy an app-private file into the public gallery.
 *
 *  - Images go to Pictures/XStudio, videos go to Movies/XStudio.
 *  - The MIME type comes from the real file extension (PNG stays PNG, MP4 stays MP4).
 *  - Success is reported only after every byte has been copied and the entry published.
 *  - A null output stream, a short copy or any exception deletes the half-written MediaStore row.
 */
object MediaStoreExporter {

    enum class Kind { IMAGE, VIDEO }

    data class Spec(val kind: Kind, val mimeType: String, val directory: String)

    sealed interface Outcome {
        data class Saved(val uri: Uri, val bytes: Long) : Outcome
        data class Failed(val reason: String) : Outcome
    }

    const val APP_FOLDER = "XStudio"

    /** Pure mapping from file name to MediaStore spec. Unknown extensions are rejected, never guessed. */
    fun specFor(fileName: String): Spec? = when (fileName.substringAfterLast('.', "").lowercase()) {
        "png" -> Spec(Kind.IMAGE, "image/png", Environment.DIRECTORY_PICTURES)
        "jpg", "jpeg" -> Spec(Kind.IMAGE, "image/jpeg", Environment.DIRECTORY_PICTURES)
        "webp" -> Spec(Kind.IMAGE, "image/webp", Environment.DIRECTORY_PICTURES)
        "mp4" -> Spec(Kind.VIDEO, "video/mp4", Environment.DIRECTORY_MOVIES)
        else -> null
    }

    suspend fun export(context: Context, file: File): Outcome =
        withContext(Dispatchers.IO) { exportBlocking(context.applicationContext, file) }

    /** Boolean convenience for callers that only need success/failure. */
    suspend fun saveToGallery(context: Context, file: File): Boolean =
        export(context, file) is Outcome.Saved

    fun exportBlocking(context: Context, file: File): Outcome {
        if (!file.isFile || file.length() <= 0L) {
            return Outcome.Failed("The file to export is missing or empty.")
        }
        val spec = specFor(file.name)
            ?: return Outcome.Failed("Unsupported export type: .${file.extension}")

        val resolver = context.contentResolver
        val collection = when (spec.kind) {
            Kind.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            Kind.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, spec.mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, spec.directory + "/" + APP_FOLDER)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(spec.directory), APP_FOLDER)
                dir.mkdirs()
                @Suppress("DEPRECATION")
                put(MediaStore.MediaColumns.DATA, File(dir, file.name).absolutePath)
            }
        }

        var uri: Uri? = null
        try {
            uri = resolver.insert(collection, values)
                ?: throw IOException("MediaStore did not create an entry.")
            val out = resolver.openOutputStream(uri)
                ?: throw IOException("MediaStore returned no output stream.")
            val expected = file.length()
            val copied = out.use { sink ->
                file.inputStream().use { source -> source.copyTo(sink) }
            }
            if (copied != expected) {
                throw IOException("Incomplete copy: wrote $copied of $expected bytes.")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val publish = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                if (resolver.update(uri, publish, null, null) <= 0) {
                    throw IOException("Could not publish the exported file.")
                }
            }
            return Outcome.Saved(uri, copied)
        } catch (e: Exception) {
            uri?.let { runCatching { resolver.delete(it, null, null) } }
            return Outcome.Failed(e.localizedMessage ?: e.javaClass.simpleName)
        }
    }
}
