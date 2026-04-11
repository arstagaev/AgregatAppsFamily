package com.tagaev.trrcrm.ui.qrscanner

import com.arkivanov.decompose.ComponentContext
import com.tagaev.data.models.qrscanner.QRResponseTRS
import com.tagaev.trrcrm.data.MainRepository
import com.tagaev.trrcrm.data.remote.ApiConfig
import com.tagaev.trrcrm.data.remote.EventsApi
import com.tagaev.trrcrm.data.remote.Resource
import com.tagaev.trrcrm.utils.getTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


data class QRScannerViewState(
    val attempts: List<QRAttempt> = emptyList(),
    val isLoading: Boolean = false,
    val lastError: String? = null,
    val flashlight: Boolean = false,
    val selectedAttempt: QRAttempt? = null,   // what dialog shows
    val isOpeningComplectation: Boolean = false,
    val openComplectationError: String? = null
)

interface IQRScannerComponent {
    val state: StateFlow<QRScannerViewState>

    fun onScanned(raw: String)
    fun onAttemptClicked(attempt: QRAttempt)
    fun onDialogDismissed()
    fun onOpenComplectationClicked()
    fun onOpenComplectationErrorShown()
    fun toggleFlash()
    fun clearHistory()
}

enum class AttemptStatus { Loading, Success, Error }

data class QRAttempt(
    val id: Long,                 // unique key for LazyColumn + state updates
    val rawText: String,
    val startedAt: Long,
    val status: AttemptStatus,
    val response: QRResponseTRS? = null,
    val error: String? = null
)

class DefaultQRScannerComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
    private val openComplectationByNumber: suspend (String) -> Boolean,
) : IQRScannerComponent, ComponentContext by componentContext, KoinComponent {

    private val api: EventsApi by inject()
    private val appScope: CoroutineScope by inject()
    private val apiConfig: ApiConfig by inject()
    private val repo by lazy { MainRepository(api, apiConfig) }

    private val _state = MutableStateFlow(QRScannerViewState())
    override val state: StateFlow<QRScannerViewState> = _state.asStateFlow()

    // simple id generator for attempts
    private var nextAttemptId = 0L
    private fun newAttemptId(): Long = ++nextAttemptId

    override fun toggleFlash() {
        _state.update { it.copy(flashlight = !it.flashlight) }
    }

    override fun clearHistory() {
        _state.update {
            it.copy(
                attempts = emptyList(),
                selectedAttempt = null,
                openComplectationError = null
            )
        }
    }

    override fun onDialogDismissed() {
        _state.update { it.copy(selectedAttempt = null, openComplectationError = null) }
    }

    override fun onAttemptClicked(attempt: QRAttempt) {
        _state.update { current ->
            val fresh = current.attempts.firstOrNull { it.id == attempt.id }
            current.copy(
                selectedAttempt = fresh,
                openComplectationError = null
            )
        }
    }

    override fun onOpenComplectationErrorShown() {
        _state.update { it.copy(openComplectationError = null) }
    }

    override fun onOpenComplectationClicked() {
        val snapshot = _state.value
        if (snapshot.isOpeningComplectation) return

        val selected = snapshot.selectedAttempt
        if (selected == null || selected.status != AttemptStatus.Success) return

        val completionNumber = selected.response?.completionNumber?.trim().orEmpty()
        if (completionNumber.isBlank()) {
            _state.update {
                it.copy(openComplectationError = "В QR нет номера комплектации")
            }
            return
        }

        appScope.launch {
            _state.update {
                it.copy(
                    isOpeningComplectation = true,
                    openComplectationError = null
                )
            }

            try {
                val opened = openComplectationByNumber(completionNumber)
                _state.update {
                    if (opened) {
                        it.copy(
                            isOpeningComplectation = false,
                            selectedAttempt = null,
                            openComplectationError = null
                        )
                    } else {
                        it.copy(
                            isOpeningComplectation = false,
                            openComplectationError = "Комплектация №$completionNumber не найдена"
                        )
                    }
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        isOpeningComplectation = false,
                        openComplectationError = t.message ?: "Не удалось открыть комплектацию"
                    )
                }
            }
        }
    }

    private fun extractQueryParam(input: String, name: String): String? {
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

        val now = getTimestamp
        val attemptId = newAttemptId()

        val loadingAttempt = QRAttempt(
            id = attemptId,
            rawText = raw,
            startedAt = now,
            status = AttemptStatus.Loading
        )

        // Add new attempt and show global loading dialog
        _state.update {
            it.copy(
                isLoading = true,
                lastError = null,
                openComplectationError = null,
                attempts = it.attempts + loadingAttempt
            )
        }

        appScope.launch {
            val code = extractTRSCode(raw) ?: raw
            println("QR >>> $code")

            when (val res = repo.getTRSData(code)) {
                is Resource.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }

                is Resource.Success -> {
                    val data = res.data
                    _state.update { old ->
                        val updatedAttempts = old.attempts.map { a ->
                            if (a.id == attemptId) {
                                a.copy(
                                    status = AttemptStatus.Success,
                                    response = data,
                                    error = null
                                )
                            } else a
                        }
                        val selected = updatedAttempts.firstOrNull { it.id == attemptId }
                        old.copy(
                            isLoading = false,
                            attempts = updatedAttempts,
                            selectedAttempt = selected   // open dialog for THIS scan
                        )
                    }
                }

                is Resource.Error -> {
                    val err = res.causes ?: res.exception?.message ?: "Ошибка запроса"
                    _state.update { old ->
                        val updatedAttempts = old.attempts.map { a ->
                            if (a.id == attemptId) {
                                a.copy(
                                    status = AttemptStatus.Error,
                                    error = err,
                                    response = null
                                )
                            } else a
                        }
                        val selected = updatedAttempts.firstOrNull { it.id == attemptId }
                        old.copy(
                            isLoading = false,
                            lastError = "Некорректный QR-код",
                            attempts = updatedAttempts,
                            selectedAttempt = selected   // show error dialog too
                        )
                    }
                }
            }
        }
    }
}
