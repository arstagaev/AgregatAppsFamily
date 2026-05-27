package com.tagaev.trrcrm.updates

import io.ktor.client.HttpClient
import com.tagaev.trrcrm.data.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class UpdaterPhase {
    IDLE,
    CHECKING,
    AVAILABLE,
    DOWNLOADING,
    VERIFYING,
    INSTALLING,
    ERROR
}

data class DesktopReleaseInfo(
    val id: String,
    val version: String,
    val changelog: String,
    val isMandatory: Boolean,
    val fileSize: Long? = null
)

data class DesktopUpdateUiState(
    val supported: Boolean = false,
    val isBusy: Boolean = false,
    val phase: UpdaterPhase = UpdaterPhase.IDLE,
    val progress: Float? = null,
    val statusMessage: String = "",
    val errorMessage: String? = null,
    val currentVersion: String? = null,
    val latestVersion: String? = null,
    val availableRelease: DesktopReleaseInfo? = null,
    val canCancelDownload: Boolean = false
)

interface DesktopUpdateService {
    val state: StateFlow<DesktopUpdateUiState>
    suspend fun checkForUpdates(manual: Boolean = false)
    suspend fun installAvailableUpdate()
    fun cancelDownload()
    fun clearError()
    fun dismissAvailableUpdate()
}

class NoOpDesktopUpdateService : DesktopUpdateService {
    private val _state = MutableStateFlow(DesktopUpdateUiState())
    override val state: StateFlow<DesktopUpdateUiState> = _state

    override suspend fun checkForUpdates(manual: Boolean) = Unit
    override suspend fun installAvailableUpdate() = Unit
    override fun cancelDownload() = Unit
    override fun clearError() = Unit
    override fun dismissAvailableUpdate() = Unit
}

expect fun createDesktopUpdateService(
    client: HttpClient,
    settings: AppSettings
): DesktopUpdateService
