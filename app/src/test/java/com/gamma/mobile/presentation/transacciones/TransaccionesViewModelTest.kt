package com.gamma.mobile.presentation.transacciones

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Movimiento
import com.gamma.mobile.domain.repository.MovimientoRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class TransaccionesViewModelTest {

    private lateinit var viewModel: TransaccionesViewModel
    private lateinit var fakeRepo: FakeMovimientoRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeMovimientoRepository()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadMovimientos groups by date and sorts descending`() = runTest {
        val movimientos = listOf(
            Movimiento("1", "2026-10-01", "Ingreso", "Cat1", 100, "Legal"),
            Movimiento("2", "2026-10-02", "Gasto", "Cat2", 50, "Legal"),
            Movimiento("3", "2026-10-01", "Gasto", "Cat3", 20, "Legal")
        )
        fakeRepo.result = NetworkResult.Success(movimientos)

        viewModel = TransaccionesViewModel(fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is TransaccionesUiState.Success)
        
        val successState = state as TransaccionesUiState.Success
        val agrupados = successState.movimientosAgrupados
        
        // Debe haber 2 fechas
        assertEquals(2, agrupados.size)
        
        // Las claves deben estar en orden descendente ("2026-10-02", luego "2026-10-01")
        val keys = agrupados.keys.toList()
        assertEquals("2026-10-02", keys[0])
        assertEquals("2026-10-01", keys[1])
        
        // En "2026-10-01" debe haber 2 movimientos
        assertEquals(2, agrupados["2026-10-01"]?.size)
    }

    @Test
    fun `setFiltro filters correctly by Ingreso and Gasto`() = runTest {
        val movimientos = listOf(
            Movimiento("1", "2026-10-01", "Ingreso", "Cat1", 100, "Legal"),
            Movimiento("2", "2026-10-02", "Gasto", "Cat2", 50, "Legal"),
            Movimiento("3", "2026-10-03", "Ingreso", "Cat1", 100, "Legal")
        )
        fakeRepo.result = NetworkResult.Success(movimientos)
        viewModel = TransaccionesViewModel(fakeRepo)
        advanceUntilIdle()

        // Filtrar por Ingreso
        viewModel.setFiltro(FiltroTipo.INGRESO)
        advanceUntilIdle()
        
        var state = viewModel.uiState.value as TransaccionesUiState.Success
        assertEquals(2, state.movimientosAgrupados.values.flatten().size)
        assertTrue(state.movimientosAgrupados.values.flatten().all { it.tipo == "Ingreso" })

        // Filtrar por Gasto
        viewModel.setFiltro(FiltroTipo.GASTO)
        advanceUntilIdle()
        
        state = viewModel.uiState.value as TransaccionesUiState.Success
        assertEquals(1, state.movimientosAgrupados.values.flatten().size)
        assertTrue(state.movimientosAgrupados.values.flatten().all { it.tipo == "Gasto" })
    }

    @Test
    fun `pull to refresh calls repository with forceRefresh true`() = runTest {
        fakeRepo.result = NetworkResult.Success(emptyList())
        viewModel = TransaccionesViewModel(fakeRepo)
        advanceUntilIdle()
        
        assertEquals(1, fakeRepo.getMovimientosCallCount)
        assertEquals(false, fakeRepo.lastForceRefreshArg)

        fakeRepo.result = NetworkResult.Success(listOf(Movimiento("1", "2026-10-01", "Ingreso", "Cat1", 100, "Legal")))
        
        viewModel.loadMovimientos(forceRefresh = true)
        
        // El estado debería ser Success pero isRefreshing = true temporalmente, 
        // y luego success normal después del delay.
        advanceUntilIdle()

        assertEquals(2, fakeRepo.getMovimientosCallCount)
        assertEquals(true, fakeRepo.lastForceRefreshArg)
        
        val state = viewModel.uiState.value
        assertTrue(state is TransaccionesUiState.Success)
        assertEquals(1, (state as TransaccionesUiState.Success).movimientosAgrupados.size)
    }
}

class FakeMovimientoRepository : MovimientoRepository {
    var result: NetworkResult<List<Movimiento>> = NetworkResult.Success(emptyList())
    var getMovimientosCallCount = 0
    var lastForceRefreshArg = false
    
    override suspend fun getMovimientos(forceRefresh: Boolean): NetworkResult<List<Movimiento>> {
        getMovimientosCallCount++
        lastForceRefreshArg = forceRefresh
        return result
    }

    override fun getMovimientosLocal(): kotlinx.coroutines.flow.Flow<List<Movimiento>> {
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }

    override suspend fun addMovimiento(movimiento: Movimiento): NetworkResult<Unit> {
        return NetworkResult.Success(Unit)
    }

    override suspend fun interpretarMovimientoIa(texto: String): NetworkResult<Movimiento> = NetworkResult.Success(Movimiento())

    override suspend fun interpretarMovimientoOcr(imageBytes: ByteArray, fileName: String): NetworkResult<Movimiento> = NetworkResult.Success(Movimiento())
}


