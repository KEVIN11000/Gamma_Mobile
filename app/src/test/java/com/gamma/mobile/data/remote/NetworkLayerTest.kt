package com.gamma.mobile.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkLayerTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: GammaApi
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val interceptor = ApiKeyInterceptor { "test-api-key" }

        // Short timeouts for test
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GammaApi::class.java)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
        Dispatchers.resetMain()
    }

    @Test
    fun `test 200 OK`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("[]")
        )

        val result = safeApiCall(testDispatcher) { api.getMovimientos() }

        assertTrue(result is NetworkResult.Success)
        val request = mockWebServer.takeRequest()
        assertEquals("test-api-key", request.getHeader("X-API-KEY"))
    }

    @Test
    fun `test 401 Unauthorized`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\": {\"code\": \"UNAUTHORIZED\", \"message\": \"Invalid API Key\"}}")
        )

        val result = safeApiCall(testDispatcher) { api.getMovimientos() }

        assertTrue(result is NetworkResult.Error)
        val errorResult = result as NetworkResult.Error
        assertEquals(401, errorResult.code)
        assertEquals("Invalid API Key", errorResult.message)
    }

    @Test
    fun `test 400 Bad Request extracts descriptive business message`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody("{\"error\": {\"code\": \"NO_CLASE_EN_CURSO\", \"message\": \"No hay clase en curso\"}, \"mensaje\": \"No hay clase en curso\"}")
        )

        val result = safeApiCall(testDispatcher) { api.marcarMateria(com.gamma.mobile.domain.model.AsistenciaMateriaRequest("Base de Datos")) }

        assertTrue(result is NetworkResult.Error)
        val errorResult = result as NetworkResult.Error
        assertEquals(400, errorResult.code)
        assertEquals("No hay clase en curso", errorResult.message)
    }

    @Test
    fun `test 500 Internal Server Error`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\": {\"code\": \"SERVER_ERROR\", \"message\": \"Internal Error\"}}")
        )

        val result = safeApiCall(testDispatcher) { api.getMovimientos() }

        assertTrue(result is NetworkResult.Error)
        assertEquals(500, (result as NetworkResult.Error).code)
    }

    @Test
    fun `test Timeout`() = runTest {
        // SocketPolicy.NO_RESPONSE forces OkHttp to wait until timeout
        mockWebServer.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
        )

        val result = safeApiCall(testDispatcher) { api.getMovimientos() }

        assertTrue(result is NetworkResult.Exception)
        assertTrue((result as NetworkResult.Exception).e is SocketTimeoutException)
    }
}
