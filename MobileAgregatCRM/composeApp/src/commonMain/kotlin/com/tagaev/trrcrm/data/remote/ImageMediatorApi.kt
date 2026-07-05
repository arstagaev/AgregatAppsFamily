package com.tagaev.trrcrm.data.remote

import com.tagaev.trrcrm.models.ImageMediatorCanUploadRequest
import com.tagaev.trrcrm.models.ImageMediatorCanUploadResponse
import com.tagaev.trrcrm.models.ImageMediatorUploadResponse
import com.tagaev.trrcrm.ui.permissions.CameraFixatorLog
import com.tagaev.trrcrm.utils.DefaultValuesConst.GLOBAL_IMAGE_MEDIATOR_URL
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

class ImageMediatorApi(
    private val client: HttpClient,
    private val json: Json = HttpClientFactory.defaultJson,
    private val baseUrl: String = GLOBAL_IMAGE_MEDIATOR_URL,
) {
    suspend fun canUpload(
        agrToken: String,
        documentNumber: String,
        documentName: String? = "Camera fixator",
    ): Resource<ImageMediatorCanUploadResponse> = resourceify {
        val response = client.post(buildUrl("/api/v1/uploads/can-upload")) {
            expectSuccess = false
            bearerAuth(agrToken)
            contentType(ContentType.Application.Json)
            setBody(
                ImageMediatorCanUploadRequest(
                    documentNumber = documentNumber,
                    documentName = documentName,
                )
            )
        }
        decodeResponse(response.status.value, response.call.request.url.toString(), response.bodyAsText())
    }

    suspend fun uploadPhotos(
        agrToken: String,
        documentNumber: String,
        files: List<ByteArray>,
        documentName: String? = "Camera fixator",
    ): Resource<ImageMediatorUploadResponse> = resourceify {
        require(files.isNotEmpty()) { "No files to upload" }
        val totalBytes = files.sumOf { it.size.toLong() }
        CameraFixatorLog.d("upload_request files=${files.size} totalBytes=$totalBytes document=$documentNumber")
        val response = client.post(buildUrl("/api/v1/uploads/photos")) {
            expectSuccess = false
            bearerAuth(agrToken)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("document_number", documentNumber)
                        documentName?.let { append("document_name", it) }
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

    private fun buildUrl(path: String): String =
        "${baseUrl.trimEnd('/')}$path"

    private fun io.ktor.client.request.HttpRequestBuilder.bearerAuth(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private inline fun <reified T> decodeResponse(statusCode: Int, url: String, raw: String): T {
        if (!io.ktor.http.HttpStatusCode.fromValue(statusCode).isSuccess()) {
            throw ImageMediatorException(statusCode, url, raw)
        }
        return decodeOrWarning(json, raw.cleanJsonStart())
    }
}
