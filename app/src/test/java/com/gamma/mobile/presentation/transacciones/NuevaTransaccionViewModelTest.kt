package com.gamma.mobile.presentation.transacciones

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Movimiento
import com.gamma.mobile.domain.repository.MovimientoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class NuevaTransaccionViewModelTest {

    private lateinit var viewModel: NuevaTransaccionViewModel
    private lateinit var fakeRepo: FakeNuevaMovimientoRepository
    private val testDispatcher = StandardTestDispatcher()
    private val fixedClock = Clock.fixed(Instant.parse("2026-10-06T10:00:00Z"), ZoneId.of("UTC"))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeNuevaMovimientoRepository()
        viewModel = NuevaTransaccionViewModel(fakeRepo, fixedClock)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is invalid`() {
        val state = viewModel.uiState.value
        assertFalse(state.isValid)
    }

    @Test
    fun `isValid is true when total and categoria are provided`() {
        viewModel.updateTotal("150000")
        viewModel.updateCategoria("Servicios")
        
        val state = viewModel.uiState.value
        assertTrue(state.isValid)
    }

    @Test
    fun `iva preview calculates correctly for 10 percent`() {
        viewModel.updateTotal("110000")
        viewModel.updateTasaIva("10%")
        
        val state = viewModel.uiState.value
        assertEquals(10000, state.ivaPreview.montoIva)
        assertEquals(100000, state.ivaPreview.montoGravado)
    }

    @Test
    fun `iva preview calculates correctly for 5 percent`() {
        viewModel.updateTotal("21000")
        viewModel.updateTasaIva("5%")
        
        val state = viewModel.uiState.value
        assertEquals(1000, state.ivaPreview.montoIva)
        assertEquals(20000, state.ivaPreview.montoGravado)
    }

    @Test
    fun `iva preview calculates correctly for Exento`() {
        viewModel.updateTotal("50000")
        viewModel.updateTasaIva("Exento")
        
        val state = viewModel.uiState.value
        assertEquals(0, state.ivaPreview.montoIva)
        assertEquals(50000, state.ivaPreview.montoGravado)
    }

    @Test
    fun `saveMovimiento calls repository and emits success`() = runTest {
        viewModel.updateTotal("100000")
        viewModel.updateCategoria("Ventas")
        
        viewModel.saveMovimiento()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertFalse(state.isLoading)
        
        assertEquals(1, fakeRepo.addedMovimientos.size)
        val mov = fakeRepo.addedMovimientos.first()
        assertEquals("Ventas", mov.categoria)
        assertEquals(100000, mov.total)
        assertEquals("2026-10-06", mov.fecha) 
    }

    @Test
    fun `interpretarTextoIa success updates state fields`() = runTest {
        fakeRepo.iaResult = NetworkResult.Success(Movimiento(tipo = "Ingreso", categoria = "Ventas IA", total = 250000))
        
        viewModel.interpretarTextoIa("vendimos por 250000")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Ingreso", state.tipo)
        assertEquals("Ventas IA", state.categoria)
        assertEquals("250000", state.totalInput)
        assertFalse(state.isLoading)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun `interpretarTextoIa timeout sets error message`() = runTest {
        fakeRepo.iaResult = NetworkResult.Exception(java.util.concurrent.TimeoutException("Timeout de IA (45s)"))
        
        viewModel.interpretarTextoIa("vendimos por 250000")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Timeout de IA (45s)", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `interpretarTextoIa invalid JSON Error sets error message`() = runTest {
        fakeRepo.iaResult = NetworkResult.Error(400, "JSON invalido")
        
        viewModel.interpretarTextoIa("vendimos por 250000")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("JSON invalido", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `interpretarImagenOcr success updates fields without saving`() = runTest {
        fakeRepo.iaResult = NetworkResult.Success(Movimiento(tipo = "Gasto", categoria = "OCR", total = 120000))
        
        viewModel.interpretarImagenOcr(byteArrayOf(1, 2, 3), "comprobante.jpg")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Gasto", state.tipo)
        assertEquals("OCR", state.categoria)
        assertEquals("120000", state.totalInput)
        assertEquals(0, fakeRepo.addedMovimientos.size) 
    }
}

class FakeNuevaMovimientoRepository : MovimientoRepository {
    val addedMovimientos = mutableListOf<Movimiento>()
    var iaResult: NetworkResult<Movimiento>? = null
    
    override suspend fun getMovimientos(forceRefresh: Boolean): NetworkResult<List<Movimiento>> {
        return NetworkResult.Success(emptyList())
    }

    override fun getMovimientosLocal(): Flow<List<Movimiento>> {
        return flowOf(emptyList())
    }

    override suspend fun addMovimiento(movimiento: Movimiento): NetworkResult<Unit> {
        addedMovimientos.add(movimiento)
        return NetworkResult.Success(Unit)
    }

    override suspend fun interpretarMovimientoIa(texto: String): NetworkResult<Movimiento> {
        return iaResult ?: NetworkResult.Success(Movimiento())
    }

    override suspend fun interpretarMovimientoOcr(imageBytes: ByteArray, fileName: String): NetworkResult<Movimiento> {
        return iaResult ?: NetworkResult.Success(Movimiento())
    }
}
