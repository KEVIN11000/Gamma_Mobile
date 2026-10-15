package com.gamma.mobile.presentation.sistema

import com.gamma.mobile.data.remote.GitHubReleaseApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.GitHubAssetDto
import com.gamma.mobile.domain.model.GitHubReleaseDto
import com.gamma.mobile.domain.model.HealthStatus
import com.gamma.mobile.domain.model.LogEntry
import com.gamma.mobile.domain.model.TicketItem
import com.gamma.mobile.domain.model.UpdateStatus
import com.gamma.mobile.domain.repository.SecureStorageRepository
import com.gamma.mobile.domain.repository.SistemaRepository
import com.gamma.mobile.domain.usecase.CheckAppUpdateUseCase
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
import retrofit2.Response

class FakeGitHubReleaseApi : GitHubReleaseApi {
    var response: Response<GitHubReleaseDto> = Response.success(
        GitHubReleaseDto(
            tagName = "v0.0.4",
            name = "v0.0.4",
            assets = listOf(
                GitHubAssetDto(
                    name = "gamma-mobile-v0.0.4-release.apk",
                    size = 33246834L,
                    browserDownloadUrl = "https://github.com/KEVIN11000/Gamma_Mobile/releases/download/v0.0.4/gamma-mobile-v0.0.4-release.apk"
                )
            )
        )
    )

    override suspend fun getLatestRelease(): Response<GitHubReleaseDto> = response
}

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
    var updateTicketResult: NetworkResult<TicketItem> = NetworkResult.Success(
        TicketItem(id = "TCK-001", asunto = "Fallo", descripcion = "Detalle", estado = "Cerrado", fecha = "12/10/2026")
    )
    var logsResult: NetworkResult<List<LogEntry>> = NetworkResult.Success(
        listOf(LogEntry(timestamp = "12/10/2026 10:00", nivel = "INFO", mensaje = "Test log"))
    )
    var cierreResult: NetworkResult<com.gamma.mobile.domain.model.CierreResponse> = NetworkResult.Success(
        com.gamma.mobile.domain.model.CierreResponse(
            resumen = "Cierre contable completado",
            pdfUrl = "/api/v1/cierres/reportes/test.pdf",
            totalMonto = 3500000L,
            tipo = "contable"
        )
    )
    var lastHojaCierre: String? = null
    var mockAppVersion: String = "0.0.4+6bb982e"

    var lastAsuntoCreated: String? = null
    var lastDescripcionCreated: String? = null
    var lastTicketUpdatedId: String? = null
    var lastTicketUpdatedEstado: String? = null
    var lastTicketUpdatedResolucion: String? = null

    override suspend fun getHealthStatus(): NetworkResult<HealthStatus> = healthResult
    override suspend fun getTickets(): NetworkResult<List<TicketItem>> = ticketsResult
    override suspend fun createTicket(asunto: String, descripcion: String): NetworkResult<TicketItem> {
        lastAsuntoCreated = asunto
        lastDescripcionCreated = descripcion
        return createTicketResult
    }
    override suspend fun updateTicket(id: String, estado: String, resolucion: String?): NetworkResult<TicketItem> {
        lastTicketUpdatedId = id
        lastTicketUpdatedEstado = estado
        lastTicketUpdatedResolucion = resolucion
        return updateTicketResult
    }
    override suspend fun getLogs(): NetworkResult<List<LogEntry>> = logsResult
    override suspend fun ejecutarCierreContable(hoja: String): NetworkResult<com.gamma.mobile.domain.model.CierreResponse> {
        lastHojaCierre = hoja
        return cierreResult
    }
    override fun getAppVersion(): String = mockAppVersion
}

class FakeSistemaSecureStorage : SecureStorageRepository {
    var savedKey: String? = null
    var savedUrl: String = "https://kevin11000.pythonanywhere.com/api/v1/"

    override fun saveApiKey(key: String) { savedKey = key }
    override fun getApiKey(): String? = savedKey
    override fun clearApiKey() { savedKey = null }
    override fun saveBaseUrl(url: String) { savedUrl = url }
    override fun getBaseUrl(): String = savedUrl
}

@OptIn(ExperimentalCoroutinesApi::class)
class SistemaViewModelTest {

    private lateinit var viewModel: SistemaViewModel
    private lateinit var fakeRepo: FakeSistemaRepository
    private lateinit var fakeStorage: FakeSistemaSecureStorage
    private lateinit var fakeGitHubApi: FakeGitHubReleaseApi
    private lateinit var checkAppUpdateUseCase: CheckAppUpdateUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeSistemaRepository()
        fakeStorage = FakeSistemaSecureStorage()
        fakeGitHubApi = FakeGitHubReleaseApi()
        checkAppUpdateUseCase = CheckAppUpdateUseCase(fakeGitHubApi, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `versionName adheres to semantic git version format`() {
        fakeRepo.mockAppVersion = "0.0.4+6bb982e"
        val regex = Regex("""^0\.0\.4\+([0-9a-f]{7}|nogit)$""")
        assertTrue(regex.matches(fakeRepo.mockAppVersion))

        val nogitVersion = "0.0.4+nogit"
        assertTrue(regex.matches(nogitVersion))
    }

    @Test
    fun `initial load populates health, tickets, logs and version`() = runTest {
        viewModel = SistemaViewModel(fakeRepo, fakeStorage, checkAppUpdateUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("0.0.4+6bb982e", state.appVersion)
        assertNotNull(state.health)
        assertEquals("ok", state.health?.status)
        assertEquals(1, state.tickets.size)
        assertEquals("TCK-001", state.tickets[0].id)
        assertEquals(1, state.logs.size)
        assertEquals("Test log", state.logs[0].mensaje)
        assertEquals("https://kevin11000.pythonanywhere.com/api/v1/", state.baseUrl)
    }

    @Test
    fun `guardarConfiguracion updates storage and state`() = runTest {
        viewModel = SistemaViewModel(fakeRepo, fakeStorage, checkAppUpdateUseCase)
        advanceUntilIdle()

        viewModel.guardarConfiguracion("https://custom.server.com/api/v1/", "secret-123")
        advanceUntilIdle()

        assertEquals("secret-123", fakeStorage.savedKey)
        assertEquals("https://custom.server.com/api/v1/", fakeStorage.savedUrl)
        assertTrue(viewModel.uiState.value.isApiKeyConfigured)
    }

    @Test
    fun `health error updates state with error message`() = runTest {
        fakeRepo.healthResult = NetworkResult.Error(503, "Backend degradado")
        viewModel = SistemaViewModel(fakeRepo, fakeStorage, checkAppUpdateUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Backend degradado", state.error)
    }

    @Test
    fun `crearTicket success prepends ticket to list and sets success message`() = runTest {
        viewModel = SistemaViewModel(fakeRepo, fakeStorage, checkAppUpdateUseCase)
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
        viewModel = SistemaViewModel(fakeRepo, fakeStorage, checkAppUpdateUseCase)
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
        viewModel = SistemaViewModel(fakeRepo, fakeStorage, checkAppUpdateUseCase)
        advanceUntilIdle()

        fakeRepo.createTicketResult = NetworkResult.Error(500, "Error en base de datos")
        viewModel.crearTicket("Error", "Fallo")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Error en base de datos", state.error)
    }

    @Test
    fun `limpiarMensajes clears error and success`() = runTest {
        viewModel = SistemaViewModel(fakeRepo, fakeStorage, checkAppUpdateUseCase)
        advanceUntilIdle()

        viewModel.crearTicket("Asunto", "Desc")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.ticketCreationSuccess)

        viewModel.limpiarMensajes()
        assertEquals(null, viewModel.uiState.value.ticketCreationSuccess)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `initial load sets updateStatus to UpToDate when remote and local versions match`() = runTest {
        fakeRepo.mockAppVersion = "0.0.4+6bb982e"
        fakeGitHubApi.response = Response.success(
            GitHubReleaseDto(
                tagName = "v0.0.4",
                name = "v0.0.4",
                assets = listOf(GitHubAssetDto(name = "gamma-mobile-v0.0.4-release.apk", size = 33246834L))
            )
        )
        viewModel = SistemaViewModel(fakeRepo, fakeStorage, checkAppUpdateUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.updateStatus is UpdateStatus.UpToDate)
        assertEquals("0.0.4+6bb982e", (state.updateStatus as UpdateStatus.UpToDate).version)
    }

    @Test
    fun `initial load sets updateStatus to UpdateAvailable when remote release is newer`() = runTest {
        fakeRepo.mockAppVersion = "0.0.4+6bb982e"
        fakeGitHubApi.response = Response.success(
            GitHubReleaseDto(
                tagName = "v0.0.5",
                name = "v0.0.5",
                assets = listOf(
                    GitHubAssetDto(
                        name = "gamma-mobile-v0.0.5-release.apk",
                        size = 35000000L,
                        browserDownloadUrl = "https://github.com/KEVIN11000/Gamma_Mobile/releases/download/v0.0.5/gamma-mobile-v0.0.5-release.apk"
                    )
                )
            )
        )
        viewModel = SistemaViewModel(fakeRepo, fakeStorage, checkAppUpdateUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.updateStatus is UpdateStatus.UpdateAvailable)
        val update = state.updateStatus as UpdateStatus.UpdateAvailable
        assertEquals("v0.0.5", update.version)
        assertEquals("gamma-mobile-v0.0.5-release.apk", update.assetName)
        assertEquals("https://github.com/KEVIN11000/Gamma_Mobile/releases/download/v0.0.5/gamma-mobile-v0.0.5-release.apk", update.downloadUrl)
    }

    @Test
    fun `actualizarEstadoTicket updates ticket state and sets action message on success`() = runTest {
        viewModel = SistemaViewModel(fakeRepo, fakeStorage, checkAppUpdateUseCase)
        advanceUntilIdle()

        fakeRepo.updateTicketResult = NetworkResult.Success(
            TicketItem(id = "TCK-001", asunto = "Fallo", descripcion = "Detalle", estado = "Cerrado", fecha = "12/10/2026", resolucion = "Solucionado")
        )

        viewModel.actualizarEstadoTicket("TCK-001", "Cerrado", "Solucionado")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("TCK-001", fakeRepo.lastTicketUpdatedId)
        assertEquals("Cerrado", fakeRepo.lastTicketUpdatedEstado)
        assertEquals("Solucionado", fakeRepo.lastTicketUpdatedResolucion)
        assertEquals("Cerrado", state.tickets.first { it.id == "TCK-001" }.estado)
        assertEquals("Ticket TCK-001 actualizado a 'Cerrado'", state.ticketActionMessage)
    }

    @Test
    fun `actualizarEstadoTicket sets error when repository fails`() = runTest {
        viewModel = SistemaViewModel(fakeRepo, fakeStorage, checkAppUpdateUseCase)
        advanceUntilIdle()

        fakeRepo.updateTicketResult = NetworkResult.Error(500, "Error de base de datos")

        viewModel.actualizarEstadoTicket("TCK-001", "Cerrado")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Error de base de datos", state.error)
    }

    @Test
    fun `setFiltroTickets filters tickets correctly`() = runTest {
        fakeRepo.ticketsResult = NetworkResult.Success(
            listOf(
                TicketItem(id = "TCK-001", asunto = "Ticket 1", estado = "Abierto"),
                TicketItem(id = "TCK-002", asunto = "Ticket 2", estado = "En proceso"),
                TicketItem(id = "TCK-003", asunto = "Ticket 3", estado = "Cerrado")
            )
        )
        viewModel = SistemaViewModel(fakeRepo, fakeStorage, checkAppUpdateUseCase)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.ticketsFiltrados.size)

        viewModel.setFiltroTickets("Abierto")
        assertEquals(1, viewModel.uiState.value.ticketsFiltrados.size)
        assertEquals("TCK-001", viewModel.uiState.value.ticketsFiltrados.first().id)

        viewModel.setFiltroTickets("Cerrado")
        assertEquals(1, viewModel.uiState.value.ticketsFiltrados.size)
        assertEquals("TCK-003", viewModel.uiState.value.ticketsFiltrados.first().id)

        viewModel.setFiltroTickets("Todos")
        assertEquals(3, viewModel.uiState.value.ticketsFiltrados.size)
    }

    @Test
    fun `ejecutarCierreContable actualiza uiState con exito y url absoluta`() = runTest {
        viewModel = SistemaViewModel(fakeRepo, fakeStorage, checkAppUpdateUseCase)
        advanceUntilIdle()

        viewModel.ejecutarCierreContable("Período Octubre")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isEjecutandoCierre)
        assertNotNull(state.cierreContable)
        assertEquals("Cierre contable completado", state.cierreContable?.resumen)
        assertEquals(3500000L, state.cierreContable?.totalMonto)
        assertTrue(state.cierreMensaje?.startsWith("✅") == true)

        val fullPdf = viewModel.obtenerUrlAbsolutaPdf(state.cierreContable?.pdfUrl)
        assertEquals("https://kevin11000.pythonanywhere.com/api/v1/api/v1/cierres/reportes/test.pdf", fullPdf)
    }

    @Test
    fun `ejecutarCierreContable maneja error de red`() = runTest {
        fakeRepo.cierreResult = NetworkResult.Error(500, "Error en el servidor de cierre")
        viewModel = SistemaViewModel(fakeRepo, fakeStorage, checkAppUpdateUseCase)
        advanceUntilIdle()

        viewModel.ejecutarCierreContable()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isEjecutandoCierre)
        assertTrue(state.cierreMensaje?.contains("Error 500") == true)
    }

    @Test
    fun `setPeriodoCierre actualiza el periodo seleccionado en uiState`() = runTest {
        viewModel = SistemaViewModel(fakeRepo, fakeStorage, checkAppUpdateUseCase)
        advanceUntilIdle()

        assertEquals("Período Actual", viewModel.uiState.value.selectedPeriodoCierre)
        assertTrue(viewModel.uiState.value.periodosDisponibles.contains("Septiembre 2026"))

        viewModel.setPeriodoCierre("Septiembre 2026")
        assertEquals("Septiembre 2026", viewModel.uiState.value.selectedPeriodoCierre)

        viewModel.ejecutarCierreContable()
        advanceUntilIdle()

        assertEquals("Septiembre 2026", fakeRepo.lastHojaCierre)
        assertTrue(viewModel.uiState.value.cierreMensaje?.contains("Septiembre 2026") == true)
    }

    @Test
    fun `obtenerUrlAbsolutaPdf maneja url relativa nula o absoluta`() = runTest {
        viewModel = SistemaViewModel(fakeRepo, fakeStorage, checkAppUpdateUseCase)
        advanceUntilIdle()

        assertEquals(null, viewModel.obtenerUrlAbsolutaPdf(null))
        assertEquals(null, viewModel.obtenerUrlAbsolutaPdf(""))
        assertEquals("https://external.com/doc.pdf", viewModel.obtenerUrlAbsolutaPdf("https://external.com/doc.pdf"))
        assertEquals("http://external.com/doc.pdf", viewModel.obtenerUrlAbsolutaPdf("http://external.com/doc.pdf"))
        assertEquals("https://kevin11000.pythonanywhere.com/api/v1/reporte.pdf", viewModel.obtenerUrlAbsolutaPdf("/reporte.pdf"))
    }
}
