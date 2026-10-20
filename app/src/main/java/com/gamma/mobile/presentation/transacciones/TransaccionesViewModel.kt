package com.gamma.mobile.presentation.transacciones

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Movimiento
import com.gamma.mobile.domain.repository.MovimientoRepository
import com.gamma.mobile.utils.ComprobanteNotificationHelper
import com.gamma.mobile.utils.DiarioPdfReportModel
import com.gamma.mobile.utils.LocalDiarioPdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

enum class FiltroTipo {
    TODOS, INGRESO, GASTO
}

sealed class TransaccionesUiState {
    object Loading : TransaccionesUiState()
    data class Success(
        val movimientosAgrupados: Map<String, List<Movimiento>>,
        val movimientosFiltrados: List<Movimiento>,
        val filtroActual: FiltroTipo,
        val fechaDesde: String? = null,
        val fechaHasta: String? = null,
        val isTableView: Boolean = false,
        val isRefreshing: Boolean = false
    ) : TransaccionesUiState()
    data class Error(val message: String, val isOffline: Boolean = false) : TransaccionesUiState()
    data class Empty(
        val filtroActual: FiltroTipo = FiltroTipo.TODOS,
        val fechaDesde: String? = null,
        val fechaHasta: String? = null,
        val isTableView: Boolean = false
    ) : TransaccionesUiState()
}

@HiltViewModel
class TransaccionesViewModel @Inject constructor(
    private val repository: MovimientoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransaccionesUiState>(TransaccionesUiState.Loading)
    val uiState: StateFlow<TransaccionesUiState> = _uiState.asStateFlow()

    private val _isExportingPdf = MutableStateFlow(false)
    val isExportingPdf: StateFlow<Boolean> = _isExportingPdf.asStateFlow()

    private val _pdfExportMessage = MutableStateFlow<String?>(null)
    val pdfExportMessage: StateFlow<String?> = _pdfExportMessage.asStateFlow()

    private var todosLosMovimientos: List<Movimiento> = emptyList()
    private var filtroActual: FiltroTipo = FiltroTipo.TODOS
    private var fechaDesde: String? = null
    private var fechaHasta: String? = null
    private var isTableView: Boolean = false

    init {
        loadMovimientos()
    }

    fun loadMovimientos(forceRefresh: Boolean = false) {
        val isRefreshing = forceRefresh && _uiState.value is TransaccionesUiState.Success
        if (!isRefreshing) {
            _uiState.value = TransaccionesUiState.Loading
        } else {
            val currentState = _uiState.value as TransaccionesUiState.Success
            _uiState.value = currentState.copy(isRefreshing = true)
        }

        viewModelScope.launch {
            when (val result = repository.getMovimientos(forceRefresh)) {
                is NetworkResult.Success -> {
                    todosLosMovimientos = result.data
                    emitFilteredAndGroupedState()
                }
                is NetworkResult.Exception -> {
                    _uiState.value = TransaccionesUiState.Error("Fallo de conexión", isOffline = true)
                }
                is NetworkResult.Error -> {
                    _uiState.value = TransaccionesUiState.Error("Error al cargar los movimientos")
                }
            }
        }
    }

    fun setFiltro(filtro: FiltroTipo) {
        if (filtroActual == filtro) return
        filtroActual = filtro
        emitFilteredAndGroupedState()
    }

    fun setRangoFechas(desde: String?, hasta: String?) {
        fechaDesde = desde?.trim()?.takeIf { it.isNotEmpty() }
        fechaHasta = hasta?.trim()?.takeIf { it.isNotEmpty() }
        emitFilteredAndGroupedState()
    }

    fun limpiarRangoFechas() {
        if (fechaDesde == null && fechaHasta == null) return
        fechaDesde = null
        fechaHasta = null
        emitFilteredAndGroupedState()
    }

    fun toggleViewMode() {
        isTableView = !isTableView
        emitFilteredAndGroupedState()
    }

    fun getMovimientosFiltrados(): List<Movimiento> {
        val epochDesde = fechaDesde?.let { parseFecha(it) } ?: 0L
        val epochHasta = fechaHasta?.let { parseFecha(it) } ?: 0L

        return todosLosMovimientos.filter { mov ->
            // Filtro por tipo
            val cumpleTipo = when (filtroActual) {
                FiltroTipo.TODOS -> true
                FiltroTipo.INGRESO -> mov.tipo.equals("Ingreso", ignoreCase = true)
                FiltroTipo.GASTO -> mov.tipo.equals("Gasto", ignoreCase = true)
            }
            if (!cumpleTipo) return@filter false

            // Filtro por fecha
            val movEpoch = parseFecha(mov.fecha)
            val cumpleDesde = (epochDesde == 0L) || (movEpoch >= epochDesde)
            val cumpleHasta = (epochHasta == 0L) || (movEpoch <= epochHasta)

            cumpleDesde && cumpleHasta
        }
    }

    fun exportarPdf(
        context: Context,
        onPdfReady: (File) -> Unit = {}
    ) {
        val lista = getMovimientosFiltrados()
        if (lista.isEmpty()) {
            _pdfExportMessage.value = "No hay movimientos para exportar en el rango seleccionado."
            return
        }

        viewModelScope.launch {
            _isExportingPdf.value = true
            try {
                val reportModel = DiarioPdfReportModel.buildFrom(
                    movimientos = lista,
                    filtro = filtroActual,
                    fechaDesde = fechaDesde,
                    fechaHasta = fechaHasta
                )
                val pdfFile = withContext(Dispatchers.IO) {
                    LocalDiarioPdfGenerator.generarPdf(context, reportModel)
                }

                // Disparo de notificación push local con ComprobanteNotificationHelper
                val etiqueta = "${reportModel.rangoPeriodo} (${lista.size} movs)"
                ComprobanteNotificationHelper.mostrarNotificacionDescarga(context, pdfFile, etiqueta)

                _pdfExportMessage.value = "PDF generado y guardado en descargas locales."
                onPdfReady(pdfFile)
            } catch (e: Exception) {
                _pdfExportMessage.value = "Error al generar el PDF: ${e.message}"
            } finally {
                _isExportingPdf.value = false
            }
        }
    }

    fun dismissPdfExportMessage() {
        _pdfExportMessage.value = null
    }

    private fun emitFilteredAndGroupedState() {
        val filtered = getMovimientosFiltrados()

        if (filtered.isEmpty()) {
            _uiState.value = TransaccionesUiState.Empty(
                filtroActual = filtroActual,
                fechaDesde = fechaDesde,
                fechaHasta = fechaHasta,
                isTableView = isTableView
            )
        } else {
            // Agrupar por fecha, ordenado cronológicamente descendente
            val sortedList = filtered.sortedWith(
                compareByDescending<Movimiento> { parseFecha(it.fecha) }
                    .thenByDescending { it.id }
            )

            val agrupados = LinkedHashMap<String, MutableList<Movimiento>>()
            for (mov in sortedList) {
                agrupados.getOrPut(mov.fecha) { mutableListOf() }.add(mov)
            }

            _uiState.value = TransaccionesUiState.Success(
                movimientosAgrupados = agrupados,
                movimientosFiltrados = sortedList,
                filtroActual = filtroActual,
                fechaDesde = fechaDesde,
                fechaHasta = fechaHasta,
                isTableView = isTableView,
                isRefreshing = false
            )
        }
    }

    fun parseFecha(fechaStr: String): Long {
        if (fechaStr.isBlank()) return 0L
        val clean = fechaStr.trim()
        val currentYear = java.time.LocalDate.now().year

        val patterns = listOf(
            "dd/MM/yyyy",
            "d/M/yyyy",
            "yyyy-MM-dd",
            "dd-MM-yyyy",
            "dd/MM",
            "d/M"
        )
        for (pattern in patterns) {
            try {
                if (pattern == "dd/MM" || pattern == "d/M") {
                    val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern, java.util.Locale.getDefault())
                    val monthDay = java.time.MonthDay.parse(clean, formatter)
                    return monthDay.atYear(currentYear).toEpochDay()
                } else {
                    val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern, java.util.Locale.getDefault())
                    val localDate = java.time.LocalDate.parse(clean, formatter)
                    return localDate.toEpochDay()
                }
            } catch (_: Exception) {}
        }
        return 0L
    }
}
