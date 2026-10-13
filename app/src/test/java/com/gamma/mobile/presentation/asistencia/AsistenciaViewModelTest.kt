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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAsistenciaRepository : AsistenciaRepository {
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
        return asistenciaResult
    }

    override suspend fun marcarMateria(materia: String): NetworkResult<AsistenciaMateriaResponse> {
        lastMateriaCalled = materia
        return materiaResult
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AsistenciaViewModelTest {

    private lateinit var viewModel: AsistenciaViewModel
    private lateinit var fakeRepo: FakeAsistenciaRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeAsistenciaRepository()
        viewModel = AsistenciaViewModel(fakeRepo)
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
        assertTrue(viewModel.materiasDisponibles.isNotEmpty())
        assertTrue(viewModel.materiasDisponibles.contains("Algoritmos y Estructuras de Datos"))

        val selected = viewModel.materiasDisponibles.first()
        viewModel.setMateria(selected)
        assertEquals(selected, viewModel.materia.value)
    }

    @Test
    fun `marcarAsistencia success normal updates state to Success`() = runTest {
        fakeRepo.asistenciaResult = NetworkResult.Success(
            AsistenciaMarcaResponse(tipo = "entrada", hora = "08:30", mensaje = "Marcación OK")
        )

        viewModel.marcarAsistencia("normal")
        advanceUntilIdle()

        assertEquals("normal", fakeRepo.lastModoCalled)
        val state = viewModel.uiState.value
        assertTrue(state is AsistenciaUiState.Success)
        val successState = state as AsistenciaUiState.Success
        assertEquals("Marcación OK", successState.mensaje)
        assertEquals("08:30", successState.hora)
        assertTrue(successState.detalle.contains("Entrada"))
        assertTrue(successState.detalle.contains("Modo normal"))
    }

    @Test
    fun `marcarAsistencia success directo updates state to Success`() = runTest {
        fakeRepo.asistenciaResult = NetworkResult.Success(
            AsistenciaMarcaResponse(tipo = "salida", hora = "17:00", mensaje = "Salida OK")
        )

        viewModel.setModo("directo")
        viewModel.marcarAsistencia()
        advanceUntilIdle()

        assertEquals("directo", fakeRepo.lastModoCalled)
        val state = viewModel.uiState.value as AsistenciaUiState.Success
        assertEquals("Salida OK", state.mensaje)
        assertEquals("17:00", state.hora)
        assertTrue(state.detalle.contains("Salida"))
        assertTrue(state.detalle.contains("Modo directo"))
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
}
