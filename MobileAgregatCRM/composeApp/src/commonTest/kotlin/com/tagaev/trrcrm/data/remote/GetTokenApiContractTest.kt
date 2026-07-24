package com.tagaev.trrcrm.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import com.tagaev.trrcrm.models.GetTokenResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class GetTokenApiContractTest {

    @Test
    fun `getToken accepts a token object wrapped in a single-item array`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("gettoken", request.url.parameters["task"])
            respond(
                content = """[{"accessapp":"true","accessmoodle":"false","token":"token-123","username":"user","ДатаРождения":"","Должность":"","Подразделение":"","ФИО":"Иван Иванов"}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val result = EventsApi(HttpClientFactory.create(engine = engine, loggingEnabled = false))
            .getToken(ApiConfig(baseUrl = "https://example.test", token = ""), "user", "hash")

        val success = assertIs<Resource.Success<GetTokenResponse>>(result)
        assertEquals("token-123", success.data.token)
        assertEquals("Иван Иванов", success.data.fullName)
    }

    @Test
    fun `getToken exposes backend error from a single-item array`() = runTest {
        val engine = MockEngine {
            respond(
                content = """[{"error":"Password not correct"}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val result = EventsApi(HttpClientFactory.create(engine = engine, loggingEnabled = false))
            .getToken(ApiConfig(baseUrl = "https://example.test", token = ""), "user", "hash")

        val error = assertIs<Resource.Error<*>>(result)
        assertEquals("Password not correct", error.causes)
    }
}
