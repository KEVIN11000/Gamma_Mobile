package com.gamma.mobile.presentation.capacidad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.CapacidadPagoResponse
import com.gamma.mobile.domain.model.EscenarioCapacidad
import com.gamma.mobile.domain.model.GastoSimulado
import com.gamma.mobile.domain.model.Obligacion
import com.gamma.mobile.domain.repository.DashboardRepository
import com.gamma.mobile.domain.repository.EscenarioCapacidadRepository
import com.gamma.mobile.domain.repository.ObligacionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CapacidadPagoUiState {
    object Loading : CapacidadPagoUiState()
    data class Success(
        val resultado: CapacidadPagoResponse,
        val deudasActivas: List<Obligacion>,
        val margenBase: Long
    ) : CapacidadPagoUiState()
    data class Error(val message: String) : CapacidadPagoUiState()
}

@HiltViewModel
class CapacidadPagoViewModel @Inject constructor(
    private val obligacionRepository: ObligacionRepository,
    private val dashboardRepository: DashboardRepository,
    private val escenarioRepository: EscenarioCapacidadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CapacidadPagoUiState>(CapacidadPagoUiState.Loading)
    val uiState: StateFlow<CapacidadPagoUiState> = _uiState.asStateFlow()

    private val _deudasPausadasSimuladas = MutableStateFlow<Set<String>>(emptySet())
    val deudasPausadasSimuladas: StateFlow<Set<String>> = _deudasPausadasSimuladas.asStateFlow()

    private val _abonosExtraordinarios = MutableStateFlow<Map<String, Long>>(emptyMap())
    val abonosExtraordinarios: StateFlow<Map<String, Long>> = _abonosExtraordinarios.asStateFlow()

    private val _ajusteIngresoExtra = MutableStateFlow<Long>(0L)
    val ajusteIngresoExtra: StateFlow<Long> = _ajusteIngresoExtra.asStateFlow()

    private val _gastosSimulados = MutableStateFlow<List<GastoSimulado>>(emptyList())
    val gastosSimulados: StateFlow<List<GastoSimulado>> = _gastosSimulados.asStateFlow()

    private val _escenariosGuardados = MutableStateFlow<List<EscenarioCapacidad>>(emptyList())
    val escenariosGuardados: StateFlow<List<EscenarioCapacidad>> = _escenariosGuardados.asStateFlow()

    private val _escenarioActivoNombre = MutableStateFlow<String?>(null)
    val escenarioActivoNombre: StateFlow<String?> = _escenarioActivoNombre.asStateFlow()

    private var todasLasDeudasActivas: List<Obligacion> = emptyList()
    private var margenBase: Long = 0L

    init {
        loadDatos()
        observarEscenarios()
    }

    private fun observarEscenarios() {
        viewModelScope.launch {
            escenarioRepository.getEscenarios().collect { list ->
                _escenariosGuardados.value = list
            }
        }
    }

    fun loadDatos(forceRefresh: Boolean = false) {
        if (!forceRefresh) {
            _uiState.value = CapacidadPagoUiState.Loading
        }
        viewModelScope.launch {
            val summaryRes = dashboardRepository.getSummary(forceRefresh)
            val obligacionesRes = obligacionRepository.getObligaciones(forceRefresh)

            if (summaryRes is NetworkResult.Success && obligacionesRes is NetworkResult.Success) {
                margenBase = summaryRes.data.margenLibreDisponible
                todasLasDeudasActivas = obligacionesRes.data.filter { it.esActiva }
                recalcular()
            } else {
                val errorMsg = when {
                    summaryRes is NetworkResult.Error -> summaryRes.message
                    summaryRes is NetworkResult.Exception -> summaryRes.e.message
                    obligacionesRes is NetworkResult.Error -> obligacionesRes.message
                    obligacionesRes is NetworkResult.Exception -> obligacionesRes.e.message
                    else -> "Error al cargar datos financieros"
                } ?: "Error al cargar datos financieros"
                _uiState.value = CapacidadPagoUiState.Error(errorMsg)
            }
        }
    }

    fun toggleSimularPausa(deudaId: String) {
        val current = _deudasPausadasSimuladas.value.toMutableSet()
        if (current.contains(deudaId)) {
            current.remove(deudaId)
        } else {
            current.add(deudaId)
            val abonos = _abonosExtraordinarios.value.toMutableMap()
            abonos.remove(deudaId)
            _abonosExtraordinarios.value = abonos
        }
        _deudasPausadasSimuladas.value = current
        recalcular()
    }

    fun setAbonoExtraordinario(deudaId: String, monto: Long) {
        val current = _abonosExtraordinarios.value.toMutableMap()
        if (monto <= 0L) {
            current.remove(deudaId)
        } else {
            current[deudaId] = monto
        }
        _abonosExtraordinarios.value = current
        recalcular()
    }

    fun setAjusteIngresoExtra(monto: Long) {
        _ajusteIngresoExtra.value = monto.coerceAtLeast(0L)
        recalcular()
    }

    fun agregarGastoAdHoc(concepto: String, monto: Long) {
        if (concepto.isNotBlank() && monto > 0L) {
            val nuevo = GastoSimulado(
                concepto = concepto.trim(),
                monto = monto,
                incluido = true
            )
            _gastosSimulados.value = _gastosSimulados.value + nuevo
            recalcular()
        }
    }

    fun toggleGastoAdHoc(id: String) {
        _gastosSimulados.value = _gastosSimulados.value.map {
            if (it.id == id) it.copy(incluido = !it.incluido) else it
        }
        recalcular()
    }

    fun eliminarGastoAdHoc(id: String) {
        _gastosSimulados.value = _gastosSimulados.value.filterNot { it.id == id }
        recalcular()
    }

    fun guardarEscenarioActual(nombre: String) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            val nuevo = EscenarioCapacidad(
                nombre = nombre.trim(),
                ingresoExtra = _ajusteIngresoExtra.value,
                deudasPausadasIds = _deudasPausadasSimuladas.value,
                abonosExtraordinarios = _abonosExtraordinarios.value,
                gastosAdHoc = _gastosSimulados.value
            )
            escenarioRepository.guardarEscenario(nuevo)
            _escenarioActivoNombre.value = nuevo.nombre
        }
    }

    fun cargarEscenario(escenario: EscenarioCapacidad) {
        _ajusteIngresoExtra.value = escenario.ingresoExtra
        _deudasPausadasSimuladas.value = escenario.deudasPausadasIds
        _abonosExtraordinarios.value = escenario.abonosExtraordinarios
        _gastosSimulados.value = escenario.gastosAdHoc
        _escenarioActivoNombre.value = escenario.nombre
        recalcular()
    }

    fun eliminarEscenario(id: String) {
        viewModelScope.launch {
            escenarioRepository.eliminarEscenario(id)
            if (_escenarioActivoNombre.value != null && _escenariosGuardados.value.find { it.id == id }?.nombre == _escenarioActivoNombre.value) {
                _escenarioActivoNombre.value = null
            }
        }
    }

    fun resetSimulacion() {
        _deudasPausadasSimuladas.value = emptySet()
        _abonosExtraordinarios.value = emptyMap()
        _ajusteIngresoExtra.value = 0L
        _gastosSimulados.value = emptyList()
        _escenarioActivoNombre.value = null
        recalcular()
    }

    private fun recalcular() {
        val fondoDisponible = (margenBase + _ajusteIngresoExtra.value).coerceAtLeast(0L)
        val deudasNoPausadas = todasLasDeudasActivas.filterNot { it.id in _deudasPausadasSimuladas.value }
        val compromisoTotalMes = deudasNoPausadas.sumOf { it.cuotaReferenciaGs }
        val totalAbonos = _abonosExtraordinarios.value.values.sum()
        val totalGastos = _gastosSimulados.value.filter { it.incluido }.sumOf { it.monto }
        val capacidadNeta = fondoDisponible - compromisoTotalMes - totalAbonos - totalGastos

        val semaforoStr = when {
            fondoDisponible <= 0L || capacidadNeta < 0L -> "DEFICIT"
            capacidadNeta < (fondoDisponible * 0.15).toLong() -> "AJUSTADO"
            else -> "SUPERAVIT"
        }

        val totalEgresosSimulados = compromisoTotalMes + totalAbonos + totalGastos
        val dti = if (fondoDisponible > 0L) {
            (totalEgresosSimulados.toDouble() / fondoDisponible.toDouble()) * 100.0
        } else {
            100.0
        }

        val mesesAhorrados = mutableMapOf<String, Int>()
        for ((id, abono) in _abonosExtraordinarios.value) {
            val deuda = todasLasDeudasActivas.find { it.id == id }
            if (deuda != null && deuda.cuotaReferenciaGs > 0L) {
                val meses = (abono / deuda.cuotaReferenciaGs).toInt()
                mesesAhorrados[id] = if (deuda.cuotasRestantes > 0) minOf(meses, deuda.cuotasRestantes) else meses
            }
        }

        val resultado = CapacidadPagoResponse(
            compromisoTotalMes = compromisoTotalMes,
            fondoDisponibleMes = fondoDisponible,
            totalGastosProyectados = totalGastos,
            capacidadNeta = capacidadNeta,
            semaforo = semaforoStr,
            dtiProyectado = dti,
            mesesAhorradosPorAbonos = mesesAhorrados
        )

        _uiState.value = CapacidadPagoUiState.Success(
            resultado = resultado,
            deudasActivas = todasLasDeudasActivas,
            margenBase = margenBase
        )
    }
}
