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

    @Test
    fun `test getBootstrap endpoint maps correctly via Retrofit`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "status": "ok",
                        "summary": {
                            "margen_libre": 2000000,
                            "saldo_acumulado": 1500000,
                            "posicion_iva": 0,
                            "es_debito_fiscal": false
                        },
                        "periodo_actual": "Noviembre 2026"
                    }
                """.trimIndent())
        )

        val result = safeApiCall(testDispatcher) { api.getBootstrap() }
        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals("ok", data.status)
        assertEquals(2000000L, data.summary?.margenLibreDisponible)
        assertEquals("Noviembre 2026", data.periodoActual)

        val request = mockWebServer.takeRequest()
        assertEquals("/bootstrap", request.path)
        assertEquals("GET", request.method)
    }

    @Test
    fun `test parseMovimientoNlp endpoint posts body and maps response`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "valido": true,
                        "monto": 85000,
                        "tipo_movimiento": "Gasto",
                        "categoria": "Supermercado",
                        "proveedor_cliente": "Stock"
                    }
                """.trimIndent())
        )

        val req = com.gamma.mobile.data.remote.dto.ParseMovementRequestDto("Gasto 85000 en Stock")
        val result = safeApiCall(testDispatcher) { api.parseMovimientoNlp(req) }
        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals(85000L, data.monto)
        assertEquals("Stock", data.proveedorCliente)

        val request = mockWebServer.takeRequest()
        assertEquals("/movimientos/parse", request.path)
        assertEquals("POST", request.method)
        assertTrue(request.body.readUtf8().contains("Gasto 85000 en Stock"))
    }

    @Test
    fun `test getClima and consultarRucSifen endpoints`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "temperatura_actual": 29.0,
                        "condicion": "Soleado",
                        "alerta_paraguas": { "activar": false, "probabilidad_lluvia": 10, "motivo": "Sin lluvia" }
                    }
                """.trimIndent())
        )
        val climaRes = safeApiCall(testDispatcher) { api.getClima() }
        assertTrue(climaRes is NetworkResult.Success)
        assertEquals(29.0, (climaRes as NetworkResult.Success).data.temperaturaActual, 0.1)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""
                    {
                        "ruc": "80001111-2",
                        "razon_social": "Petrobras Paraguay"
                    }
                """.trimIndent())
        )
        val sifenRes = safeApiCall(testDispatcher) { 
            api.consultarRucSifen(com.gamma.mobile.data.remote.dto.SifenRucRequestDto("80001111-2")) 
        }
        assertTrue(sifenRes is NetworkResult.Success)
        assertEquals("Petrobras Paraguay", (sifenRes as NetworkResult.Success).data.razonSocial)
    }
}
