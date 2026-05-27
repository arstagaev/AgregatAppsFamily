package com.tagaev.trrcrm.updates

import com.tagaev.secrets.Secrets
import com.tagaev.trrcrm.data.AppSettings
import com.tagaev.trrcrm.data.AppSettingsKeys
import com.tagaev.trrcrm.utils.DefaultValuesConst
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlin.random.Random
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import kotlin.system.exitProcess

class DesktopUpdateServiceImpl(
    private val client: HttpClient,
    private val settings: AppSettings
) : DesktopUpdateService {

    private val baseUrl = DefaultValuesConst.GLOBAL_CATALOG_URL.trimEnd('/')
    private val allowHosts = setOf("trrservice.agregatka.ru")
    private val appId = "trr-crm-desktop"
    private val channel = "stable"
    private val platform = "windows"
    private val operationMutex = Mutex()
    @Volatile private var cancelRequested = false

    private val _state = MutableStateFlow(
        DesktopUpdateUiState(
            supported = true,
            currentVersion = Secrets.VERSION
        )
    )
    override val state: StateFlow<DesktopUpdateUiState> = _state

    override suspend fun checkForUpdates(manual: Boolean) {
        operationMutex.withLock {
            if (_state.value.isBusy) return
            val nowMs = System.currentTimeMillis()
            val lastCheckMs = settings.getLong(AppSettingsKeys.DESKTOP_UPDATER_LAST_CHECK_AT_MS, 0L)
            if (!manual && nowMs - lastCheckMs < AUTO_CHECK_INTERVAL_MS) return

            emitPhase(UpdaterPhase.CHECKING, isBusy = true, status = "Проверяем обновления...", error = null)
            logEvent("update_check_started", "manual=$manual current=${Secrets.VERSION}")

            val result = withRetry("check") { fetchUpdateCheck() }
            settings.setLong(AppSettingsKeys.DESKTOP_UPDATER_LAST_CHECK_AT_MS, nowMs)

            result.fold(
                onSuccess = { response ->
                    val latest = response.latestVersion?.let { SemVerParser.parseOrNull(it) }
                        ?: throw UpdateMalformedException("Некорректная latest_version")
                    val current = (response.currentVersion ?: Secrets.VERSION).let { SemVerParser.parseOrNull(it) }
                        ?: throw UpdateMalformedException("Некорректная current_version")

                    if (current >= latest || !response.updateAvailable) {
                        emitPhase(
                            phase = UpdaterPhase.IDLE,
                            isBusy = false,
                            status = if (manual) "Обновлений нет" else "",
                            available = null,
                            progress = null
                        )
                        logEvent("update_check_completed", "available=false latest=${response.latestVersion}")
                        return@fold
                    }

                    val release = response.release ?: throw UpdateMalformedException("Отсутствует release при update_available=true")
                    val skippedVersion = settings.getStringOrNull(AppSettingsKeys.DESKTOP_UPDATER_SKIPPED_VERSION)
                    if (!manual && !release.isMandatory && skippedVersion == release.version) {
                        emitPhase(UpdaterPhase.IDLE, isBusy = false, status = "")
                        logEvent("update_check_completed", "available=false skipped=${release.version}")
                        return@fold
                    }

                    val available = DesktopReleaseInfo(
                        id = release.id,
                        version = release.version,
                        changelog = release.changelog.orEmpty(),
                        isMandatory = release.isMandatory,
                        fileSize = release.fileSize.takeIf { it > 0 }
                    )
                    _state.value = _state.value.copy(
                        phase = UpdaterPhase.AVAILABLE,
                        isBusy = false,
                        statusMessage = "Доступно обновление ${release.version}",
                        errorMessage = null,
                        latestVersion = response.latestVersion,
                        currentVersion = response.currentVersion ?: Secrets.VERSION,
                        availableRelease = available,
                        progress = null,
                        canCancelDownload = false
                    )
                    logEvent("update_found", "version=${release.version} mandatory=${release.isMandatory}")
                    logEvent("update_check_completed", "available=true latest=${response.latestVersion}")
                },
                onFailure = { t ->
                    val mapped = mapError(t)
                    emitPhase(
                        phase = UpdaterPhase.ERROR,
                        isBusy = false,
                        status = "",
                        error = mapped
                    )
                    logEvent("update_check_failed", "reason=${t.safeMessage()}")
                }
            )
        }
    }

    override suspend fun installAvailableUpdate() {
        operationMutex.withLock {
            if (_state.value.isBusy) return
            if (!isWindowsDesktop()) {
                emitPhase(UpdaterPhase.ERROR, false, "", "Установка обновления поддерживается только на Windows Desktop.")
                return
            }
            val release = _state.value.availableRelease ?: return
            cancelRequested = false

            emitPhase(
                phase = UpdaterPhase.DOWNLOADING,
                isBusy = true,
                status = "Загружаем обновление...",
                error = null,
                progress = 0f,
                canCancel = !release.isMandatory
            )
            logEvent("update_download_started", "version=${release.version}")

            val releaseMeta = withRetry("check-before-install") { fetchUpdateCheck() }.getOrNull()?.release
                ?: run {
                    emitPhase(UpdaterPhase.ERROR, false, "", "Не удалось получить метаданные обновления")
                    logEvent("update_download_failed", "reason=missing_release_meta")
                    return
                }
            if (releaseMeta.id != release.id) {
                emitPhase(UpdaterPhase.ERROR, false, "", "Доступна более новая версия. Проверьте обновления снова.")
                logEvent("update_download_failed", "reason=stale_release")
                return
            }

            val downloaded = withRetry("download") { downloadRelease(releaseMeta) }
            downloaded.fold(
                onSuccess = { file ->
                    emitPhase(
                        phase = UpdaterPhase.VERIFYING,
                        isBusy = true,
                        status = "Проверяем пакет обновления...",
                        progress = 1f,
                        canCancel = false
                    )
                    logEvent("update_download_completed", "file=${file.name}")
                    emitPhase(
                        phase = UpdaterPhase.INSTALLING,
                        isBusy = true,
                        status = "Запускаем установщик...",
                        progress = 1f,
                        canCancel = false
                    )
                    logEvent("update_install_started", "release_id=${release.id}")
                    launchInstallerAndExit(file)
                },
                onFailure = { t ->
                    if (t is UpdateCancelledException) {
                        val status = if (release.isMandatory) "Загрузка обязательного обновления была прервана" else "Загрузка обновления отменена"
                        emitPhase(UpdaterPhase.AVAILABLE, false, status, null, available = release, progress = null)
                        logEvent("update_download_failed", "reason=cancelled")
                    } else {
                        val mapped = mapError(t)
                        emitPhase(UpdaterPhase.ERROR, false, "", mapped, available = release, progress = null)
                        if (t is UpdateChecksumException) {
                            logEvent("update_checksum_failed", "reason=${t.safeMessage()}")
                        }
                        logEvent("update_download_failed", "reason=${t.safeMessage()}")
                    }
                }
            )
        }
    }

    override fun cancelDownload() {
        val current = _state.value
        if (current.phase != UpdaterPhase.DOWNLOADING || !current.canCancelDownload) return
        cancelRequested = true
    }

    override fun clearError() {
        _state.value = _state.value.copy(errorMessage = null).normalizePhaseAfterError()
    }

    override fun dismissAvailableUpdate() {
        val release = _state.value.availableRelease ?: return
        if (release.isMandatory) return
        settings.setString(AppSettingsKeys.DESKTOP_UPDATER_SKIPPED_VERSION, release.version)
        _state.value = _state.value.copy(
            phase = UpdaterPhase.IDLE,
            availableRelease = null,
            statusMessage = "",
            errorMessage = null,
            progress = null,
            canCancelDownload = false
        )
    }

    private suspend fun fetchUpdateCheck(): UpdateCheckResponse {
        val url = "$baseUrl/updates/check"
        ensureUrlAllowed(url)
        val response = client.get(url) {
            parameter("app_id", appId)
            parameter("platform", platform)
            parameter("channel", channel)
            parameter("current_version", Secrets.VERSION)
        }
        if (!response.status.isSuccess()) {
            throw UpdateServerException(response.status.value)
        }
        val parsed = try {
            response.body<UpdateCheckResponse>()
        } catch (e: SerializationException) {
            throw UpdateMalformedException("Некорректный ответ сервера")
        }
        if (parsed.status.lowercase() != "ok") {
            throw UpdateMalformedException("Поле status != ok")
        }
        return parsed
    }

    private suspend fun downloadRelease(release: UpdateReleaseDto): File {
        val downloadUrl = resolveDownloadUrl(release.downloadUrl, release.id)
        ensureUrlAllowed(downloadUrl)
        val tempDir = File(System.getProperty("java.io.tmpdir"), "trrcrm-updates").apply { mkdirs() }
        val target = File(tempDir, release.fileName)
        val fileSize = release.fileSize
        var existing = if (target.exists()) target.length() else 0L
        if (existing > fileSize && fileSize > 0L) {
            target.delete()
            existing = 0L
        }

        val response = client.get(downloadUrl) {
            if (existing in 1 until fileSize) {
                header(HttpHeaders.Range, "bytes=$existing-")
            }
        }
        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.PartialContent) {
            throw UpdateServerException(response.status.value)
        }
        val channel = response.body<io.ktor.utils.io.ByteReadChannel>()
        if (response.status == HttpStatusCode.OK && existing > 0L) {
            target.delete()
            existing = 0L
        }
        var written = existing

        RandomAccessFile(target, "rw").use { raf ->
            if (written > 0L) raf.seek(written)
            val buffer = ByteArray(64 * 1024)
            while (!channel.isClosedForRead) {
                if (cancelRequested) throw UpdateCancelledException()
                val read = channel.readAvailable(buffer)
                if (read <= 0) continue
                raf.write(buffer, 0, read)
                written += read
                if (fileSize > 0L) {
                    val progress = (written.toDouble() / fileSize.toDouble()).coerceIn(0.0, 1.0).toFloat()
                    _state.value = _state.value.copy(progress = progress, statusMessage = "Загружаем обновление... ${(progress * 100).toInt()}%")
                    logEvent("update_download_progress", "progress=${(progress * 100).toInt()}")
                }
            }
        }

        verifyDownloaded(target, release.fileSize, release.sha256)
        return target
    }

    private fun verifyDownloaded(file: File, expectedSize: Long, expectedSha256: String) {
        if (!file.exists()) throw UpdateMalformedException("Файл обновления не найден")
        if (expectedSize > 0L && file.length() != expectedSize) {
            file.delete()
            throw UpdateMalformedException("Размер файла не совпадает")
        }
        val actualHash = sha256(file)
        if (!actualHash.equals(expectedSha256, ignoreCase = true)) {
            file.delete()
            throw UpdateChecksumException("Контрольная сумма не совпадает")
        }
    }

    private fun launchInstallerAndExit(installerFile: File) {
        val javaBin = ProcessHandle.current().info().command().orElse("java")
        val classPath = System.getProperty("java.class.path").orEmpty()
        val parentPid = ProcessHandle.current().pid().toString()
        val restartCmd = ProcessHandle.current().info().command().orElse("")
        val command = listOf(
            javaBin,
            "-cp",
            classPath,
            "com.tagaev.trrcrm.updates.DesktopUpdaterLauncher",
            installerFile.absolutePath,
            parentPid,
            restartCmd
        )
        runCatching {
            ProcessBuilder(command).start()
            exitProcess(0)
        }.onFailure {
            emitPhase(UpdaterPhase.ERROR, false, "", "Не удалось запустить установщик: ${it.safeMessage()}")
            logEvent("update_install_failed", "reason=${it.safeMessage()}")
        }
    }

    private suspend fun <T> withRetry(label: String, block: suspend () -> T): Result<T> {
        val baseDelaysMs = longArrayOf(0L, 800L, 1_600L, 3_200L)
        var lastError: Throwable? = null
        for (attempt in baseDelaysMs.indices) {
            val baseDelay = baseDelaysMs[attempt]
            val jitter = if (baseDelay == 0L) 0L else Random.nextLong(0L, 350L)
            if (baseDelay > 0L) delay(baseDelay + jitter)
            runCatching { block() }
                .onSuccess { return Result.success(it) }
                .onFailure { error ->
                    lastError = error
                    val retriable = error.isRetryable()
                    logEvent("${label}_attempt_failed", "attempt=${attempt + 1} retriable=$retriable reason=${error.safeMessage()}")
                    if (!retriable || attempt == baseDelaysMs.lastIndex) {
                        return Result.failure(error)
                    }
                }
        }
        return Result.failure(lastError ?: UpdateMalformedException("Неизвестная ошибка обновления"))
    }

    private fun resolveDownloadUrl(downloadUrl: String?, releaseId: String): String {
        val path = downloadUrl?.takeIf { it.isNotBlank() } ?: "/updates/download/$releaseId"
        return if (path.startsWith("http://") || path.startsWith("https://")) path else "$baseUrl/${path.trimStart('/')}"
    }

    private fun ensureUrlAllowed(url: String) {
        val parsed = io.ktor.http.Url(url)
        val host = parsed.host.lowercase()
        if (host !in allowHosts) throw UpdateMalformedException("Хост обновлений не разрешен: $host")
        val protocol = parsed.protocol
        val isSecure = protocol == URLProtocol.HTTPS
        val isTrustedHttp = protocol == URLProtocol.HTTP && host in allowHosts
        if (!isSecure && !isTrustedHttp) {
            throw UpdateMalformedException("Недопустимая схема URL обновлений: ${protocol.name}")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buf)
                if (read <= 0) break
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun Throwable.isRetryable(): Boolean {
        if (this is UpdateServerException) return statusCode >= 500
        if (this is UpdateNetworkException || this is HttpRequestTimeoutException) return true
        val lower = message.orEmpty().lowercase()
        return lower.contains("timeout") ||
            lower.contains("connection reset") ||
            lower.contains("network is unreachable") ||
            lower.contains("unknown host")
    }

    private fun mapError(error: Throwable): String = when (error) {
        is UpdateNetworkException, is HttpRequestTimeoutException -> "Сеть недоступна. Проверьте подключение и повторите."
        is UpdateServerException -> "Сервер обновлений вернул ошибку (${error.statusCode})."
        is UpdateMalformedException -> "Получен некорректный ответ сервера обновлений."
        is UpdateChecksumException -> "Файл обновления повреждён (проверка контрольной суммы не пройдена)."
        is UpdateInstallerException -> "Не удалось запустить установщик обновления."
        is UpdateCancelledException -> "Загрузка обновления отменена."
        else -> if (error.isRetryable()) {
            "Сеть недоступна. Проверьте подключение и повторите."
        } else {
            "Не удалось выполнить обновление: ${error.safeMessage()}"
        }
    }

    private fun emitPhase(
        phase: UpdaterPhase,
        isBusy: Boolean,
        status: String,
        error: String? = null,
        available: DesktopReleaseInfo? = _state.value.availableRelease,
        progress: Float? = _state.value.progress,
        canCancel: Boolean = false
    ) {
        _state.value = _state.value.copy(
            phase = phase,
            isBusy = isBusy,
            statusMessage = status,
            errorMessage = error,
            availableRelease = available,
            progress = progress,
            canCancelDownload = canCancel
        )
    }

    private fun logEvent(event: String, details: String) {
        println("UPDATE_EVENT: $event $details")
    }

    private fun Throwable.safeMessage(): String = message ?: this::class.simpleName.orEmpty().ifBlank { "unknown" }

    private fun DesktopUpdateUiState.normalizePhaseAfterError(): DesktopUpdateUiState {
        if (errorMessage != null) return this
        return if (availableRelease != null) copy(phase = UpdaterPhase.AVAILABLE) else copy(phase = UpdaterPhase.IDLE)
    }

    private fun isWindowsDesktop(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("windows")

    private companion object {
        const val AUTO_CHECK_INTERVAL_MS = 6L * 60L * 60L * 1_000L
    }
}

private class UpdateNetworkException(message: String) : RuntimeException(message)
private class UpdateServerException(val statusCode: Int) : RuntimeException("HTTP $statusCode")
private class UpdateMalformedException(message: String) : RuntimeException(message)
private class UpdateChecksumException(message: String) : RuntimeException(message)
private class UpdateInstallerException(message: String) : RuntimeException(message)
private class UpdateCancelledException : RuntimeException("cancelled")

@Serializable
private data class UpdateCheckResponse(
    val status: String,
    @SerialName("update_available") val updateAvailable: Boolean,
    @SerialName("current_version") val currentVersion: String? = null,
    @SerialName("latest_version") val latestVersion: String? = null,
    val release: UpdateReleaseDto? = null
)

@Serializable
private data class UpdateReleaseDto(
    val id: String,
    val version: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("file_size") val fileSize: Long,
    val sha256: String,
    @SerialName("download_url") val downloadUrl: String? = null,
    val changelog: String? = null,
    @SerialName("is_mandatory") val isMandatory: Boolean = false
)
