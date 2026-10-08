package com.gamma.mobile.presentation.avisos

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.repository.AvisoRepository
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

class FakeAvisoRepository : AvisoRepository {
    var avisosResult: NetworkResult<List<Aviso>> = NetworkResult.Success(emptyList())
    var createResult: NetworkResult<Unit> = NetworkResult.Success(Unit)
    var deleteResult: NetworkResult<Unit> = NetworkResult.Success(Unit)
    var iaResult: NetworkResult<Aviso> = NetworkResult.Success(Aviso(id = "ia-1", titulo = "Test IA", fechaEvento = "01/01/2027 10:00"))

    var createdAvisos = mutableListOf<Aviso>()
    var deletedAvisoIds = mutableListOf<String>()
    var interpretedTexts = mutableListOf<String>()

    override suspend fun getAvisos(forceRefresh: Boolean): NetworkResult<List<Aviso>> = avisosResult

    override suspend fun createAviso(aviso: Aviso): NetworkResult<Unit> {
        if (createResult is NetworkResult.Success) {
            createdAvisos.add(aviso)
        }
        return createResult
    }

    override suspend fun deleteAviso(id: String): NetworkResult<Unit> {
        if (deleteResult is NetworkResult.Success) {
            deletedAvisoIds.add(id)
        }
        return deleteResult
    }

    override suspend fun interpretarAvisoIa(texto: String): NetworkResult<Aviso> {
        if (iaResult is NetworkResult.Success) {
            interpretedTexts.add(texto)
        }
        return iaResult
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AvisosViewModelTest {

    private lateinit var viewModel: AvisosViewModel
    private lateinit var fakeRepo: FakeAvisoRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAvisoRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadAvisos success with empty list updates state to Empty`() = runTest {
        fakeRepo.avisosResult = NetworkResult.Success(emptyList())
        viewModel = AvisosViewModel(fakeRepo)
        
        advanceUntilIdle()
        
        assertEquals(AvisosUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `loadAvisos success with data updates state to Success and sorts by date`() = runTest {
        fakeRepo.avisosResult = NetworkResult.Success(listOf(
            Aviso(id = "2", titulo = "B", fechaEvento = "2026-10-10 10:00"),
            Aviso(id = "1", titulo = "A", fechaEvento = "2026-10-09 10:00")
        ))
        viewModel = AvisosViewModel(fakeRepo)
        
        advanceUntilIdle()
        
        val state = viewModel.uiState.value as AvisosUiState.Success
        assertEquals(2, state.avisos.size)
        assertEquals("1", state.avisos[0].id) // 09 is before 10
    }

    @Test
    fun `loadAvisos error updates state to Error`() = runTest {
        fakeRepo.avisosResult = NetworkResult.Error(500, "Error de servidor")
        viewModel = AvisosViewModel(fakeRepo)
        
        advanceUntilIdle()
        
        val state = viewModel.uiState.value as AvisosUiState.Error
        assertEquals("Error de servidor", state.message)
    }

    @Test
    fun `createAviso success reloads list`() = runTest {
        fakeRepo.avisosResult = NetworkResult.Success(emptyList())
        viewModel = AvisosViewModel(fakeRepo)
        advanceUntilIdle()
        
        fakeRepo.avisosResult = NetworkResult.Success(listOf(Aviso(id = "1", titulo = "New")))
        viewModel.createAviso(Aviso(id = "1", titulo = "New"))
        advanceUntilIdle()
        
        assertEquals(1, fakeRepo.createdAvisos.size)
        val state = viewModel.uiState.value as AvisosUiState.Success
        assertEquals(1, state.avisos.size)
        assertEquals("New", state.avisos[0].titulo)
    }

    @Test
    fun `deleteAviso success reloads list`() = runTest {
        fakeRepo.avisosResult = NetworkResult.Success(listOf(Aviso(id = "1")))
        viewModel = AvisosViewModel(fakeRepo)
        advanceUntilIdle()
        
        fakeRepo.avisosResult = NetworkResult.Success(emptyList())
        viewModel.deleteAviso("1")
        advanceUntilIdle()
        
        assertEquals(1, fakeRepo.deletedAvisoIds.size)
        assertEquals("1", fakeRepo.deletedAvisoIds[0])
        assertEquals(AvisosUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `interpretarAvisoIa success creates aviso and reloads list`() = runTest {
        fakeRepo.avisosResult = NetworkResult.Success(emptyList())
        viewModel = AvisosViewModel(fakeRepo)
        advanceUntilIdle()
        
        val newAviso = Aviso(id = "ia-1", titulo = "Interpreted", fechaEvento = "01/01")
        fakeRepo.iaResult = NetworkResult.Success(newAviso)
        fakeRepo.avisosResult = NetworkResult.Success(listOf(newAviso))
        
        viewModel.interpretarAvisoIa("texto libre")
        advanceUntilIdle()
        
        assertEquals(1, fakeRepo.interpretedTexts.size)
        assertEquals(1, fakeRepo.createdAvisos.size)
        assertEquals("Interpreted", fakeRepo.createdAvisos[0].titulo)
        
        val state = viewModel.uiState.value as AvisosUiState.Success
        assertEquals(1, state.avisos.size)
    }
}
