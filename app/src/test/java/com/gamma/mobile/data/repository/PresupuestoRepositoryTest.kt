package com.gamma.mobile.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gamma.mobile.data.remote.BaseFakeGammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.GastoFijoCreateDto
import com.gamma.mobile.data.remote.dto.GastoFijoItemDto
import com.gamma.mobile.data.remote.dto.GastoFijoUpdateDto
import com.gamma.mobile.data.remote.dto.PresupuestoAccionResponseDto
import com.gamma.mobile.data.remote.dto.PresupuestoFijosResponseDto
import com.gamma.mobile.data.remote.dto.SalarioBaseUpdateDto
import com.gamma.mobile.data.remote.dto.TechoPresupuestarioItemDto
import com.gamma.mobile.data.remote.dto.TechoPresupuestarioUpdateDto
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PresupuestoRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val gson = Gson()
    private lateinit var context: Context
    private lateinit var fakeApi: FakePresupuestoApi
    private lateinit var repository: PresupuestoRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("gamma_cache", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        fakeApi = FakePresupuestoApi()
        repository = PresupuestoRepositoryImpl(
            api = fakeApi,
            context = context,
            gson = gson,
            dispatcher = testDispatcher
        )
    }

    @Test
    fun `getGastosFijos caches response and returns cached when not forced`() = runTest(testDispatcher) {
        val testFijos = PresupuestoFijosResponseDto(
            salarioBase = 8000000L,
            totalGastosFijos = 2000000L,
            categorias = mapOf("Servicios" to listOf(GastoFijoItemDto("Luz", 250000L)))
        )
        fakeApi.fijosResponse = Response.success(testFijos)

        val result1 = repository.getGastosFijos(forceRefresh = true)
        assertTrue(result1 is NetworkResult.Success)
        assertEquals(8000000L, (result1 as NetworkResult.Success).data.salarioBase)

        // Change API response to verify cache is returned when forceRefresh=false
        fakeApi.fijosResponse = Response.success(PresupuestoFijosResponseDto(salarioBase = 9999L))
        val resultCached = repository.getGastosFijos(forceRefresh = false)
        assertTrue(resultCached is NetworkResult.Success)
        assertEquals(8000000L, (resultCached as NetworkResult.Success).data.salarioBase)
    }

    @Test
    fun `getTechosPresupuestarios caches response and returns list`() = runTest(testDispatcher) {
        val testTechos = listOf(
            TechoPresupuestarioItemDto(categoria = "Combustible", limiteMensual = 500000L, consumido = 200000L)
        )
        fakeApi.techosResponse = Response.success(testTechos)

        val result = repository.getTechosPresupuestarios(forceRefresh = true)
        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data.size)
        assertEquals("Combustible", result.data[0].categoria)
    }

    @Test
    fun `mutations call api and succeed`() = runTest(testDispatcher) {
        fakeApi.accionResponse = Response.success(PresupuestoAccionResponseDto(status = "ok"))

        val res1 = repository.createGastoFijo(GastoFijoCreateDto("Vivienda", "Agua", 50000L))
        assertTrue(res1 is NetworkResult.Success)

        val res2 = repository.updateGastoFijo("Agua", GastoFijoUpdateDto(60000L))
        assertTrue(res2 is NetworkResult.Success)

        val res3 = repository.deleteGastoFijo("Agua")
        assertTrue(res3 is NetworkResult.Success)

        val res4 = repository.updateSalarioBase(SalarioBaseUpdateDto(8500000L))
        assertTrue(res4 is NetworkResult.Success)

        val res5 = repository.updateTechoPresupuestario("Comida", TechoPresupuestarioUpdateDto(limiteMensual = 800000L))
        assertTrue(res5 is NetworkResult.Success)
    }
}

class FakePresupuestoApi : BaseFakeGammaApi() {
    var fijosResponse: Response<PresupuestoFijosResponseDto> = Response.success(PresupuestoFijosResponseDto())
    var techosResponse: Response<List<TechoPresupuestarioItemDto>> = Response.success(emptyList())
    var accionResponse: Response<PresupuestoAccionResponseDto> = Response.success(PresupuestoAccionResponseDto())

    override suspend fun getGastosFijos(): Response<PresupuestoFijosResponseDto> = fijosResponse
    override suspend fun createGastoFijo(request: GastoFijoCreateDto): Response<PresupuestoAccionResponseDto> = accionResponse
    override suspend fun updateGastoFijo(concepto: String, request: GastoFijoUpdateDto): Response<PresupuestoAccionResponseDto> = accionResponse
    override suspend fun deleteGastoFijo(concepto: String): Response<PresupuestoAccionResponseDto> = accionResponse
    override suspend fun updateSalarioBase(request: SalarioBaseUpdateDto): Response<PresupuestoAccionResponseDto> = accionResponse
    override suspend fun getTechosPresupuestarios(): Response<List<TechoPresupuestarioItemDto>> = techosResponse
    override suspend fun updateTechoPresupuestario(categoria: String, request: TechoPresupuestarioUpdateDto): Response<PresupuestoAccionResponseDto> = accionResponse
}
