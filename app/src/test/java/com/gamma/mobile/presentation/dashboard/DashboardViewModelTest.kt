package com.gamma.mobile.presentation.dashboard

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.repository.AvisoRepository
import com.gamma.mobile.domain.repository.DashboardRepository
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
class DashboardViewModelTest {

    private lateinit var viewModel: DashboardViewModel
    private lateinit var fakeDashboardRepo: FakeDashboardRepository
    private lateinit var fakeAvisoRepo: FakeAvisoRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDashboardRepo = FakeDashboardRepository()
        fakeAvisoRepo = FakeAvisoRepository()
        // No instanciamos el viewModel aún para poder configurar los fakes antes del init block
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadDashboard with successful data emits Success state`() = runTest {
        val summary = DashboardSummary(100, 200, 50, false)
        val avisos = listOf(Aviso("1", "Aviso 1", 5))
        
        fakeDashboardRepo.result = NetworkResult.Success(summary)
        fakeAvisoRepo.result = NetworkResult.Success(avisos)

        viewModel = DashboardViewModel(fakeDashboardRepo, fakeAvisoRepo)
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

        viewModel = DashboardViewModel(fakeDashboardRepo, fakeAvisoRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Empty)
    }

    @Test
    fun `loadDashboard network exception emits Error state with offline flag`() = runTest {
        fakeDashboardRepo.result = NetworkResult.Exception(java.net.SocketTimeoutException())
        fakeAvisoRepo.result = NetworkResult.Success(emptyList())

        viewModel = DashboardViewModel(fakeDashboardRepo, fakeAvisoRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Error)
        assertTrue((state as DashboardUiState.Error).isOffline)
    }

    @Test
    fun `loadDashboard API error emits Error state without offline flag`() = runTest {
        fakeDashboardRepo.result = NetworkResult.Error(500, "Server Error")
        fakeAvisoRepo.result = NetworkResult.Success(emptyList())

        viewModel = DashboardViewModel(fakeDashboardRepo, fakeAvisoRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Error)
        assertEquals(false, (state as DashboardUiState.Error).isOffline)
    }
}

class FakeDashboardRepository : DashboardRepository {
    var result: NetworkResult<DashboardSummary> = NetworkResult.Error(500, "Uninitialized")
    
    override suspend fun getSummary(forceRefresh: Boolean): NetworkResult<DashboardSummary> {
        return result
    }
}

class FakeAvisoRepository : AvisoRepository {
    var result: NetworkResult<List<Aviso>> = NetworkResult.Success(emptyList())
    
    override suspend fun getAvisos(forceRefresh: Boolean): NetworkResult<List<Aviso>> {
        return result
    }
}
