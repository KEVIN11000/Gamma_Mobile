package com.gamma.mobile.presentation.presupuesto

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.GastoFijoCreateDto
import com.gamma.mobile.data.remote.dto.GastoFijoItemDto
import com.gamma.mobile.data.remote.dto.GastoFijoUpdateDto
import com.gamma.mobile.data.remote.dto.PresupuestoAccionResponseDto
import com.gamma.mobile.data.remote.dto.PresupuestoFijosResponseDto
import com.gamma.mobile.data.remote.dto.SalarioBaseUpdateDto
import com.gamma.mobile.data.remote.dto.TechoPresupuestarioItemDto
import com.gamma.mobile.data.remote.dto.TechoPresupuestarioUpdateDto
import com.gamma.mobile.domain.repository.CapacidadesRepository
import com.gamma.mobile.domain.repository.PresupuestoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PresupuestoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakePresupuestoRepository
    private lateinit var fakeCapacidadesRepo: FakeCapacidadesRepoForPresupuesto
    private lateinit var viewModel: PresupuestoViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakePresupuestoRepository()
        fakeCapacidadesRepo = FakeCapacidadesRepoForPresupuesto()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadPresupuesto emits Success with fijos and techos and capabilities`() = runTest(testDispatcher) {
        fakeRepo.gastosFijosResponse = NetworkResult.Success(
            PresupuestoFijosResponseDto(
                salarioBase = 7500000L,
                totalGastosFijos = 2500000L,
                categorias = mapOf(
                    "Vivienda" to listOf(GastoFijoItemDto("Alquiler", 1500000L)),
                    "Servicios" to listOf(GastoFijoItemDto("Luz", 300000L))
                )
            )
        )
        fakeRepo.techosResponse = NetworkResult.Success(
            listOf(
                TechoPresupuestarioItemDto(
                    categoria = "Supermercado",
                    limiteMensual = 1200000L,
                    consumido = 600000L,
                    porcentaje = 50.0,
                    estado = "NORMAL"
                )
            )
        )
        fakeCapacidadesRepo.capacidades["presupuesto_fijos_delete"] = false

        viewModel = PresupuestoViewModel(fakeRepo, fakeCapacidadesRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is PresupuestoUiState.Success)
        val success = state as PresupuestoUiState.Success
        assertEquals(7500000L, success.salarioBase)
        assertEquals(2500000L, success.totalGastosFijos)
        assertEquals(5000000L, success.margenEstructural)
        assertEquals(2, success.categoriasFijos.size)
        assertEquals(1, success.techosVariables.size)
        assertFalse(success.puedeEliminarFijos) // Governed by CapacidadesRepository
        assertTrue(success.puedeEditarTechos)
    }

    @Test
    fun `actualizarSalarioBase calls repository and triggers reload`() = runTest(testDispatcher) {
        viewModel = PresupuestoViewModel(fakeRepo, fakeCapacidadesRepo)
        advanceUntilIdle()

        viewModel.actualizarSalarioBase(8000000L)
        advanceUntilIdle()

        assertEquals(8000000L, fakeRepo.lastSalarioBaseUpdated)
        assertEquals("Salario base actualizado con éxito.", viewModel.feedbackMessage.value)
    }

    @Test
    fun `crearGastoFijo calls repository and triggers reload`() = runTest(testDispatcher) {
        viewModel = PresupuestoViewModel(fakeRepo, fakeCapacidadesRepo)
        advanceUntilIdle()

        viewModel.crearGastoFijo("Vivienda", "Expensas", 200000L)
        advanceUntilIdle()

        assertEquals("Expensas", fakeRepo.lastCreatedGastoFijo?.concepto)
        assertEquals(200000L, fakeRepo.lastCreatedGastoFijo?.monto)
        assertEquals("Gasto fijo 'Expensas' registrado.", viewModel.feedbackMessage.value)
    }

    @Test
    fun `eliminarGastoFijo calls repository and triggers reload`() = runTest(testDispatcher) {
        viewModel = PresupuestoViewModel(fakeRepo, fakeCapacidadesRepo)
        advanceUntilIdle()

        viewModel.eliminarGastoFijo("Alquiler")
        advanceUntilIdle()

        assertEquals("Alquiler", fakeRepo.lastDeletedConcepto)
        assertEquals("Gasto fijo 'Alquiler' eliminado.", viewModel.feedbackMessage.value)
    }

    @Test
    fun `actualizarTecho calls repository and triggers reload`() = runTest(testDispatcher) {
        viewModel = PresupuestoViewModel(fakeRepo, fakeCapacidadesRepo)
        advanceUntilIdle()

        viewModel.actualizarTecho("Supermercado", 1500000L, 85)
        advanceUntilIdle()

        assertEquals("Supermercado", fakeRepo.lastTechoCategoriaUpdated)
        assertEquals(1500000L, fakeRepo.lastTechoUpdated?.limiteMensual)
        assertEquals(85, fakeRepo.lastTechoUpdated?.umbralAlerta)
    }
}

class FakePresupuestoRepository : PresupuestoRepository {
    var gastosFijosResponse: NetworkResult<PresupuestoFijosResponseDto> = NetworkResult.Success(PresupuestoFijosResponseDto())
    var techosResponse: NetworkResult<List<TechoPresupuestarioItemDto>> = NetworkResult.Success(emptyList())

    var lastSalarioBaseUpdated: Long? = null
    var lastCreatedGastoFijo: GastoFijoCreateDto? = null
    var lastUpdatedGastoFijo: Pair<String, Long>? = null
    var lastDeletedConcepto: String? = null
    var lastTechoCategoriaUpdated: String? = null
    var lastTechoUpdated: TechoPresupuestarioUpdateDto? = null

    override suspend fun getGastosFijos(forceRefresh: Boolean): NetworkResult<PresupuestoFijosResponseDto> = gastosFijosResponse

    override suspend fun createGastoFijo(request: GastoFijoCreateDto): NetworkResult<PresupuestoAccionResponseDto> {
        lastCreatedGastoFijo = request
        return NetworkResult.Success(PresupuestoAccionResponseDto(status = "ok"))
    }

    override suspend fun updateGastoFijo(concepto: String, request: GastoFijoUpdateDto): NetworkResult<PresupuestoAccionResponseDto> {
        lastUpdatedGastoFijo = Pair(concepto, request.nuevoMonto)
        return NetworkResult.Success(PresupuestoAccionResponseDto(status = "ok"))
    }

    override suspend fun deleteGastoFijo(concepto: String): NetworkResult<PresupuestoAccionResponseDto> {
        lastDeletedConcepto = concepto
        return NetworkResult.Success(PresupuestoAccionResponseDto(status = "ok"))
    }

    override suspend fun updateSalarioBase(request: SalarioBaseUpdateDto): NetworkResult<PresupuestoAccionResponseDto> {
        lastSalarioBaseUpdated = request.nuevoSalario
        return NetworkResult.Success(PresupuestoAccionResponseDto(status = "ok"))
    }

    override suspend fun getTechosPresupuestarios(forceRefresh: Boolean): NetworkResult<List<TechoPresupuestarioItemDto>> = techosResponse

    override suspend fun updateTechoPresupuestario(categoria: String, request: TechoPresupuestarioUpdateDto): NetworkResult<PresupuestoAccionResponseDto> {
        lastTechoCategoriaUpdated = categoria
        lastTechoUpdated = request
        return NetworkResult.Success(PresupuestoAccionResponseDto(status = "ok"))
    }
}

class FakeCapacidadesRepoForPresupuesto : CapacidadesRepository {
    val capacidades = mutableMapOf<String, Boolean>()
    override val capacidadesFlow: StateFlow<Map<String, Boolean>> = MutableStateFlow(capacidades)

    override suspend fun syncCapacidades(): NetworkResult<com.gamma.mobile.data.remote.dto.CapacidadesResponseDto> =
        NetworkResult.Success(com.gamma.mobile.data.remote.dto.CapacidadesResponseDto())

    override fun isCapacidadHabilitada(clave: String, defaultSiNoExiste: Boolean): Boolean =
        capacidades[clave] ?: defaultSiNoExiste

    override fun getCapacidadesClave(): Map<String, Boolean> = capacidades
    override fun getModulos(): List<String> = listOf("presupuesto")
    override fun getVersionApi(): String = "2.1.4"
}
