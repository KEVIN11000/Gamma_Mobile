package com.gamma.mobile.presentation.cierre

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.CierreHistorialItem
import com.gamma.mobile.domain.repository.CierresRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeCierresRepositoryForVm : CierresRepository {
    var cierresResult: NetworkResult<List<CierreHistorialItem>> = NetworkResult.Success(emptyList())
    var downloadResult: NetworkResult<ByteArray> = NetworkResult.Success(byteArrayOf(1, 2, 3))
    var lastForceRefreshCalled: Boolean? = null
    var lastFilenameCalled: String? = null

    override suspend fun getCierres(forceRefresh: Boolean): NetworkResult<List<CierreHistorialItem>> {
        lastForceRefreshCalled = forceRefresh
        return cierresResult
    }

    override suspend fun descargarReporteCierre(filename: String): NetworkResult<ByteArray> {
        lastFilenameCalled = filename
        return downloadResult
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CierresViewModelTest {

    private lateinit var viewModel: CierresViewModel
    private lateinit var fakeRepo: FakeCierresRepositoryForVm
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeCierresRepositoryForVm()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init calls getCierres and emits Success state`() = runTest {
        val testData = listOf(
            CierreHistorialItem("1", "contable", "10/2026", "2026-10-20", 5000000L, 0.0, "Cierre Contable"),
            CierreHistorialItem("2", "laboral", "10/2026", "2026-10-21", 3000000L, 160.0, "Cierre Laboral")
        )
        fakeRepo.cierresResult = NetworkResult.Success(testData)

        viewModel = CierresViewModel(fakeRepo)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is HistorialCierresUiState.Success)
        val state = viewModel.uiState.value as HistorialCierresUiState.Success
        assertEquals(2, state.cierres.size)
        assertEquals(FiltroTipoCierre.TODOS, viewModel.filtroTipo.value)
    }

    @Test
    fun `setFiltroTipo correctly filters list by CONTABLE and LABORAL`() = runTest {
        val testData = listOf(
            CierreHistorialItem("1", "contable", "10/2026", "2026-10-20", 5000000L, 0.0, "Contable 1"),
            CierreHistorialItem("2", "laboral", "10/2026", "2026-10-21", 3000000L, 160.0, "Laboral 1"),
            CierreHistorialItem("3", "contable", "09/2026", "2026-09-30", 4000000L, 0.0, "Contable 2")
        )
        fakeRepo.cierresResult = NetworkResult.Success(testData)

        viewModel = CierresViewModel(fakeRepo)
        advanceUntilIdle()

        // Filtrar CONTABLE (2)
        viewModel.setFiltroTipo(FiltroTipoCierre.CONTABLE)
        var state = viewModel.uiState.value as HistorialCierresUiState.Success
        assertEquals(2, state.cierres.size)
        assertTrue(state.cierres.all { it.tipo.equals("contable", ignoreCase = true) })

        // Filtrar LABORAL (1)
        viewModel.setFiltroTipo(FiltroTipoCierre.LABORAL)
        state = viewModel.uiState.value as HistorialCierresUiState.Success
        assertEquals(1, state.cierres.size)
        assertEquals("2", state.cierres[0].id)

        // Volver a TODOS (3)
        viewModel.setFiltroTipo(FiltroTipoCierre.TODOS)
        state = viewModel.uiState.value as HistorialCierresUiState.Success
        assertEquals(3, state.cierres.size)
    }

    @Test
    fun `loadCierres on error emits Error state`() = runTest {
        fakeRepo.cierresResult = NetworkResult.Error(500, "Error de backend")

        viewModel = CierresViewModel(fakeRepo)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is HistorialCierresUiState.Error)
        val state = viewModel.uiState.value as HistorialCierresUiState.Error
        assertEquals("Error: Error de backend", state.message)
    }

    @Test
    fun `descargarComprobante when pdfDisponible is false sets Error download state`() = runTest {
        fakeRepo.cierresResult = NetworkResult.Success(emptyList())
        viewModel = CierresViewModel(fakeRepo)
        advanceUntilIdle()

        val cierreSinPdf = CierreHistorialItem("c-no-pdf", "contable", "10/2026", pdfDisponible = false)
        viewModel.descargarComprobante(cierreSinPdf)

        assertTrue(viewModel.downloadState.value is DownloadPdfState.Error)
        val state = viewModel.downloadState.value as DownloadPdfState.Error
        assertEquals("Comprobante PDF no disponible para este período", state.message)
    }

    @Test
    fun `descargarComprobante when pdfDisponible is true downloads and sets Downloaded state`() = runTest {
        val testBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46)
        fakeRepo.downloadResult = NetworkResult.Success(testBytes)
        fakeRepo.cierresResult = NetworkResult.Success(emptyList())

        viewModel = CierresViewModel(fakeRepo)
        advanceUntilIdle()

        val cierreConPdf = CierreHistorialItem(
            id = "c-pdf-1",
            tipo = "contable",
            periodo = "Octubre 2026",
            pdfUrl = "reporte_octubre.pdf",
            pdfDisponible = true
        )

        viewModel.descargarComprobante(cierreConPdf)
        advanceUntilIdle()

        assertTrue(viewModel.downloadState.value is DownloadPdfState.Downloaded)
        val state = viewModel.downloadState.value as DownloadPdfState.Downloaded
        assertEquals("c-pdf-1", state.id)
        assertEquals("cierre_contable_Octubre_2026.pdf", state.fileName)
        assertTrue(testBytes.contentEquals(state.fileBytes))

        // Reset
        viewModel.clearDownloadState()
        assertNull(viewModel.downloadState.value)
    }

    @Test
    fun `descargarComprobante on failure sets Error state`() = runTest {
        fakeRepo.downloadResult = NetworkResult.Error(404, "Reporte no encontrado")
        fakeRepo.cierresResult = NetworkResult.Success(emptyList())

        viewModel = CierresViewModel(fakeRepo)
        advanceUntilIdle()

        val cierre = CierreHistorialItem(id = "c1", tipo = "laboral", periodo = "10/2026", pdfDisponible = true)
        viewModel.descargarComprobante(cierre)
        advanceUntilIdle()

        assertTrue(viewModel.downloadState.value is DownloadPdfState.Error)
        val state = viewModel.downloadState.value as DownloadPdfState.Error
        assertEquals("Reporte no encontrado", state.message)
    }
}
