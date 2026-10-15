package com.gamma.mobile.presentation.sistema

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.CierreResponse
import com.gamma.mobile.domain.model.HealthStatus
import com.gamma.mobile.domain.model.LogEntry
import com.gamma.mobile.domain.model.TicketItem
import com.gamma.mobile.domain.repository.SecureStorageRepository
import com.gamma.mobile.domain.repository.SistemaRepository
import com.gamma.mobile.domain.model.UpdateStatus
import com.gamma.mobile.domain.usecase.CheckAppUpdateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
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
    val updateStatus: UpdateStatus = UpdateStatus.Idle,
    val cierreContable: CierreResponse? = null,
    val isEjecutandoCierre: Boolean = false,
    val cierreMensaje: String? = null,
    val selectedPeriodoCierre: String = "Período Actual",
    val periodosDisponibles: List<String> = listOf(
        "Período Actual",
        "Octubre 2026",
        "Septiembre 2026",
        "Agosto 2026",
        "Julio 2026",
        "Junio 2026"
    ),
    val isDescargandoPdf: Boolean = false
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

    fun setPeriodoCierre(periodo: String) {
        _uiState.update { it.copy(selectedPeriodoCierre = periodo) }
    }

    fun ejecutarCierreContable(hoja: String = _uiState.value.selectedPeriodoCierre) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEjecutandoCierre = true, cierreMensaje = null) }
            when (val result = repository.ejecutarCierreContable(hoja)) {
                is NetworkResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isEjecutandoCierre = false,
                            cierreContable = result.data,
                            cierreMensaje = "✅ Cierre contable completado exitosamente para $hoja"
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isEjecutandoCierre = false,
                            cierreMensaje = "❌ Error ${result.code}: ${result.message.ifEmpty { "Error ejecutando cierre" }}"
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isEjecutandoCierre = false,
                            cierreMensaje = "❌ Fallo de red: ${result.e.localizedMessage ?: "Error de conexión"}"
                        )
                    }
                }
            }
        }
    }

    fun abrirPdfConCache(
        context: Context,
        pdfUrlRelativa: String?,
        periodo: String = _uiState.value.selectedPeriodoCierre,
        onError: (String) -> Unit = {}
    ) {
        val fullUrl = obtenerUrlAbsolutaPdf(pdfUrlRelativa)
        if (fullUrl.isNullOrBlank()) {
            onError("URL del comprobante no disponible")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDescargandoPdf = true) }
            val localFileResult = withContext(Dispatchers.IO) {
                try {
                    val cacheDir = File(context.cacheDir, "comprobantes")
                    if (!cacheDir.exists()) {
                        cacheDir.mkdirs()
                    }
                    val sanitized = periodo.lowercase().replace(" ", "_").replace("/", "_")
                    val file = File(cacheDir, "cierre_contable_${sanitized}.pdf")

                    if (!file.exists() || file.length() == 0L) {
                        val tempFile = File(cacheDir, "cierre_contable_${sanitized}.tmp")
                        try {
                            URL(fullUrl).openStream().use { input ->
                                tempFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            if (!tempFile.renameTo(file)) {
                                tempFile.copyTo(file, overwrite = true)
                                tempFile.delete()
                            }
                        } catch (e: Exception) {
                            if (tempFile.exists()) {
                                tempFile.delete()
                            }
                            throw e
                        }
                    }
                    Result.success(file)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            _uiState.update { it.copy(isDescargandoPdf = false) }

            localFileResult.fold(
                onSuccess = { file ->
                    try {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        onError("No se encontró una aplicación para abrir archivos PDF")
                    } catch (e: Exception) {
                        onError("Error al abrir comprobante: ${e.localizedMessage ?: "Fallo desconocido"}")
                    }
                },
                onFailure = { error ->
                    onError("No se pudo obtener el comprobante: ${error.localizedMessage ?: "Error de descarga"}")
                }
            )
        }
    }

    fun obtenerUrlAbsolutaPdf(pdfUrlRelativa: String?): String? {
        if (pdfUrlRelativa.isNullOrBlank()) return null
        if (pdfUrlRelativa.startsWith("http://") || pdfUrlRelativa.startsWith("https://")) {
            return pdfUrlRelativa
        }
        val base = _uiState.value.baseUrl.trimEnd('/')
        if (base.isBlank()) return null
        val rel = pdfUrlRelativa.trimStart('/')
        return "$base/$rel"
    }

    fun limpiarMensajes() {
        _uiState.update {
            it.copy(
                error = null,
                ticketCreationSuccess = null,
                ticketActionMessage = null,
                configMessage = null,
                cierreMensaje = null
            )
        }
    }
}
