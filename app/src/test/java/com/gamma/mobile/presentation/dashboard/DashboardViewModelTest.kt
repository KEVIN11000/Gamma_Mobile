package com.gamma.mobile.presentation.dashboard

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.BootstrapResponseDto
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.AsistenciaMarcaResponse
import com.gamma.mobile.domain.model.AsistenciaMateriaResponse
import com.gamma.mobile.domain.model.ClimaInfo
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.model.FlujoCaja
import com.gamma.mobile.domain.repository.AsistenciaRepository
import com.gamma.mobile.domain.repository.AvisoRepository
import com.gamma.mobile.domain.repository.BootstrapRepository
import com.gamma.mobile.domain.repository.ClimaRepository
import com.gamma.mobile.domain.repository.DashboardRepository
import com.gamma.mobile.domain.repository.FlujoCajaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
class DashboardViewModelTest {

    private lateinit var viewModel: DashboardViewModel
    private lateinit var fakeDashboardRepo: FakeDashboardRepository
    private lateinit var fakeAvisoRepo: FakeAvisoRepository
    private lateinit var fakeAsistenciaRepo: FakeAsistenciaRepository
    private lateinit var fakeBootstrapRepo: FakeBootstrapRepository
    private lateinit var fakeFlujoCajaRepo: FakeFlujoCajaRepository
    private lateinit var fakeClimaRepo: FakeClimaRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDashboardRepo = FakeDashboardRepository()
        fakeAvisoRepo = FakeAvisoRepository()
        fakeAsistenciaRepo = FakeAsistenciaRepository()
        fakeBootstrapRepo = FakeBootstrapRepository()
        fakeFlujoCajaRepo = FakeFlujoCajaRepository()
        fakeClimaRepo = FakeClimaRepository()
        // No instanciamos el viewModel aún para poder configurar los fakes antes del init block
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadDashboard with successful data emits Success state`() = runTest {
        val summary = DashboardSummary(100, 200, 50, false)
        val avisos = listOf(Aviso("1", "Aviso 1", "06/10/2026 12:00"))
        
        fakeDashboardRepo.result = NetworkResult.Success(summary)
        fakeAvisoRepo.result = NetworkResult.Success(avisos)

        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-06T10:00:00Z"), java.time.ZoneId.of("UTC"))
        viewModel = DashboardViewModel(fakeDashboardRepo, fakeAvisoRepo, clock)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        assertEquals(summary, (state as DashboardUiState.Success).summary)
        assertEquals(1, state.avisos.size)
    }

    @Test
    fun `loadDashboard with zeroes emits Empty state`() = runTest {
        val summary = DashboardSummary(
            margenLibreDisponible = 0,
            saldoAcumulado = 0,
            posicionIva = 0,
            esDebitoFiscal = false
        )
        fakeDashboardRepo.result = NetworkResult.Success(summary)
        fakeAvisoRepo.result = NetworkResult.Success(emptyList())

        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-06T10:00:00Z"), java.time.ZoneId.of("UTC"))
        viewModel = DashboardViewModel(fakeDashboardRepo, fakeAvisoRepo, clock)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Empty)
    }

    @Test
    fun `loadDashboard network exception emits Error state with offline flag`() = runTest {
        fakeDashboardRepo.result = NetworkResult.Exception(java.net.SocketTimeoutException())
        fakeAvisoRepo.result = NetworkResult.Success(emptyList())

        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-06T10:00:00Z"), java.time.ZoneId.of("UTC"))
        viewModel = DashboardViewModel(fakeDashboardRepo, fakeAvisoRepo, clock)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Error)
        assertTrue((state as DashboardUiState.Error).isOffline)
    }

    @Test
    fun `loadDashboard API error emits Error state without offline flag`() = runTest {
        fakeDashboardRepo.result = NetworkResult.Error(500, "Server Error")
        fakeAvisoRepo.result = NetworkResult.Success(emptyList())

        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-06T10:00:00Z"), java.time.ZoneId.of("UTC"))
        viewModel = DashboardViewModel(fakeDashboardRepo, fakeAvisoRepo, clock)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Error)
        assertEquals(false, (state as DashboardUiState.Error).isOffline)
    }

    @Test
    fun `loadDashboard filters past avisos and sorts correctly with fixed clock`() = runTest {
        val summary = DashboardSummary(100, 200, 50, false)
        // Fijamos la fecha a 2026-10-06 10:00 (UTC)
        val fixedClock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-06T10:00:00Z"), java.time.ZoneId.of("UTC"))
        
        // 05/10/2026 = Pasado (debe omitirse)
        // 06/10/2026 = Hoy
        // 07/10/2026 = Mañana
        // 13/10/2026 = 7 días
        // 14/10/2026 = 8 días (debe omitirse)
        
        val avisos = listOf(
            Aviso("1", "Pasado", "05/10/2026 12:00"),
            Aviso("2", "7 días", "13/10/2026 12:00"),
            Aviso("3", "Hoy", "06/10/2026 15:00"),
            Aviso("4", "Mañana", "07/10/2026 09:00"),
            Aviso("5", "Fuera rango", "14/10/2026 12:00")
        )
        
        fakeDashboardRepo.result = NetworkResult.Success(summary)
        fakeAvisoRepo.result = NetworkResult.Success(avisos)

        viewModel = DashboardViewModel(fakeDashboardRepo, fakeAvisoRepo, fixedClock)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val uiAvisos = (state as DashboardUiState.Success).avisos
        
        // Solo 3 avisos válidos: Hoy, Mañana, 7 días. Ordenados.
        assertEquals(3, uiAvisos.size)
        assertEquals("Hoy", uiAvisos[0].titulo)
        assertEquals("Hoy", uiAvisos[0].textoTiempo)
        assertEquals(true, uiAvisos[0].esCritico)
        
        assertEquals("Mañana", uiAvisos[1].titulo)
        assertEquals("Mañana", uiAvisos[1].textoTiempo)
        assertEquals(true, uiAvisos[1].esCritico)
        
        assertEquals("7 días", uiAvisos[2].titulo)
        assertEquals("En 7 días", uiAvisos[2].textoTiempo)
        assertEquals(false, uiAvisos[2].esCritico)
    }

    @Test
    fun `marcarAsistencia success sets message and error flag false`() = runTest {
        val summary = DashboardSummary(100, 200, 50, false)
        fakeDashboardRepo.result = NetworkResult.Success(summary)
        fakeAvisoRepo.result = NetworkResult.Success(emptyList())
        fakeAsistenciaRepo.marcaResult = NetworkResult.Success(
            AsistenciaMarcaResponse(tipo = "entrada", hora = "08:15", mensaje = "Entrada registrada correctamente")
        )

        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-06T10:00:00Z"), java.time.ZoneId.of("UTC"))
        viewModel = DashboardViewModel(fakeDashboardRepo, fakeAvisoRepo, clock, fakeAsistenciaRepo)
        advanceUntilIdle()

        viewModel.marcarAsistencia()
        advanceUntilIdle()

        val msg = viewModel.asistenciaMessage.value
        assertFalse(msg.isNullOrEmpty())
        assertTrue(msg!!.contains("Entrada registrada"))
        assertFalse(viewModel.asistenciaIsError.value)
    }

    @Test
    fun `marcarAsistencia sets dispatching message with clock time before completion`() = runTest {
        val summary = DashboardSummary(100, 200, 50, false)
        fakeDashboardRepo.result = NetworkResult.Success(summary)
        fakeAvisoRepo.result = NetworkResult.Success(emptyList())
        fakeAsistenciaRepo.delayMs = 200L
        fakeAsistenciaRepo.marcaResult = NetworkResult.Success(
            AsistenciaMarcaResponse(tipo = "entrada", hora = "10:15", mensaje = "Entrada registrada", planilla = "Octubre 2026", confirmadoEnPlanilla = true)
        )

        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-06T10:15:00Z"), java.time.ZoneId.of("UTC"))
        viewModel = DashboardViewModel(fakeDashboardRepo, fakeAvisoRepo, clock, fakeAsistenciaRepo)
        advanceUntilIdle()

        viewModel.marcarAsistencia()
        runCurrent()
        // Stage 1: Enqueued / Sending
        val sendingMsg = viewModel.asistenciaMessage.value
        assertNotNull(sendingMsg)
        assertTrue(sendingMsg!!.contains("Marcación enviada al servidor"))
        assertTrue(sendingMsg.contains("10:15"))
        assertFalse(viewModel.asistenciaIsError.value)

        // Stage 2: Confirmed in spreadsheet
        advanceUntilIdle()
        val finalMsg = viewModel.asistenciaMessage.value
        assertNotNull(finalMsg)
        assertTrue(finalMsg!!.startsWith("✅"))
        assertTrue(finalMsg.contains("10:15"))
        assertTrue(finalMsg.contains("Octubre 2026"))
        assertFalse(viewModel.asistenciaIsError.value)
    }

    @Test
    fun `marcarAsistencia with error keywords sets error flag true`() = runTest {
        val summary = DashboardSummary(100, 200, 50, false)
        fakeDashboardRepo.result = NetworkResult.Success(summary)
        fakeAvisoRepo.result = NetworkResult.Success(emptyList())
        fakeAsistenciaRepo.marcaResult = NetworkResult.Success(
            AsistenciaMarcaResponse(tipo = "error", hora = "08:15", mensaje = "No se pudo conectar con la planilla")
        )

        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-06T10:00:00Z"), java.time.ZoneId.of("UTC"))
        viewModel = DashboardViewModel(fakeDashboardRepo, fakeAvisoRepo, clock, fakeAsistenciaRepo)
        advanceUntilIdle()

        viewModel.marcarAsistencia()
        advanceUntilIdle()

        val msg = viewModel.asistenciaMessage.value
        assertFalse(msg.isNullOrEmpty())
        assertTrue(viewModel.asistenciaIsError.value)
    }

    @Test
    fun `marcarAsistencia with network error sets error message`() = runTest {
        val summary = DashboardSummary(100, 200, 50, false)
        fakeDashboardRepo.result = NetworkResult.Success(summary)
        fakeAvisoRepo.result = NetworkResult.Success(emptyList())
        fakeAsistenciaRepo.marcaResult = NetworkResult.Error(500, "Error de servidor")

        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-06T10:00:00Z"), java.time.ZoneId.of("UTC"))
        viewModel = DashboardViewModel(fakeDashboardRepo, fakeAvisoRepo, clock, fakeAsistenciaRepo)
        advanceUntilIdle()

        viewModel.marcarAsistencia()
        advanceUntilIdle()

        val msg = viewModel.asistenciaMessage.value
        assertFalse(msg.isNullOrEmpty())
        assertTrue(viewModel.asistenciaIsError.value)
    }

    @Test
    fun `setModoMarcacion accepts valid modes only`() = runTest {
        val summary = DashboardSummary(100, 200, 50, false)
        fakeDashboardRepo.result = NetworkResult.Success(summary)
        fakeAvisoRepo.result = NetworkResult.Success(emptyList())

        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-06T10:00:00Z"), java.time.ZoneId.of("UTC"))
        viewModel = DashboardViewModel(fakeDashboardRepo, fakeAvisoRepo, clock)
        advanceUntilIdle()

        assertEquals(DashboardViewModel.MODO_NORMAL, viewModel.modoMarcacion.value)

        viewModel.setModoMarcacion(DashboardViewModel.MODO_DIRECTO)
        assertEquals(DashboardViewModel.MODO_DIRECTO, viewModel.modoMarcacion.value)

        viewModel.setModoMarcacion("invalido")
        assertEquals(DashboardViewModel.MODO_DIRECTO, viewModel.modoMarcacion.value)

        viewModel.setModoMarcacion(DashboardViewModel.MODO_NORMAL)
        assertEquals(DashboardViewModel.MODO_NORMAL, viewModel.modoMarcacion.value)
    }

    @Test
    fun `onScreenVisible reloads dashboard without resetting ui to Loading`() = runTest {
        val initialSummary = DashboardSummary(100, 200, 50, false)
        val updatedSummary = DashboardSummary(500, 800, 70, false)

        fakeDashboardRepo.result = NetworkResult.Success(initialSummary)
        fakeAvisoRepo.result = NetworkResult.Success(emptyList())

        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-06T10:00:00Z"), java.time.ZoneId.of("UTC"))
        viewModel = DashboardViewModel(fakeDashboardRepo, fakeAvisoRepo, clock)
        advanceUntilIdle()

        val state1 = viewModel.uiState.value
        assertTrue(state1 is DashboardUiState.Success)
        assertEquals(100L, (state1 as DashboardUiState.Success).summary.margenLibreDisponible)

        // Cambiamos el repo para simular nuevos datos tras una transacción
        fakeDashboardRepo.result = NetworkResult.Success(updatedSummary)
        viewModel.onScreenVisible()
        advanceUntilIdle()

        val state2 = viewModel.uiState.value
        assertTrue(state2 is DashboardUiState.Success)
        assertEquals(500L, (state2 as DashboardUiState.Success).summary.margenLibreDisponible)
        assertEquals(false, fakeDashboardRepo.lastForceRefresh)
        assertEquals(false, fakeAvisoRepo.lastForceRefresh)
    }

    @Test
    fun `loadDashboard with BootstrapRepository success hydrates atomically`() = runTest {
        val summary = DashboardSummary(margenLibreDisponible = 3000000L, saldoAcumulado = 1500000L)
        val avisos = listOf(Aviso("b1", "Vencimiento Tarjeta", "06/10/2026 18:00"))
        val bootstrapDto = BootstrapResponseDto(
            status = "ok",
            summary = summary,
            proximosAvisos = avisos,
            periodoActual = "Octubre 2026"
        )
        fakeBootstrapRepo.syncResult = NetworkResult.Success(bootstrapDto)
        fakeFlujoCajaRepo.cached = FlujoCaja(saldoActual = 1500000L, saldoProyectado30d = 1200000L)
        fakeClimaRepo.cached = ClimaInfo(temperaturaActual = 26.0, condicion = "Soleado")

        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-06T10:00:00Z"), java.time.ZoneId.of("UTC"))
        viewModel = DashboardViewModel(
            dashboardRepository = fakeDashboardRepo,
            avisoRepository = fakeAvisoRepo,
            clock = clock,
            asistenciaRepository = fakeAsistenciaRepo,
            bootstrapRepository = fakeBootstrapRepo,
            flujoCajaRepository = fakeFlujoCajaRepo,
            climaRepository = fakeClimaRepo
        )
        advanceUntilIdle()

        assertTrue(fakeBootstrapRepo.syncCalled)
        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success
        assertEquals(3000000L, success.summary.margenLibreDisponible)
        assertEquals(1, success.avisos.size)
        assertEquals("Vencimiento Tarjeta", success.avisos[0].titulo)
        assertEquals("Octubre 2026", success.periodoActual)
        assertNotNull(success.flujoCaja)
        assertEquals(1500000L, success.flujoCaja!!.saldoActual)
        assertNotNull(success.clima)
        assertEquals(26.0, success.clima!!.temperaturaActual, 0.01)
    }

    @Test
    fun `loadDashboard with BootstrapRepository failure falls back to cached data without destructive error`() = runTest {
        fakeBootstrapRepo.syncResult = NetworkResult.Exception(java.net.SocketTimeoutException())
        // Room/Cache tiene datos locales
        val cachedSummary = DashboardSummary(margenLibreDisponible = 900000L, saldoAcumulado = 450000L)
        val cachedAvisos = listOf(Aviso("c1", "Aviso Cacheado", "06/10/2026 12:00"))
        fakeDashboardRepo.result = NetworkResult.Success(cachedSummary)
        fakeAvisoRepo.result = NetworkResult.Success(cachedAvisos)

        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-06T10:00:00Z"), java.time.ZoneId.of("UTC"))
        viewModel = DashboardViewModel(
            dashboardRepository = fakeDashboardRepo,
            avisoRepository = fakeAvisoRepo,
            clock = clock,
            asistenciaRepository = fakeAsistenciaRepo,
            bootstrapRepository = fakeBootstrapRepo,
            flujoCajaRepository = fakeFlujoCajaRepo,
            climaRepository = fakeClimaRepo
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Success)
        val success = state as DashboardUiState.Success
        assertEquals(900000L, success.summary.margenLibreDisponible)
        assertEquals("Aviso Cacheado", success.avisos[0].titulo)
    }

    @Test
    fun `loadDashboard with BootstrapRepository failure and no cache emits Error state`() = runTest {
        fakeBootstrapRepo.syncResult = NetworkResult.Error(500, "Backend error")
        fakeDashboardRepo.result = NetworkResult.Error(500, "No cache")
        fakeAvisoRepo.result = NetworkResult.Error(500, "No cache")

        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-06T10:00:00Z"), java.time.ZoneId.of("UTC"))
        viewModel = DashboardViewModel(
            dashboardRepository = fakeDashboardRepo,
            avisoRepository = fakeAvisoRepo,
            clock = clock,
            asistenciaRepository = fakeAsistenciaRepo,
            bootstrapRepository = fakeBootstrapRepo,
            flujoCajaRepository = fakeFlujoCajaRepo,
            climaRepository = fakeClimaRepo
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Error)
    }

    @Test
    fun `flujoCaja and clima flows update DashboardUiState Success reactively`() = runTest {
        val summary = DashboardSummary(100, 200, 50, false)
        val bootstrapDto = BootstrapResponseDto(
            status = "ok",
            summary = summary,
            proximosAvisos = emptyList()
        )
        fakeBootstrapRepo.syncResult = NetworkResult.Success(bootstrapDto)

        val clock = java.time.Clock.fixed(java.time.Instant.parse("2026-10-06T10:00:00Z"), java.time.ZoneId.of("UTC"))
        viewModel = DashboardViewModel(
            dashboardRepository = fakeDashboardRepo,
            avisoRepository = fakeAvisoRepo,
            clock = clock,
            asistenciaRepository = fakeAsistenciaRepo,
            bootstrapRepository = fakeBootstrapRepo,
            flujoCajaRepository = fakeFlujoCajaRepo,
            climaRepository = fakeClimaRepo
        )
        advanceUntilIdle()

        val stateInitial = viewModel.uiState.value
        assertTrue(stateInitial is DashboardUiState.Success)
        assertEquals(null, (stateInitial as DashboardUiState.Success).flujoCaja)

        // Emisión reactiva desde Room
        fakeFlujoCajaRepo.flow.value = FlujoCaja(saldoActual = 500000L, saldoProyectado30d = 300000L)
        fakeClimaRepo.flow.value = ClimaInfo(temperaturaActual = 31.0, condicion = "Caluroso")
        advanceUntilIdle()

        val stateUpdated = viewModel.uiState.value
        assertTrue(stateUpdated is DashboardUiState.Success)
        val success = stateUpdated as DashboardUiState.Success
        assertEquals(500000L, success.flujoCaja?.saldoActual)
        assertEquals(31.0, success.clima?.temperaturaActual ?: 0.0, 0.01)
    }
}

class FakeDashboardRepository : DashboardRepository {
    var result: NetworkResult<DashboardSummary> = NetworkResult.Error(500, "Uninitialized")
    var lastForceRefresh: Boolean? = null
    
    override suspend fun getSummary(forceRefresh: Boolean): NetworkResult<DashboardSummary> {
        lastForceRefresh = forceRefresh
        return result
    }
}

class FakeAvisoRepository : AvisoRepository {
    var result: NetworkResult<List<Aviso>> = NetworkResult.Success(emptyList())
    var lastForceRefresh: Boolean? = null
    
    override suspend fun getAvisos(forceRefresh: Boolean): NetworkResult<List<Aviso>> {
        lastForceRefresh = forceRefresh
        return result
    }

    override suspend fun createAviso(aviso: Aviso): NetworkResult<Unit> = NetworkResult.Success(Unit)
    override suspend fun deleteAviso(id: String): NetworkResult<Unit> = NetworkResult.Success(Unit)
    override suspend fun updateAviso(aviso: Aviso): NetworkResult<Unit> = NetworkResult.Success(Unit)
    override suspend fun interpretarAvisoIa(texto: String): NetworkResult<Aviso> = NetworkResult.Success(Aviso())
}

class FakeAsistenciaRepository : AsistenciaRepository {
    var marcaResult: NetworkResult<AsistenciaMarcaResponse> =
        NetworkResult.Success(AsistenciaMarcaResponse(tipo = "entrada", hora = "08:00", mensaje = "OK"))
    var materiaResult: NetworkResult<AsistenciaMateriaResponse> =
        NetworkResult.Success(AsistenciaMateriaResponse(materia = "Test", hora = "08:00", mensaje = "OK"))
    var materiasListResult: NetworkResult<List<String>> = NetworkResult.Success(emptyList())
    var delayMs: Long = 0L

    override suspend fun marcarAsistencia(modo: String): NetworkResult<AsistenciaMarcaResponse> {
        if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
        return marcaResult
    }
    override suspend fun marcarMateria(materia: String): NetworkResult<AsistenciaMateriaResponse> = materiaResult
    override suspend fun getMaterias(): NetworkResult<List<String>> = materiasListResult
    override suspend fun getPeriodos(): NetworkResult<com.gamma.mobile.domain.model.PeriodosResponse> =
        NetworkResult.Success(com.gamma.mobile.domain.model.PeriodosResponse("Octubre 2026", "Octubre 2026", true))
    override suspend fun getAnaliticas(hoja: String?): NetworkResult<com.gamma.mobile.domain.model.AnaliticasHorasResponse> =
        NetworkResult.Success(com.gamma.mobile.domain.model.AnaliticasHorasResponse(periodo = "Octubre 2026"))
    override suspend fun descargarReporteExcel(
        hoja: String?,
        descuento: Double,
        saldoAnterior: Double,
        incluirIva: Boolean
    ): NetworkResult<ByteArray> = NetworkResult.Success(byteArrayOf(1, 2, 3))
    override suspend fun descargarReportePdfAsistencia(
        hoja: String?,
        descuento: Double,
        saldoAnterior: Double,
        incluirIva: Boolean
    ): NetworkResult<ByteArray> = NetworkResult.Success(byteArrayOf(4, 5, 6))
}

class FakeBootstrapRepository : BootstrapRepository {
    var syncResult: NetworkResult<BootstrapResponseDto> = NetworkResult.Error(500, "Uninitialized")
    var syncCalled = false

    override suspend fun syncBootstrap(): NetworkResult<BootstrapResponseDto> {
        syncCalled = true
        return syncResult
    }
}

class FakeFlujoCajaRepository : FlujoCajaRepository {
    val flow = MutableStateFlow<FlujoCaja?>(null)
    var cached: FlujoCaja? = null
    var refreshResult: NetworkResult<FlujoCaja> = NetworkResult.Error(500, "Uninitialized")

    override fun getFlujoCajaFlow(): Flow<FlujoCaja?> = flow
    override suspend fun getFlujoCajaCached(): FlujoCaja? = cached
    override suspend fun refreshFlujoCaja(
        saldoInicial: Long?,
        gastoDiario: Long?,
        umbralReserva: Long?
    ): NetworkResult<FlujoCaja> = refreshResult
}

class FakeClimaRepository : ClimaRepository {
    val flow = MutableStateFlow<ClimaInfo?>(null)
    var cached: ClimaInfo? = null
    var refreshResult: NetworkResult<ClimaInfo> = NetworkResult.Error(500, "Uninitialized")

    override fun getClimaFlow(): Flow<ClimaInfo?> = flow
    override suspend fun getClimaCached(): ClimaInfo? = cached
    override suspend fun refreshClima(): NetworkResult<ClimaInfo> = refreshResult
}

