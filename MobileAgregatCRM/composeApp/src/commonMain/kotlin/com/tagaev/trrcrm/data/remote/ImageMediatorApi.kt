package com.tagaev.trrcrm.data.remote

import com.tagaev.trrcrm.models.ImageDocumentType
import com.tagaev.trrcrm.models.DocumentUploadPeriod
import com.tagaev.trrcrm.models.ImageMediatorCanUploadRequest
import com.tagaev.trrcrm.models.ImageMediatorCanUploadResponse
import com.tagaev.trrcrm.models.ImageMediatorImageCountResponse
import com.tagaev.trrcrm.models.ImageMediatorImageListResponse
import com.tagaev.trrcrm.models.ImageMediatorUploadResponse
import com.tagaev.trrcrm.ui.permissions.CameraFixatorLog
import com.tagaev.trrcrm.utils.DefaultValuesConst.GLOBAL_IMAGE_MEDIATOR_URL
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.random.Random

class ImageMediatorApi(
    private val client: HttpClient,
    private val json: Json = HttpClientFactory.defaultJson,
    private val baseUrl: String = GLOBAL_IMAGE_MEDIATOR_URL,
) {
    suspend fun canUpload(
        agrToken: String,
        documentNumber: String,
        uploadPeriod: DocumentUploadPeriod,
        documentType: ImageDocumentType = ImageDocumentType.Complects,
    ): Resource<ImageMediatorCanUploadResponse> = resourceify {
        val response = client.post(buildUrl("/api/v1/uploads/can-upload")) {
            expectSuccess = false
            bearerAuth(agrToken)
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
        decodeResponse(response.status.value, response.call.request.url.toString(), response.bodyAsText())
    }

    suspend fun getDocumentPhotoCount(
        agrToken: String,
        documentNumber: String,
        documentType: ImageDocumentType = ImageDocumentType.Complects,
    ): Resource<Int> = resourceify {
        val response = client.get(
            buildUrl("/api/v1/documents/$documentNumber/images/count?document_name=${documentType.wireName}"),
        ) {
            expectSuccess = false
            bearerAuth(agrToken)
        }
        val body = response.bodyAsText()
        val parsed = decodeResponse<ImageMediatorImageCountResponse>(
            response.status.value,
            response.call.request.url.toString(),
            body,
        )
        parsed.count
    }

    suspend fun listDocumentImages(
        agrToken: String,
        documentNumber: String,
        page: Int,
        documentType: ImageDocumentType = ImageDocumentType.Complects,
    ): Resource<ImageMediatorImageListResponse> = resourceify {
        val response = client.get(
            buildUrl(
                "/api/v1/documents/$documentNumber/images?page=$page&document_name=${documentType.wireName}",
            ),
        ) {
            expectSuccess = false
            bearerAuth(agrToken)
        }
        decodeResponse(
            response.status.value,
            response.call.request.url.toString(),
            response.bodyAsText(),
        )
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
        val response = client.post(buildUrl("/api/v1/uploads/photos")) {
            expectSuccess = false
            bearerAuth(agrToken)
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
        decodeResponse(response.status.value, response.call.request.url.toString(), response.bodyAsText())
    }

    private suspend fun downloadImageContentInternal(
        agrToken: String,
        contentUrl: String,
        allowRetry: Boolean,
    ): ByteArray {
        val response = client.get(buildUrl(contentUrl)) {
            expectSuccess = false
            bearerAuth(agrToken)
        }
        if (response.status.value == 503 && allowRetry) {
            val retryAfterSeconds = response.headers[HttpHeaders.RetryAfter]
                ?.toLongOrNull()
                ?.coerceAtLeast(1L)
                ?: 2L
            delay(retryAfterSeconds * 1_000L)
            return downloadImageContentInternal(agrToken, contentUrl, allowRetry = false)
        }
        if (!response.status.isSuccess()) {
            throw ImageMediatorException(
                statusCode = response.status.value,
                url = response.call.request.url.toString(),
                responseBody = runCatching { response.bodyAsText() }.getOrDefault(""),
            )
        }
        return response.readRawBytes()
    }

    private fun buildUrl(path: String): String {
        // Preserve absolute content_url paths and any query params from the server.
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path
        }
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return "${baseUrl.trimEnd('/')}$normalizedPath"
    }

    private fun io.ktor.client.request.HttpRequestBuilder.bearerAuth(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private inline fun <reified T> decodeResponse(statusCode: Int, url: String, raw: String): T {
        if (!io.ktor.http.HttpStatusCode.fromValue(statusCode).isSuccess()) {
            throw ImageMediatorException(statusCode, url, raw)
        }
        return decodeOrWarning(json, raw.cleanJsonStart())
    }

    companion object {
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
