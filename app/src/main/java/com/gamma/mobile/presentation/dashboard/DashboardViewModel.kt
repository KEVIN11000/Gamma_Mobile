package com.gamma.mobile.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.repository.AvisoRepository
import com.gamma.mobile.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import java.time.Clock

import com.gamma.mobile.domain.repository.AsistenciaRepository

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val summary: DashboardSummary, val avisos: List<AvisoUiModel>) : DashboardUiState()
    data class Error(val message: String, val isOffline: Boolean = false) : DashboardUiState()
    object Empty : DashboardUiState()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val avisoRepository: AvisoRepository,
    private val clock: Clock,
    private val asistenciaRepository: AsistenciaRepository? = null
) : ViewModel() {

    // Constructor secundario para compatibilidad con tests unitarios existentes
    constructor(
        dashboardRepository: DashboardRepository,
        avisoRepository: AvisoRepository,
        clock: Clock
    ) : this(dashboardRepository, avisoRepository, clock, null)

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _asistenciaMessage = MutableStateFlow<String?>(null)
    val asistenciaMessage: StateFlow<String?> = _asistenciaMessage.asStateFlow()

    private val _asistenciaIsError = MutableStateFlow(false)
    val asistenciaIsError: StateFlow<Boolean> = _asistenciaIsError.asStateFlow()

    private val _isMarkingAsistencia = MutableStateFlow(false)
    val isMarkingAsistencia: StateFlow<Boolean> = _isMarkingAsistencia.asStateFlow()

    private val _modoMarcacion = MutableStateFlow(MODO_NORMAL)
    val modoMarcacion: StateFlow<String> = _modoMarcacion.asStateFlow()

    private var loadJob: Job? = null
    private var initialLoadDone = false

    init {
        loadDashboard()
    }

    /**
     * Carga el dashboard. Con [silent] = true no vuelve a mostrar el indicador de carga ni reemplaza
     * el contenido visible por un error si el refresco falla (útil al volver a la pantalla).
     */
    fun loadDashboard(forceRefresh: Boolean = false, silent: Boolean = false) {
        loadJob?.cancel()
        if (!silent) {
            _uiState.value = DashboardUiState.Loading
        }

        loadJob = viewModelScope.launch {
            val summaryResult = dashboardRepository.getSummary(forceRefresh)
            val avisosResult = avisoRepository.getAvisos(forceRefresh)

            if (summaryResult is NetworkResult.Success && avisosResult is NetworkResult.Success) {
                // Determine if it's considered Empty
                if (summaryResult.data.saldoAcumulado == 0L && summaryResult.data.margenLibreDisponible == 0L) {
                    _uiState.value = DashboardUiState.Empty
                } else {
                    val avisosUi = avisosResult.data.filterAndSortForDashboard(clock)
                    _uiState.value = DashboardUiState.Success(summaryResult.data, avisosUi)
                }
            } else if (!silent) {
                val isOffline = summaryResult is NetworkResult.Exception || avisosResult is NetworkResult.Exception
                _uiState.value = DashboardUiState.Error("No se pudo cargar el dashboard", isOffline)
            }
            initialLoadDone = true
        }
    }

    /**
     * Se invoca cada vez que el Dashboard vuelve a ser visible (p. ej. tras guardar una transacción).
     * Este ViewModel vive tanto como la Activity, por lo que sin esto los saldos quedarían congelados
     * con los datos de la primera carga.
     */
    fun onScreenVisible() {
        if (!initialLoadDone) return // la carga inicial (init) todavía está en curso
        loadDashboard(forceRefresh = true, silent = true)
    }

    fun setModoMarcacion(modo: String) {
        if (modo == MODO_NORMAL || modo == MODO_DIRECTO) {
            _modoMarcacion.value = modo
        }
    }

    fun marcarAsistencia(modo: String = _modoMarcacion.value) {
        if (asistenciaRepository == null) return
        if (_isMarkingAsistencia.value) return
        viewModelScope.launch {
            _isMarkingAsistencia.value = true
            when (val res = asistenciaRepository.marcarAsistencia(modo)) {
                is NetworkResult.Success -> {
                    val resultado = evaluarMarcacion(res.data)
                    _asistenciaMessage.value = resultado.mensaje
                    _asistenciaIsError.value = resultado.esError
                }
                is NetworkResult.Error -> {
                    _asistenciaMessage.value = "❌ ${res.message.ifEmpty { "Error al marcar" }}"
                    _asistenciaIsError.value = true
                }
                is NetworkResult.Exception -> {
                    _asistenciaMessage.value = "❌ Error de conexión al marcar asistencia"
                    _asistenciaIsError.value = true
                }
            }
            _isMarkingAsistencia.value = false
        }
    }

    fun dismissAsistenciaMessage() {
        _asistenciaMessage.value = null
        _asistenciaIsError.value = false
    }

    companion object {
        const val MODO_NORMAL = "normal"
        const val MODO_DIRECTO = "directo"
    }
}
