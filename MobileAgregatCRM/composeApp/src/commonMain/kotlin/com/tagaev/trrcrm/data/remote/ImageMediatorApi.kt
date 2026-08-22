package com.tagaev.trrcrm.data.remote

import com.tagaev.trrcrm.models.ImageDocumentType
import com.tagaev.trrcrm.models.DocumentUploadPeriod
import com.tagaev.trrcrm.models.ImageMediatorCanUploadRequest
import com.tagaev.trrcrm.models.ImageMediatorCanUploadResponse
import com.tagaev.trrcrm.models.ImageMediatorImageCountResponse
import com.tagaev.trrcrm.models.ImageMediatorImageListResponse
import com.tagaev.trrcrm.models.ImageMediatorUploadResponse
import com.tagaev.trrcrm.ui.permissions.CameraFixatorLog
import com.tagaev.trrcrm.ui.permissions.inspectImagePayload
import com.tagaev.trrcrm.utils.DefaultValuesConst.GLOBAL_IMAGE_MEDIATOR_URL
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.random.Random

class ImageMediatorApi(
    private val client: HttpClient,
    private val json: Json = HttpClientFactory.defaultJson,
    private val baseUrl: String = GLOBAL_IMAGE_MEDIATOR_URL,
    private val apiContract: String? = IMAGE_MEDIATOR_API_CONTRACT,
) {
    suspend fun canUpload(
        agrToken: String,
        documentNumber: String,
        uploadPeriod: DocumentUploadPeriod,
        documentType: ImageDocumentType = ImageDocumentType.Complects,
    ): Resource<ImageMediatorCanUploadResponse> = resourceify {
        val response = postWithLimitedRetry {
            client.post(buildUrl("/api/v1/uploads/can-upload")) {
                expectSuccess = false
                imageMediatorHeaders(agrToken)
                contentType(ContentType.Application.Json)
                setBody(
                    ImageMediatorCanUploadRequest(
                        documentNumber = documentNumber,
                        documentName = documentType.wireName,
                        year = uploadPeriod.year,
                        month = uploadPeriod.month,
                    )
                )
            }
        }
        decodeResponse(response, response.bodyAsText())
    }

    suspend fun getDocumentPhotoCount(
        agrToken: String,
        documentNumber: String,
        uploadPeriod: DocumentUploadPeriod,
        documentType: ImageDocumentType = ImageDocumentType.Complects,
    ): Resource<Int> = resourceify {
        val response = client.get(
            documentImagesUrl(
                year = uploadPeriod.year,
                month = uploadPeriod.month,
                documentNumber = documentNumber,
                tail = listOf("images", "count"),
                query = mapOf("document_name" to documentType.wireName),
            ),
        ) {
            expectSuccess = false
            imageMediatorHeaders(agrToken)
        }
        if (response.status.value == 404) {
            return@resourceify 0
        }
        val parsed = decodeResponse<ImageMediatorImageCountResponse>(
            response,
            response.bodyAsText(),
        )
        parsed.count
    }

    suspend fun listDocumentImages(
        agrToken: String,
        documentNumber: String,
        uploadPeriod: DocumentUploadPeriod,
        page: Int,
        documentType: ImageDocumentType = ImageDocumentType.Complects,
    ): Resource<ImageMediatorImageListResponse> = resourceify {
        val response = client.get(
            documentImagesUrl(
                year = uploadPeriod.year,
                month = uploadPeriod.month,
                documentNumber = documentNumber,
                tail = listOf("images"),
                query = mapOf(
                    "page" to page.toString(),
                    "document_name" to documentType.wireName,
                ),
            ),
        ) {
            expectSuccess = false
            imageMediatorHeaders(agrToken)
        }
        decodeResponse(response, response.bodyAsText())
    }

    suspend fun downloadImageContent(
        agrToken: String,
        contentUrl: String,
    ): Resource<ByteArray> = resourceify {
        downloadImageContentInternal(agrToken, contentUrl, allowRetry = true)
    }

    suspend fun uploadPhotos(
        agrToken: String,
        documentNumber: String,
        files: List<ByteArray>,
        uploadPeriod: DocumentUploadPeriod,
        documentType: ImageDocumentType = ImageDocumentType.Complects,
        idempotencyKey: String,
    ): Resource<ImageMediatorUploadResponse> = resourceify {
        require(files.isNotEmpty()) { "No files to upload" }
        val totalBytes = files.sumOf { it.size.toLong() }
        CameraFixatorLog.d(
            "upload_request files=${files.size} totalBytes=$totalBytes document=$documentNumber type=${documentType.wireName}",
        )
        val response = postWithLimitedRetry {
            client.post(buildUrl("/api/v1/uploads/photos")) {
                expectSuccess = false
                imageMediatorHeaders(agrToken)
                header("Idempotency-Key", idempotencyKey)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("document_number", documentNumber)
                            append("document_name", documentType.wireName)
                            append("year", uploadPeriod.year.toString())
                            append("month", uploadPeriod.month.toString())
                            files.forEachIndexed { index, bytes ->
                                append(
                                    key = "files[]",
                                    value = bytes,
                                    headers = io.ktor.http.Headers.build {
                                        append(HttpHeaders.ContentType, "image/jpeg")
                                        append(
                                            HttpHeaders.ContentDisposition,
                                            "filename=\"photo_$index.jpg\"",
                                        )
                                    },
                                )
                            }
                        },
                    ),
                )
            }
        }
        decodeResponse(response, response.bodyAsText())
    }

    private suspend fun downloadImageContentInternal(
        agrToken: String,
        contentUrl: String,
        allowRetry: Boolean,
    ): ByteArray {
        val response = client.get(buildUrl(contentUrl)) {
            expectSuccess = false
            imageMediatorHeaders(agrToken)
        }
        val requestUrl = response.call.request.url.toString()
        val contentType = response.headers[HttpHeaders.ContentType]
        val declaredLen = response.headers[HttpHeaders.ContentLength]
        val requestId = response.headers["x-request-id"] ?: response.headers["X-Request-Id"]
        val headerSummary =
            "url=$requestUrl status=${response.status.value} contentType=$contentType declaredLen=$declaredLen requestId=$requestId"
        if (response.status.value == 503 && allowRetry) {
            CameraFixatorLog.d("image_download_retry $headerSummary reason=http_503")
            delay(retryAfterMillis(response))
            return downloadImageContentInternal(agrToken, contentUrl, allowRetry = false)
        }
        if (!response.status.isSuccess()) {
            val raw = runCatching { response.bodyAsText() }.getOrDefault("")
            val prefix = raw.take(80).replace('\n', ' ')
            CameraFixatorLog.d(
                "image_download_failed $headerSummary reason=http_${response.status.value} bodyPrefix=$prefix",
            )
            throw imageMediatorException(response, raw)
        }
        val bytes = response.readRawBytes()
        val inspection = inspectImagePayload(bytes)
        val outcome = if (inspection.structuralReason == null) "image_download_ok" else "image_download_suspect"
        CameraFixatorLog.d("$outcome $headerSummary ${inspection.toLogFields()}")
        return bytes
    }

    private suspend fun postWithLimitedRetry(block: suspend () -> HttpResponse): HttpResponse {
        val first = block()
        if (first.status.value != 503) return first
        delay(retryAfterMillis(first))
        return block()
    }

    private fun HttpRequestBuilder.imageMediatorHeaders(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
        val contract = apiContract?.trim()
        if (!contract.isNullOrEmpty()) {
            header(CONTRACT_HEADER, contract)
        }
    }

    private fun documentImagesUrl(
        year: Int,
        month: Int,
        documentNumber: String,
        tail: List<String>,
        query: Map<String, String> = emptyMap(),
    ): String {
        val builder = URLBuilder().takeFrom(baseUrl.trimEnd('/'))
        builder.appendPathSegments(
            "api",
            "v1",
            "documents",
            year.toString(),
            month.toString(),
            documentNumber,
            *tail.toTypedArray(),
        )
        query.forEach { (key, value) -> builder.parameters.append(key, value) }
        return builder.buildString()
    }

    private fun buildUrl(path: String): String {
        // Preserve absolute content_url paths and any query params from the server.
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path
        }
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return "${baseUrl.trimEnd('/')}$normalizedPath"
    }

    private inline fun <reified T> decodeResponse(response: HttpResponse, raw: String): T {
        if (!response.status.isSuccess()) {
            throw imageMediatorException(response, raw)
        }
        return decodeOrWarning(json, raw.cleanJsonStart())
    }

    private fun imageMediatorException(response: HttpResponse, raw: String): ImageMediatorException =
        ImageMediatorException(
            statusCode = response.status.value,
            url = response.call.request.url.toString(),
            responseBody = raw,
            errorCode = parseImageMediatorErrorCode(raw),
            retryAfterSeconds = response.headers[HttpHeaders.RetryAfter]?.toLongOrNull(),
        )

    companion object {
        const val IMAGE_MEDIATOR_API_CONTRACT = "1.7"
        const val CONTRACT_HEADER = "X-ImageMediator-Contract"

        fun encodeDocumentNumberPathSegment(documentNumber: String): String =
            documentNumber.encodeURLPathPart()

        fun generateIdempotencyKey(): String {
            val bytes = Random.Default.nextBytes(16)
            val hex = bytes.joinToString(separator = "") { b ->
                (b.toInt() and 0xFF).toString(16).padStart(2, '0')
            }
            return buildString {
                append(hex.substring(0, 8))
                append('-')
                append(hex.substring(8, 12))
                append('-')
                append(hex.substring(12, 16))
                append('-')
                append(hex.substring(16, 20))
                append('-')
                append(hex.substring(20, 32))
            }
        }
    }
}

private fun retryAfterMillis(response: HttpResponse): Long {
    val seconds = response.headers[HttpHeaders.RetryAfter]
        ?.toLongOrNull()
        ?.coerceAtLeast(1L)
        ?: 2L
    return seconds * 1_000L
}
