package com.gamma.mobile.presentation.capacidad

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.model.EscenarioCapacidad
import com.gamma.mobile.domain.model.GastoSimulado
import com.gamma.mobile.domain.model.Obligacion
import com.gamma.mobile.domain.model.SemaforoViabilidad
import com.gamma.mobile.domain.repository.DashboardRepository
import com.gamma.mobile.domain.repository.EscenarioCapacidadRepository
import com.gamma.mobile.domain.repository.ObligacionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private lateinit var fakeEscenarioRepo: FakeEscenarioCapacidadRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeObligacionRepo = FakeCapacidadObligacionRepo()
        fakeDashboardRepo = FakeCapacidadDashboardRepo()
        fakeEscenarioRepo = FakeEscenarioCapacidadRepository()
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

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo, fakeEscenarioRepo)
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

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo, fakeEscenarioRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(100000L, state.resultado.capacidadNeta)
        assertEquals(SemaforoViabilidad.AJUSTADO, state.resultado.semaforoEnum)
    }

    @Test
    fun `initial load calculates DEFICIT when commitments exceed margin`() = runTest {
        fakeDashboardRepo.summary = DashboardSummary(margenLibreDisponible = 1000000L)
        fakeObligacionRepo.obligaciones = listOf(
            Obligacion(id = "1", nombre = "Super Cuota", saldoActual = 4500000L, cuotaReferenciaGs = 1500000L, cuotasRestantes = 3, estado = "Activo")
        )

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo, fakeEscenarioRepo)
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

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo, fakeEscenarioRepo)
        advanceUntilIdle()

        viewModel.setAbonoExtraordinario("1", 200000L)
        advanceUntilIdle()
        assertEquals(200000L, viewModel.abonosExtraordinarios.value["1"])

        viewModel.toggleSimularPausa("1")
        advanceUntilIdle()

        assertTrue(viewModel.deudasPausadasSimuladas.value.contains("1"))
        assertFalse(viewModel.abonosExtraordinarios.value.containsKey("1"))

        val state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(500000L, state.resultado.compromisoTotalMes)
        assertEquals(2500000L, state.resultado.capacidadNeta)

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

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo, fakeEscenarioRepo)
        advanceUntilIdle()

        var state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(2000000L, state.resultado.fondoDisponibleMes)
        assertEquals(500000L, state.resultado.capacidadNeta)

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

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo, fakeEscenarioRepo)
        advanceUntilIdle()

        viewModel.setAbonoExtraordinario("1", 800000L)
        advanceUntilIdle()

        var state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(2, state.resultado.mesesAhorradosPorAbonos["1"])
        assertEquals(3000000L - 400000L - 800000L, state.resultado.capacidadNeta)

        viewModel.setAbonoExtraordinario("1", 2400000L)
        advanceUntilIdle()

        state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(5, state.resultado.mesesAhorradosPorAbonos["1"])
    }

    @Test
    fun `agregarGastoAdHoc adds expense and updates totalGastosProyectados and capacidadNeta`() = runTest {
        fakeDashboardRepo.summary = DashboardSummary(margenLibreDisponible = 4000000L)
        fakeObligacionRepo.obligaciones = listOf(
            Obligacion(id = "1", nombre = "Deuda", saldoActual = 2000000L, cuotaReferenciaGs = 1000000L, cuotasRestantes = 2, estado = "Activo")
        )

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo, fakeEscenarioRepo)
        advanceUntilIdle()

        viewModel.agregarGastoAdHoc("Mantenimiento Vehículo", 500000L)
        advanceUntilIdle()

        assertEquals(1, viewModel.gastosSimulados.value.size)
        assertEquals("Mantenimiento Vehículo", viewModel.gastosSimulados.value[0].concepto)
        assertEquals(500000L, viewModel.gastosSimulados.value[0].monto)
        assertTrue(viewModel.gastosSimulados.value[0].incluido)

        val state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(500000L, state.resultado.totalGastosProyectados)
        // 4.000.000 - 1.000.000 - 500.000 = 2.500.000
        assertEquals(2500000L, state.resultado.capacidadNeta)
        // DTI: (1.000.000 + 500.000) / 4.000.000 = 37.5%
        assertEquals(37.5, state.resultado.dtiProyectado, 0.1)
    }

    @Test
    fun `toggleGastoAdHoc pauses expense without deleting it`() = runTest {
        fakeDashboardRepo.summary = DashboardSummary(margenLibreDisponible = 3000000L)
        fakeObligacionRepo.obligaciones = emptyList()

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo, fakeEscenarioRepo)
        advanceUntilIdle()

        viewModel.agregarGastoAdHoc("Gasto Opcional", 1000000L)
        advanceUntilIdle()

        val gastoId = viewModel.gastosSimulados.value[0].id
        var state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(1000000L, state.resultado.totalGastosProyectados)
        assertEquals(2000000L, state.resultado.capacidadNeta)

        // Pausar gasto
        viewModel.toggleGastoAdHoc(gastoId)
        advanceUntilIdle()

        assertFalse(viewModel.gastosSimulados.value[0].incluido)
        state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(0L, state.resultado.totalGastosProyectados)
        assertEquals(3000000L, state.resultado.capacidadNeta)

        // Reactivar gasto
        viewModel.toggleGastoAdHoc(gastoId)
        advanceUntilIdle()

        assertTrue(viewModel.gastosSimulados.value[0].incluido)
        state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(1000000L, state.resultado.totalGastosProyectados)
        assertEquals(2000000L, state.resultado.capacidadNeta)
    }

    @Test
    fun `eliminarGastoAdHoc removes expense completely from calculation`() = runTest {
        fakeDashboardRepo.summary = DashboardSummary(margenLibreDisponible = 3000000L)
        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo, fakeEscenarioRepo)
        advanceUntilIdle()

        viewModel.agregarGastoAdHoc("Gasto 1", 500000L)
        viewModel.agregarGastoAdHoc("Gasto 2", 700000L)
        advanceUntilIdle()
        assertEquals(2, viewModel.gastosSimulados.value.size)

        val idAEliminar = viewModel.gastosSimulados.value[0].id
        viewModel.eliminarGastoAdHoc(idAEliminar)
        advanceUntilIdle()

        assertEquals(1, viewModel.gastosSimulados.value.size)
        assertEquals("Gasto 2", viewModel.gastosSimulados.value[0].concepto)
        val state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(700000L, state.resultado.totalGastosProyectados)
    }

    @Test
    fun `guardarEscenarioActual and cargarEscenario restores entire simulation state`() = runTest {
        fakeDashboardRepo.summary = DashboardSummary(margenLibreDisponible = 5000000L)
        fakeObligacionRepo.obligaciones = listOf(
            Obligacion(id = "1", nombre = "Deuda 1", saldoActual = 5000000L, cuotaReferenciaGs = 500000L, cuotasRestantes = 10, estado = "Activo"),
            Obligacion(id = "2", nombre = "Deuda 2", saldoActual = 3000000L, cuotaReferenciaGs = 600000L, cuotasRestantes = 5, estado = "Activo")
        )

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo, fakeEscenarioRepo)
        advanceUntilIdle()

        // Configurar simulación compleja
        viewModel.setAjusteIngresoExtra(1000000L)
        viewModel.toggleSimularPausa("1")
        viewModel.setAbonoExtraordinario("2", 1200000L)
        viewModel.agregarGastoAdHoc("Viaje", 1500000L)
        advanceUntilIdle()

        // Guardar escenario
        viewModel.guardarEscenarioActual("Vacaciones Julio")
        advanceUntilIdle()

        assertEquals(1, fakeEscenarioRepo.escenarios.size)
        val guardado = fakeEscenarioRepo.escenarios[0]
        assertEquals("Vacaciones Julio", guardado.nombre)
        assertEquals(viewModel.escenarioActivoNombre.value, "Vacaciones Julio")

        // Resetear simulación
        viewModel.resetSimulacion()
        advanceUntilIdle()

        assertTrue(viewModel.deudasPausadasSimuladas.value.isEmpty())
        assertTrue(viewModel.abonosExtraordinarios.value.isEmpty())
        assertEquals(0L, viewModel.ajusteIngresoExtra.value)
        assertTrue(viewModel.gastosSimulados.value.isEmpty())

        // Recargar escenario guardado
        viewModel.cargarEscenario(guardado)
        advanceUntilIdle()

        assertEquals(setOf("1"), viewModel.deudasPausadasSimuladas.value)
        assertEquals(1200000L, viewModel.abonosExtraordinarios.value["2"])
        assertEquals(1000000L, viewModel.ajusteIngresoExtra.value)
        assertEquals(1, viewModel.gastosSimulados.value.size)
        assertEquals("Viaje", viewModel.gastosSimulados.value[0].concepto)
        assertEquals("Vacaciones Julio", viewModel.escenarioActivoNombre.value)

        val state = viewModel.uiState.value as CapacidadPagoUiState.Success
        // Fondo: 5.000.000 + 1.000.000 = 6.000.000
        // Deudas activas no pausadas: solo Deuda 2 (600.000)
        // Abono: 1.200.000
        // Gastos: 1.500.000
        // Capacidad neta: 6.000.000 - 600.000 - 1.200.000 - 1.500.000 = 2.700.000
        assertEquals(6000000L, state.resultado.fondoDisponibleMes)
        assertEquals(600000L, state.resultado.compromisoTotalMes)
        assertEquals(1500000L, state.resultado.totalGastosProyectados)
        assertEquals(2700000L, state.resultado.capacidadNeta)
    }

    @Test
    fun `resetSimulacion clears all simulation parameters including ad hoc expenses`() = runTest {
        fakeDashboardRepo.summary = DashboardSummary(margenLibreDisponible = 4000000L)
        fakeObligacionRepo.obligaciones = listOf(
            Obligacion(id = "1", nombre = "Deuda A", saldoActual = 2000000L, cuotaReferenciaGs = 500000L, cuotasRestantes = 4, estado = "Activo")
        )

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo, fakeEscenarioRepo)
        advanceUntilIdle()

        viewModel.setAjusteIngresoExtra(500000L)
        viewModel.setAbonoExtraordinario("1", 1000000L)
        viewModel.toggleSimularPausa("1")
        viewModel.agregarGastoAdHoc("Reparación", 300000L)
        advanceUntilIdle()

        viewModel.resetSimulacion()
        advanceUntilIdle()

        assertTrue(viewModel.deudasPausadasSimuladas.value.isEmpty())
        assertTrue(viewModel.abonosExtraordinarios.value.isEmpty())
        assertEquals(0L, viewModel.ajusteIngresoExtra.value)
        assertTrue(viewModel.gastosSimulados.value.isEmpty())

        val state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(4000000L, state.resultado.fondoDisponibleMes)
        assertEquals(500000L, state.resultado.compromisoTotalMes)
        assertEquals(0L, state.resultado.totalGastosProyectados)
        assertEquals(3500000L, state.resultado.capacidadNeta)
    }

    @Test
    fun `loadDatos emits Error state when repository returns failure`() = runTest {
        fakeDashboardRepo.shouldFail = true

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo, fakeEscenarioRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CapacidadPagoUiState.Error)
        assertEquals("Fallo al obtener dashboard", (state as CapacidadPagoUiState.Error).message)
    }

    @Test
    fun `sincronizarProyeccionRemota merges backend projection with ad-hoc expenses`() = runTest {
        fakeDashboardRepo.summary = DashboardSummary(margenLibreDisponible = 5000000L)
        fakeObligacionRepo.obligaciones = listOf(
            Obligacion(id = "1", nombre = "Préstamo", saldoActual = 5000000L, cuotaReferenciaGs = 500000L, cuotasRestantes = 10, estado = "Activo")
        )
        fakeObligacionRepo.proyeccionResponse = NetworkResult.Success(
            com.gamma.mobile.domain.model.CapacidadPagoResponse(
                compromisoTotalMes = 500000L,
                fondoDisponibleMes = 5000000L,
                capacidadNeta = 4500000L,
                semaforo = "SUPERAVIT",
                dtiProyectado = 10.0
            )
        )

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo, fakeEscenarioRepo)
        advanceUntilIdle()

        viewModel.agregarGastoAdHoc("Reparación coche", 300000L)
        advanceUntilIdle()

        viewModel.sincronizarProyeccionRemota()
        advanceUntilIdle()

        val state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(300000L, state.resultado.totalGastosProyectados)
        assertEquals(4200000L, state.resultado.capacidadNeta)
        assertEquals("SUPERAVIT", state.resultado.semaforo)
    }

    @Test
    fun `sincronizarProyeccionRemota preserves local calculation if backend returns error`() = runTest {
        fakeDashboardRepo.summary = DashboardSummary(margenLibreDisponible = 5000000L)
        fakeObligacionRepo.obligaciones = listOf(
            Obligacion(id = "1", nombre = "Préstamo", saldoActual = 5000000L, cuotaReferenciaGs = 500000L, cuotasRestantes = 10, estado = "Activo")
        )
        fakeObligacionRepo.proyeccionResponse = NetworkResult.Error(500, "Backend error")

        viewModel = CapacidadPagoViewModel(fakeObligacionRepo, fakeDashboardRepo, fakeEscenarioRepo)
        advanceUntilIdle()

        val localCapacidadNeta = (viewModel.uiState.value as CapacidadPagoUiState.Success).resultado.capacidadNeta

        viewModel.sincronizarProyeccionRemota()
        advanceUntilIdle()

        val state = viewModel.uiState.value as CapacidadPagoUiState.Success
        assertEquals(localCapacidadNeta, state.resultado.capacidadNeta)
    }
}

class FakeEscenarioCapacidadRepository : EscenarioCapacidadRepository {
    val escenarios = mutableListOf<EscenarioCapacidad>()
    private val flow = MutableStateFlow<List<EscenarioCapacidad>>(emptyList())

    override fun getEscenarios(): Flow<List<EscenarioCapacidad>> = flow

    override suspend fun guardarEscenario(escenario: EscenarioCapacidad) {
        escenarios.removeAll { it.id == escenario.id }
        escenarios.add(escenario)
        flow.value = escenarios.toList()
    }

    override suspend fun eliminarEscenario(id: String) {
        escenarios.removeAll { it.id == id }
        flow.value = escenarios.toList()
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
    var lastProyectarRequest: com.gamma.mobile.domain.model.CapacidadPagoRequest? = null
    var proyeccionResponse: NetworkResult<com.gamma.mobile.domain.model.CapacidadPagoResponse>? = null

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
    override suspend fun getCronogramaVencimientos(forceRefresh: Boolean): NetworkResult<List<com.gamma.mobile.domain.model.VencimientoCronogramaItem>> = NetworkResult.Success(emptyList())
    override suspend fun getDeudaFoco(criterio: String): NetworkResult<com.gamma.mobile.domain.model.DeudaFocoResponse> = NetworkResult.Success(com.gamma.mobile.domain.model.DeudaFocoResponse())
    override suspend fun proyectarCapacidad(request: com.gamma.mobile.domain.model.CapacidadPagoRequest): NetworkResult<com.gamma.mobile.domain.model.CapacidadPagoResponse> {
        lastProyectarRequest = request
        return proyeccionResponse ?: NetworkResult.Success(com.gamma.mobile.domain.model.CapacidadPagoResponse())
    }
}
