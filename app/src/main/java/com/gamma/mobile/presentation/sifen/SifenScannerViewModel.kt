package com.gamma.mobile.presentation.sifen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Movimiento
import com.gamma.mobile.domain.model.SifenInvoiceData
import com.gamma.mobile.domain.repository.MovimientoRepository
import com.gamma.mobile.domain.usecase.ParseSifenQrUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SifenScannerUiState(
    val scannedInvoice: SifenInvoiceData? = null,
    val isScanning: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SifenScannerViewModel @Inject constructor(
    private val parseSifenQrUseCase: ParseSifenQrUseCase,
    private val movimientoRepository: MovimientoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SifenScannerUiState())
    val uiState: StateFlow<SifenScannerUiState> = _uiState.asStateFlow()

    private var lastScannedCode: String? = null

    fun onQrScanned(rawCode: String) {
        if (_uiState.value.scannedInvoice != null || _uiState.value.isSaving) return
        if (rawCode == lastScannedCode) return
        lastScannedCode = rawCode

        val result = parseSifenQrUseCase(rawCode)
        result.onSuccess { invoice ->
            _uiState.update {
                it.copy(
                    scannedInvoice = invoice,
                    isScanning = false,
                    errorMessage = null
                )
            }
        }.onFailure { error ->
            _uiState.update {
                it.copy(
                    errorMessage = error.message ?: "Código QR inválido o no reconocido por SIFEN"
                )
            }
        }
    }

    fun confirmInvoice(categoria: String = "Gastos Generales") {
        val invoice = _uiState.value.scannedInvoice ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val categoriaFinal = if (invoice.nroFactura.isNotBlank()) {
                "$categoria (Fac ${invoice.nroFactura})"
            } else {
                categoria
            }

            val proveedor = invoice.rucEmisor.ifBlank { "DNIT SIFEN" }
            val nroFac = invoice.nroFactura.ifBlank { "S/N" }
            val tasaIvaFinal = if (invoice.iva5 > 0 && invoice.iva10 == 0L) "5%" else if (invoice.iva10 > 0) "10%" else "Exento"

            val movimiento = Movimiento(
                id = UUID.randomUUID().toString(),
                fecha = invoice.fechaEmision,
                tipo = "Gasto",
                categoria = categoriaFinal,
                total = invoice.total,
                comprobante = "Virtual Legal",
                proveedorCliente = proveedor,
                nroFactura = nroFac,
                tasaIva = tasaIvaFinal,
                clientId = UUID.randomUUID().toString()
            )

            when (val result = movimientoRepository.addMovimiento(movimiento)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isSaved = true,
                            scannedInvoice = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.message
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = result.e.localizedMessage ?: "Error de red al guardar factura"
                        )
                    }
                }
            }
        }
    }

    fun dismissInvoice() {
        lastScannedCode = null
        _uiState.update {
            it.copy(
                scannedInvoice = null,
                isScanning = true,
                errorMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
