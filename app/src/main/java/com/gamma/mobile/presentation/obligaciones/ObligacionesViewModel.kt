package com.gamma.mobile.presentation.obligaciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Obligacion
import com.gamma.mobile.domain.model.VencimientoCronogramaItem
import com.gamma.mobile.domain.model.DeudaFocoResponse
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

enum class FiltroObligacion {
    TODAS, ACTIVAS, PAUSADAS, SALDADAS, SIMULADAS
}

data class SimulacionResult(
    val cuotaMensual: Long,
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

    private var todasLasObligaciones: List<Obligacion> = emptyList()
    private val _filtroActual = MutableStateFlow(FiltroObligacion.TODAS)
    val filtroActual: StateFlow<FiltroObligacion> = _filtroActual.asStateFlow()

    private val _compromisoMensual = MutableStateFlow(0L)
    val compromisoMensual: StateFlow<Long> = _compromisoMensual.asStateFlow()

    private val _cronogramaVencimientos = MutableStateFlow<List<VencimientoCronogramaItem>>(emptyList())
    val cronogramaVencimientos: StateFlow<List<VencimientoCronogramaItem>> = _cronogramaVencimientos.asStateFlow()

    private val _deudaFoco = MutableStateFlow<DeudaFocoResponse?>(null)
    val deudaFoco: StateFlow<DeudaFocoResponse?> = _deudaFoco.asStateFlow()

    private val _criterioFoco = MutableStateFlow<String>("saldo")
    val criterioFoco: StateFlow<String> = _criterioFoco.asStateFlow()

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
                    todasLasObligaciones = result.data
                    emitFilteredState()
                }
                is NetworkResult.Exception -> {
                    _uiState.value = ObligacionesUiState.Error("Fallo de red al obtener deudas.")
                }
                is NetworkResult.Error -> {
                    _uiState.value = ObligacionesUiState.Error("Error: ${result.message}")
                }
            }
        }
        loadCronograma(forceRefresh)
        loadDeudaFoco(_criterioFoco.value)
    }

    fun loadCronograma(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            when (val result = repository.getCronogramaVencimientos(forceRefresh)) {
                is NetworkResult.Success -> _cronogramaVencimientos.value = result.data
                else -> {}
            }
        }
    }

    fun loadDeudaFoco(criterio: String = _criterioFoco.value) {
        viewModelScope.launch {
            when (val result = repository.getDeudaFoco(criterio)) {
                is NetworkResult.Success -> _deudaFoco.value = result.data
                else -> {}
            }
        }
    }

    fun cambiarCriterioFoco(criterio: String) {
        if (_criterioFoco.value != criterio) {
            _criterioFoco.value = criterio
            loadDeudaFoco(criterio)
        }
    }

    fun setFiltro(filtro: FiltroObligacion) {
        if (_filtroActual.value == filtro) return
        _filtroActual.value = filtro
        emitFilteredState()
    }

    private fun emitFilteredState() {
        val filtered = when (_filtroActual.value) {
            FiltroObligacion.TODAS -> todasLasObligaciones
            FiltroObligacion.ACTIVAS -> todasLasObligaciones.filter { it.esActiva }
            FiltroObligacion.PAUSADAS -> todasLasObligaciones.filter { it.esPausada }
            FiltroObligacion.SALDADAS -> todasLasObligaciones.filter { it.esSaldada }
            FiltroObligacion.SIMULADAS -> todasLasObligaciones.filter { it.esSimulada }
        }
        _compromisoMensual.value = todasLasObligaciones.filter { it.esActiva && !it.esPausada }.sumOf { it.cuotaReferenciaGs }
        _uiState.value = ObligacionesUiState.Success(filtered)
    }

    fun calcularCompromisoMensual(obligaciones: List<Obligacion> = todasLasObligaciones): Long {
        return obligaciones.filter { it.esActiva && !it.esPausada }.sumOf { it.cuotaReferenciaGs }
    }

    fun simularCuota(monto: Long, cuotas: Int, cuotaPersonalizada: Long? = null) {
        if (monto <= 0L || cuotas <= 0) {
            _simulacionResult.value = null
            return
        }
        viewModelScope.launch {
            val summaryResult = dashboardRepository.getSummary()
            val margen = if (summaryResult is NetworkResult.Success) {
                summaryResult.data.margenLibreDisponible
            } else {
                0L
            }

            val cuota = if (cuotaPersonalizada != null && cuotaPersonalizada > 0L) {
                cuotaPersonalizada
            } else {
                monto / cuotas
            }

            val (nivel, mensaje) = when {
                margen > 0L && cuota <= margen * 0.3 -> NivelRiesgo.VERDE to "Seguro. Tienes margen suficiente (consumirá ≤30% de tu margen)."
                margen > 0L && cuota <= margen * 0.6 -> NivelRiesgo.AMARILLO to "Precaución. La cuota consumirá una porción considerable de tu margen."
                else -> NivelRiesgo.ROJO to "Peligro. La cuota excede tu capacidad de pago recomendada."
            }

            _simulacionResult.value = SimulacionResult(cuota, nivel, mensaje)
        }
    }

    fun clearSimulacion() {
        _simulacionResult.value = null
    }

    fun crearObligacion(obligacion: Obligacion) {
        val nueva = if (obligacion.id.isBlank()) {
            obligacion.copy(id = UUID.randomUUID().toString())
        } else {
            obligacion
        }
        todasLasObligaciones = todasLasObligaciones + nueva
        emitFilteredState()

        viewModelScope.launch {
            repository.createObligacion(nueva)
            loadCronograma(true)
            loadDeudaFoco()
        }
    }

    // Sobrecarga para compatibilidad existente
    fun crearObligacion(nombre: String, montoInicial: Long) {
        val nueva = Obligacion(
            id = UUID.randomUUID().toString(),
            nombre = nombre,
            montoInicial = montoInicial,
            saldoActual = montoInicial,
            estado = "Simulado"
        )
        crearObligacion(nueva)
    }

    fun eliminarObligacion(id: String) {
        todasLasObligaciones = todasLasObligaciones.filterNot { it.id == id }
        emitFilteredState()

        viewModelScope.launch {
            repository.deleteObligacion(id)
            loadCronograma(true)
            loadDeudaFoco()
        }
    }

    fun cancelarObligacionSimulada(obligacion: Obligacion) {
        if (obligacion.esSimulada) {
            eliminarObligacion(obligacion.id)
        }
    }

    fun pausarObligacion(obligacion: Obligacion) {
        if (obligacion.esActiva) {
            val pausada = obligacion.copy(estado = "Pausada")
            updateInListAndRemote(pausada)
        }
    }

    fun reanudarObligacion(obligacion: Obligacion) {
        if (obligacion.esPausada) {
            val reactivada = obligacion.copy(estado = "Activo")
            updateInListAndRemote(reactivada)
        }
    }

    fun togglePausarObligacion(obligacion: Obligacion) {
        if (obligacion.esPausada) {
            reanudarObligacion(obligacion)
        } else if (obligacion.esActiva) {
            pausarObligacion(obligacion)
        }
    }

    fun aprobarObligacion(obligacion: Obligacion) {
        val currentState = _uiState.value
        if (currentState is ObligacionesUiState.Success && obligacion.esSimulada) {
            val aprobada = obligacion.copy(estado = "Activo")
            updateInListAndRemote(aprobada)
        }
    }

    fun abonarObligacion(obligacion: Obligacion, montoAbono: Long) {
        val currentState = _uiState.value
        if (currentState is ObligacionesUiState.Success && obligacion.esActiva) {
            val nuevoSaldo = (obligacion.saldoActual - montoAbono).coerceAtLeast(0L)
            val nuevoEstado = if (nuevoSaldo == 0L) "Saldada" else "Activo"
            val nuevasRestantes = (obligacion.cuotasRestantes - 1).coerceAtLeast(0)

            val actualizada = obligacion.copy(
                saldoActual = nuevoSaldo,
                estado = nuevoEstado,
                cuotasRestantes = nuevasRestantes
            )
            todasLasObligaciones = todasLasObligaciones.map { if (it.id == actualizada.id) actualizada else it }
            emitFilteredState()

            viewModelScope.launch {
                repository.updateObligacion(actualizada)
                repository.abonarObligacion(obligacion.id, montoAbono)
                loadCronograma(true)
                loadDeudaFoco()
            }
        }
    }

    private fun updateInListAndRemote(updated: Obligacion) {
        todasLasObligaciones = todasLasObligaciones.map { if (it.id == updated.id) updated else it }
        emitFilteredState()

        viewModelScope.launch {
            repository.updateObligacion(updated)
            loadCronograma(true)
            loadDeudaFoco()
        }
    }
}
