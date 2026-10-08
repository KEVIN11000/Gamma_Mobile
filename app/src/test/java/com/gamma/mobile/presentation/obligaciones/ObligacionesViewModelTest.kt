package com.gamma.mobile.presentation.obligaciones

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Obligacion
import com.gamma.mobile.domain.repository.ObligacionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObligacionesViewModelTest {

    private lateinit var viewModel: ObligacionesViewModel
    private lateinit var fakeRepo: FakeObligacionRepository
    private lateinit var fakeDashboardRepo: FakeDashboardRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeObligacionRepository()
        fakeDashboardRepo = FakeDashboardRepository()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadObligaciones emits Success with fetched data`() = runTest {
        val initialData = listOf(
            Obligacion("1", "Deuda Test", 100000, 100000, "Simulado")
        )
        fakeRepo.data = initialData

        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ObligacionesUiState.Success)
        assertEquals(1, (state as ObligacionesUiState.Success).obligaciones.size)
    }

    @Test
    fun `crearObligacion adds debt and calls repo optimistically`() = runTest {
        fakeRepo.data = emptyList()
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        viewModel.crearObligacion("Préstamo", 500000)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals(1, state.obligaciones.size)
        assertEquals("Préstamo", state.obligaciones[0].nombre)
        assertEquals(500000, state.obligaciones[0].montoInicial)
        assertEquals("Simulado", state.obligaciones[0].estado)
        
        // Also added to repo
        assertEquals(1, fakeRepo.createdList.size)
    }

    @Test
    fun `aprobarObligacion changes estado from Simulado to Activo`() = runTest {
        val obligacion = Obligacion("1", "Deuda", 1000, 1000, "Simulado")
        fakeRepo.data = listOf(obligacion)
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        viewModel.aprobarObligacion(obligacion)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals("Activo", state.obligaciones[0].estado)
        assertEquals("Activo", fakeRepo.updatedList[0].estado)
    }

    @Test
    fun `abonarObligacion reduces saldoActual`() = runTest {
        val obligacion = Obligacion("1", "Deuda", 10000, 10000, "Activo")
        fakeRepo.data = listOf(obligacion)
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        viewModel.abonarObligacion(obligacion, 4000)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals(6000, state.obligaciones[0].saldoActual)
        assertEquals("Activo", state.obligaciones[0].estado)
    }

    @Test
    fun `abonarObligacion fully pays and sets state to Saldado`() = runTest {
        val obligacion = Obligacion("1", "Deuda", 10000, 4000, "Activo")
        fakeRepo.data = listOf(obligacion)
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        viewModel.abonarObligacion(obligacion, 4000)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals(0, state.obligaciones[0].saldoActual)
        assertEquals("Saldado", state.obligaciones[0].estado)
    }

    @Test
    fun `simularCuota maps to correct NivelRiesgo based on margin`() = runTest {
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        
        // Verde (<= 30%)
        viewModel.simularCuota(monto = 3000, cuotas = 10) // 300 / 1000 = 30%
        advanceUntilIdle()
        assertEquals(NivelRiesgo.VERDE, viewModel.simulacionResult.value?.nivelRiesgo)

        // Amarillo (<= 60%)
        viewModel.simularCuota(monto = 6000, cuotas = 10) // 600 / 1000 = 60%
        advanceUntilIdle()
        assertEquals(NivelRiesgo.AMARILLO, viewModel.simulacionResult.value?.nivelRiesgo)

        // Rojo (> 60%)
        viewModel.simularCuota(monto = 7000, cuotas = 10) // 700 / 1000 = 70%
        advanceUntilIdle()
        assertEquals(NivelRiesgo.ROJO, viewModel.simulacionResult.value?.nivelRiesgo)
    }
}

class FakeDashboardRepository : com.gamma.mobile.domain.repository.DashboardRepository {
    override suspend fun getSummary(forceRefresh: Boolean): NetworkResult<com.gamma.mobile.domain.model.DashboardSummary> {
        return NetworkResult.Success(com.gamma.mobile.domain.model.DashboardSummary(
            margenLibreDisponible = 1000,
            saldoAcumulado = 5000,
            posicionIva = 100,
            esDebitoFiscal = true
        ))
    }
}

class FakeObligacionRepository : ObligacionRepository {
    var data: List<Obligacion> = emptyList()
    val createdList = mutableListOf<Obligacion>()
    val updatedList = mutableListOf<Obligacion>()

    override suspend fun getObligaciones(forceRefresh: Boolean): NetworkResult<List<Obligacion>> {
        return NetworkResult.Success(data)
    }

    override suspend fun createObligacion(obligacion: Obligacion): NetworkResult<Unit> {
        createdList.add(obligacion)
        return NetworkResult.Success(Unit)
    }

    override suspend fun updateObligacion(obligacion: Obligacion): NetworkResult<Unit> {
        updatedList.add(obligacion)
        return NetworkResult.Success(Unit)
    }
}
