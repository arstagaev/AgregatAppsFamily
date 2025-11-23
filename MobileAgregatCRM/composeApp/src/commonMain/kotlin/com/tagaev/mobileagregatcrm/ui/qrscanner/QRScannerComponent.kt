package com.tagaev.mobileagregatcrm.ui.qrscanner

import com.arkivanov.decompose.ComponentContext
import com.tagaev.data.models.qrscanner.QRResponseTRS
import com.tagaev.mobileagregatcrm.data.MainRepository
import com.tagaev.mobileagregatcrm.data.remote.ApiConfig
import com.tagaev.mobileagregatcrm.data.remote.EventsApi
import com.tagaev.mobileagregatcrm.data.remote.Resource
import com.tagaev.mobileagregatcrm.utils.getTimestamp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

data class QRScannerViewState(
    val attempts: List<QRAttempt> = emptyList(),
    val isLoading: Boolean = false,
    var lastSuccess: QRResponseTRS? = null,
    val lastError: String? = null,
    val flashlight: Boolean = false
)

interface IQRScannerComponent {
    val state: StateFlow<QRScannerViewState>
    fun onScanned(raw: String)
    fun toggleFlash()
    fun clearHistory()
}

enum class AttemptStatus { Loading, Success, Error }

data class QRAttempt(
    val id: String,
    val rawText: String,
    val startedAt: Long,
    val status: AttemptStatus,
    val response: QRResponseTRS? = null,
    val error: String? = null
)

class DefaultQRScannerComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
) : IQRScannerComponent, ComponentContext by componentContext, KoinComponent {

    private val api: EventsApi by inject()
    private val appScope: CoroutineScope by inject()
    private val apiConfig: ApiConfig by inject()
//    private val eventsCacheStore: EventsCacheStore by inject()
    private val repo by lazy { MainRepository(api, apiConfig) }

    private val _state = MutableStateFlow(QRScannerViewState())
    override val state: StateFlow<QRScannerViewState> = _state.asStateFlow()

    private var lastScanValue: String? = null
    private var lastScanTs: Long = 0

    override fun toggleFlash() {
        _state.update { it.copy(flashlight = !it.flashlight) }
    }

    override fun clearHistory() {
        _state.update { it.copy(attempts = emptyList()) }
    }

    private fun extractQueryParam(input: String, name: String): String? {
        // Works for: "... ?code=XYZ&...", "...&code=XYZ", "code=XYZ" and ignores fragments
        val query = input.substringAfter('?', missingDelimiterValue = input)
            .substringBefore('#')
        if (query.isEmpty()) return null
        val target = name.lowercase()
        for (part in query.split('&')) {
            if (part.isEmpty()) continue
            val eq = part.indexOf('=')
            val key = if (eq >= 0) part.substring(0, eq) else part
            if (key.lowercase() == target) {
                val value = if (eq >= 0) part.substring(eq + 1) else ""
                return value.takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    private fun extractTRSCode(input: String): String? {
        // 1) Prefer explicit query param ?code=...
        extractQueryParam(input, "code")?.let { return it }
        // 2) Fallback: any standalone TRS token in the text
        val m = Regex("""\bTRS[0-9A-Za-z]+\b""").find(input)
        return m?.value
    }

    override fun onScanned(raw: String) {
        println("Scanned: $raw")
        // simple dedupe/rate-limit: ignore same value within 2s
        val now = getTimestamp
        if (raw == lastScanValue && now - lastScanTs < 2000) return
        lastScanValue = raw
        lastScanTs = now

        val id = (0..1_000_000).random().toString()
        _state.update {
            it.copy(
                isLoading = true,
                lastError = null,
                attempts = it.attempts + QRAttempt(
                    id = id.toString(), rawText = raw, startedAt = now, status = AttemptStatus.Loading
                )
            )
        }

        appScope.launch {
            val code = extractTRSCode(raw) ?: raw
            println("QR >>> ${code}")
            when (val res = repo.getTRSData(code)) {
                is Resource.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }
                is Resource.Success -> {
                    val data = res.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            lastSuccess = data,
                            attempts = it.attempts.map { a ->
                                if (a.id == id) a.copy(status = AttemptStatus.Success, response = data)
                                else a
                            }
                        )
                    }
                }
                is Resource.Error -> {
                    val err = res.causes ?: res.exception?.message ?: "Ошибка запроса"
                    _state.update {
                        it.copy(
                            isLoading = false,
                            lastError = err,
                            attempts = it.attempts.map { a ->
                                if (a.id == id) a.copy(status = AttemptStatus.Error, error = err)
                                else a
                            }
                        )
                    }
                }
            }
        }
    }
}