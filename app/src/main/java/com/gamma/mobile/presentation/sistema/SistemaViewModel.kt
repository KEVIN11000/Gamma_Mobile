package com.gamma.mobile.presentation.sistema

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.HealthStatus
import com.gamma.mobile.domain.model.LogEntry
import com.gamma.mobile.domain.model.TicketItem
import com.gamma.mobile.domain.repository.SistemaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SistemaUiState(
    val isLoading: Boolean = false,
    val appVersion: String = "",
    val health: HealthStatus? = null,
    val tickets: List<TicketItem> = emptyList(),
    val logs: List<LogEntry> = emptyList(),
    val error: String? = null,
    val ticketCreationSuccess: String? = null
)

@HiltViewModel
class SistemaViewModel @Inject constructor(
    private val repository: SistemaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SistemaUiState(appVersion = repository.getAppVersion())
    )
    val uiState: StateFlow<SistemaUiState> = _uiState.asStateFlow()

    init {
        cargarDatosSistema()
    }

    fun cargarDatosSistema() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 1. Health Status
            val healthResult = repository.getHealthStatus()
            val healthData = if (healthResult is NetworkResult.Success) healthResult.data else null

            // 2. Tickets
            val ticketsResult = repository.getTickets()
            val ticketsData = if (ticketsResult is NetworkResult.Success) ticketsResult.data else emptyList()

            // 3. Logs
            val logsResult = repository.getLogs()
            val logsData = if (logsResult is NetworkResult.Success) logsResult.data else emptyList()

            val errorMessage = when {
                healthResult is NetworkResult.Error -> healthResult.message
                healthResult is NetworkResult.Exception -> healthResult.e.localizedMessage
                else -> null
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    appVersion = repository.getAppVersion(),
                    health = healthData,
                    tickets = ticketsData,
                    logs = logsData,
                    error = errorMessage
                )
            }
        }
    }

    fun crearTicket(asunto: String, descripcion: String) {
        val asu = asunto.trim()
        val desc = descripcion.trim()

        if (asu.isEmpty()) {
            _uiState.update { it.copy(error = "El asunto del ticket no puede estar vacío") }
            return
        }

        if (desc.isEmpty()) {
            _uiState.update { it.copy(error = "La descripción del ticket no puede estar vacía") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, ticketCreationSuccess = null) }
            when (val result = repository.createTicket(asu, desc)) {
                is NetworkResult.Success -> {
                    val nuevoTicket = result.data
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            tickets = listOf(nuevoTicket) + state.tickets,
                            ticketCreationSuccess = "Ticket ${nuevoTicket.id} creado con éxito"
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message.ifEmpty { "Error ${result.code} al crear ticket" }) }
                }
                is NetworkResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false, error = result.e.localizedMessage ?: "Error de red al crear ticket") }
                }
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update { it.copy(error = null, ticketCreationSuccess = null) }
    }
}
