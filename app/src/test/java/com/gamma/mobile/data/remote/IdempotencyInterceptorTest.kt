package com.gamma.mobile.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class IdempotencyInterceptorTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `test POST request injects generated X-Client-Id header when absent`() {
        val fixedUuid = "uuid-test-12345"
        val interceptor = IdempotencyInterceptor(uuidGenerator = { fixedUuid })
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .post("test-body".toRequestBody(null))
            .build()

        val response = client.newCall(request).execute()
        response.close()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals(fixedUuid, recordedRequest.getHeader(IdempotencyInterceptor.HEADER_CLIENT_ID))
    }

    @Test
    fun `test POST request preserves existing X-Client-Id header`() {
        val customClientId = "custom-client-id-999"
        val interceptor = IdempotencyInterceptor(uuidGenerator = { "should-not-be-used" })
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .header(IdempotencyInterceptor.HEADER_CLIENT_ID, customClientId)
            .post("test-body".toRequestBody(null))
            .build()

        val response = client.newCall(request).execute()
        response.close()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals(customClientId, recordedRequest.getHeader(IdempotencyInterceptor.HEADER_CLIENT_ID))
    }

    @Test
    fun `test PUT, PATCH and DELETE requests inject X-Client-Id header`() {
        var counter = 0
        val interceptor = IdempotencyInterceptor(uuidGenerator = { "id-${++counter}" })
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        // Test PUT
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val putReq = Request.Builder()
            .url(mockWebServer.url("/put"))
            .put("put-body".toRequestBody(null))
            .build()
        client.newCall(putReq).execute().close()
        assertEquals("id-1", mockWebServer.takeRequest().getHeader(IdempotencyInterceptor.HEADER_CLIENT_ID))

        // Test PATCH
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val patchReq = Request.Builder()
            .url(mockWebServer.url("/patch"))
            .patch("patch-body".toRequestBody(null))
            .build()
        client.newCall(patchReq).execute().close()
        assertEquals("id-2", mockWebServer.takeRequest().getHeader(IdempotencyInterceptor.HEADER_CLIENT_ID))

        // Test DELETE
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        val deleteReq = Request.Builder()
            .url(mockWebServer.url("/delete"))
            .delete()
            .build()
        client.newCall(deleteReq).execute().close()
        assertEquals("id-3", mockWebServer.takeRequest().getHeader(IdempotencyInterceptor.HEADER_CLIENT_ID))
    }

    @Test
    fun `test GET request does not inject X-Client-Id header`() {
        val interceptor = IdempotencyInterceptor(uuidGenerator = { "never-injected" })
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/get"))
            .get()
            .build()

        val response = client.newCall(request).execute()
        response.close()

        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader(IdempotencyInterceptor.HEADER_CLIENT_ID))
    }
}
