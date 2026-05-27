package com.tagaev.trrcrm.updates

import com.tagaev.secrets.Secrets
import com.tagaev.trrcrm.utils.DefaultValuesConst
import com.tagaev.trrcrm.utils.Env
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import kotlin.system.exitProcess

class DesktopUpdateServiceImpl(
    private val client: HttpClient
) : DesktopUpdateService {

    private val baseUrl = DefaultValuesConst.GLOBAL_CATALOG_URL.trimEnd('/')
    private val allowHosts = setOf("trrservice.agregatka.ru")
    private val appId = "trr-crm-desktop"
    private val channel = "stable"
    private val platform = "windows"

    private val _state = MutableStateFlow(
        DesktopUpdateUiState(
            supported = true,
            currentVersion = Secrets.VERSION
        )
    )
    override val state: StateFlow<DesktopUpdateUiState> = _state

    override suspend fun checkForUpdates(manual: Boolean) {
        val current = _state.value
        _state.value = current.copy(isBusy = true, errorMessage = null, statusMessage = "Проверяем обновления...")
        val result = withRetry("check") {
            fetchUpdateCheck()
        }
        result.fold(
            onSuccess = { response ->
                val release = response.release
                if (response.updateAvailable && release != null) {
                    _state.value = _state.value.copy(
                        isBusy = false,
                        statusMessage = "Доступно обновление ${release.version}",
                        latestVersion = response.latestVersion,
                        currentVersion = response.currentVersion,
                        availableRelease = DesktopReleaseInfo(
                            id = release.id,
                            version = release.version,
                            changelog = release.changelog.orEmpty(),
                            isMandatory = release.isMandatory
                        ),
                        progress = null
                    )
                    println("UPDATE_SERVICE: check success update_available=true version=${release.version}")
                } else {
                    _state.value = _state.value.copy(
                        isBusy = false,
                        statusMessage = if (manual) "Обновлений нет" else "",
                        latestVersion = response.latestVersion,
                        currentVersion = response.currentVersion,
                        availableRelease = null,
                        progress = null
                    )
                    println("UPDATE_SERVICE: check success update_available=false")
                }
            },
            onFailure = { t ->
                _state.value = _state.value.copy(
                    isBusy = false,
                    errorMessage = "Не удалось проверить обновления: ${t.safeMessage()}",
                    statusMessage = ""
                )
                println("UPDATE_SERVICE: check failed reason=${t.safeMessage()}")
            }
        )
    }

    override suspend fun installAvailableUpdate() {
        if (!isWindowsDesktop()) {
            _state.value = _state.value.copy(
                isBusy = false,
                errorMessage = "Установка обновления поддерживается только на Windows Desktop."
            )
            return
        }
        val release = _state.value.availableRelease ?: return
        _state.value = _state.value.copy(
            isBusy = true,
            errorMessage = null,
            progress = 0f,
            statusMessage = "Загружаем обновление..."
        )
        val releaseMeta = withRetry("check-before-install") { fetchUpdateCheck() }.getOrNull()?.release
            ?: run {
                _state.value = _state.value.copy(isBusy = false, errorMessage = "Не удалось получить метаданные обновления")
                return
            }
        if (releaseMeta.id != release.id) {
            _state.value = _state.value.copy(isBusy = false, errorMessage = "Доступна более новая версия. Проверьте обновления снова.")
            return
        }
        val downloaded = withRetry("download") { downloadRelease(releaseMeta) }
        downloaded.fold(
            onSuccess = { file ->
                _state.value = _state.value.copy(
                    isBusy = true,
                    statusMessage = "Запускаем установщик...",
                    progress = 1f
                )
                println("UPDATE_SERVICE: download+verify success file=${file.absolutePath}")
                launchInstallerAndExit(file)
            },
            onFailure = { t ->
                _state.value = _state.value.copy(
                    isBusy = false,
                    progress = null,
                    errorMessage = "Ошибка загрузки обновления: ${t.safeMessage()}",
                    statusMessage = ""
                )
                println("UPDATE_SERVICE: download failed reason=${t.safeMessage()}")
            }
        )
    }

    override fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    override fun dismissAvailableUpdate() {
        if (_state.value.availableRelease?.isMandatory == true) return
        _state.value = _state.value.copy(availableRelease = null, statusMessage = "")
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
            throw IllegalStateException("HTTP ${response.status.value}")
        }
        return response.body()
    }

    private suspend fun downloadRelease(release: UpdateReleaseDto): File {
        val downloadUrl = resolveDownloadUrl(release.downloadUrl, release.id)
        ensureUrlAllowed(downloadUrl)
        val tempDir = File(System.getProperty("java.io.tmpdir"), "trrcrm-updates").apply { mkdirs() }
        val target = File(tempDir, release.fileName)
        val existing = if (target.exists()) target.length() else 0L
        val fileSize = release.fileSize
        var written = existing
        val response = client.get(downloadUrl) {
            if (existing in 1 until fileSize) {
                header(HttpHeaders.Range, "bytes=$existing-")
            }
        }
        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.PartialContent) {
            throw IllegalStateException("HTTP ${response.status.value}")
        }
        val channel = response.body<io.ktor.utils.io.ByteReadChannel>()
        if (response.status == HttpStatusCode.OK && existing > 0L) {
            target.delete()
            written = 0L
        }
        RandomAccessFile(target, "rw").use { raf ->
            if (written > 0L) raf.seek(written)
            val buffer = ByteArray(64 * 1024)
            while (!channel.isClosedForRead) {
                val read = channel.readAvailable(buffer)
                if (read <= 0) continue
                raf.write(buffer, 0, read)
                written += read
                val pct = (written.toDouble() / fileSize.toDouble()).coerceIn(0.0, 1.0)
                _state.value = _state.value.copy(progress = pct.toFloat(), statusMessage = "Загружаем обновление... ${((pct * 100).toInt())}%")
            }
        }

        verifyDownloaded(target, fileSize, release.sha256)
        return target
    }

    private fun verifyDownloaded(file: File, expectedSize: Long, expectedSha256: String) {
        if (!file.exists()) throw IllegalStateException("Файл обновления не найден")
        if (file.length() != expectedSize) {
            file.delete()
            throw IllegalStateException("Размер файла не совпадает")
        }
        val actualHash = sha256(file)
        if (!actualHash.equals(expectedSha256, ignoreCase = true)) {
            file.delete()
            throw IllegalStateException("Контрольная сумма не совпадает")
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
            println("UPDATE_SERVICE: updater helper started")
            exitProcess(0)
        }.onFailure {
            _state.value = _state.value.copy(
                isBusy = false,
                errorMessage = "Не удалось запустить установщик: ${it.safeMessage()}",
                statusMessage = ""
            )
            println("UPDATE_SERVICE: helper start failed reason=${it.safeMessage()}")
        }
    }

    private suspend fun <T> withRetry(label: String, block: suspend () -> T): Result<T> {
        val delays = longArrayOf(0L, 800L, 1_600L, 3_200L)
        var lastError: Throwable? = null
        for (i in delays.indices) {
            if (delays[i] > 0) delay(delays[i])
            runCatching { block() }
                .onSuccess { return Result.success(it) }
                .onFailure { error ->
                    lastError = error
                    println("UPDATE_SERVICE: $label attempt=${i + 1} failed reason=${error.safeMessage()}")
                    if (!error.isRetryable() || i == delays.lastIndex) return Result.failure(error)
                }
        }
        return Result.failure(lastError ?: IllegalStateException("Unknown update error"))
    }

    private fun resolveDownloadUrl(downloadUrl: String?, releaseId: String): String {
        val path = downloadUrl?.takeIf { it.isNotBlank() } ?: "/updates/download/$releaseId"
        return if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            "$baseUrl/${path.trimStart('/')}"
        }
    }

    private fun ensureUrlAllowed(url: String) {
        val parsed = io.ktor.http.Url(url)
        if (Env.isProd && parsed.protocol != URLProtocol.HTTPS) {
            throw IllegalStateException("Небезопасный URL обновлений (нужен HTTPS)")
        }
        val host = parsed.host.lowercase()
        if (host !in allowHosts) {
            throw IllegalStateException("Хост обновлений не разрешен: $host")
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

    private fun Throwable.safeMessage(): String = message ?: this::class.simpleName.orEmpty().ifBlank { "unknown" }

    private fun Throwable.isRetryable(): Boolean {
        if (this is HttpRequestTimeoutException) return true
        val msg = message.orEmpty().lowercase()
        return msg.contains("timeout") ||
            msg.contains("temporarily") ||
            msg.contains("connection reset") ||
            msg.contains("network is unreachable") ||
            msg.contains("unknown host")
    }

    private fun isWindowsDesktop(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("windows")
}

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
