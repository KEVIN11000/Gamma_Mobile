package com.gamma.mobile.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.repository.AvisoRepository
import com.gamma.mobile.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _isMarkingAsistencia = MutableStateFlow(false)
    val isMarkingAsistencia: StateFlow<Boolean> = _isMarkingAsistencia.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard(forceRefresh: Boolean = false) {
        _uiState.value = DashboardUiState.Loading

        viewModelScope.launch {
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
            } else {
                val isOffline = summaryResult is NetworkResult.Exception || avisosResult is NetworkResult.Exception
                _uiState.value = DashboardUiState.Error("No se pudo cargar el dashboard", isOffline)
            }
        }
    }

    fun marcarAsistencia(modo: String = "normal") {
        if (asistenciaRepository == null) return
        viewModelScope.launch {
            _isMarkingAsistencia.value = true
            when (val res = asistenciaRepository.marcarAsistencia(modo)) {
                is NetworkResult.Success -> {
                    val data = res.data
                    val hora = data.hora
                    _asistenciaMessage.value = "✅ ${data.mensaje.ifEmpty { "Marcación registrada" }}${if (hora.isNotEmpty()) " a las $hora" else ""}"
                }
                is NetworkResult.Error -> {
                    _asistenciaMessage.value = "❌ ${res.message.ifEmpty { "Error al marcar" }}"
                }
                is NetworkResult.Exception -> {
                    _asistenciaMessage.value = "❌ Error de conexión al marcar asistencia"
                }
            }
            _isMarkingAsistencia.value = false
        }
    }

    fun dismissAsistenciaMessage() {
        _asistenciaMessage.value = null
    }
}
