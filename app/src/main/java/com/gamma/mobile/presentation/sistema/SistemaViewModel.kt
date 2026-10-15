package com.gamma.mobile.presentation.sistema

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.HealthStatus
import com.gamma.mobile.domain.model.LogEntry
import com.gamma.mobile.domain.model.TicketItem
import com.gamma.mobile.domain.repository.SecureStorageRepository
import com.gamma.mobile.domain.repository.SistemaRepository
import com.gamma.mobile.domain.model.UpdateStatus
import com.gamma.mobile.domain.usecase.CheckAppUpdateUseCase
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
    val ticketFiltro: String = "Todos", // "Todos", "Abierto", "En proceso", "Cerrado"
    val logs: List<LogEntry> = emptyList(),
    val baseUrl: String = "",
    val apiKey: String = "",
    val isApiKeyConfigured: Boolean = false,
    val configMessage: String? = null,
    val error: String? = null,
    val ticketCreationSuccess: String? = null,
    val ticketActionMessage: String? = null,
    val updateStatus: UpdateStatus = UpdateStatus.Idle
) {
    val ticketsFiltrados: List<TicketItem>
        get() = when (ticketFiltro) {
            "Todos" -> tickets
            else -> tickets.filter { it.estado.equals(ticketFiltro, ignoreCase = true) }
        }
}

@HiltViewModel
class SistemaViewModel @Inject constructor(
    private val repository: SistemaRepository,
    private val secureStorage: SecureStorageRepository,
    private val checkAppUpdateUseCase: CheckAppUpdateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SistemaUiState(
            appVersion = repository.getAppVersion(),
            baseUrl = secureStorage.getBaseUrl(),
            apiKey = secureStorage.getApiKey() ?: "",
            isApiKeyConfigured = !secureStorage.getApiKey().isNullOrBlank()
        )
    )
    val uiState: StateFlow<SistemaUiState> = _uiState.asStateFlow()

    init {
        cargarDatosSistema()
    }

    fun guardarConfiguracion(url: String, key: String) {
        val sanitizedUrl = url.trim()
        val sanitizedKey = key.trim()

        if (sanitizedUrl.isNotBlank()) {
            secureStorage.saveBaseUrl(sanitizedUrl)
        }
        if (sanitizedKey.isNotBlank()) {
            secureStorage.saveApiKey(sanitizedKey)
        }

        _uiState.update {
            it.copy(
                baseUrl = secureStorage.getBaseUrl(),
                apiKey = secureStorage.getApiKey() ?: "",
                isApiKeyConfigured = !secureStorage.getApiKey().isNullOrBlank(),
                configMessage = "Configuración guardada. Probando conexión..."
            )
        }
        cargarDatosSistema(isTestingConnection = true)
    }

    fun cargarDatosSistema(isTestingConnection: Boolean = false) {
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

            // 4. Comprobación autónoma de Release en GitHub
            val updateStatus = checkAppUpdateUseCase(repository.getAppVersion())

            val errorMessage = when {
                healthResult is NetworkResult.Error -> healthResult.message
                healthResult is NetworkResult.Exception -> healthResult.e.localizedMessage
                else -> null
            }

            _uiState.update { state ->
                val updatedConfigMessage = if (isTestingConnection) {
                    if (healthResult is NetworkResult.Success) {
                        "✅ Conexión establecida exitosamente con el backend"
                    } else if (!errorMessage.isNullOrBlank()) {
                        "❌ Error de conexión: $errorMessage"
                    } else {
                        "❌ No se pudo conectar con el backend"
                    }
                } else {
                    state.configMessage
                }

                state.copy(
                    isLoading = false,
                    appVersion = repository.getAppVersion(),
                    health = healthData,
                    tickets = ticketsData,
                    logs = logsData,
                    error = errorMessage,
                    configMessage = updatedConfigMessage,
                    updateStatus = updateStatus
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
 
    fun setFiltroTickets(filtro: String) {
        _uiState.update { it.copy(ticketFiltro = filtro) }
    }

    fun actualizarEstadoTicket(ticketId: String, nuevoEstado: String, resolucion: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, ticketActionMessage = null) }
            when (val result = repository.updateTicket(ticketId, nuevoEstado, resolucion)) {
                is NetworkResult.Success -> {
                    val ticketActualizado = result.data
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            tickets = state.tickets.map { if (it.id == ticketId) ticketActualizado else it },
                            ticketActionMessage = "Ticket $ticketId actualizado a '$nuevoEstado'"
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message.ifEmpty { "Error ${result.code} al actualizar ticket" }
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.e.localizedMessage ?: "Error de red al actualizar ticket"
                        )
                    }
                }
            }
        }
    }

    fun limpiarMensajes() {
        _uiState.update {
            it.copy(
                error = null,
                ticketCreationSuccess = null,
                ticketActionMessage = null,
                configMessage = null
            )
        }
    }
}
