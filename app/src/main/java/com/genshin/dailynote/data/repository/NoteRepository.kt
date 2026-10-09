package com.genshin.dailynote.data.repository

import com.genshin.dailynote.data.api.ApiResult
import com.genshin.dailynote.data.api.GenshinApiService
import com.genshin.dailynote.data.local.ExtrapolationUtils
import com.genshin.dailynote.data.model.DailyNoteData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepository(
    private val apiService: GenshinApiService,
    private val preferencesRepository: UserPreferencesRepository
) {

    val userConfigFlow: Flow<UserConfig> = preferencesRepository.userConfigFlow

    suspend fun getCachedOrExtrapolatedNote(): ExtrapolationUtils.ExtrapolatedNote? {
        return preferencesRepository.getExtrapolatedNote()
    }

    suspend fun testConnection(
        uid: String,
        server: String,
        cookie: String
    ): ApiResult<DailyNoteData> {
        return apiService.fetchDailyNote(uid, server, cookie)
    }

    suspend fun syncDailyNote(): ApiResult<DailyNoteData> {
        val config = preferencesRepository.getUserConfig()
        if (config.uid.isBlank() || config.cookie.isBlank()) {
            val errorMsg = "Credentials not set. Please configure UID and Cookie."
            preferencesRepository.saveSyncError(errorMsg)
            return ApiResult.Error(-1, errorMsg)
        }

        val result = apiService.fetchDailyNote(
            uid = config.uid,
            server = config.server,
            cookie = config.cookie
        )

        when (result) {
            is ApiResult.Success -> {
                preferencesRepository.saveCachedNote(result.data)
                preferencesRepository.clearSyncError()
            }
            is ApiResult.Error -> {
                preferencesRepository.saveSyncError("[Error ${result.code}] ${result.message}")
            }
            is ApiResult.NetworkError -> {
                preferencesRepository.saveSyncError("Network error: ${result.exception.localizedMessage}")
            }
        }

        return result
    }

    suspend fun saveCredentials(uid: String, server: String, cookie: String) {
        preferencesRepository.saveCredentials(uid, server, cookie)
    }
}
