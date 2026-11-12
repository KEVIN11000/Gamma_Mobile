package com.gamma.mobile.presentation.presupuesto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.GastoFijoCreateDto
import com.gamma.mobile.data.remote.dto.GastoFijoItemDto
import com.gamma.mobile.data.remote.dto.GastoFijoUpdateDto
import com.gamma.mobile.data.remote.dto.SalarioBaseUpdateDto
import com.gamma.mobile.data.remote.dto.TechoPresupuestarioItemDto
import com.gamma.mobile.data.remote.dto.TechoPresupuestarioUpdateDto
import com.gamma.mobile.domain.repository.CapacidadesRepository
import com.gamma.mobile.domain.repository.PresupuestoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PresupuestoUiState {
    object Loading : PresupuestoUiState()
    data class Success(
        val salarioBase: Long,
        val totalGastosFijos: Long,
        val margenEstructural: Long,
        val categoriasFijos: Map<String, List<GastoFijoItemDto>>,
        val techosVariables: List<TechoPresupuestarioItemDto>,
        val puedeEliminarFijos: Boolean = true,
        val puedeEditarTechos: Boolean = true,
        val puedeEditarSalario: Boolean = true
    ) : PresupuestoUiState()
    data class Error(val message: String) : PresupuestoUiState()
}

@HiltViewModel
class PresupuestoViewModel @Inject constructor(
    private val repository: PresupuestoRepository,
    private val capacidadesRepository: CapacidadesRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<PresupuestoUiState>(PresupuestoUiState.Loading)
    val uiState: StateFlow<PresupuestoUiState> = _uiState.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _feedbackIsError = MutableStateFlow(false)
    val feedbackIsError: StateFlow<Boolean> = _feedbackIsError.asStateFlow()

    init {
        loadPresupuesto()
    }

    fun loadPresupuesto(forceRefresh: Boolean = false) {
        if (!forceRefresh) {
            _uiState.value = PresupuestoUiState.Loading
        }
        viewModelScope.launch {
            val fijosResult = repository.getGastosFijos(forceRefresh)
            val techosResult = repository.getTechosPresupuestarios(forceRefresh)

            if (fijosResult is NetworkResult.Success && techosResult is NetworkResult.Success) {
                val fijos = fijosResult.data
                val techos = techosResult.data

                val puedeEliminar = capacidadesRepository?.isCapacidadHabilitada("presupuesto_fijos_delete", true) ?: true
                val puedeEditarTechos = capacidadesRepository?.isCapacidadHabilitada("presupuesto_techos_put", true) ?: true
                val puedeEditarSalario = capacidadesRepository?.isCapacidadHabilitada("presupuesto_salario_put", true) ?: true

                _uiState.value = PresupuestoUiState.Success(
                    salarioBase = fijos.salarioBase,
                    totalGastosFijos = fijos.totalGastosFijos,
                    margenEstructural = (fijos.salarioBase - fijos.totalGastosFijos).coerceAtLeast(0L),
                    categoriasFijos = fijos.categorias,
                    techosVariables = techos,
                    puedeEliminarFijos = puedeEliminar,
                    puedeEditarTechos = puedeEditarTechos,
                    puedeEditarSalario = puedeEditarSalario
                )
            } else {
                val errorMsg = when {
                    fijosResult is NetworkResult.Error -> fijosResult.message
                    fijosResult is NetworkResult.Exception -> "Error de red al consultar gastos fijos."
                    techosResult is NetworkResult.Error -> techosResult.message
                    techosResult is NetworkResult.Exception -> "Error de red al consultar techos."
                    else -> "No se pudo cargar el presupuesto."
                }
                _uiState.value = PresupuestoUiState.Error(errorMsg)
            }
        }
    }

    fun actualizarSalarioBase(nuevoSalario: Long) {
        viewModelScope.launch {
            when (val res = repository.updateSalarioBase(SalarioBaseUpdateDto(nuevoSalario))) {
                is NetworkResult.Success -> {
                    _feedbackMessage.value = "Salario base actualizado con éxito."
                    _feedbackIsError.value = false
                    loadPresupuesto(true)
                }
                is NetworkResult.Error -> {
                    _feedbackMessage.value = "Error al actualizar salario: ${res.message}"
                    _feedbackIsError.value = true
                }
                is NetworkResult.Exception -> {
                    _feedbackMessage.value = "Fallo de conexión al actualizar salario."
                    _feedbackIsError.value = true
                }
            }
        }
    }

    fun crearGastoFijo(categoria: String, concepto: String, monto: Long) {
        viewModelScope.launch {
            val req = GastoFijoCreateDto(categoria = categoria, concepto = concepto, monto = monto)
            when (val res = repository.createGastoFijo(req)) {
                is NetworkResult.Success -> {
                    _feedbackMessage.value = "Gasto fijo '$concepto' registrado."
                    _feedbackIsError.value = false
                    loadPresupuesto(true)
                }
                is NetworkResult.Error -> {
                    _feedbackMessage.value = "Error al registrar gasto fijo: ${res.message}"
                    _feedbackIsError.value = true
                }
                is NetworkResult.Exception -> {
                    _feedbackMessage.value = "Fallo de conexión al registrar gasto fijo."
                    _feedbackIsError.value = true
                }
            }
        }
    }

    fun actualizarGastoFijo(concepto: String, nuevoMonto: Long) {
        viewModelScope.launch {
            when (val res = repository.updateGastoFijo(concepto, GastoFijoUpdateDto(nuevoMonto))) {
                is NetworkResult.Success -> {
                    _feedbackMessage.value = "Monto de '$concepto' actualizado."
                    _feedbackIsError.value = false
                    loadPresupuesto(true)
                }
                is NetworkResult.Error -> {
                    _feedbackMessage.value = "Error al actualizar '$concepto': ${res.message}"
                    _feedbackIsError.value = true
                }
                is NetworkResult.Exception -> {
                    _feedbackMessage.value = "Fallo de conexión al actualizar '$concepto'."
                    _feedbackIsError.value = true
                }
            }
        }
    }

    fun eliminarGastoFijo(concepto: String) {
        viewModelScope.launch {
            when (val res = repository.deleteGastoFijo(concepto)) {
                is NetworkResult.Success -> {
                    _feedbackMessage.value = "Gasto fijo '$concepto' eliminado."
                    _feedbackIsError.value = false
                    loadPresupuesto(true)
                }
                is NetworkResult.Error -> {
                    _feedbackMessage.value = "Error al eliminar '$concepto': ${res.message}"
                    _feedbackIsError.value = true
                }
                is NetworkResult.Exception -> {
                    _feedbackMessage.value = "Fallo de conexión al eliminar '$concepto'."
                    _feedbackIsError.value = true
                }
            }
        }
    }

    fun actualizarTecho(categoria: String, limiteMensual: Long?, umbralAlerta: Int?) {
        viewModelScope.launch {
            val req = TechoPresupuestarioUpdateDto(limiteMensual = limiteMensual, umbralAlerta = umbralAlerta)
            when (val res = repository.updateTechoPresupuestario(categoria, req)) {
                is NetworkResult.Success -> {
                    _feedbackMessage.value = "Techo de '$categoria' actualizado."
                    _feedbackIsError.value = false
                    loadPresupuesto(true)
                }
                is NetworkResult.Error -> {
                    _feedbackMessage.value = "Error al actualizar techo: ${res.message}"
                    _feedbackIsError.value = true
                }
                is NetworkResult.Exception -> {
                    _feedbackMessage.value = "Fallo de conexión al actualizar techo."
                    _feedbackIsError.value = true
                }
            }
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
        _feedbackIsError.value = false
    }
}
