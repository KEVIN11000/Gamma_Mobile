package com.gamma.mobile.presentation.cierre

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.CierreResponse
import com.gamma.mobile.domain.repository.CierreRepository
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

class FakeCierreRepository : CierreRepository {
    var cierreResult: NetworkResult<CierreResponse> = NetworkResult.Success(
        CierreResponse(
            resumen = "Cierre OK",
            pdfUrl = "https://gamma.com/reportes/test.pdf",
            totalHoras = 160.0,
            totalMonto = 4000000
        )
    )
    var downloadResult: NetworkResult<ByteArray> = NetworkResult.Success(
        byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF
    )

    var lastHojaCalled: String? = null
    var lastDescuentoCalled: Boolean? = null
    var lastTipoCalled: String? = null
    var lastUrlCalled: String? = null

    override suspend fun ejecutarCierre(hoja: String, aplicarDescuento: Boolean, tipo: String): NetworkResult<CierreResponse> {
        lastHojaCalled = hoja
        lastDescuentoCalled = aplicarDescuento
        lastTipoCalled = tipo
        return cierreResult
    }

    override suspend fun descargarPdf(url: String): NetworkResult<ByteArray> {
        lastUrlCalled = url
        return downloadResult
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CierreViewModelTest {

    private lateinit var viewModel: CierreViewModel
    private lateinit var fakeRepo: FakeCierreRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeCierreRepository()
        viewModel = CierreViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle with defaults`() {
        assertEquals(CierreUiState.Idle, viewModel.uiState.value)
        assertEquals("Octubre 2026", viewModel.hoja.value)
        assertEquals(false, viewModel.aplicarDescuento.value)
    }

    @Test
    fun `setHoja, setAplicarDescuento and setTipoCierre update properties`() {
        viewModel.setHoja("Setiembre 2026")
        assertEquals("Setiembre 2026", viewModel.hoja.value)

        viewModel.setAplicarDescuento(true)
        assertEquals(true, viewModel.aplicarDescuento.value)

        viewModel.setTipoCierre("contable")
        assertEquals("contable", viewModel.tipoCierre.value)
    }

    @Test
    fun `periodosDisponibles contains monthly periods and can be selected`() {
        assertTrue(viewModel.periodosDisponibles.isNotEmpty())
        assertTrue(viewModel.periodosDisponibles.contains("Octubre 2026"))

        val nuevoPeriodo = "Diciembre 2026"
        viewModel.setHoja(nuevoPeriodo)
        assertEquals(nuevoPeriodo, viewModel.hoja.value)
    }

    @Test
    fun `ejecutarCierre blank hoja sets validation error without calling repo`() = runTest {
        viewModel.ejecutarCierre("   ", false)
        advanceUntilIdle()

        assertEquals(null, fakeRepo.lastHojaCalled)
        val state = viewModel.uiState.value as CierreUiState.Error
        assertEquals("Debe indicar la hoja o período para el cierre", state.message)
    }

    @Test
    fun `ejecutarCierre success updates state to CierreSuccess`() = runTest {
        viewModel.setHoja("Octubre 2026")
        viewModel.setAplicarDescuento(false)
        viewModel.ejecutarCierre()
        advanceUntilIdle()

        assertEquals("Octubre 2026", fakeRepo.lastHojaCalled)
        assertEquals(false, fakeRepo.lastDescuentoCalled)

        val state = viewModel.uiState.value
        assertTrue(state is CierreUiState.CierreSuccess)
        val success = state as CierreUiState.CierreSuccess
        assertEquals("Cierre OK", success.response.resumen)
        assertEquals("https://gamma.com/reportes/test.pdf", success.response.pdfUrl)
        assertEquals(160.0, success.response.totalHoras, 0.001)
        assertEquals(4000000, success.response.totalMonto)
    }

    @Test
    fun `ejecutarCierre with discount sets discount flag`() = runTest {
        viewModel.setHoja("Noviembre 2026")
        viewModel.setAplicarDescuento(true)
        viewModel.ejecutarCierre()
        advanceUntilIdle()

        assertEquals("Noviembre 2026", fakeRepo.lastHojaCalled)
        assertEquals(true, fakeRepo.lastDescuentoCalled)
        assertTrue(viewModel.uiState.value is CierreUiState.CierreSuccess)
    }

    @Test
    fun `ejecutarCierre error updates state to Error`() = runTest {
        fakeRepo.cierreResult = NetworkResult.Error(500, "Error en Google Sheets")
        viewModel.ejecutarCierre("Octubre 2026", false)
        advanceUntilIdle()

        val state = viewModel.uiState.value as CierreUiState.Error
        assertEquals("Error en Google Sheets", state.message)
    }

    @Test
    fun `descargarPdf blank url sets error without calling repo`() = runTest {
        viewModel.descargarPdf("   ")
        advanceUntilIdle()

        assertEquals(null, fakeRepo.lastUrlCalled)
        val state = viewModel.uiState.value as CierreUiState.Error
        assertEquals("URL del archivo PDF inválida o no disponible", state.message)
    }

    @Test
    fun `descargarPdf success updates state to PdfDownloaded`() = runTest {
        val dummyBytes = byteArrayOf(1, 2, 3, 4)
        fakeRepo.downloadResult = NetworkResult.Success(dummyBytes)

        viewModel.descargarPdf("https://gamma.com/reportes/cierre.pdf", "reporte.pdf")
        advanceUntilIdle()

        assertEquals("https://gamma.com/reportes/cierre.pdf", fakeRepo.lastUrlCalled)
        val state = viewModel.uiState.value as CierreUiState.PdfDownloaded
        assertEquals("reporte.pdf", state.fileName)
        assertTrue(dummyBytes.contentEquals(state.fileBytes))
    }

    @Test
    fun `descargarPdf error updates state to Error`() = runTest {
        fakeRepo.downloadResult = NetworkResult.Error(404, "Reporte no encontrado")

        viewModel.descargarPdf("https://gamma.com/reportes/cierre.pdf")
        advanceUntilIdle()

        val state = viewModel.uiState.value as CierreUiState.Error
        assertEquals("Reporte no encontrado", state.message)
    }

    @Test
    fun `resetState resets state to Idle`() = runTest {
        viewModel.ejecutarCierre("Octubre 2026", false)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is CierreUiState.CierreSuccess)

        viewModel.resetState()
        assertEquals(CierreUiState.Idle, viewModel.uiState.value)
    }
}
