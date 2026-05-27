package com.tagaev.trrcrm.updates

import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class DesktopReleaseInfo(
    val id: String,
    val version: String,
    val changelog: String,
    val isMandatory: Boolean
)

data class DesktopUpdateUiState(
    val supported: Boolean = false,
    val isBusy: Boolean = false,
    val progress: Float? = null,
    val statusMessage: String = "",
    val errorMessage: String? = null,
    val currentVersion: String? = null,
    val latestVersion: String? = null,
    val availableRelease: DesktopReleaseInfo? = null
)

interface DesktopUpdateService {
    val state: StateFlow<DesktopUpdateUiState>
    suspend fun checkForUpdates(manual: Boolean = false)
    suspend fun installAvailableUpdate()
    fun clearError()
    fun dismissAvailableUpdate()
}

class NoOpDesktopUpdateService : DesktopUpdateService {
    private val _state = MutableStateFlow(DesktopUpdateUiState())
    override val state: StateFlow<DesktopUpdateUiState> = _state

    override suspend fun checkForUpdates(manual: Boolean) = Unit
    override suspend fun installAvailableUpdate() = Unit
    override fun clearError() = Unit
    override fun dismissAvailableUpdate() = Unit
}

expect fun createDesktopUpdateService(client: HttpClient): DesktopUpdateService
