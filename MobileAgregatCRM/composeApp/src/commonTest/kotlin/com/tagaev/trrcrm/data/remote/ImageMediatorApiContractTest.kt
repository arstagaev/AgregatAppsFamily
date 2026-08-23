package com.tagaev.trrcrm.data.remote

import com.tagaev.trrcrm.domain.ImageMediatorFileKind
import com.tagaev.trrcrm.domain.ImageMediatorUploadPart
import com.tagaev.trrcrm.domain.jpegUploadPart
import com.tagaev.trrcrm.models.DocumentUploadPeriod
import com.tagaev.trrcrm.models.ImageDocumentType
import com.tagaev.trrcrm.models.ImageMediatorCanUploadResponse
import com.tagaev.trrcrm.models.ImageMediatorImageListResponse
import com.tagaev.trrcrm.models.ImageMediatorUploadResponse
import com.tagaev.trrcrm.ui.i18n.AppLanguage
import com.tagaev.trrcrm.ui.i18n.AppLanguageHolder
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.test.runTest
import kotlinx.io.readByteArray
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageMediatorApiContractTest {

    @BeforeTest
    fun useRussian() {
        AppLanguageHolder.current = AppLanguage.Russian
    }

    private suspend fun OutgoingContent.asRequestText(): String = when (this) {
        is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
        is OutgoingContent.WriteChannelContent -> {
            val channel = ByteChannel(autoFlush = true)
            writeTo(channel)
            channel.readRemaining().readByteArray().decodeToString()
        }
        else -> error("Unsupported request content: ${this::class}")
    }

    private fun api(
        engine: MockEngine,
        contract: String? = ImageMediatorApi.IMAGE_MEDIATOR_API_CONTRACT,
    ) = ImageMediatorApi(
        client = HttpClientFactory.create(engine = engine, loggingEnabled = false),
        baseUrl = "http://trrservice.agregatka.ru:8777",
        apiContract = contract,
    )

    private fun jsonHeaders() = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    @Test
    fun `canUpload Complects 0000198950 sends full number year month and contract 1_7`() = runTest {
        var authHeader: String? = null
        var contractHeader: String? = null
        var idempotencyKey: String? = null
        var requestBody = ""
        val engine = MockEngine { request ->
            authHeader = request.headers[HttpHeaders.Authorization]
            contractHeader = request.headers[ImageMediatorApi.CONTRACT_HEADER]
            idempotencyKey = request.headers["Idempotency-Key"]
            requestBody = request.body.asRequestText()
            assertEquals(HttpMethod.Post, request.method)
            assertTrue(request.url.encodedPath.endsWith("/api/v1/uploads/can-upload"))
            respond(
                content = """
                    {
                      "allowed": true,
                      "api_version": "1.7",
                      "document_number": "0000198950",
                      "resolved_document_number": "0000198950",
                      "document_type": "Complects",
                      "resolved_year": 2025,
                      "resolved_month": 3,
                      "folder_found": true,
                      "folder_path": "/2025/3/0000198950",
                      "limits": {
                        "max_photos_per_document": 15,
                        "photos_in_folder": 0,
                        "remaining": 15,
                        "max_files_per_request": 10
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }

        val result = api(engine).canUpload(
            "test-token",
            "0000198950",
            DocumentUploadPeriod(2025, 3),
            ImageDocumentType.Complects,
        )
        val success = assertIs<Resource.Success<ImageMediatorCanUploadResponse>>(result)

        assertContains(requestBody, "\"document_number\":\"0000198950\"")
        assertContains(requestBody, "\"document_name\":\"Complects\"")
        assertContains(requestBody, "\"year\":2025")
        assertContains(requestBody, "\"month\":3")
        assertEquals("Bearer test-token", authHeader)
        assertEquals("1.7", contractHeader)
        assertTrue(!idempotencyKey.isNullOrBlank())
        assertTrue(success.data.allowed)
        assertEquals("0000198950", success.data.resolvedDocumentNumber)
        assertEquals(2025, success.data.resolvedYear)
        assertEquals(3, success.data.resolvedMonth)
    }

    @Test
    fun `uploadPhotos WorkOrder 0000560730 keeps full number and stable idempotency key on 503 retry`() = runTest {
        val seenKeys = mutableListOf<String?>()
        val seenBodies = mutableListOf<String>()
        var attempts = 0
        val engine = MockEngine { request ->
            attempts += 1
            seenKeys += request.headers["Idempotency-Key"]
            seenBodies += request.body.asRequestText()
            assertEquals("1.7", request.headers[ImageMediatorApi.CONTRACT_HEADER])
            if (attempts == 1) {
                respond(
                    content = """{"detail":{"code":"capacity","message":"busy"}}""",
                    status = HttpStatusCode.ServiceUnavailable,
                    headers = headersOf(
                        HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                        HttpHeaders.RetryAfter to listOf("1"),
                    ),
                )
            } else {
                respond(
                    content = """
                        {
                          "document_number": "0000560730",
                          "resolved_document_number": "0000560730",
                          "resolved_year": 2024,
                          "resolved_month": 11,
                          "ftp_folder_path": "/2024/11/0000560730",
                          "uploaded_files": [
                            { "stored_filename": "photo.jpg" }
                          ]
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders(),
                )
            }
        }

        val result = api(engine).uploadPhotos(
            agrToken = "test-token",
            documentNumber = "0000560730",
            files = listOf(jpegUploadPart(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))),
            uploadPeriod = DocumentUploadPeriod(2024, 11),
            documentType = ImageDocumentType.WorkOrder,
            idempotencyKey = "11111111-2222-3333-4444-555555555555",
        )
        val success = assertIs<Resource.Success<ImageMediatorUploadResponse>>(result)

        assertEquals(2, attempts)
        assertEquals(
            listOf<String?>(
                "11111111-2222-3333-4444-555555555555",
                "11111111-2222-3333-4444-555555555555",
            ),
            seenKeys,
        )
        seenBodies.forEach { body ->
            assertContains(body, "0000560730")
            assertContains(body, "WorkOrder")
            assertContains(body, "\r\n2024\r\n")
            assertContains(body, "\r\n11\r\n")
        }
        assertEquals("0000560730", success.data.resolvedDocumentNumber)
    }

    @Test
    fun `canUpload ТСК0000777 keeps cyrillic prefix`() = runTest {
        var requestBody = ""
        val engine = MockEngine { request ->
            requestBody = request.body.asRequestText()
            respond(
                content = """
                    {
                      "allowed": true,
                      "document_number": "ТСК0000777",
                      "resolved_document_number": "ТСК0000777",
                      "folder_found": true,
                      "limits": { "remaining": 10, "max_photos_per_document": 15 }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }

        val result = api(engine).canUpload(
            "token",
            "ТСК0000777",
            DocumentUploadPeriod(2026, 1),
            ImageDocumentType.Complects,
        )
        assertIs<Resource.Success<ImageMediatorCanUploadResponse>>(result)
        assertContains(requestBody, "ТСК0000777")
        assertTrue("0000777\"" !in requestBody.replace("ТСК0000777", ""))
    }

    @Test
    fun `viewer period-aware count encodes cyrillic АР0000337 as own path segment`() = runTest {
        val encoded = ImageMediatorApi.encodeDocumentNumberPathSegment("АР0000337")
        assertEquals(
            ImageMediatorApi.encodeDocumentNumberPathSegment("АР0000337"),
            encoded,
        )
        assertTrue(encoded.contains("%D0%90") || encoded.contains("%d0%90"))
        assertTrue(encoded.contains("%D0%A0") || encoded.contains("%d0%a0"))
        assertTrue(encoded.endsWith("0000337") || encoded.contains("0000337"))
        assertTrue("/" !in encoded)

        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("1.7", request.headers[ImageMediatorApi.CONTRACT_HEADER])
            assertEquals(
                "/api/v1/documents/2023/8/${encoded}/images/count",
                request.url.encodedPath,
            )
            assertEquals("Complects", request.url.parameters["document_name"])
            respond(
                content = """{"document_number":"АР0000337","resolved_year":2023,"resolved_month":8,"count":0}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }

        val result = api(engine).getDocumentPhotoCount(
            "token",
            "АР0000337",
            DocumentUploadPeriod(2023, 8),
            ImageDocumentType.Complects,
        )
        val success = assertIs<Resource.Success<Int>>(result)
        assertEquals(0, success.data)
    }

    @Test
    fun `viewer list uses year month document_number path and content_url from server`() = runTest {
        val engine = MockEngine { request ->
            assertEquals(
                "/api/v1/documents/2025/3/0000198950/images",
                request.url.encodedPath,
            )
            assertEquals("1", request.url.parameters["page"])
            assertEquals("Complects", request.url.parameters["document_name"])
            respond(
                content = """
                    {
                      "document_number": "0000198950",
                      "resolved_document_number": "0000198950",
                      "resolved_year": 2025,
                      "resolved_month": 3,
                      "page": 1,
                      "page_size": 10,
                      "total_count": 1,
                      "total_pages": 1,
                      "has_next": false,
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
                headers = jsonHeaders(),
            )
        }

        val result = api(engine).listDocumentImages(
            agrToken = "test-token",
            documentNumber = "0000198950",
            uploadPeriod = DocumentUploadPeriod(2025, 3),
            page = 1,
            documentType = ImageDocumentType.Complects,
        )
        val success = assertIs<Resource.Success<ImageMediatorImageListResponse>>(result)
        assertEquals("/api/v1/images/img_a83fd912e2c4f719/content", success.data.images.first().contentUrl)
        assertEquals("0000198950", success.data.resolvedDocumentNumber)
    }

    @Test
    fun `viewer list parses original_filename and stored_filename`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "document_number": "0000198950",
                      "page": 1,
                      "page_size": 10,
                      "total_count": 1,
                      "total_pages": 1,
                      "has_next": false,
                      "has_previous": false,
                      "images": [
                        {
                          "image_id": "img_file",
                          "content_url": "/api/v1/images/img_file/content",
                          "size_bytes": 1200,
                          "original_filename": "Акт.pdf",
                          "stored_filename": "act.pdf"
                        }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }
        val result = api(engine).listDocumentImages(
            agrToken = "token",
            documentNumber = "0000198950",
            uploadPeriod = DocumentUploadPeriod(2025, 3),
            page = 1,
        )
        val success = assertIs<Resource.Success<ImageMediatorImageListResponse>>(result)
        val image = success.data.images.single()
        assertEquals("Акт.pdf", image.originalFilename)
        assertEquals("act.pdf", image.storedFilename)
    }

    @Test
    fun `cyrillic percent-encoding for ТСК0000777 list path`() = runTest {
        val encoded = ImageMediatorApi.encodeDocumentNumberPathSegment("ТСК0000777")
        val engine = MockEngine { request ->
            assertEquals(
                "/api/v1/documents/2026/2/$encoded/images",
                request.url.encodedPath,
            )
            respond(
                content = """
                    {
                      "document_number": "ТСК0000777",
                      "page": 1,
                      "page_size": 10,
                      "total_count": 0,
                      "total_pages": 0,
                      "has_next": false,
                      "has_previous": false,
                      "images": []
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }

        val result = api(engine).listDocumentImages(
            "token",
            "ТСК0000777",
            DocumentUploadPeriod(2026, 2),
            page = 1,
        )
        assertIs<Resource.Success<ImageMediatorImageListResponse>>(result)
        assertTrue(encoded.startsWith("%"))
        assertTrue("ТСК" !in encoded)
    }

    @Test
    fun `missing contract header is accepted as legacy by mock`() = runTest {
        var contractHeader: String? = "sentinel"
        val engine = MockEngine { request ->
            contractHeader = request.headers[ImageMediatorApi.CONTRACT_HEADER]
            assertTrue(contractHeader.isNullOrBlank())
            respond(
                content = """
                    {
                      "allowed": true,
                      "document_number": "0000198950",
                      "folder_found": true,
                      "limits": { "remaining": 5 }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }

        val result = api(engine, contract = null).canUpload(
            "token",
            "0000198950",
            DocumentUploadPeriod(2025, 3),
        )
        assertIs<Resource.Success<ImageMediatorCanUploadResponse>>(result)
        assertNull(contractHeader)
    }

    @Test
    fun `header 1_7 is sent on count and list`() = runTest {
        val headers = mutableListOf<String?>()
        val engine = MockEngine { request ->
            headers += request.headers[ImageMediatorApi.CONTRACT_HEADER]
            when {
                request.url.encodedPath.endsWith("/images/count") -> respond(
                    content = """{"document_number":"0000560730","count":2}""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders(),
                )
                else -> respond(
                    content = """
                        {
                          "document_number": "0000560730",
                          "page": 1,
                          "page_size": 10,
                          "total_count": 0,
                          "total_pages": 0,
                          "has_next": false,
                          "has_previous": false,
                          "images": []
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders(),
                )
            }
        }

        val client = api(engine, contract = "1.7")
        val period = DocumentUploadPeriod(2024, 11)
        assertIs<Resource.Success<Int>>(
            client.getDocumentPhotoCount("t", "0000560730", period, ImageDocumentType.WorkOrder),
        )
        assertIs<Resource.Success<ImageMediatorImageListResponse>>(
            client.listDocumentImages("t", "0000560730", period, page = 1, ImageDocumentType.WorkOrder),
        )
        assertEquals(listOf<String?>("1.7", "1.7"), headers)
    }

    @Test
    fun `unknown contract maps to 400 unsupported_api_contract`() = runTest {
        val engine = MockEngine { request ->
            val sent = request.headers[ImageMediatorApi.CONTRACT_HEADER]
            if (sent != "1.7" && !sent.isNullOrBlank()) {
                respond(
                    content = """{"detail":{"code":"unsupported_api_contract","message":"unsupported"}}""",
                    status = HttpStatusCode.BadRequest,
                    headers = jsonHeaders(),
                )
            } else {
                error("expected unknown contract")
            }
        }

        val result = api(engine, contract = "9.9").canUpload(
            "token",
            "0000198950",
            DocumentUploadPeriod(2025, 3),
        )
        val error = assertIs<Resource.Error<ImageMediatorCanUploadResponse>>(result)
        assertContains(error.causes.orEmpty(), "несовместим")
        val exception = assertIs<ImageMediatorException>(error.exception)
        assertEquals(400, exception.statusCode)
        assertEquals("unsupported_api_contract", exception.errorCode)
    }

    @Test
    fun `uploadPhotos maps http 401 to error without treating as missing photos`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"detail":"Unauthorized"}""",
                status = HttpStatusCode.Unauthorized,
                headers = jsonHeaders(),
            )
        }

        val result = api(engine).uploadPhotos(
            "bad-token",
            "0000198950",
            listOf(jpegUploadPart(byteArrayOf(1, 2, 3))),
            DocumentUploadPeriod(2025, 3),
            idempotencyKey = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
        )
        val error = assertIs<Resource.Error<ImageMediatorUploadResponse>>(result)
        assertContains(error.causes.orEmpty(), "Войдите заново")
    }

    @Test
    fun `getDocumentPhotoCount 404 before folder exists is count 0`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"detail":"Not Found"}""",
                status = HttpStatusCode.NotFound,
                headers = jsonHeaders(),
            )
        }

        val result = api(engine).getDocumentPhotoCount(
            "token",
            "0000198950",
            DocumentUploadPeriod(2025, 3),
        )
        val success = assertIs<Resource.Success<Int>>(result)
        assertEquals(0, success.data)
    }

    @Test
    fun `409 ambiguous_document_folder is mapped distinctly`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"detail":{"code":"ambiguous_document_folder"}}""",
                status = HttpStatusCode.Conflict,
                headers = jsonHeaders(),
            )
        }
        val result = api(engine).canUpload("t", "0000198950", DocumentUploadPeriod(2025, 3))
        val error = assertIs<Resource.Error<ImageMediatorCanUploadResponse>>(result)
        assertContains(error.causes.orEmpty(), "полный номер")
    }

    @Test
    fun `downloadImageContent uses server content_url`() = runTest {
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

        val result = api(engine).downloadImageContent(
            agrToken = "test-token",
            contentUrl = "/api/v1/images/img_test/content",
        )
        val success = assertIs<Resource.Success<ByteArray>>(result)
        assertEquals("Bearer test-token", authHeader)
        assertTrue(success.data.contentEquals(imageBytes))
    }

    @Test
    fun `downloadImageContent 200 html is error and not treated as file`() = runTest {
        val html = "<!DOCTYPE html><html><body>login</body></html>"
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertTrue(request.url.encodedPath.endsWith("/api/v1/images/img_html/content"))
            respond(
                content = html,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html; charset=utf-8"),
            )
        }

        val result = api(engine).downloadImageContent(
            agrToken = "test-token",
            contentUrl = "/api/v1/images/img_html/content",
        )
        val error = assertIs<Resource.Error<ByteArray>>(result)
        val exception = assertIs<ImageMediatorException>(error.exception)
        assertEquals(200, exception.statusCode)
        assertEquals(ImageMediatorApi.UNEXPECTED_HTML_OR_JSON, exception.errorCode)
        assertContains(error.causes.orEmpty(), "файл")
    }

    @Test
    fun `downloadImageContent 200 json body is error even if octet-stream`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"detail":"not a file"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/octet-stream"),
            )
        }
        val result = api(engine).downloadImageContent(
            agrToken = "token",
            contentUrl = "/api/v1/images/img_json/content",
        )
        val error = assertIs<Resource.Error<ByteArray>>(result)
        val exception = assertIs<ImageMediatorException>(error.exception)
        assertEquals(ImageMediatorApi.UNEXPECTED_HTML_OR_JSON, exception.errorCode)
    }

    @Test
    fun `downloadImageContent 200 pdf is success`() = runTest {
        val pdf = "%PDF-1.4 test-bytes".encodeToByteArray()
        val engine = MockEngine {
            respond(
                content = pdf,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/pdf"),
            )
        }
        val result = api(engine).downloadImageContent(
            agrToken = "token",
            contentUrl = "/api/v1/images/img_pdf/content",
        )
        val success = assertIs<Resource.Success<ByteArray>>(result)
        assertTrue(success.data.contentEquals(pdf))
    }

    @Test
    fun `WorkOrder document_name on period-aware count and list`() = runTest {
        val seenNames = mutableListOf<String?>()
        val engine = MockEngine { request ->
            seenNames += request.url.parameters["document_name"]
            assertTrue(request.url.encodedPath.contains("/documents/2024/11/0000560730/"))
            when {
                request.url.encodedPath.endsWith("/images/count") -> respond(
                    content = """{"document_number":"0000560730","count":2}""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders(),
                )
                else -> respond(
                    content = """
                        {
                          "document_number": "0000560730",
                          "page": 1,
                          "page_size": 10,
                          "total_count": 0,
                          "total_pages": 0,
                          "has_next": false,
                          "has_previous": false,
                          "images": []
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders(),
                )
            }
        }
        val type = ImageDocumentType.WorkOrder
        val period = DocumentUploadPeriod(2024, 11)
        val countResult = api(engine).getDocumentPhotoCount("t", "0000560730", period, type)
        val listResult = api(engine).listDocumentImages("t", "0000560730", period, page = 1, type)
        assertIs<Resource.Success<Int>>(countResult)
        assertIs<Resource.Success<ImageMediatorImageListResponse>>(listResult)
        assertEquals(listOf<String?>("WorkOrder", "WorkOrder"), seenNames)
    }

    @Test
    fun `uploadPhotos pdf keeps mime filename not photo_0_jpg`() = runTest {
        var requestBody = ""
        val engine = MockEngine { request ->
            requestBody = request.body.asRequestText()
            respond(
                content = """
                    {
                      "document_number": "0000198950",
                      "uploaded_files": [
                        { "stored_filename": "act.pdf", "mime_type": "application/pdf" }
                      ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }
        val pdf = ImageMediatorUploadPart(
            bytes = "%PDF-1.4 test".encodeToByteArray(),
            fileName = "act.pdf",
            mimeType = "application/pdf",
            kind = ImageMediatorFileKind.Document,
        )
        val result = api(engine).uploadPhotos(
            agrToken = "token",
            documentNumber = "0000198950",
            files = listOf(pdf),
            uploadPeriod = DocumentUploadPeriod(2025, 3),
            documentType = ImageDocumentType.Event,
            idempotencyKey = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
        )
        assertIs<Resource.Success<ImageMediatorUploadResponse>>(result)
        assertContains(requestBody, "application/pdf")
        assertContains(requestBody, "act.pdf")
        assertContains(requestBody, "Event")
        assertTrue("photo_0.jpg" !in requestBody)
    }

    @Test
    fun `canUpload sends all canonical document_name values`() = runTest {
        val seen = mutableListOf<String>()
        val engine = MockEngine { request ->
            seen += request.body.asRequestText()
            respond(
                content = """{"allowed":true,"document_number":"0000198950","folder_found":true,"limits":{"remaining":5}}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }
        val client = api(engine)
        val period = DocumentUploadPeriod(2025, 3)
        ImageDocumentType.entries.forEach { type ->
            assertIs<Resource.Success<ImageMediatorCanUploadResponse>>(
                client.canUpload("t", "0000198950", period, type),
            )
        }
        assertEquals(5, seen.size)
        ImageDocumentType.entries.forEach { type ->
            assertTrue(seen.any { it.contains("\"document_name\":\"${type.wireName}\"") }, type.wireName)
        }
    }

    @Test
    fun `canUpload reuses Idempotency-Key on 503 retry`() = runTest {
        val keys = mutableListOf<String?>()
        var attempts = 0
        val engine = MockEngine { request ->
            attempts += 1
            keys += request.headers["Idempotency-Key"]
            if (attempts == 1) {
                respond(
                    content = """{"detail":"busy"}""",
                    status = HttpStatusCode.ServiceUnavailable,
                    headers = headersOf(
                        HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                        HttpHeaders.RetryAfter to listOf("1"),
                    ),
                )
            } else {
                respond(
                    content = """{"allowed":true,"document_number":"0000198950","folder_found":true,"limits":{"remaining":4}}""",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders(),
                )
            }
        }
        assertIs<Resource.Success<ImageMediatorCanUploadResponse>>(
            api(engine).canUpload("t", "0000198950", DocumentUploadPeriod(2025, 3)),
        )
        assertEquals(2, attempts)
        assertEquals(keys[0], keys[1])
        assertTrue(!keys[0].isNullOrBlank())
    }

    @Test
    fun `429 is not retried`() = runTest {
        var attempts = 0
        val engine = MockEngine {
            attempts += 1
            respond(
                content = """{"detail":"rate"}""",
                status = HttpStatusCode.TooManyRequests,
                headers = jsonHeaders(),
            )
        }
        val error = assertIs<Resource.Error<ImageMediatorCanUploadResponse>>(
            api(engine).canUpload("t", "0000198950", DocumentUploadPeriod(2025, 3)),
        )
        assertEquals(1, attempts)
        assertContains(error.causes.orEmpty(), "лимит")
    }

    @Test
    fun `409 legacy_document_identity_incomplete is mapped distinctly`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"detail":{"code":"legacy_document_identity_incomplete"}}""",
                status = HttpStatusCode.Conflict,
                headers = jsonHeaders(),
            )
        }
        val error = assertIs<Resource.Error<ImageMediatorCanUploadResponse>>(
            api(engine).canUpload("t", "0000198950", DocumentUploadPeriod(2025, 3)),
        )
        assertContains(error.causes.orEmpty(), "Недостаточно")
    }

    @Test
    fun `png upload uses image png mime and filename`() = runTest {
        var requestBody = ""
        val engine = MockEngine { request ->
            requestBody = request.body.asRequestText()
            respond(
                content = """{"document_number":"АР0000337","uploaded_files":[{"stored_filename":"scan.png"}]}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders(),
            )
        }
        val pngHeader = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3,
        )
        val result = api(engine).uploadPhotos(
            "t",
            "АР0000337",
            listOf(
                ImageMediatorUploadPart(
                    bytes = pngHeader,
                    fileName = "scan.png",
                    mimeType = "image/png",
                    kind = ImageMediatorFileKind.Image,
                ),
            ),
            DocumentUploadPeriod(2023, 8),
            ImageDocumentType.InnerOrder,
            idempotencyKey = "cccccccc-cccc-cccc-cccc-cccccccccccc",
        )
        assertIs<Resource.Success<ImageMediatorUploadResponse>>(result)
        assertContains(requestBody, "image/png")
        assertContains(requestBody, "scan.png")
        assertContains(requestBody, "InnerOrder")
        assertContains(requestBody, "АР0000337")
    }
}
