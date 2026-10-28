package com.gamma.mobile.presentation.asistencia

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.AsistenciaMarcaResponse
import com.gamma.mobile.domain.model.AsistenciaMateriaResponse
import com.gamma.mobile.domain.repository.AsistenciaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class FakeAsistenciaRepository : AsistenciaRepository {
    var delayMs: Long = 0L
    var asistenciaResult: NetworkResult<AsistenciaMarcaResponse> = NetworkResult.Success(
        AsistenciaMarcaResponse(tipo = "entrada", hora = "08:00", mensaje = "Entrada registrada")
    )
    var materiaResult: NetworkResult<AsistenciaMateriaResponse> = NetworkResult.Success(
        AsistenciaMateriaResponse(materia = "Matemáticas", hora = "14:00", mensaje = "Materia registrada")
    )

    var lastModoCalled: String? = null
    var lastMateriaCalled: String? = null

    override suspend fun marcarAsistencia(modo: String): NetworkResult<AsistenciaMarcaResponse> {
        lastModoCalled = modo
        if (delayMs > 0) {
            kotlinx.coroutines.delay(delayMs)
        }
        return asistenciaResult
    }

    override suspend fun marcarMateria(materia: String): NetworkResult<AsistenciaMateriaResponse> {
        lastMateriaCalled = materia
        return materiaResult
    }

    override suspend fun getMaterias(): NetworkResult<List<String>> {
        return NetworkResult.Success(listOf("Algoritmos", "Base de Datos"))
    }

    var periodosResult: NetworkResult<com.gamma.mobile.domain.model.PeriodosResponse> =
        NetworkResult.Success(com.gamma.mobile.domain.model.PeriodosResponse("Octubre 2026", "Octubre 2026", true))
    var analiticasResult: NetworkResult<com.gamma.mobile.domain.model.AnaliticasHorasResponse> =
        NetworkResult.Success(com.gamma.mobile.domain.model.AnaliticasHorasResponse(periodo = "Octubre 2026", totalHorasDecimal = 107.3, totalHorasFormato = "107h 18m", salarioAcumuladoGs = 1570228L))
    var excelResult: NetworkResult<ByteArray> = NetworkResult.Success(byteArrayOf(1, 2, 3))
    var pdfResult: NetworkResult<ByteArray> = NetworkResult.Success(byteArrayOf(4, 5, 6))

    override suspend fun getPeriodos(): NetworkResult<com.gamma.mobile.domain.model.PeriodosResponse> = periodosResult
    override suspend fun getAnaliticas(hoja: String?): NetworkResult<com.gamma.mobile.domain.model.AnaliticasHorasResponse> = analiticasResult
    override suspend fun descargarReporteExcel(
        hoja: String?,
        descuento: Double,
        saldoAnterior: Double,
        incluirIva: Boolean
    ): NetworkResult<ByteArray> = excelResult
    override suspend fun descargarReportePdfAsistencia(
        hoja: String?,
        descuento: Double,
        saldoAnterior: Double,
        incluirIva: Boolean
    ): NetworkResult<ByteArray> = pdfResult
}

@OptIn(ExperimentalCoroutinesApi::class)
class AsistenciaViewModelTest {

    private lateinit var viewModel: AsistenciaViewModel
    private lateinit var fakeRepo: FakeAsistenciaRepository
    private val testDispatcher = StandardTestDispatcher()
    private val fixedInstant = Instant.parse("2026-10-28T08:30:00Z")
    private val fixedZone = ZoneId.of("UTC")
    private val fixedClock = Clock.fixed(fixedInstant, fixedZone)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAsistenciaRepository()
        viewModel = AsistenciaViewModel(fakeRepo, fixedClock)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle with default modo normal`() {
        assertEquals(AsistenciaUiState.Idle, viewModel.uiState.value)
        assertEquals("normal", viewModel.modo.value)
        assertEquals("", viewModel.materia.value)
    }

    @Test
    fun `setModo and setMateria update values`() {
        viewModel.setModo("directo")
        assertEquals("directo", viewModel.modo.value)

        viewModel.setMateria("Inteligencia Artificial")
        assertEquals("Inteligencia Artificial", viewModel.materia.value)
    }

    @Test
    fun `materiasDisponibles contains configured subjects and can be selected`() {
        assertTrue(viewModel.materiasDisponibles.value.isNotEmpty())
        assertTrue(viewModel.materiasDisponibles.value.contains("Algoritmos") || viewModel.materiasDisponibles.value.contains("Algoritmos y Estructuras de Datos"))

        val selected = viewModel.materiasDisponibles.value.first()
        viewModel.setMateria(selected)
        assertEquals(selected, viewModel.materia.value)
    }

    @Test
    fun `marcarAsistencia sets Enviando state with clock time before completion and Success confirmed in planilla`() = runTest {
        fakeRepo.delayMs = 200L
        fakeRepo.asistenciaResult = NetworkResult.Success(
            AsistenciaMarcaResponse(
                tipo = "entrada",
                hora = "08:30",
                mensaje = "Entrada registrada",
                planilla = "Octubre 2026",
                confirmadoEnPlanilla = true
            )
        )

        viewModel.marcarAsistencia("normal")
        runCurrent()

        // Stage 1: Enviando
        val sendingState = viewModel.uiState.value
        assertTrue(sendingState is AsistenciaUiState.Enviando)
        assertEquals("08:30", (sendingState as AsistenciaUiState.Enviando).horaEnvio)

        // Stage 2: Confirmed in spreadsheet
        advanceUntilIdle()
        assertEquals("normal", fakeRepo.lastModoCalled)
        val finalState = viewModel.uiState.value
        assertTrue(finalState is AsistenciaUiState.Success)
        val successState = finalState as AsistenciaUiState.Success
        assertTrue(successState.confirmadoEnPlanilla)
        assertEquals("Octubre 2026", successState.planilla)
        assertEquals("08:30", successState.hora)
        assertTrue(successState.mensaje.startsWith("✅"))
        assertTrue(successState.detalle.contains("Entrada"))
        assertTrue(successState.detalle.contains("registrada en planilla"))
        assertTrue(successState.detalle.contains("Modo normal"))
    }

    @Test
    fun `marcarAsistencia unconfirmed updates state to Success without spreadsheet confirmation`() = runTest {
        fakeRepo.asistenciaResult = NetworkResult.Success(
            AsistenciaMarcaResponse(
                tipo = "salida",
                hora = "17:00",
                mensaje = "Salida OK",
                confirmadoEnPlanilla = false
            )
        )

        viewModel.setModo("directo")
        viewModel.marcarAsistencia()
        advanceUntilIdle()

        assertEquals("directo", fakeRepo.lastModoCalled)
        val state = viewModel.uiState.value as AsistenciaUiState.Success
        assertFalse(state.confirmadoEnPlanilla)
        assertEquals("17:00", state.hora)
        assertTrue(state.mensaje.contains("procesada") || state.mensaje.contains("Salida"))
        assertTrue(state.detalle.contains("Salida"))
        assertTrue(state.detalle.contains("Modo directo"))
    }

    @Test
    fun `marcarAsistencia informative message updates state to Success with info prefix`() = runTest {
        fakeRepo.asistenciaResult = NetworkResult.Success(
            AsistenciaMarcaResponse(
                tipo = "salida",
                hora = "17:00",
                mensaje = "ya tiene salida registrada para hoy",
                confirmadoEnPlanilla = false
            )
        )

        viewModel.marcarAsistencia("normal")
        advanceUntilIdle()

        val state = viewModel.uiState.value as AsistenciaUiState.Success
        assertFalse(state.confirmadoEnPlanilla)
        assertTrue(state.mensaje.startsWith("ℹ️"))
    }

    @Test
    fun `marcarAsistencia error updates state to Error`() = runTest {
        fakeRepo.asistenciaResult = NetworkResult.Error(500, "Fallo en servidor WSGI")

        viewModel.marcarAsistencia("normal")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AsistenciaUiState.Error)
        assertEquals("Fallo en servidor WSGI", (state as AsistenciaUiState.Error).message)
    }

    @Test
    fun `marcarMateria success updates state to Success`() = runTest {
        fakeRepo.materiaResult = NetworkResult.Success(
            AsistenciaMateriaResponse(materia = "Física", hora = "10:15", mensaje = "Asistencia confirmada")
        )

        viewModel.marcarMateria("Física")
        advanceUntilIdle()

        assertEquals("Física", fakeRepo.lastMateriaCalled)
        val state = viewModel.uiState.value as AsistenciaUiState.Success
        assertEquals("Asistencia confirmada", state.mensaje)
        assertEquals("10:15", state.hora)
        assertTrue(state.detalle.contains("Física"))
    }

    @Test
    fun `marcarMateria blank sets error without calling repository`() = runTest {
        viewModel.marcarMateria("   ")
        advanceUntilIdle()

        assertEquals(null, fakeRepo.lastMateriaCalled)
        val state = viewModel.uiState.value as AsistenciaUiState.Error
        assertEquals("Debe ingresar el nombre de la materia", state.message)
    }

    @Test
    fun `marcarMateria error updates state to Error`() = runTest {
        fakeRepo.materiaResult = NetworkResult.Error(404, "Materia no encontrada")

        viewModel.marcarMateria("Química")
        advanceUntilIdle()

        val state = viewModel.uiState.value as AsistenciaUiState.Error
        assertEquals("Materia no encontrada", state.message)
    }

    @Test
    fun `resetState changes state back to Idle`() = runTest {
        viewModel.marcarAsistencia("normal")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is AsistenciaUiState.Success)

        viewModel.resetState()
        assertEquals(AsistenciaUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `loadAnaliticas success updates analiticasState to Success with periodos and metrics`() = runTest {
        viewModel.loadAnaliticas("Octubre 2026")
        advanceUntilIdle()

        val state = viewModel.analiticasState.value
        assertTrue(state is AnaliticasUiState.Success)
        val success = state as AnaliticasUiState.Success
        assertEquals("Octubre 2026", success.analiticas.periodo)
        assertEquals("107h 18m", success.analiticas.totalHorasFormato)
        assertEquals(1570228L, success.analiticas.salarioAcumuladoGs)
        assertEquals(true, success.periodos?.sincronizados)
    }

    @Test
    fun `loadAnaliticas error updates analiticasState to Error`() = runTest {
        fakeRepo.analiticasResult = NetworkResult.Error(500, "Error en Google Sheets")
        viewModel.loadAnaliticas("Noviembre 2026")
        advanceUntilIdle()

        val state = viewModel.analiticasState.value
        assertTrue(state is AnaliticasUiState.Error)
        assertEquals("Error en Google Sheets", (state as AnaliticasUiState.Error).message)
    }

    @Test
    fun `descargarExcel success updates excelDownloadState to Success and reset works`() = runTest {
        viewModel.descargarExcel("Octubre 2026")
        advanceUntilIdle()

        val state = viewModel.excelDownloadState.value
        assertTrue(state is ExcelDownloadState.Success)
        val success = state as ExcelDownloadState.Success
        assertEquals("Planilla_Octubre 2026.xlsx", success.fileName)
        assertEquals(3, success.fileBytes.size)

        viewModel.resetExcelDownloadState()
        assertEquals(ExcelDownloadState.Idle, viewModel.excelDownloadState.value)
    }

    @Test
    fun `descargarExcel error updates excelDownloadState to Error`() = runTest {
        fakeRepo.excelResult = NetworkResult.Error(404, "Planilla no encontrada")
        viewModel.descargarExcel("Inexistente")
        advanceUntilIdle()

        val state = viewModel.excelDownloadState.value
        assertTrue(state is ExcelDownloadState.Error)
        assertEquals("Planilla no encontrada", (state as ExcelDownloadState.Error).message)
    }
}
