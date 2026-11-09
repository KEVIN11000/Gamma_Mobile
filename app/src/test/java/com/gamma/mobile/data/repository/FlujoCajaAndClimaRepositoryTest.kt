package com.gamma.mobile.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gamma.mobile.data.local.GammaDatabase
import com.gamma.mobile.data.local.dao.ClimaDao
import com.gamma.mobile.data.local.dao.FlujoCajaDao
import com.gamma.mobile.data.remote.BaseFakeGammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.ClimaAlertaParaguasDto
import com.gamma.mobile.data.remote.dto.ClimaResponseDto
import com.gamma.mobile.data.remote.dto.DiaCriticoDto
import com.gamma.mobile.data.remote.dto.FlujoCajaItemDiarioDto
import com.gamma.mobile.data.remote.dto.FlujoCajaResponseDto
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FlujoCajaAndClimaRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: GammaDatabase
    private lateinit var flujoCajaDao: FlujoCajaDao
    private lateinit var climaDao: ClimaDao
    private lateinit var fakeApi: FakeFlujoClimaApi
    private lateinit var flujoCajaRepository: FlujoCajaRepositoryImpl
    private lateinit var climaRepository: ClimaRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()
    private val gson = Gson()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, GammaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        flujoCajaDao = db.flujoCajaDao()
        climaDao = db.climaDao()
        fakeApi = FakeFlujoClimaApi()

        flujoCajaRepository = FlujoCajaRepositoryImpl(
            api = fakeApi,
            dao = flujoCajaDao,
            gson = gson,
            dispatcher = testDispatcher
        )

        climaRepository = ClimaRepositoryImpl(
            api = fakeApi,
            dao = climaDao,
            dispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test refreshFlujoCaja success inserts in Room and returns domain model`() = runTest(testDispatcher) {
        val dto = FlujoCajaResponseDto(
            saldoActual = 5000000L,
            saldoProyectado30d = 3200000L,
            totalIngresosProyectados = 8000000L,
            totalGastosFijos = 4000000L,
            totalGastosVariablesEstimados = 5800000L,
            diasCriticos = listOf(
                DiaCriticoDto(fecha = "2026-11-25", saldo = -250000L, motivo = "Pago Préstamo")
            ),
            dias = listOf(
                FlujoCajaItemDiarioDto(fecha = "2026-11-10", saldoInicial = 5000000L, saldoFinal = 4900000L)
            )
        )
        fakeApi.flujoCajaResponse = Response.success(dto)

        val result = flujoCajaRepository.refreshFlujoCaja()
        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals(5000000L, data.saldoActual)
        assertEquals(3200000L, data.saldoProyectado30d)
        assertEquals(1, data.diasCriticos.size)
        assertEquals("Pago Préstamo", data.diasCriticos[0].motivo)

        // Verificar persistencia en Room vía Flow
        val flowValue = flujoCajaRepository.getFlujoCajaFlow().first()
        assertNotNull(flowValue)
        assertEquals(5000000L, flowValue!!.saldoActual)
        assertEquals(3200000L, flowValue.saldoProyectado30d)
        assertEquals(1, flowValue.diasCriticos.size)
        assertEquals(-250000L, flowValue.diasCriticos[0].saldo)

        // Verificar cached
        val cached = flujoCajaRepository.getFlujoCajaCached()
        assertNotNull(cached)
        assertEquals(1, cached!!.dias.size)
    }

    @Test
    fun `test refreshFlujoCaja error preserves cached Room data`() = runTest(testDispatcher) {
        // Precargar datos en Room
        val dto = FlujoCajaResponseDto(
            saldoActual = 1200000L,
            saldoProyectado30d = 800000L
        )
        fakeApi.flujoCajaResponse = Response.success(dto)
        flujoCajaRepository.refreshFlujoCaja()

        // Simular error de red
        fakeApi.flujoCajaResponse = Response.error(500, "Server Error".toResponseBody(null))
        val result = flujoCajaRepository.refreshFlujoCaja()
        assertTrue(result is NetworkResult.Error)

        // Los datos cacheados deben conservarse
        val cached = flujoCajaRepository.getFlujoCajaCached()
        assertNotNull(cached)
        assertEquals(1200000L, cached!!.saldoActual)
    }

    @Test
    fun `test refreshClima success inserts in Room and returns domain model`() = runTest(testDispatcher) {
        val dto = ClimaResponseDto(
            temperaturaActual = 28.5,
            temperaturaMin = 22.0,
            temperaturaMax = 33.0,
            condicion = "Tormenta dispersa",
            alertaParaguas = ClimaAlertaParaguasDto(
                activar = true,
                probabilidadLluvia = 75,
                motivo = "Alta probabilidad de tormentas en Asunción"
            ),
            actualizadoAt = "2026-11-09 11:30"
        )
        fakeApi.climaResponse = Response.success(dto)

        val result = climaRepository.refreshClima()
        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals(28.5, data.temperaturaActual, 0.01)
        assertEquals("Tormenta dispersa", data.condicion)
        assertTrue(data.alertaParaguas.activar)
        assertEquals(75, data.alertaParaguas.probabilidadLluvia)

        // Verificar Flow reactivo desde Room
        val flowValue = climaRepository.getClimaFlow().first()
        assertNotNull(flowValue)
        assertEquals(28.5, flowValue!!.temperaturaActual, 0.01)
        assertTrue(flowValue.alertaParaguas.activar)

        // Verificar cached
        val cached = climaRepository.getClimaCached()
        assertNotNull(cached)
        assertEquals("Alta probabilidad de tormentas en Asunción", cached!!.alertaParaguas.motivo)
    }

    @Test
    fun `test refreshClima error preserves cached Room data`() = runTest(testDispatcher) {
        // Precargar en Room
        val dto = ClimaResponseDto(
            temperaturaActual = 25.0,
            condicion = "Soleado"
        )
        fakeApi.climaResponse = Response.success(dto)
        climaRepository.refreshClima()

        // Simular error 503
        fakeApi.climaResponse = Response.error(503, "Unavailable".toResponseBody(null))
        val result = climaRepository.refreshClima()
        assertTrue(result is NetworkResult.Error)

        // Los datos cacheados persisten
        val cached = climaRepository.getClimaCached()
        assertNotNull(cached)
        assertEquals(25.0, cached!!.temperaturaActual, 0.01)
        assertEquals("Soleado", cached.condicion)
    }

    private class FakeFlujoClimaApi : BaseFakeGammaApi() {
        var flujoCajaResponse: Response<FlujoCajaResponseDto>? = null
        var climaResponse: Response<ClimaResponseDto>? = null

        override suspend fun getFlujoCaja(
            saldoInicial: Long?,
            gastoDiario: Long?,
            umbralReserva: Long?
        ): Response<FlujoCajaResponseDto> {
            return flujoCajaResponse ?: throw IllegalStateException("flujoCajaResponse not set")
        }

        override suspend fun getClima(): Response<ClimaResponseDto> {
            return climaResponse ?: throw IllegalStateException("climaResponse not set")
        }
    }
}
