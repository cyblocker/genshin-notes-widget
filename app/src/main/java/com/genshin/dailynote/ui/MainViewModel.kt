package com.genshin.dailynote.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.genshin.dailynote.GenshinApp
import com.genshin.dailynote.data.api.ApiResult
import com.genshin.dailynote.data.api.GenshinApiService
import com.genshin.dailynote.data.local.ExtrapolationUtils
import com.genshin.dailynote.data.model.DailyNoteData
import com.genshin.dailynote.worker.NoteSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import android.net.Uri
import androidx.glance.appwidget.updateAll
import com.genshin.dailynote.data.repository.WidgetAppearance
import com.genshin.dailynote.util.ImageUtils
import com.genshin.dailynote.widget.GenshinGlanceWidget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    data class StringResource(val resId: Int, val args: List<Any> = emptyList()) : UiText()

    fun asString(context: Context): String = when (this) {
        is DynamicString -> value
        is StringResource -> context.getString(resId, *args.toTypedArray())
    }
}

data class MainUiState(
    val uid: String = "",
    val server: String = "os_asia",
    val cookie: String = "",
    val isTesting: Boolean = false,
    val testResult: ApiResult<DailyNoteData>? = null,
    val isSavingAndSyncing: Boolean = false,
    val statusMessage: UiText? = null,
    val lastSyncTimestamp: Long = 0L,
    val lastSyncError: String? = null,
    val extrapolatedNote: ExtrapolationUtils.ExtrapolatedNote? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GenshinApp
    private val repository = app.noteRepository
    private val preferences = app.preferencesRepository

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _appearance = MutableStateFlow(WidgetAppearance())
    val widgetAppearance: StateFlow<WidgetAppearance> = _appearance.asStateFlow()

    private val _hasCustomBgImage = MutableStateFlow(ImageUtils.hasBackgroundImage(application))
    val hasCustomBgImage: StateFlow<Boolean> = _hasCustomBgImage.asStateFlow()

    init {
        viewModelScope.launch {
            _appearance.value = preferences.getWidgetAppearance()
        }
        loadConfigAndCache()
    }

    private fun loadConfigAndCache() {
        viewModelScope.launch {
            preferences.userConfigFlow.collect { config ->
                val note = preferences.getExtrapolatedNote()
                _uiState.update { current ->
                    current.copy(
                        uid = if (current.uid.isEmpty()) config.uid else current.uid,
                        server = if (current.uid.isEmpty()) config.server else current.server,
                        cookie = if (current.cookie.isEmpty()) config.cookie else current.cookie,
                        lastSyncTimestamp = config.lastSyncTimestamp,
                        lastSyncError = config.lastSyncError,
                        extrapolatedNote = note
                    )
                }
            }
        }
    }

    fun onUidChanged(newUid: String) {
        val trimmed = newUid.trim()
        val guessedServer = GenshinApiService.guessServerByUid(trimmed)
        _uiState.update {
            it.copy(
                uid = trimmed,
                // Automatically adapt server if user hasn't explicitly set another one
                server = if (it.server == "os_asia" && trimmed.isNotEmpty()) guessedServer else it.server,
                statusMessage = null,
                testResult = null
            )
        }
    }

    fun onServerChanged(newServer: String) {
        _uiState.update { it.copy(server = newServer, statusMessage = null, testResult = null) }
    }

    fun onCookieChanged(newCookie: String) {
        _uiState.update { it.copy(cookie = newCookie.trim(), statusMessage = null, testResult = null) }
    }

    fun testConnection() {
        val current = _uiState.value
        if (current.uid.isBlank() || current.cookie.isBlank()) {
            _uiState.update { it.copy(statusMessage = UiText.StringResource(com.genshin.dailynote.R.string.msg_fill_required)) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTesting = true,
                    statusMessage = UiText.StringResource(com.genshin.dailynote.R.string.msg_connecting),
                    testResult = null
                )
            }
            val result = repository.testConnection(
                uid = current.uid,
                server = current.server,
                cookie = current.cookie
            )
            _uiState.update {
                it.copy(
                    isTesting = false,
                    testResult = result,
                    statusMessage = when (result) {
                        is ApiResult.Success -> UiText.StringResource(com.genshin.dailynote.R.string.msg_success_verified)
                        is ApiResult.Error -> UiText.StringResource(
                            com.genshin.dailynote.R.string.msg_api_error,
                            listOf(result.code, result.message)
                        )
                        is ApiResult.NetworkError -> UiText.StringResource(
                            com.genshin.dailynote.R.string.msg_network_error,
                            listOf(result.exception.localizedMessage ?: "")
                        )
                    }
                )
            }
        }
    }

    fun saveAndSync() {
        val current = _uiState.value
        if (current.uid.isBlank() || current.cookie.isBlank()) {
            _uiState.update { it.copy(statusMessage = UiText.StringResource(com.genshin.dailynote.R.string.msg_fill_required_save)) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingAndSyncing = true,
                    statusMessage = UiText.StringResource(com.genshin.dailynote.R.string.msg_saving_syncing)
                )
            }
            repository.saveCredentials(current.uid, current.server, current.cookie)

            // Trigger immediate sync
            NoteSyncWorker.enqueueImmediateSync(getApplication())

            // Also reload extrapolated note
            val note = preferences.getExtrapolatedNote()
            // Ensure current appearance is also persisted and widget updated
            preferences.saveWidgetAppearance(_appearance.value)
            GenshinGlanceWidget().updateAll(getApplication())

            _uiState.update {
                it.copy(
                    isSavingAndSyncing = false,
                    statusMessage = UiText.StringResource(com.genshin.dailynote.R.string.msg_save_success),
                    extrapolatedNote = note
                )
            }
        }
    }

    fun updateAppearance(newAppearance: WidgetAppearance) {
        _appearance.value = newAppearance
    }

    fun applyAppearance(context: Context) {
        viewModelScope.launch {
            val current = _appearance.value
            preferences.saveWidgetAppearance(current)
            GenshinGlanceWidget().updateAll(getApplication())
            android.widget.Toast.makeText(context, com.genshin.dailynote.R.string.toast_appearance_applied, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun saveCustomBgImage(uri: Uri, context: Context) {
        viewModelScope.launch {
            val success = ImageUtils.saveWidgetBackgroundImage(getApplication(), uri)
            if (success) {
                _hasCustomBgImage.value = true
                val updated = _appearance.value.copy(bgType = "IMAGE")
                _appearance.value = updated
                preferences.saveWidgetAppearance(updated)
                GenshinGlanceWidget().updateAll(getApplication())
                android.widget.Toast.makeText(context, com.genshin.dailynote.R.string.toast_appearance_applied, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun removeCustomBgImage(context: Context) {
        viewModelScope.launch {
            ImageUtils.deleteBackgroundImage(getApplication())
            _hasCustomBgImage.value = false
            val updated = _appearance.value.copy(bgType = "COLOR")
            _appearance.value = updated
            preferences.saveWidgetAppearance(updated)
            GenshinGlanceWidget().updateAll(getApplication())
            android.widget.Toast.makeText(context, com.genshin.dailynote.R.string.toast_appearance_applied, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
