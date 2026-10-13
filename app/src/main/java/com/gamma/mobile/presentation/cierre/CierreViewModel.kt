package com.gamma.mobile.presentation.cierre

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.CierreResponse
import com.gamma.mobile.domain.repository.CierreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CierreUiState {
    data object Idle : CierreUiState
    data object LoadingCierre : CierreUiState
    data class CierreSuccess(val response: CierreResponse) : CierreUiState
    data object DownloadingPdf : CierreUiState
    data class PdfDownloaded(val fileBytes: ByteArray, val fileName: String) : CierreUiState {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as PdfDownloaded
            return fileBytes.contentEquals(other.fileBytes) && fileName == other.fileName
        }

        override fun hashCode(): Int {
            var result = fileBytes.contentHashCode()
            result = 31 * result + fileName.hashCode()
            return result
        }
    }
    data class Error(val message: String) : CierreUiState
}

@HiltViewModel
class CierreViewModel @Inject constructor(
    private val repository: CierreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CierreUiState>(CierreUiState.Idle)
    val uiState: StateFlow<CierreUiState> = _uiState.asStateFlow()

    private val _hoja = MutableStateFlow("Octubre 2026")
    val hoja: StateFlow<String> = _hoja.asStateFlow()

    val periodosDisponibles: List<String> = listOf(
        "Enero 2026",
        "Febrero 2026",
        "Marzo 2026",
        "Abril 2026",
        "Mayo 2026",
        "Junio 2026",
        "Julio 2026",
        "Agosto 2026",
        "Setiembre 2026",
        "Octubre 2026",
        "Noviembre 2026",
        "Diciembre 2026"
    )

    private val _aplicarDescuento = MutableStateFlow(false)
    val aplicarDescuento: StateFlow<Boolean> = _aplicarDescuento.asStateFlow()

    fun setHoja(nuevaHoja: String) {
        _hoja.value = nuevaHoja
    }

    fun setAplicarDescuento(aplicar: Boolean) {
        _aplicarDescuento.value = aplicar
    }

    fun ejecutarCierre(hojaCierre: String = _hoja.value, conDescuento: Boolean = _aplicarDescuento.value) {
        val hojaValida = hojaCierre.trim()
        if (hojaValida.isEmpty()) {
            _uiState.value = CierreUiState.Error("Debe indicar la hoja o período para el cierre")
            return
        }

        viewModelScope.launch {
            _uiState.value = CierreUiState.LoadingCierre
            when (val result = repository.ejecutarCierre(hojaValida, conDescuento)) {
                is NetworkResult.Success -> {
                    _uiState.value = CierreUiState.CierreSuccess(result.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = CierreUiState.Error(result.message.ifEmpty { "Error ${result.code} al ejecutar cierre" })
                }
                is NetworkResult.Exception -> {
                    _uiState.value = CierreUiState.Error(result.e.localizedMessage ?: "Error de conexión al ejecutar cierre")
                }
            }
        }
    }

    fun descargarPdf(url: String, fileName: String = "Reporte_Cierre.pdf") {
        if (url.isBlank()) {
            _uiState.value = CierreUiState.Error("URL del archivo PDF inválida o no disponible")
            return
        }

        viewModelScope.launch {
            _uiState.value = CierreUiState.DownloadingPdf
            when (val result = repository.descargarPdf(url)) {
                is NetworkResult.Success -> {
                    _uiState.value = CierreUiState.PdfDownloaded(result.data, fileName)
                }
                is NetworkResult.Error -> {
                    _uiState.value = CierreUiState.Error(result.message.ifEmpty { "Error al descargar PDF (${result.code})" })
                }
                is NetworkResult.Exception -> {
                    _uiState.value = CierreUiState.Error(result.e.localizedMessage ?: "Error de red al descargar PDF")
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = CierreUiState.Idle
    }
}
