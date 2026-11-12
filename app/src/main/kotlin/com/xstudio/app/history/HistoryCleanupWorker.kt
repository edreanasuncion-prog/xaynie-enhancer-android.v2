package com.xstudio.app.history

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class HistoryCleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        HistoryFiles.purgeExpiredPhotos(applicationContext)
        Result.success()
    } catch (e: Exception) {
        // Database or storage problem: let WorkManager retry with backoff.
        Result.retry()
    }
}
