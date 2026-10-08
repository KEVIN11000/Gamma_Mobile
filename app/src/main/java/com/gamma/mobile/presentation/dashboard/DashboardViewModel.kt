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
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

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
                if (summaryResult.data.saldoAcumulado == 0 && summaryResult.data.margenLibreDisponible == 0) {
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
}
