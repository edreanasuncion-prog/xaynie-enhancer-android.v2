package com.xstudio.app.history

import android.content.Context
import com.xstudio.app.data.local.XStudioDatabase
import java.io.File
import java.io.IOException

/**
 * Rules for photo-history expiry and for which files history cleanup is allowed to touch.
 * Everything that decides "is this expired?" or "may I delete this?" lives here so the
 * enhancer, the history screens and the background worker can never disagree.
 */
object HistoryFiles {

    /** Photo history lives exactly 72 hours. */
    const val PHOTO_TTL_MS: Long = 3L * 24L * 60L * 60L * 1000L

    fun photoExpiryFor(createdAtEpochMs: Long): Long = createdAtEpochMs + PHOTO_TTL_MS

    /** A record with no expiry (videos) never expires. */
    fun isExpired(expiresAtEpochMs: Long?, nowEpochMs: Long): Boolean =
        expiresAtEpochMs != null && expiresAtEpochMs <= nowEpochMs

    /** True only if [candidate] resolves (symlinks and ".." included) to a location strictly inside [root]. */
    fun isStrictlyInside(root: File, candidate: File): Boolean = try {
        val rootPath = root.canonicalFile.toPath()
        val candidatePath = candidate.canonicalFile.toPath()
        candidatePath != rootPath && candidatePath.startsWith(rootPath)
    } catch (e: IOException) {
        false
    }

    /** The only directories history cleanup may delete from: this app's own storage. */
    fun ownedRoots(context: Context): List<File> = listOfNotNull(
        context.getExternalFilesDir(null),
        context.filesDir,
        context.cacheDir
    )

    fun isOwnedFile(context: Context, file: File): Boolean =
        ownedRoots(context).any { isStrictlyInside(it, file) }

    /** Deletes [path] only when it is a regular file inside app-owned storage. Returns true if a file was removed. */
    fun deleteOwnedFile(context: Context, path: String): Boolean {
        if (path.isBlank()) return false
        val file = File(path)
        if (!file.isFile || !isOwnedFile(context, file)) return false
        return runCatching { file.delete() }.getOrDefault(false)
    }

    /**
     * Removes expired PHOTO history rows and their generated files.
     * Video rows are never selected by the DAO query, and files outside app storage are left alone
     * (the row is still removed so it stops showing up).
     * Returns the number of history rows removed.
     */
    suspend fun purgeExpiredPhotos(context: Context, nowEpochMs: Long = System.currentTimeMillis()): Int {
        val dao = XStudioDatabase.getInstance(context).historyDao()
        var removed = 0
        for (item in dao.getExpiredPhotoItems(nowEpochMs)) {
            deleteOwnedFile(context, item.enhancedFilePath)
            dao.deleteById(item.id)
            removed++
        }
        return removed
    }
}
