package com.xstudio.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import com.xstudio.app.history.HistoryCleanupWorker
import java.util.concurrent.TimeUnit

class XStudioApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        scheduleThreeDayPhotoCleanup()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.15)
                    .build()
            }
            .build()
    }

    private fun scheduleThreeDayPhotoCleanup() {
        val cleanupRequest = PeriodicWorkRequestBuilder<HistoryCleanupWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "xstudio_3day_photo_history_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
        // Also sweep right now so photos past their 72 hours never linger until the next periodic run.
        WorkManager.getInstance(this).enqueueUniqueWork(
            "xstudio_3day_photo_history_cleanup_on_launch",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<HistoryCleanupWorker>().build()
        )
    }
}
