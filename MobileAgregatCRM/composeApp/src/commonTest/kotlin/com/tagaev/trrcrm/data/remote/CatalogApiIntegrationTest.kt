package com.tagaev.trrcrm.data.remote

import com.tagaev.trrcrm.models.CatalogSignupRequest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CatalogApiIntegrationTest {

    @Test
    fun `catalog products retries once after transient server error`() = runTest {
        var callCount = 0
        val engine = MockEngine { request ->
            callCount += 1
            if (request.url.encodedPath == "/catalog/products" && callCount == 1) {
                return@MockEngine respond(
                    content = """{"status":"error","error":{"code":"server_error","message":"temporary"}}""",
                    status = HttpStatusCode.InternalServerError,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            respond(
                content = """{"status":"ok","items":[{"id":"prd_9","name":"N","description":"D"}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val api = EventsApi(HttpClientFactory.create(engine = engine, loggingEnabled = false))
        val res = api.catalogProducts()

        assertIs<Resource.Success<*>>(res)
        assertEquals(2, callCount)
    }

    @Test
    fun `catalog products pagination progression uses requested pages`() = runTest {
        val requestedPages = mutableListOf<String?>()
        val requestedSearch = mutableListOf<String?>()
        val requestedBrands = mutableListOf<String?>()
        val requestedModels = mutableListOf<String?>()
        val requestedCategoryIds = mutableListOf<String?>()
        val engine = MockEngine { request ->
            requestedPages += request.url.parameters["page"]
            requestedSearch += request.url.parameters["search"]
            requestedBrands += request.url.parameters["brand"]
            requestedModels += request.url.parameters["model"]
            requestedCategoryIds += request.url.parameters["category_id"]
            val page = request.url.parameters["page"] ?: "1"
            respond(
                content = """{"status":"ok","items":[],"pagination":{"page":$page,"limit":20,"total":60,"total_pages":3,"has_next":${page != "3"},"has_prev":${page != "1"}}}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val api = EventsApi(HttpClientFactory.create(engine = engine, loggingEnabled = false))
        val first = api.catalogProducts(search = "09G", brand = "VW", model = "Aisin 09G", categoryId = "cat_09g", page = 1, limit = 20)
        val second = api.catalogProducts(search = "09G", brand = "VW", model = "Aisin 09G", categoryId = "cat_09g", page = 2, limit = 20)

        assertIs<Resource.Success<*>>(first)
        assertIs<Resource.Success<*>>(second)
        assertEquals(listOf<String?>("1", "2"), requestedPages)
        assertEquals(listOf<String?>("09G", "09G"), requestedSearch)
        assertEquals(listOf<String?>("VW", "VW"), requestedBrands)
        assertEquals(listOf<String?>("Aisin 09G", "Aisin 09G"), requestedModels)
        assertEquals(listOf<String?>("cat_09g", "cat_09g"), requestedCategoryIds)
    }

    @Test
    fun `catalog products timeout mapped to friendly message`() = runTest {
        val engine = MockEngine {
            throw Exception("connect timeout has expired")
        }

        val api = EventsApi(HttpClientFactory.create(engine = engine, loggingEnabled = false))
        val res = api.catalogProducts()

        val error = assertIs<Resource.Error<*>>(res)
        assertTrue(error.causes?.contains("ожидания", ignoreCase = true) == true)
    }

    @Test
    fun `catalog signup does not retry on server error`() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount += 1
            respond(
                content = """{"status":"error","error":{"code":"server_error","message":"boom"}}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val api = EventsApi(HttpClientFactory.create(engine = engine, loggingEnabled = false))
        val res = api.catalogSignupRequest(
            CatalogSignupRequest(
                name = "User",
                email = "u@example.com",
                source = "mobile_app"
            )
        )

        assertIs<Resource.Error<*>>(res)
        assertEquals(1, callCount)
    }
}
