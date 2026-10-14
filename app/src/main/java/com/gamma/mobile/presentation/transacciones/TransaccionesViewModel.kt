package com.gamma.mobile.presentation.transacciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Movimiento
import com.gamma.mobile.domain.repository.MovimientoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FiltroTipo {
    TODOS, INGRESO, GASTO
}

sealed class TransaccionesUiState {
    object Loading : TransaccionesUiState()
    data class Success(
        val movimientosAgrupados: Map<String, List<Movimiento>>,
        val filtroActual: FiltroTipo,
        val isRefreshing: Boolean = false
    ) : TransaccionesUiState()
    data class Error(val message: String, val isOffline: Boolean = false) : TransaccionesUiState()
    object Empty : TransaccionesUiState()
}

@HiltViewModel
class TransaccionesViewModel @Inject constructor(
    private val repository: MovimientoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TransaccionesUiState>(TransaccionesUiState.Loading)
    val uiState: StateFlow<TransaccionesUiState> = _uiState.asStateFlow()

    private var todosLosMovimientos: List<Movimiento> = emptyList()
    private var filtroActual: FiltroTipo = FiltroTipo.TODOS

    init {
        loadMovimientos()
    }

    fun loadMovimientos(forceRefresh: Boolean = false) {
        val isRefreshing = forceRefresh && _uiState.value is TransaccionesUiState.Success
        if (!isRefreshing) {
            _uiState.value = TransaccionesUiState.Loading
        } else {
            // Emit success with isRefreshing = true
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
        
        if (_uiState.value is TransaccionesUiState.Success || _uiState.value is TransaccionesUiState.Empty) {
            emitFilteredAndGroupedState()
        }
    }

    private fun emitFilteredAndGroupedState() {
        val filtered = if (filtroActual == FiltroTipo.TODOS) {
            todosLosMovimientos
        } else {
            val tipoString = if (filtroActual == FiltroTipo.INGRESO) "Ingreso" else "Gasto"
            todosLosMovimientos.filter { it.tipo.equals(tipoString, ignoreCase = true) }
        }

        if (filtered.isEmpty()) {
            _uiState.value = TransaccionesUiState.Empty
        } else {
            // Agrupar por fecha, ordenado cronológicamente descendente (más recientes primero)
            val sortedList = filtered.sortedWith(
                compareByDescending<Movimiento> { parseFecha(it.fecha) }
                    .thenByDescending { it.id }
            )

            // Mantener el orden de inserción de las fechas ordenadas
            val agrupados = LinkedHashMap<String, MutableList<Movimiento>>()
            for (mov in sortedList) {
                agrupados.getOrPut(mov.fecha) { mutableListOf() }.add(mov)
            }
            
            _uiState.value = TransaccionesUiState.Success(
                movimientosAgrupados = agrupados,
                filtroActual = filtroActual,
                isRefreshing = false
            )
        }
    }

    private fun parseFecha(fechaStr: String): Long {
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

