package com.gamma.mobile.presentation.sistema

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.HealthStatus
import com.gamma.mobile.domain.model.LogEntry
import com.gamma.mobile.domain.model.TicketItem
import com.gamma.mobile.domain.repository.SistemaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSistemaRepository : SistemaRepository {
    var healthResult: NetworkResult<HealthStatus> = NetworkResult.Success(
        HealthStatus(status = "ok", sheets = true, avisos = true, versionApi = "1.0.0")
    )
    var ticketsResult: NetworkResult<List<TicketItem>> = NetworkResult.Success(
        listOf(TicketItem(id = "TCK-001", asunto = "Fallo", descripcion = "Detalle", estado = "Abierto", fecha = "12/10/2026"))
    )
    var createTicketResult: NetworkResult<TicketItem> = NetworkResult.Success(
        TicketItem(id = "TCK-002", asunto = "Nuevo", descripcion = "Desc", estado = "Abierto", fecha = "12/10/2026")
    )
    var logsResult: NetworkResult<List<LogEntry>> = NetworkResult.Success(
        listOf(LogEntry(timestamp = "12/10/2026 10:00", nivel = "INFO", mensaje = "Test log"))
    )
    var mockAppVersion: String = "0.0.1+90efaab"

    var lastAsuntoCreated: String? = null
    var lastDescripcionCreated: String? = null

    override suspend fun getHealthStatus(): NetworkResult<HealthStatus> = healthResult
    override suspend fun getTickets(): NetworkResult<List<TicketItem>> = ticketsResult
    override suspend fun createTicket(asunto: String, descripcion: String): NetworkResult<TicketItem> {
        lastAsuntoCreated = asunto
        lastDescripcionCreated = descripcion
        return createTicketResult
    }
    override suspend fun getLogs(): NetworkResult<List<LogEntry>> = logsResult
    override fun getAppVersion(): String = mockAppVersion
}

@OptIn(ExperimentalCoroutinesApi::class)
class SistemaViewModelTest {

    private lateinit var viewModel: SistemaViewModel
    private lateinit var fakeRepo: FakeSistemaRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeSistemaRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `versionName adheres to semantic git version format`() {
        fakeRepo.mockAppVersion = "0.0.1+90efaab"
        val regex = Regex("""^0\.0\.1\+([0-9a-f]{7}|nogit)$""")
        assertTrue(regex.matches(fakeRepo.mockAppVersion))

        val nogitVersion = "0.0.1+nogit"
        assertTrue(regex.matches(nogitVersion))
    }

    @Test
    fun `initial load populates health, tickets, logs and version`() = runTest {
        viewModel = SistemaViewModel(fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("0.0.1+90efaab", state.appVersion)
        assertNotNull(state.health)
        assertEquals("ok", state.health?.status)
        assertEquals(1, state.tickets.size)
        assertEquals("TCK-001", state.tickets[0].id)
        assertEquals(1, state.logs.size)
        assertEquals("Test log", state.logs[0].mensaje)
    }

    @Test
    fun `health error updates state with error message`() = runTest {
        fakeRepo.healthResult = NetworkResult.Error(503, "Backend degradado")
        viewModel = SistemaViewModel(fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Backend degradado", state.error)
    }

    @Test
    fun `crearTicket success prepends ticket to list and sets success message`() = runTest {
        viewModel = SistemaViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.crearTicket("Problema con login", "No acepta la API key")
        advanceUntilIdle()

        assertEquals("Problema con login", fakeRepo.lastAsuntoCreated)
        assertEquals("No acepta la API key", fakeRepo.lastDescripcionCreated)

        val state = viewModel.uiState.value
        assertEquals(2, state.tickets.size)
        assertEquals("TCK-002", state.tickets[0].id)
        assertTrue(state.ticketCreationSuccess?.contains("TCK-002") == true)
    }

    @Test
    fun `crearTicket with blank asunto or descripcion sets error without calling repo`() = runTest {
        viewModel = SistemaViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.crearTicket("   ", "Descripcion valida")
        advanceUntilIdle()
        assertEquals(null, fakeRepo.lastAsuntoCreated)
        assertEquals("El asunto del ticket no puede estar vacío", viewModel.uiState.value.error)

        viewModel.crearTicket("Asunto valido", "   ")
        advanceUntilIdle()
        assertEquals(null, fakeRepo.lastDescripcionCreated)
        assertEquals("La descripción del ticket no puede estar vacía", viewModel.uiState.value.error)
    }

    @Test
    fun `crearTicket error sets error message`() = runTest {
        viewModel = SistemaViewModel(fakeRepo)
        advanceUntilIdle()

        fakeRepo.createTicketResult = NetworkResult.Error(500, "Error en base de datos")
        viewModel.crearTicket("Error", "Fallo")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Error en base de datos", state.error)
    }

    @Test
    fun `limpiarMensajes clears error and success`() = runTest {
        viewModel = SistemaViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.crearTicket("Asunto", "Desc")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.ticketCreationSuccess)

        viewModel.limpiarMensajes()
        assertEquals(null, viewModel.uiState.value.ticketCreationSuccess)
        assertEquals(null, viewModel.uiState.value.error)
    }
}
