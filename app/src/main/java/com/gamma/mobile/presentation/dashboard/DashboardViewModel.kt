package com.gamma.mobile.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.ClimaInfo
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.model.FlujoCaja
import com.gamma.mobile.domain.repository.AsistenciaRepository
import com.gamma.mobile.domain.repository.AvisoRepository
import com.gamma.mobile.domain.repository.BootstrapRepository
import com.gamma.mobile.domain.repository.ClimaRepository
import com.gamma.mobile.domain.repository.DashboardRepository
import com.gamma.mobile.domain.repository.FlujoCajaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val summary: DashboardSummary,
        val avisos: List<AvisoUiModel>,
        val flujoCaja: FlujoCaja? = null,
        val clima: ClimaInfo? = null,
        val periodoActual: String? = null
    ) : DashboardUiState()
    data class Error(val message: String, val isOffline: Boolean = false) : DashboardUiState()
    object Empty : DashboardUiState()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val avisoRepository: AvisoRepository,
    private val clock: Clock,
    private val asistenciaRepository: AsistenciaRepository? = null,
    private val bootstrapRepository: BootstrapRepository? = null,
    private val flujoCajaRepository: FlujoCajaRepository? = null,
    private val climaRepository: ClimaRepository? = null
) : ViewModel() {

    // Constructores secundarios para compatibilidad con tests unitarios existentes
    constructor(
        dashboardRepository: DashboardRepository,
        avisoRepository: AvisoRepository,
        clock: Clock
    ) : this(dashboardRepository, avisoRepository, clock, null, null, null, null)

    constructor(
        dashboardRepository: DashboardRepository,
        avisoRepository: AvisoRepository,
        clock: Clock,
        asistenciaRepository: AsistenciaRepository?
    ) : this(dashboardRepository, avisoRepository, clock, asistenciaRepository, null, null, null)

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
        observeAuxiliaryData()
    }

    private fun observeAuxiliaryData() {
        if (flujoCajaRepository != null) {
            viewModelScope.launch {
                flujoCajaRepository.getFlujoCajaFlow().collect { flujo ->
                    if (flujo != null) {
                        val current = _uiState.value
                        if (current is DashboardUiState.Success) {
                            _uiState.value = current.copy(flujoCaja = flujo)
                        }
                    }
                }
            }
        }
        if (climaRepository != null) {
            viewModelScope.launch {
                climaRepository.getClimaFlow().collect { clima ->
                    if (clima != null) {
                        val current = _uiState.value
                        if (current is DashboardUiState.Success) {
                            _uiState.value = current.copy(clima = clima)
                        }
                    }
                }
            }
        }
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
            if (bootstrapRepository != null) {
                cargarConBootstrap(forceRefresh, silent)
            } else {
                cargarLegacy(forceRefresh, silent)
            }
            initialLoadDone = true
        }
    }

    private suspend fun cargarConBootstrap(forceRefresh: Boolean, silent: Boolean) {
        val bootstrapResult = bootstrapRepository?.syncBootstrap()

        if (bootstrapResult is NetworkResult.Success) {
            val dto = bootstrapResult.data
            val summary = dto.summary ?: DashboardSummary()
            val avisosUi = dto.proximosAvisos.filterAndSortForDashboard(clock)
            val flujoCaja = flujoCajaRepository?.getFlujoCajaCached()
            val clima = climaRepository?.getClimaCached()

            if (summary.saldoAcumulado == 0L &&
                summary.margenLibreDisponible == 0L &&
                avisosUi.isEmpty() &&
                flujoCaja == null &&
                clima == null
            ) {
                _uiState.value = DashboardUiState.Empty
            } else {
                _uiState.value = DashboardUiState.Success(
                    summary = summary,
                    avisos = avisosUi,
                    flujoCaja = flujoCaja,
                    clima = clima,
                    periodoActual = dto.periodoActual
                )
            }

            if (forceRefresh) {
                flujoCajaRepository?.refreshFlujoCaja()
                climaRepository?.refreshClima()
            }
        } else {
            // Fallback offline-first resiliente: servir desde cache local de Room / SharedPreferences
            val summaryResult = dashboardRepository.getSummary(forceRefresh = false)
            val avisosResult = avisoRepository.getAvisos(forceRefresh = false)
            val flujoCaja = flujoCajaRepository?.getFlujoCajaCached()
            val clima = climaRepository?.getClimaCached()

            if (summaryResult is NetworkResult.Success && avisosResult is NetworkResult.Success) {
                val avisosUi = avisosResult.data.filterAndSortForDashboard(clock)
                if (summaryResult.data.saldoAcumulado == 0L &&
                    summaryResult.data.margenLibreDisponible == 0L &&
                    avisosUi.isEmpty() &&
                    flujoCaja == null &&
                    clima == null
                ) {
                    _uiState.value = DashboardUiState.Empty
                } else {
                    _uiState.value = DashboardUiState.Success(
                        summary = summaryResult.data,
                        avisos = avisosUi,
                        flujoCaja = flujoCaja,
                        clima = clima
                    )
                }
            } else if (!silent) {
                val isOffline = bootstrapResult is NetworkResult.Exception ||
                                summaryResult is NetworkResult.Exception ||
                                avisosResult is NetworkResult.Exception
                _uiState.value = DashboardUiState.Error("No se pudo cargar el dashboard", isOffline)
            }
        }
    }

    private suspend fun cargarLegacy(forceRefresh: Boolean, silent: Boolean) {
        val summaryResult = dashboardRepository.getSummary(forceRefresh)
        val avisosResult = avisoRepository.getAvisos(forceRefresh)

        if (summaryResult is NetworkResult.Success && avisosResult is NetworkResult.Success) {
            val avisosUi = avisosResult.data.filterAndSortForDashboard(clock)
            if (summaryResult.data.saldoAcumulado == 0L &&
                summaryResult.data.margenLibreDisponible == 0L &&
                avisosUi.isEmpty()
            ) {
                _uiState.value = DashboardUiState.Empty
            } else {
                _uiState.value = DashboardUiState.Success(summaryResult.data, avisosUi)
            }
        } else if (!silent) {
            val isOffline = summaryResult is NetworkResult.Exception || avisosResult is NetworkResult.Exception
            _uiState.value = DashboardUiState.Error("No se pudo cargar el dashboard", isOffline)
        }
    }

    /**
     * Se invoca cada vez que el Dashboard vuelve a ser visible (p. ej. tras guardar una transacción).
     * Este ViewModel vive tanto como la Activity, por lo que sin esto los saldos quedarían congelados
     * con los datos de la primera carga.
     */
    fun onScreenVisible() {
        if (!initialLoadDone) return
        loadDashboard(forceRefresh = false, silent = true)
    }

    fun setModoMarcacion(modo: String) {
        if (modo == MODO_NORMAL || modo == MODO_DIRECTO) {
            _modoMarcacion.value = modo
        }
    }

    fun marcarAsistencia(modo: String = _modoMarcacion.value, context: android.content.Context? = null) {
        if (asistenciaRepository == null) return
        if (_isMarkingAsistencia.value) return
        viewModelScope.launch {
            _isMarkingAsistencia.value = true
            val ahora = java.time.LocalTime.now(clock)
            val horaEnvio = String.format(java.util.Locale.US, "%02d:%02d", ahora.hour, ahora.minute)
            _asistenciaMessage.value = "⏳ Marcación enviada al servidor a las $horaEnvio..."
            _asistenciaIsError.value = false

            when (val res = asistenciaRepository.marcarAsistencia(modo)) {
                is NetworkResult.Success -> {
                    val resultado = evaluarMarcacion(res.data)
                    _asistenciaMessage.value = resultado.mensaje
                    _asistenciaIsError.value = resultado.esError
                    if (!resultado.esError && resultado.registradoEnPlanilla && context != null) {
                        com.gamma.mobile.utils.MarcacionNotificationHelper.mostrarNotificacionMarcacion(
                            context = context,
                            hora = resultado.hora,
                            tipo = resultado.tipo,
                            planilla = resultado.planilla
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _asistenciaMessage.value = "❌ ${res.message.ifEmpty { "Error al registrar en planilla" }}"
                    _asistenciaIsError.value = true
                }
                is NetworkResult.Exception -> {
                    _asistenciaMessage.value = "❌ Error de conexión al asentar marcación"
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
