package com.gamma.mobile.presentation.capacidad

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.model.Obligacion
import com.gamma.mobile.domain.model.SemaforoViabilidad
import com.gamma.mobile.domain.repository.DashboardRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CapacidadPagoViewModelTest {

    private lateinit var viewModel: CapacidadPagoViewModel
    private lateinit var fakeObligacionRepo: FakeCapacidadObligacionRepo
    private lateinit var fakeDashboardRepo: FakeCapacidadDashboardRepo
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeObligacionRepo = FakeCapacidadObligacionRepo()
        fakeDashboardRepo = FakeCapacidadDashboardRepo()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load calculates SUPERAVIT when remaining margin is ample`() = runTest {
        fakeDashboardRepo.summary = DashboardSummary(margenLibreDisponible = 5000000L)
        fakeObligacionRepo.obligaciones = listOf(
            Obligacion(id = "1", nombre = "Préstamo", saldoActual = 5000000L, cuotaReferenciaGs = 500000L, cuotasRestantes = 10, estado = "Activo"),
            Obligacion(id = "2", nombre = "Tarjeta", saldoActual = 4000000L, cuotaReferenciaGs = 800000L, cuotasRestantes = 5, estado = "Activo")
        )

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CapacidadPagoUiState.Success)
        val success = state as CapacidadPagoUiState.Success

        assertEquals(5000000L, success.resultado.fondoDisponibleMes)
        assertEquals(1300000L, success.resultado.compromisoTotalMes)
        assertEquals(3700000L, success.resultado.capacidadNeta)
        assertEquals(SemaforoViabilidad.SUPERAVIT, success.resultado.semaforoEnum)
        assertEquals(26.0, success.resultado.dtiProyectado, 0.1)
    }

    @Test
    fun `initial load calculates AJUSTADO when buffer is less than 15 percent`() = runTest {
        fakeDashboardRepo.summary = DashboardSummary(margenLibreDisponible = 1000000L)
        fakeObligacionRepo.obligaciones = listOf(
            Obligacion(id = "1", nombre = "Deuda Pesada", saldoActual = 3600000L, cuotaReferenciaGs = 900000L, cuotasRestantes = 4, estado = "Activo")
        )

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(100000L, state.resultado.capacidadNeta)
        // 100000 < (1000000 * 0.15 = 150000) -> AJUSTADO
        assertEquals(SemaforoViabilidad.AJUSTADO, state.resultado.semaforoEnum)
    }

    @Test
    fun `initial load calculates DEFICIT when commitments exceed margin`() = runTest {
        fakeDashboardRepo.summary = DashboardSummary(margenLibreDisponible = 1000000L)
        fakeObligacionRepo.obligaciones = listOf(
            Obligacion(id = "1", nombre = "Super Cuota", saldoActual = 4500000L, cuotaReferenciaGs = 1500000L, cuotasRestantes = 3, estado = "Activo")
        )

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(-500000L, state.resultado.capacidadNeta)
        assertEquals(SemaforoViabilidad.DEFICIT, state.resultado.semaforoEnum)
    }

    @Test
    fun `toggleSimularPausa removes debt cuota from monthly commitments and clears abono`() = runTest {
        fakeDashboardRepo.summary = DashboardSummary(margenLibreDisponible = 3000000L)
        fakeObligacionRepo.obligaciones = listOf(
            Obligacion(id = "1", nombre = "Auto", saldoActual = 12000000L, cuotaReferenciaGs = 1000000L, cuotasRestantes = 12, estado = "Activo"),
            Obligacion(id = "2", nombre = "Electrodoméstico", saldoActual = 3000000L, cuotaReferenciaGs = 500000L, cuotasRestantes = 6, estado = "Activo")
        )

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo)
        advanceUntilIdle()

        // Asignar abono a deuda 1 primero
        viewModel.setAbonoExtraordinario("1", 200000L)
        advanceUntilIdle()
        assertEquals(200000L, viewModel.abonosExtraordinarios.value["1"])

        // Simular pausar la deuda 1
        viewModel.toggleSimularPausa("1")
        advanceUntilIdle()

        assertTrue(viewModel.deudasPausadasSimuladas.value.contains("1"))
        assertFalse(viewModel.abonosExtraordinarios.value.containsKey("1"))

        val state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(500000L, state.resultado.compromisoTotalMes) // Solo la deuda 2
        assertEquals(2500000L, state.resultado.capacidadNeta)

        // Despausar la deuda 1
        viewModel.toggleSimularPausa("1")
        advanceUntilIdle()
        assertFalse(viewModel.deudasPausadasSimuladas.value.contains("1"))

        val stateRestored = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(1500000L, stateRestored.resultado.compromisoTotalMes)
    }

    @Test
    fun `setAjusteIngresoExtra increases fondoDisponibleMes and recalculates net capacity`() = runTest {
        fakeDashboardRepo.summary = DashboardSummary(margenLibreDisponible = 2000000L)
        fakeObligacionRepo.obligaciones = listOf(
            Obligacion(id = "1", nombre = "Deuda", saldoActual = 7500000L, cuotaReferenciaGs = 1500000L, cuotasRestantes = 5, estado = "Activo")
        )

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo)
        advanceUntilIdle()

        var state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(2000000L, state.resultado.fondoDisponibleMes)
        assertEquals(500000L, state.resultado.capacidadNeta)

        // Ajuste con aguinaldo / ingreso extra
        viewModel.setAjusteIngresoExtra(1000000L)
        advanceUntilIdle()

        state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(3000000L, state.resultado.fondoDisponibleMes)
        assertEquals(1500000L, state.resultado.capacidadNeta)
    }

    @Test
    fun `setAbonoExtraordinario accurately calculates mesesAhorrados`() = runTest {
        fakeDashboardRepo.summary = DashboardSummary(margenLibreDisponible = 3000000L)
        fakeObligacionRepo.obligaciones = listOf(
            Obligacion(id = "1", nombre = "Préstamo", saldoActual = 2000000L, cuotaReferenciaGs = 400000L, cuotasRestantes = 5, estado = "Activo")
        )

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo)
        advanceUntilIdle()

        // Abono de 800.000 Gs (2 cuotas de 400.000)
        viewModel.setAbonoExtraordinario("1", 800000L)
        advanceUntilIdle()

        var state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(2, state.resultado.mesesAhorradosPorAbonos["1"])
        assertEquals(3000000L - 400000L - 800000L, state.resultado.capacidadNeta)

        // Abono que excede las cuotas restantes (6 cuotas equivalentes vs 5 restantes)
        viewModel.setAbonoExtraordinario("1", 2400000L)
        advanceUntilIdle()

        state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(5, state.resultado.mesesAhorradosPorAbonos["1"]) // tope en cuotasRestantes (5)
    }

    @Test
    fun `resetSimulacion clears all simulation parameters`() = runTest {
        fakeDashboardRepo.summary = DashboardSummary(margenLibreDisponible = 4000000L)
        fakeObligacionRepo.obligaciones = listOf(
            Obligacion(id = "1", nombre = "Deuda A", saldoActual = 2000000L, cuotaReferenciaGs = 500000L, cuotasRestantes = 4, estado = "Activo")
        )

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo)
        advanceUntilIdle()

        viewModel.setAjusteIngresoExtra(500000L)
        viewModel.setAbonoExtraordinario("1", 1000000L)
        viewModel.toggleSimularPausa("1")
        advanceUntilIdle()

        assertTrue(viewModel.deudasPausadasSimuladas.value.isNotEmpty())
        assertEquals(500000L, viewModel.ajusteIngresoExtra.value)

        viewModel.resetSimulacion()
        advanceUntilIdle()

        assertTrue(viewModel.deudasPausadasSimuladas.value.isEmpty())
        assertTrue(viewModel.abonosExtraordinarios.value.isEmpty())
        assertEquals(0L, viewModel.ajusteIngresoExtra.value)

        val state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(4000000L, state.resultado.fondoDisponibleMes)
        assertEquals(500000L, state.resultado.compromisoTotalMes)
        assertEquals(3500000L, state.resultado.capacidadNeta)
    }

    @Test
    fun `loadDatos emits Error state when repository returns failure`() = runTest {
        fakeDashboardRepo.shouldFail = true

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CapacidadPagoUiState.Error)
        assertEquals("Fallo al obtener dashboard", (state as CapacidadPagoUiState.Error).message)
    }
}

class FakeCapacidadDashboardRepo : DashboardRepository {
    var summary: DashboardSummary = DashboardSummary()
    var shouldFail: Boolean = false

    override suspend fun getSummary(forceRefresh: Boolean): NetworkResult<DashboardSummary> {
        return if (shouldFail) {
            NetworkResult.Error(500, "Fallo al obtener dashboard")
        } else {
            NetworkResult.Success(summary)
        }
    }
}

class FakeCapacidadObligacionRepo : ObligacionRepository {
    var obligaciones: List<Obligacion> = emptyList()
    var shouldFail: Boolean = false

    override suspend fun getObligaciones(forceRefresh: Boolean): NetworkResult<List<Obligacion>> {
        return if (shouldFail) {
            NetworkResult.Error(500, "Fallo al obtener obligaciones")
        } else {
            NetworkResult.Success(obligaciones)
        }
    }

    override suspend fun createObligacion(obligacion: Obligacion): NetworkResult<Unit> = NetworkResult.Success(Unit)
    override suspend fun updateObligacion(obligacion: Obligacion): NetworkResult<Unit> = NetworkResult.Success(Unit)
    override suspend fun deleteObligacion(id: String): NetworkResult<Unit> = NetworkResult.Success(Unit)
    override suspend fun abonarObligacion(idObligacion: String, monto: Long): NetworkResult<Unit> = NetworkResult.Success(Unit)
}
