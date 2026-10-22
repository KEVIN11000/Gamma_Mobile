package com.gamma.mobile.presentation.cierre

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.CierreHistorialItem
import com.gamma.mobile.domain.repository.CierresRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HistorialCierresUiState {
    object Loading : HistorialCierresUiState()
    data class Success(val cierres: List<CierreHistorialItem>) : HistorialCierresUiState()
    data class Error(val message: String) : HistorialCierresUiState()
}

enum class FiltroTipoCierre {
    TODOS, CONTABLE, LABORAL
}

sealed class DownloadPdfState {
    data class Downloading(val id: String) : DownloadPdfState()
    data class Downloaded(val id: String, val fileName: String, val fileBytes: ByteArray) : DownloadPdfState()
    data class Error(val message: String) : DownloadPdfState()
}

@HiltViewModel
class CierresViewModel @Inject constructor(
    private val repository: CierresRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HistorialCierresUiState>(HistorialCierresUiState.Loading)
    val uiState: StateFlow<HistorialCierresUiState> = _uiState.asStateFlow()

    private val _filtroTipo = MutableStateFlow(FiltroTipoCierre.TODOS)
    val filtroTipo: StateFlow<FiltroTipoCierre> = _filtroTipo.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadPdfState?>(null)
    val downloadState: StateFlow<DownloadPdfState?> = _downloadState.asStateFlow()

    private var todosLosCierres: List<CierreHistorialItem> = emptyList()

    init {
        loadCierres()
    }

    fun loadCierres(forceRefresh: Boolean = false) {
        if (!forceRefresh) {
            _uiState.value = HistorialCierresUiState.Loading
        }
        viewModelScope.launch {
            when (val result = repository.getCierres(forceRefresh)) {
                is NetworkResult.Success -> {
                    todosLosCierres = result.data
                    emitFilteredState()
                }
                is NetworkResult.Error -> {
                    _uiState.value = HistorialCierresUiState.Error("Error: ${result.message}")
                }
                is NetworkResult.Exception -> {
                    _uiState.value = HistorialCierresUiState.Error("Error de conexión al cargar cierres")
                }
            }
        }
    }

    fun setFiltroTipo(filtro: FiltroTipoCierre) {
        if (_filtroTipo.value == filtro) return
        _filtroTipo.value = filtro
        emitFilteredState()
    }

    private fun emitFilteredState() {
        val filtered = when (_filtroTipo.value) {
            FiltroTipoCierre.TODOS -> todosLosCierres
            FiltroTipoCierre.CONTABLE -> todosLosCierres.filter { it.tipo.equals("contable", ignoreCase = true) }
            FiltroTipoCierre.LABORAL -> todosLosCierres.filter { it.tipo.equals("laboral", ignoreCase = true) }
        }
        _uiState.value = HistorialCierresUiState.Success(filtered)
    }

    fun descargarComprobante(cierre: CierreHistorialItem) {
        if (!cierre.pdfDisponible) {
            _downloadState.value = DownloadPdfState.Error("Comprobante PDF no disponible para este período")
            return
        }

        val url = cierre.pdfUrl.orEmpty().trim()
        val safePeriodo = cierre.periodo.replace("/", "_").replace(" ", "_")
        val fileName = "cierre_${cierre.tipo}_${safePeriodo}.pdf"

        _downloadState.value = DownloadPdfState.Downloading(cierre.id)

        viewModelScope.launch {
            val result = if (url.startsWith("http://") || url.startsWith("https://")) {
                if (repository is com.gamma.mobile.domain.repository.CierreRepository) {
                    repository.descargarPdf(url)
                } else {
                    repository.descargarReporteCierre(url.substringAfterLast("/"))
                }
            } else {
                repository.descargarReporteCierre(if (url.isNotBlank()) url else fileName)
            }

            when (result) {
                is NetworkResult.Success -> {
                    _downloadState.value = DownloadPdfState.Downloaded(
                        id = cierre.id,
                        fileName = fileName,
                        fileBytes = result.data
                    )
                }
                is NetworkResult.Error -> {
                    _downloadState.value = DownloadPdfState.Error(result.message ?: "Error al descargar el PDF")
                }
                is NetworkResult.Exception -> {
                    _downloadState.value = DownloadPdfState.Error(result.e.message ?: "Fallo de conexión al descargar PDF")
                }
            }
        }
    }

    fun clearDownloadState() {
        _downloadState.value = null
    }
}
