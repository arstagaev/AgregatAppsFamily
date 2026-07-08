package com.tagaev.trrcrm.data.remote

import com.tagaev.trrcrm.models.ImageMediatorCanUploadResponse
import com.tagaev.trrcrm.models.ImageMediatorImageListResponse
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

    @Test
    fun `getDocumentPhotoCount sends bearer token and parses count`() = runTest {
        var authHeader: String? = null
        val engine = MockEngine { request ->
            authHeader = request.headers[HttpHeaders.Authorization]
            assertEquals(HttpMethod.Get, request.method)
            assertTrue(request.url.encodedPath.endsWith("/api/v1/documents/0000549041/images/count"))
            respond(
                content = """
                    {
                      "document_number": "0000549041",
                      "resolved_year": 2026,
                      "resolved_month": 6,
                      "count": 27
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
        val result = api.getDocumentPhotoCount("test-token", "0000549041")
        val success = assertIs<Resource.Success<Int>>(result)

        assertEquals("Bearer test-token", authHeader)
        assertEquals(27, success.data)
    }

    @Test
    fun `listDocumentImages parses pagination metadata`() = runTest {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertTrue(request.url.encodedPath.endsWith("/api/v1/documents/0000549041/images"))
            assertEquals("1", request.url.parameters["page"])
            respond(
                content = """
                    {
                      "document_number": "0000549041",
                      "resolved_year": 2026,
                      "resolved_month": 6,
                      "page": 1,
                      "page_size": 10,
                      "total_count": 27,
                      "total_pages": 3,
                      "has_next": true,
                      "has_previous": false,
                      "images": [
                        {
                          "image_id": "img_a83fd912e2c4f719",
                          "content_url": "/api/v1/images/img_a83fd912e2c4f719/content",
                          "size_bytes": 2488120
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
        val result = api.listDocumentImages("test-token", "0000549041", page = 1)
        val success = assertIs<Resource.Success<ImageMediatorImageListResponse>>(result)

        assertEquals(1, success.data.page)
        assertEquals(3, success.data.totalPages)
        assertEquals("/api/v1/images/img_a83fd912e2c4f719/content", success.data.images.first().contentUrl)
    }

    @Test
    fun `downloadImageContent returns raw bytes`() = runTest {
        var authHeader: String? = null
        val imageBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        val engine = MockEngine { request ->
            authHeader = request.headers[HttpHeaders.Authorization]
            assertEquals(HttpMethod.Get, request.method)
            assertTrue(request.url.encodedPath.endsWith("/api/v1/images/img_test/content"))
            respond(
                content = imageBytes,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "image/jpeg"),
            )
        }

        val api = ImageMediatorApi(
            client = HttpClientFactory.create(engine = engine, loggingEnabled = false),
            baseUrl = "http://trrservice.agregatka.ru:8777",
        )
        val result = api.downloadImageContent(
            agrToken = "test-token",
            contentUrl = "/api/v1/images/img_test/content",
        )
        val success = assertIs<Resource.Success<ByteArray>>(result)

        assertEquals("Bearer test-token", authHeader)
        assertTrue(success.data.contentEquals(imageBytes))
    }

    @Test
    fun `getDocumentPhotoCount maps http 401 to error`() = runTest {
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
        val result = api.getDocumentPhotoCount("bad-token", "0000549041")
        val error = assertIs<Resource.Error<Int>>(result)

        assertContains(error.causes.orEmpty(), "Войдите заново")
    }
}
