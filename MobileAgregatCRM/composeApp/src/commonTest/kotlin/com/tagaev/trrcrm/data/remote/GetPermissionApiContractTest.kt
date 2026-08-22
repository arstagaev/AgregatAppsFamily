package com.tagaev.trrcrm.data.remote

import com.tagaev.trrcrm.models.UserPermissionEntryDto
import com.tagaev.trrcrm.data.remote.isTokenAuthenticationError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class GetPermissionApiContractTest {

    @Test
    fun `getPermission exposes rejected token response for reauthentication`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("getpermission", request.url.parameters["task"])
            assertEquals("expired-token", request.url.parameters["token"])
            respond(
                content = """{"error":"Token authentification error","token":"expired-token"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

        val result = EventsApi(HttpClientFactory.create(engine = engine, loggingEnabled = false))
            .getPermission(ApiConfig(baseUrl = "https://example.test", token = "expired-token"))

        val error = assertIs<Resource.Error<List<UserPermissionEntryDto>>>(result)
        assertEquals("Token authentification error", error.causes)
        assertTrue(isTokenAuthenticationError(error.causes))
    }

    @Test
    fun `network failures are not mistaken for an expired token`() {
        assertTrue(!isTokenAuthenticationError("connect timeout has expired"))
    }
}
