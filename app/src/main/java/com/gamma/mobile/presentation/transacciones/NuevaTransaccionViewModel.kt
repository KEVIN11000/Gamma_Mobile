package com.gamma.mobile.presentation.transacciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Movimiento
import com.gamma.mobile.domain.repository.MovimientoRepository
import com.gamma.mobile.domain.usecase.ConsultarRucSifenUseCase
import com.gamma.mobile.domain.usecase.ParseMovimientoNlpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class IvaPreview(
    val montoGravado: Long = 0L,
    val montoIva: Long = 0L
)

data class NuevaTransaccionUiState(
    val tipo: String = "Gasto",
    val categoria: String = "",
    val totalInput: String = "",
    val comprobante: String = "Física Legal",
    val tasaIva: String = "10%",
    val proveedorCliente: String = "Varios",
    val nroFactura: String = "S/N",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val totalParsed: Long
        get() = totalInput.toLongOrNull() ?: 0L
        
    val isValid: Boolean
        get() = totalParsed > 0L && categoria.isNotBlank()
        
    val ivaPreview: IvaPreview
        get() {
            val total = totalParsed
            return when (tasaIva) {
                "10%" -> {
                    val iva = total / 11
                    IvaPreview(montoGravado = total - iva, montoIva = iva)
                }
                "5%" -> {
                    val iva = total / 21
                    IvaPreview(montoGravado = total - iva, montoIva = iva)
                }
                else -> { // Exento
                    IvaPreview(montoGravado = total, montoIva = 0L)
                }
            }
        }
}

@HiltViewModel
class NuevaTransaccionViewModel @Inject constructor(
    private val repository: MovimientoRepository,
    private val clock: Clock,
    private val parseMovimientoNlpUseCase: ParseMovimientoNlpUseCase? = null,
    private val consultarRucSifenUseCase: ConsultarRucSifenUseCase? = null
) : ViewModel() {

    // Constructor secundario para compatibilidad
    constructor(
        repository: MovimientoRepository,
        clock: Clock
    ) : this(repository, clock, null, null)

    private val _uiState = MutableStateFlow(NuevaTransaccionUiState())
    val uiState: StateFlow<NuevaTransaccionUiState> = _uiState.asStateFlow()

    fun updateTipo(tipo: String) {
        _uiState.update { it.copy(tipo = tipo) }
    }

    fun updateCategoria(categoria: String) {
        _uiState.update { it.copy(categoria = categoria) }
    }

    fun updateTotal(total: String) {
        val digits = total.replace(".", "").replace(",", "").replace(" ", "").trim()
        if (digits.all { it.isDigit() }) {
            _uiState.update { it.copy(totalInput = digits) }
        }
    }

    fun updateComprobante(comprobante: String) {
        _uiState.update { it.copy(comprobante = comprobante) }
    }

    fun updateTasaIva(tasaIva: String) {
        _uiState.update { it.copy(tasaIva = tasaIva) }
    }

    fun updateProveedorCliente(proveedor: String) {
        _uiState.update { it.copy(proveedorCliente = proveedor) }
    }

    fun updateNroFactura(nroFactura: String) {
        _uiState.update { it.copy(nroFactura = nroFactura) }
    }
    
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetState() {
        _uiState.value = NuevaTransaccionUiState()
    }

    fun interpretarTextoIa(texto: String, onSuccess: () -> Unit = {}) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.interpretarMovimientoIa(texto)) {
                is NetworkResult.Success -> {
                    val propuesta = result.data
                    _uiState.update { 
                        it.copy(
                            tipo = if (propuesta.tipo.isNotBlank()) propuesta.tipo else it.tipo,
                            categoria = if (propuesta.categoria.isNotBlank()) propuesta.categoria else it.categoria,
                            totalInput = if (propuesta.total > 0L) propuesta.total.toString() else it.totalInput,
                            proveedorCliente = if (propuesta.proveedorCliente.isNotBlank()) propuesta.proveedorCliente else it.proveedorCliente,
                            isLoading = false
                        ) 
                    }
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is NetworkResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.e.message ?: "Error de red al interpretar con IA") }
                }
            }
        }
    }

    /**
     * Parser rápido determinista NLP (< 50 ms).
     */
    fun interpretarTextoNlp(texto: String, onSuccess: () -> Unit = {}) {
        val useCase = parseMovimientoNlpUseCase
        if (useCase == null) {
            interpretarTextoIa(texto, onSuccess)
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = useCase(texto)) {
                is NetworkResult.Success -> {
                    val parsed = result.data
                    _uiState.update {
                        it.copy(
                            tipo = if (parsed.tipoMovimiento.isNotBlank()) parsed.tipoMovimiento else it.tipo,
                            categoria = if (parsed.categoria.isNotBlank()) parsed.categoria else it.categoria,
                            totalInput = if (parsed.monto > 0L) parsed.monto.toString() else it.totalInput,
                            proveedorCliente = if (parsed.proveedorCliente.isNotBlank()) parsed.proveedorCliente else it.proveedorCliente,
                            isLoading = false
                        )
                    }
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is NetworkResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.e.message ?: "Error al interpretar con NLP") }
                }
            }
        }
    }

    /**
     * Consulta y autocompleta la Razón Social a partir de un RUC leído de comprobante electrónico / QR.
     */
    fun resolverRucSifen(ruc: String) {
        val useCase = consultarRucSifenUseCase ?: return
        if (ruc.isBlank()) return
        viewModelScope.launch {
            when (val result = useCase(ruc)) {
                is NetworkResult.Success -> {
                    val info = result.data
                    if (info.razonSocial.isNotBlank()) {
                        _uiState.update { it.copy(proveedorCliente = info.razonSocial) }
                    }
                }
                else -> { /* Preservar RUC existente si la consulta remota falla */ }
            }
        }
    }

    fun interpretarImagenOcr(imageBytes: ByteArray, fileName: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = repository.interpretarMovimientoOcr(imageBytes, fileName)) {
                is NetworkResult.Success -> {
                    val propuesta = result.data
                    _uiState.update { 
                        it.copy(
                            tipo = if (propuesta.tipo.isNotBlank()) propuesta.tipo else it.tipo,
                            categoria = if (propuesta.categoria.isNotBlank()) propuesta.categoria else it.categoria,
                            totalInput = if (propuesta.total > 0L) propuesta.total.toString() else it.totalInput,
                            proveedorCliente = if (propuesta.proveedorCliente.isNotBlank()) propuesta.proveedorCliente else it.proveedorCliente,
                            isLoading = false
                        ) 
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                is NetworkResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.e.message ?: "Error de red al procesar imagen") }
                }
            }
        }
    }

    fun saveMovimiento() {
        val state = _uiState.value
        if (!state.isValid) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val today = LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            
            val movimiento = Movimiento(
                id = UUID.randomUUID().toString(),
                fecha = today,
                tipo = state.tipo,
                categoria = state.categoria,
                total = state.totalParsed,
                comprobante = state.comprobante,
                proveedorCliente = state.proveedorCliente,
                nroFactura = state.nroFactura,
                tasaIva = state.tasaIva,
                clientId = UUID.randomUUID().toString()
            )

            when (repository.addMovimiento(movimiento)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is NetworkResult.Exception, is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Error al guardar la transacción") }
                }
            }
        }
    }
}
