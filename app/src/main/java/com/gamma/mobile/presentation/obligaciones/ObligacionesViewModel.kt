package com.gamma.mobile.presentation.obligaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Obligacion
import com.gamma.mobile.domain.repository.DashboardRepository
import com.gamma.mobile.domain.repository.ObligacionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class ObligacionesUiState {
    object Loading : ObligacionesUiState()
    data class Success(val obligaciones: List<Obligacion>) : ObligacionesUiState()
    data class Error(val message: String) : ObligacionesUiState()
}

enum class NivelRiesgo { VERDE, AMARILLO, ROJO }

data class SimulacionResult(
    val cuotaMensual: Int,
    val nivelRiesgo: NivelRiesgo,
    val mensaje: String
)

@HiltViewModel
class ObligacionesViewModel @Inject constructor(
    private val repository: ObligacionRepository,
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ObligacionesUiState>(ObligacionesUiState.Loading)
    val uiState: StateFlow<ObligacionesUiState> = _uiState.asStateFlow()

    private val _simulacionResult = MutableStateFlow<SimulacionResult?>(null)
    val simulacionResult: StateFlow<SimulacionResult?> = _simulacionResult.asStateFlow()

    init {
        loadObligaciones()
    }

    fun loadObligaciones(forceRefresh: Boolean = false) {
        if (!forceRefresh) {
            _uiState.value = ObligacionesUiState.Loading
        }
        viewModelScope.launch {
            when (val result = repository.getObligaciones(forceRefresh)) {
                is NetworkResult.Success -> {
                    _uiState.value = ObligacionesUiState.Success(result.data)
                }
                is NetworkResult.Exception -> {
                    _uiState.value = ObligacionesUiState.Error("Fallo de red al obtener deudas.")
                }
                is NetworkResult.Error -> {
                    _uiState.value = ObligacionesUiState.Error("Error: ${result.message}")
                }
            }
        }
    }

    fun simularCuota(monto: Int, cuotas: Int) {
        if (monto <= 0 || cuotas <= 0) {
            _simulacionResult.value = null
            return
        }
        viewModelScope.launch {
            val summaryResult = dashboardRepository.getSummary()
            val margen = if (summaryResult is NetworkResult.Success) {
                summaryResult.data.margenLibreDisponible
            } else {
                0 // Si falla, asumimos margen 0
            }

            val cuota = monto / cuotas
            val (nivel, mensaje) = when {
                cuota <= margen * 0.3 -> NivelRiesgo.VERDE to "Seguro. Tienes margen suficiente."
                cuota <= margen * 0.6 -> NivelRiesgo.AMARILLO to "Precaución. La cuota consumirá gran parte de tu margen."
                else -> NivelRiesgo.ROJO to "Peligro. La cuota excede tu capacidad de pago recomendada."
            }

            _simulacionResult.value = SimulacionResult(cuota, nivel, mensaje)
        }
    }

    fun clearSimulacion() {
        _simulacionResult.value = null
    }

    fun crearObligacion(nombre: String, montoInicial: Int) {
        val currentState = _uiState.value
        if (currentState is ObligacionesUiState.Success) {
            val nueva = Obligacion(
                id = UUID.randomUUID().toString(),
                nombre = nombre,
                montoInicial = montoInicial,
                saldoActual = montoInicial,
                estado = "Simulado"
            )
            
            // Actualización optimista local
            val newList = currentState.obligaciones.toMutableList().apply { add(nueva) }
            _uiState.value = ObligacionesUiState.Success(newList)

            viewModelScope.launch {
                repository.createObligacion(nueva)
            }
        }
    }

    fun aprobarObligacion(obligacion: Obligacion) {
        val currentState = _uiState.value
        if (currentState is ObligacionesUiState.Success && obligacion.estado == "Simulado") {
            val aprobada = obligacion.copy(estado = "Activo")
            updateInListAndRemote(currentState.obligaciones, aprobada)
        }
    }

    fun abonarObligacion(obligacion: Obligacion, montoAbono: Int) {
        val currentState = _uiState.value
        if (currentState is ObligacionesUiState.Success && obligacion.estado == "Activo") {
            val nuevoSaldo = (obligacion.saldoActual - montoAbono).coerceAtLeast(0)
            val nuevoEstado = if (nuevoSaldo == 0) "Saldado" else "Activo"
            
            val actualizada = obligacion.copy(saldoActual = nuevoSaldo, estado = nuevoEstado)
            updateInListAndRemote(currentState.obligaciones, actualizada)
        }
    }

    private fun updateInListAndRemote(currentList: List<Obligacion>, updated: Obligacion) {
        val newList = currentList.map { if (it.id == updated.id) updated else it }
        _uiState.value = ObligacionesUiState.Success(newList)
        
        viewModelScope.launch {
            repository.updateObligacion(updated)
        }
    }
}
