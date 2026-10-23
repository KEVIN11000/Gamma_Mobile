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
import org.junit.Assert.assertFalse
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
        
        assertEquals(1, fakeRepo.createdList.size)
    }

    @Test
    fun `crearObligacion with full model preserves all extended attributes`() = runTest {
        fakeRepo.data = emptyList()
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        val obligacionCompleta = Obligacion(
            id = "ext-1",
            nombre = "Préstamo Auto",
            montoInicial = 12000000,
            saldoActual = 12000000,
            estado = "Activo",
            entidad = "Banco Continental",
            tipo = "Préstamo",
            cuotaReferenciaGs = 1000000,
            cuotasTotales = 12,
            cuotasRestantes = 12,
            diaVencimiento = 15,
            fechaInicio = "15/11/2026",
            observaciones = "Cuotas con seguro incluido"
        )

        viewModel.crearObligacion(obligacionCompleta)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals(1, state.obligaciones.size)
        val guardada = state.obligaciones[0]
        assertEquals("Banco Continental", guardada.entidad)
        assertEquals(1000000L, guardada.cuotaReferenciaGs)
        assertEquals(15, guardada.diaVencimiento)
        assertEquals(1, fakeRepo.createdList.size)
    }

    @Test
    fun `eliminarObligacion removes item optimistically and calls repo`() = runTest {
        val o1 = Obligacion("1", "Deuda 1", 1000, 1000, "Activo")
        val o2 = Obligacion("2", "Deuda 2", 2000, 2000, "Activo")
        fakeRepo.data = listOf(o1, o2)
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        viewModel.eliminarObligacion("1")
        advanceUntilIdle()

        val state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals(1, state.obligaciones.size)
        assertEquals("2", state.obligaciones[0].id)
        assertTrue(fakeRepo.deletedIds.contains("1"))
    }

    @Test
    fun `cancelarObligacionSimulada removes simulated debt`() = runTest {
        val simulada = Obligacion("sim-1", "Proyección", 5000, 5000, "Simulado")
        fakeRepo.data = listOf(simulada)
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        viewModel.cancelarObligacionSimulada(simulada)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals(0, state.obligaciones.size)
        assertTrue(fakeRepo.deletedIds.contains("sim-1"))
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
        assertTrue(state.obligaciones[0].esSaldada)
    }

    @Test
    fun `simularCuota maps to correct NivelRiesgo based on margin and accepts custom installment`() = runTest {
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

        // Cuota personalizada explícita (e.g. monto 3000 pero cuota con interés 800 -> Rojo)
        viewModel.simularCuota(monto = 3000, cuotas = 10, cuotaPersonalizada = 800)
        advanceUntilIdle()
        assertEquals(NivelRiesgo.ROJO, viewModel.simulacionResult.value?.nivelRiesgo)
        assertEquals(800L, viewModel.simulacionResult.value?.cuotaMensual)
    }

    @Test
    fun `setFiltro filters list by Activas Saldadas and Simuladas`() = runTest {
        val list = listOf(
            Obligacion("1", "Deuda 1", 100000, 100000, "Activo"),
            Obligacion("2", "Deuda 2", 200000, 0, "Saldado"),
            Obligacion("3", "Deuda 3", 300000, 300000, "Simulado")
        )
        fakeRepo.data = list
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        // Por defecto: TODAS (size 3)
        var state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals(3, state.obligaciones.size)

        // Filtrar ACTIVAS (size 1)
        viewModel.setFiltro(FiltroObligacion.ACTIVAS)
        state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals(1, state.obligaciones.size)
        assertEquals("Activo", state.obligaciones[0].estado)

        // Filtrar SALDADAS (size 1)
        viewModel.setFiltro(FiltroObligacion.SALDADAS)
        state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals(1, state.obligaciones.size)
        assertEquals("Saldado", state.obligaciones[0].estado)

        // Filtrar SIMULADAS (size 1)
        viewModel.setFiltro(FiltroObligacion.SIMULADAS)
        state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals(1, state.obligaciones.size)
        assertEquals("Simulado", state.obligaciones[0].estado)
    }

    @Test
    fun `setFiltro handles grammatical variations like Activa Saldada Simulada and zero balance`() = runTest {
        val list = listOf(
            Obligacion("1", "Deuda 1", 100000, 100000, "Activa"),
            Obligacion("2", "Deuda 2", 200000, 0, "Saldada"),
            Obligacion("3", "Deuda 3", 300000, 300000, "Simulada"),
            Obligacion("4", "Deuda 4", 400000, 0, "Activo")
        )
        fakeRepo.data = list
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        // Filtrar ACTIVAS: Solo id="1" (id="4" tiene saldo 0)
        viewModel.setFiltro(FiltroObligacion.ACTIVAS)
        var state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals(1, state.obligaciones.size)
        assertEquals("1", state.obligaciones[0].id)

        // Filtrar SALDADAS: id="2" y id="4"
        viewModel.setFiltro(FiltroObligacion.SALDADAS)
        state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals(2, state.obligaciones.size)
        assertTrue(state.obligaciones.all { it.esSaldada })

        // Filtrar SIMULADAS: id="3"
        viewModel.setFiltro(FiltroObligacion.SIMULADAS)
        state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals(1, state.obligaciones.size)
        assertEquals("3", state.obligaciones[0].id)
    }

    @Test
    fun `abonarObligacion updates debt balance and invokes repository abonarObligacion`() = runTest {
        val o1 = Obligacion("1", "Préstamo", 1000000, 1000000, "Activo", cuotasRestantes = 10)
        fakeRepo.data = listOf(o1)
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        viewModel.abonarObligacion(o1, 200000)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals(800000L, state.obligaciones[0].saldoActual)
        assertEquals(9, state.obligaciones[0].cuotasRestantes)
        assertEquals(1, fakeRepo.abonosList.size)
        assertEquals("1" to 200000L, fakeRepo.abonosList[0])
    }

    @Test
    fun `pausarObligacion changes estado to Pausada and excludes from compromisoMensual`() = runTest {
        val o1 = Obligacion("1", "Préstamo", 1000000, 1000000, "Activo", cuotaReferenciaGs = 200000)
        val o2 = Obligacion("2", "Tarjeta", 500000, 500000, "Activo", cuotaReferenciaGs = 100000)
        fakeRepo.data = listOf(o1, o2)
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        // Compromiso mensual inicial: 200.000 + 100.000 = 300.000
        assertEquals(300000L, viewModel.compromisoMensual.value)

        // Pausar o1
        viewModel.pausarObligacion(o1)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ObligacionesUiState.Success
        val o1Actualizada = state.obligaciones.first { it.id == "1" }
        assertEquals("Pausada", o1Actualizada.estado)
        assertTrue(o1Actualizada.esPausada)
        assertFalse(o1Actualizada.esActiva)

        // Compromiso mensual recalculado excluyendo la deuda pausada: solo 100.000
        assertEquals(100000L, viewModel.compromisoMensual.value)
        assertEquals("Pausada", fakeRepo.updatedList.last().estado)
    }

    @Test
    fun `reanudarObligacion changes estado to Activo and restores into compromisoMensual`() = runTest {
        val oPausada = Obligacion("1", "Préstamo", 1000000, 1000000, "Pausada", cuotaReferenciaGs = 200000)
        fakeRepo.data = listOf(oPausada)
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        assertEquals(0L, viewModel.compromisoMensual.value)

        viewModel.reanudarObligacion(oPausada)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals("Activo", state.obligaciones[0].estado)
        assertTrue(state.obligaciones[0].esActiva)
        assertEquals(200000L, viewModel.compromisoMensual.value)
    }

    @Test
    fun `togglePausarObligacion alternates correctly between Pausada and Activo`() = runTest {
        val oActiva = Obligacion("1", "Préstamo", 1000000, 1000000, "Activo", cuotaReferenciaGs = 150000)
        fakeRepo.data = listOf(oActiva)
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        // Toggle: activa -> pausada
        viewModel.togglePausarObligacion(oActiva)
        advanceUntilIdle()
        var item = (viewModel.uiState.value as ObligacionesUiState.Success).obligaciones[0]
        assertEquals("Pausada", item.estado)
        assertEquals(0L, viewModel.compromisoMensual.value)

        // Toggle: pausada -> activa
        viewModel.togglePausarObligacion(item)
        advanceUntilIdle()
        item = (viewModel.uiState.value as ObligacionesUiState.Success).obligaciones[0]
        assertEquals("Activo", item.estado)
        assertEquals(150000L, viewModel.compromisoMensual.value)
    }

    @Test
    fun `setFiltro PAUSADAS displays only paused obligations`() = runTest {
        val list = listOf(
            Obligacion("1", "Deuda Activa", 100000, 100000, "Activo"),
            Obligacion("2", "Deuda Pausada 1", 200000, 200000, "Pausada"),
            Obligacion("3", "Deuda Pausada 2", 300000, 300000, "Pausado"),
            Obligacion("4", "Deuda Saldada", 400000, 0, "Saldado")
        )
        fakeRepo.data = list
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        viewModel.setFiltro(FiltroObligacion.PAUSADAS)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ObligacionesUiState.Success
        assertEquals(2, state.obligaciones.size)
        assertTrue(state.obligaciones.all { it.esPausada })
        assertEquals(listOf("2", "3"), state.obligaciones.map { it.id })
    }

    @Test
    fun `loadObligaciones loads cronograma de vencimientos and updates StateFlow`() = runTest {
        val cronograma = listOf(
            com.gamma.mobile.domain.model.VencimientoCronogramaItem(
                id = "v1",
                nombre = "Préstamo Banco",
                diaVencimiento = 10,
                cuotaGs = 450000L
            )
        )
        fakeRepo.cronogramaItems = cronograma
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        assertEquals(1, viewModel.cronogramaVencimientos.value.size)
        assertEquals("v1", viewModel.cronogramaVencimientos.value[0].id)
        assertEquals(450000L, viewModel.cronogramaVencimientos.value[0].cuotaGs)
    }

    @Test
    fun `loadObligaciones loads deuda foco with default criterio saldo`() = runTest {
        fakeRepo.deudaFocoMap["saldo"] = com.gamma.mobile.domain.model.DeudaFocoResponse(
            idDeuda = "d_foco",
            nombre = "Tarjeta Express",
            criterio = "saldo",
            saldoActual = 500000L
        )
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        val foco = viewModel.deudaFoco.value
        assertEquals("d_foco", foco?.idDeuda)
        assertEquals("saldo", viewModel.criterioFoco.value)
    }

    @Test
    fun `cambiarCriterioFoco updates criterio and requests new deuda foco`() = runTest {
        fakeRepo.deudaFocoMap["saldo"] = com.gamma.mobile.domain.model.DeudaFocoResponse(
            idDeuda = "d_saldo",
            criterio = "saldo"
        )
        fakeRepo.deudaFocoMap["prioridad"] = com.gamma.mobile.domain.model.DeudaFocoResponse(
            idDeuda = "d_prioridad",
            criterio = "prioridad"
        )
        viewModel = ObligacionesViewModel(fakeRepo, fakeDashboardRepo)
        advanceUntilIdle()

        assertEquals("d_saldo", viewModel.deudaFoco.value?.idDeuda)

        viewModel.cambiarCriterioFoco("prioridad")
        advanceUntilIdle()

        assertEquals("prioridad", viewModel.criterioFoco.value)
        assertEquals("d_prioridad", viewModel.deudaFoco.value?.idDeuda)
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
    var cronogramaItems: List<com.gamma.mobile.domain.model.VencimientoCronogramaItem> = emptyList()
    val deudaFocoMap = mutableMapOf<String, com.gamma.mobile.domain.model.DeudaFocoResponse>()
    val createdList = mutableListOf<Obligacion>()
    val updatedList = mutableListOf<Obligacion>()
    val deletedIds = mutableListOf<String>()
    val abonosList = mutableListOf<Pair<String, Long>>()

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

    override suspend fun deleteObligacion(id: String): NetworkResult<Unit> {
        deletedIds.add(id)
        return NetworkResult.Success(Unit)
    }

    override suspend fun abonarObligacion(idObligacion: String, monto: Long): NetworkResult<Unit> {
        abonosList.add(idObligacion to monto)
        return NetworkResult.Success(Unit)
    }

    override suspend fun getCronogramaVencimientos(forceRefresh: Boolean): NetworkResult<List<com.gamma.mobile.domain.model.VencimientoCronogramaItem>> = NetworkResult.Success(cronogramaItems)
    override suspend fun getDeudaFoco(criterio: String): NetworkResult<com.gamma.mobile.domain.model.DeudaFocoResponse> = NetworkResult.Success(deudaFocoMap[criterio] ?: com.gamma.mobile.domain.model.DeudaFocoResponse(criterio = criterio))
    override suspend fun proyectarCapacidad(request: com.gamma.mobile.domain.model.CapacidadPagoRequest): NetworkResult<com.gamma.mobile.domain.model.CapacidadPagoResponse> = NetworkResult.Success(com.gamma.mobile.domain.model.CapacidadPagoResponse())
}
