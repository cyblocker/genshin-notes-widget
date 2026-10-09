package com.genshin.dailynote.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.genshin.dailynote.GenshinApp
import com.genshin.dailynote.data.api.ApiResult
import com.genshin.dailynote.widget.GenshinGlanceWidget

class NoteSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? GenshinApp ?: GenshinApp.instance
        val repository = app.noteRepository

        return try {
            val syncResult = repository.syncDailyNote()

            // Update all Glance widgets regardless of success/error so user sees status/extrapolated data
            GenshinGlanceWidget().updateAll(applicationContext)

            when (syncResult) {
                is ApiResult.Success -> Result.success()
                is ApiResult.Error -> {
                    // Non-retryable error (e.g. invalid cookie, bad UID)
                    Result.failure()
                }
                is ApiResult.NetworkError -> {
                    // Retryable transient network failure
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            GenshinGlanceWidget().updateAll(applicationContext)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "genshin_notes_periodic_sync"
        const val WORK_NAME_IMMEDIATE = "genshin_notes_immediate_sync"

        fun enqueueImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<NoteSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
