package com.genshin.dailynote

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.genshin.dailynote.data.api.GenshinApiService
import com.genshin.dailynote.data.repository.NoteRepository
import com.genshin.dailynote.data.repository.UserPreferencesRepository
import com.genshin.dailynote.worker.NoteSyncWorker
import java.util.concurrent.TimeUnit

class GenshinApp : Application() {

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var apiService: GenshinApiService
        private set

    lateinit var noteRepository: NoteRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferencesRepository = UserPreferencesRepository(this)
        apiService = GenshinApiService()
        noteRepository = NoteRepository(apiService, preferencesRepository)

        schedulePeriodicSync()
    }

    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<NoteSyncWorker>(
            30, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            NoteSyncWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    companion object {
        lateinit var instance: GenshinApp
            private set
    }
}
