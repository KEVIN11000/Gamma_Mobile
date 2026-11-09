package com.gamma.mobile.data.remote

import com.gamma.mobile.utils.AppLogBuffer
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RateLimitAndPayloadGuardTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        AppLogBuffer.clear()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `test RateLimitInterceptor captures HTTP 429 with Retry-After and logs warning`() {
        var capturedRetryAfter: Long? = null
        val interceptor = RateLimitInterceptor { retrySeconds ->
            capturedRetryAfter = retrySeconds
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After", "45")
                .setBody("{\"error\": \"RATE_LIMIT_EXCEEDED\"}")
        )

        val request = Request.Builder()
            .url(mockWebServer.url("/api/v1/movimientos"))
            .get()
            .build()

        val response = client.newCall(request).execute()
        assertEquals(429, response.code)
        response.close()

        assertEquals(45L, capturedRetryAfter)
        val logs = AppLogBuffer.getLogs()
        assertTrue(logs.any { it.nivel == "WARN" && it.mensaje.contains("Retry-After: 45s") })
    }

    @Test
    fun `test RateLimitInterceptor handles HTTP 429 without Retry-After gracefully`() {
        var listenerCalled = false
        var capturedValue: Long? = 123L
        val interceptor = RateLimitInterceptor { retrySeconds ->
            listenerCalled = true
            capturedValue = retrySeconds
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("{\"error\": \"RATE_LIMIT_EXCEEDED\"}")
        )

        val request = Request.Builder()
            .url(mockWebServer.url("/api/v1/health"))
            .get()
            .build()

        val response = client.newCall(request).execute()
        assertEquals(429, response.code)
        response.close()

        assertTrue(listenerCalled)
        assertEquals(null, capturedValue)
        val logs = AppLogBuffer.getLogs()
        assertTrue(logs.any { it.nivel == "WARN" && it.mensaje.contains("Rate Limit detectado") })
    }

    @Test
    fun `test PayloadSizeGuardInterceptor allows request under 64KB`() {
        val interceptor = PayloadSizeGuardInterceptor(maxSizeBytes = 64 * 1024L)
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{\"status\": \"ok\"}"))

        val smallBody = ByteArray(10 * 1024) { 1 } // 10 KB
        val request = Request.Builder()
            .url(mockWebServer.url("/upload"))
            .post(RequestBody.create(null as MediaType?, smallBody))
            .build()

        val response = client.newCall(request).execute()
        assertEquals(200, response.code)
        response.close()

        val recorded = mockWebServer.takeRequest()
        assertEquals(10 * 1024L, recorded.bodySize)
    }

    @Test
    fun `test PayloadSizeGuardInterceptor aborts request over 64KB with HTTP 413`() {
        val interceptor = PayloadSizeGuardInterceptor(maxSizeBytes = 64 * 1024L)
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val oversizedBody = ByteArray(70 * 1024) { 1 } // 70 KB > 64 KB
        val request = Request.Builder()
            .url(mockWebServer.url("/upload"))
            .post(RequestBody.create(null as MediaType?, oversizedBody))
            .build()

        val response = client.newCall(request).execute()
        assertEquals(413, response.code)
        val bodyString = response.body?.string().orEmpty()
        assertTrue(bodyString.contains("PAYLOAD_TOO_LARGE"))
        response.close()

        // Verify request never reached MockWebServer
        assertEquals(0, mockWebServer.requestCount)

        val logs = AppLogBuffer.getLogs()
        assertTrue(logs.any { it.nivel == "ERROR" && it.mensaje.contains("HTTP 413 Preventivo") })
    }
}
