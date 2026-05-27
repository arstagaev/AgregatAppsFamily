package com.tagaev.trrcrm.data.remote

import com.tagaev.trrcrm.models.CatalogProductRequest
import com.tagaev.trrcrm.models.CatalogProductRequestContact
import com.tagaev.trrcrm.models.CatalogProductRequestItem
import com.tagaev.trrcrm.models.CatalogSignupRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.parametersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class CatalogApiContractTest {

    @Test
    fun `catalog products success parsing`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "status":"ok",
                      "items":[
                        {
                          "id":"prd_1",
                          "name":"A",
                          "description":"D",
                          "image_url":"https://example.com/p1.jpg",
                          "is_active":true,
                          "brand":"VW",
                          "model":"09G",
                          "category_id":"cat_1",
                          "parent_category_id":"parent_1"
                        }
                      ],
                      "pagination":{
                        "page":1,
                        "limit":20,
                        "total":1,
                        "total_pages":1,
                        "has_next":false,
                        "has_prev":false
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val api = EventsApi(HttpClientFactory.create(engine = engine, loggingEnabled = false))
        val res = api.catalogProducts()
        val success = assertIs<Resource.Success<*>>(res)
        val payload = success.data as com.tagaev.trrcrm.models.CatalogProductsResponse
        assertEquals(1, payload.items.size)
        assertEquals("prd_1", payload.items.first().id)
        assertEquals("VW", payload.items.first().brand)
        assertEquals("09G", payload.items.first().model)
        assertEquals("cat_1", payload.items.first().categoryId)
        assertEquals(false, payload.pagination?.hasNext)
        assertEquals(1, payload.pagination?.page)
    }

    @Test
    fun `catalog products sends full filter and paging query params`() = runTest {
        var captured = parametersOf()
        val engine = MockEngine { request ->
            captured = request.url.parameters
            respond(
                content = """{"status":"ok","items":[],"pagination":{"page":2,"limit":15,"total":0,"total_pages":0,"has_next":false,"has_prev":true}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val api = EventsApi(HttpClientFactory.create(engine = engine, loggingEnabled = false))
        val res = api.catalogProducts(
            search = "09G",
            brand = "VW",
            model = "Aisin 09G",
            categoryId = "cat_09g",
            page = 2,
            limit = 15
        )
        assertIs<Resource.Success<*>>(res)
        assertEquals("09G", captured["search"])
        assertEquals("VW", captured["brand"])
        assertEquals("Aisin 09G", captured["model"])
        assertEquals("cat_09g", captured["category_id"])
        assertEquals("2", captured["page"])
        assertEquals("15", captured["limit"])
    }

    @Test
    fun `catalog signup maps 422 field errors`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "status":"error",
                      "error":{
                        "code":"validation_error",
                        "message":"Validation failed",
                        "fields":[
                          {"field":"email","message":"Invalid email format"}
                        ]
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.UnprocessableEntity,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val api = EventsApi(HttpClientFactory.create(engine = engine, loggingEnabled = false))
        val res = api.catalogSignupRequest(
            CatalogSignupRequest(
                name = "Test",
                phone = null,
                email = "bad-email",
                source = "mobile_app"
            )
        )

        val error = assertIs<Resource.Error<*>>(res)
        val coreEx = assertIs<CoreApiException>(error.exception)
        assertEquals(422, coreEx.statusCode)
        assertEquals("validation_error", coreEx.errorCode)
        assertEquals("email", coreEx.fields.firstOrNull()?.field)
        assertEquals("Invalid email format", coreEx.fields.firstOrNull()?.message)
    }

    @Test
    fun `catalog signup attaches idempotency key header`() = runTest {
        var capturedHeader: String? = null
        val engine = MockEngine { request ->
            if (request.url.encodedPath == "/catalog/signup-request") {
                capturedHeader = request.headers["Idempotency-Key"]
            }
            respond(
                content = """{"status":"ok","request_id":"r1"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val api = EventsApi(HttpClientFactory.create(engine = engine, loggingEnabled = false))
        val res = api.catalogSignupRequest(
            CatalogSignupRequest(
                name = "Test",
                phone = "+79998887766",
                email = "test@company.tld",
                source = "mobile_app"
            )
        )

        assertIs<Resource.Success<*>>(res)
        assertNotNull(capturedHeader)
        assertTrue(capturedHeader!!.length >= 30)
    }

    @Test
    fun `catalog product request attaches idempotency key header`() = runTest {
        var capturedHeader: String? = null
        val engine = MockEngine { request ->
            if (request.url.encodedPath == "/catalog/product-request") {
                capturedHeader = request.headers["Idempotency-Key"]
            }
            respond(
                content = """{"status":"ok","request_id":"r2"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val api = EventsApi(HttpClientFactory.create(engine = engine, loggingEnabled = false))
        val res = api.catalogProductRequest(
            CatalogProductRequest(
                contact = CatalogProductRequestContact(
                    name = "Test",
                    email = "test@company.tld"
                ),
                items = listOf(
                    CatalogProductRequestItem(
                        productId = "prd_1",
                        productName = "A",
                        qty = 1
                    )
                ),
                source = "mobile_app"
            )
        )

        assertIs<Resource.Success<*>>(res)
        assertNotNull(capturedHeader)
        assertTrue(capturedHeader!!.length >= 30)
    }

    @Test
    fun `catalog product request maps 422 when source is missing`() = runTest {
        val engine = MockEngine {
            respond(
                content = """
                    {
                      "status":"error",
                      "error":{
                        "code":"validation_error",
                        "message":"Validation failed",
                        "fields":[
                          {"field":"source","message":"Field required"}
                        ]
                      }
                    }
                """.trimIndent(),
                status = HttpStatusCode.UnprocessableEntity,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val api = EventsApi(HttpClientFactory.create(engine = engine, loggingEnabled = false))
        val res = api.catalogProductRequest(
            CatalogProductRequest(
                contact = CatalogProductRequestContact(
                    name = "User",
                    email = "u@example.com"
                ),
                items = listOf(CatalogProductRequestItem(productId = "p1", productName = "P1", qty = 1)),
                source = ""
            )
        )
        val error = assertIs<Resource.Error<*>>(res)
        val coreEx = assertIs<CoreApiException>(error.exception)
        assertEquals(422, coreEx.statusCode)
        assertEquals("validation_error", coreEx.errorCode)
        assertEquals("source", coreEx.fields.firstOrNull()?.field)
    }
}
