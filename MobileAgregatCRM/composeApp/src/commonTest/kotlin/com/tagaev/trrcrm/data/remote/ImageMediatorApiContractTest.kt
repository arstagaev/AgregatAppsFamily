package com.tagaev.trrcrm.data.remote

import com.tagaev.trrcrm.models.ImageMediatorCanUploadResponse
import com.tagaev.trrcrm.models.ImageMediatorUploadResponse
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ImageMediatorApiContractTest {

    @Test
    fun `canUpload sends bearer token and parses success`() = runTest {
        var authHeader: String? = null
        val engine = MockEngine { request ->
            authHeader = request.headers[HttpHeaders.Authorization]
            assertEquals(HttpMethod.Post, request.method)
            assertTrue(request.url.encodedPath.endsWith("/api/v1/uploads/can-upload"))
            respond(
                content = """
                    {
                      "allowed": true,
                      "document_number": "0000549041",
                      "resolved_year": 2026,
                      "resolved_month": 7,
                      "folder_found": true,
                      "folder_path": "/2026/7/0000549041"
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val api = ImageMediatorApi(
            client = HttpClientFactory.create(engine = engine, loggingEnabled = false),
            baseUrl = "http://trrservice.agregatka.ru:8777",
        )
        val result = api.canUpload("test-token", "0000549041")
        val success = assertIs<Resource.Success<ImageMediatorCanUploadResponse>>(result)

        assertEquals("Bearer test-token", authHeader)
        assertTrue(success.data.allowed)
        assertTrue(success.data.folderFound)
        assertEquals("0000549041", success.data.documentNumber)
    }

    @Test
    fun `uploadPhotos sends multipart with files field`() = runTest {
        var authHeader: String? = null
        val engine = MockEngine { request ->
            authHeader = request.headers[HttpHeaders.Authorization]
            assertEquals(HttpMethod.Post, request.method)
            assertTrue(request.url.encodedPath.endsWith("/api/v1/uploads/photos"))
            respond(
                content = """
                    {
                      "document_number": "0000549041",
                      "resolved_year": 2026,
                      "resolved_month": 7,
                      "ftp_folder_path": "/2026/7/0000549041",
                      "uploaded_files": [
                        {
                          "stored_filename": "photo.jpg",
                          "ftp_file_path": "/2026/7/0000549041/photo.jpg"
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val api = ImageMediatorApi(
            client = HttpClientFactory.create(engine = engine, loggingEnabled = false),
            baseUrl = "http://trrservice.agregatka.ru:8777",
        )
        val result = api.uploadPhotos(
            agrToken = "test-token",
            documentNumber = "0000549041",
            files = listOf(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())),
        )
        val success = assertIs<Resource.Success<ImageMediatorUploadResponse>>(result)

        assertEquals("Bearer test-token", authHeader)
        assertEquals("0000549041", success.data.documentNumber)
        assertEquals("photo.jpg", success.data.uploadedFiles.first().storedFilename)
    }

    @Test
    fun `uploadPhotos maps http 401 to error`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"detail":"Unauthorized"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val api = ImageMediatorApi(
            client = HttpClientFactory.create(engine = engine, loggingEnabled = false),
            baseUrl = "http://trrservice.agregatka.ru:8777",
        )
        val result = api.uploadPhotos("bad-token", "0000549041", listOf(byteArrayOf(1, 2, 3)))
        val error = assertIs<Resource.Error<ImageMediatorUploadResponse>>(result)

        assertContains(error.causes.orEmpty(), "Войдите заново")
    }
}
