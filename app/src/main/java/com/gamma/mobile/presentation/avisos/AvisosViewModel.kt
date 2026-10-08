package com.gamma.mobile.presentation.avisos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.repository.AvisoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AvisosUiState {
    object Loading : AvisosUiState()
    data class Success(val avisos: List<Aviso>) : AvisosUiState()
    data class Error(val message: String, val isOffline: Boolean = false) : AvisosUiState()
    object Empty : AvisosUiState()
}

@HiltViewModel
class AvisosViewModel @Inject constructor(
    private val avisoRepository: AvisoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AvisosUiState>(AvisosUiState.Loading)
    val uiState: StateFlow<AvisosUiState> = _uiState.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _createError = MutableStateFlow<String?>(null)
    val createError: StateFlow<String?> = _createError.asStateFlow()

    init {
        loadAvisos()
    }

    fun loadAvisos(forceRefresh: Boolean = false) {
        _uiState.value = AvisosUiState.Loading
        viewModelScope.launch {
            when (val result = avisoRepository.getAvisos(forceRefresh)) {
                is NetworkResult.Success -> {
                    if (result.data.isEmpty()) {
                        _uiState.value = AvisosUiState.Empty
                    } else {
                        _uiState.value = AvisosUiState.Success(result.data.sortedBy { it.fechaEvento })
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.value = AvisosUiState.Error(result.message)
                }
                is NetworkResult.Exception -> {
                    _uiState.value = AvisosUiState.Error("Error de red", true)
                }
            }
        }
    }

    fun createAviso(aviso: Aviso) {
        _isCreating.value = true
        _createError.value = null
        viewModelScope.launch {
            when (val result = avisoRepository.createAviso(aviso)) {
                is NetworkResult.Success -> {
                    loadAvisos(forceRefresh = true)
                }
                is NetworkResult.Error -> {
                    _createError.value = result.message
                }
                is NetworkResult.Exception -> {
                    _createError.value = "Error de red al crear"
                }
            }
            _isCreating.value = false
        }
    }

    fun deleteAviso(id: String) {
        viewModelScope.launch {
            when (avisoRepository.deleteAviso(id)) {
                is NetworkResult.Success -> {
                    loadAvisos(forceRefresh = true)
                }
                else -> {
                    loadAvisos(forceRefresh = true)
                }
            }
        }
    }

    fun interpretarAvisoIa(texto: String) {
        _isCreating.value = true
        _createError.value = null
        viewModelScope.launch {
            when (val result = avisoRepository.interpretarAvisoIa(texto)) {
                is NetworkResult.Success -> {
                    val avisoPropuesto = result.data
                    when (val createResult = avisoRepository.createAviso(avisoPropuesto)) {
                        is NetworkResult.Success -> loadAvisos(forceRefresh = true)
                        is NetworkResult.Error -> _createError.value = createResult.message
                        is NetworkResult.Exception -> _createError.value = "Error de red"
                    }
                }
                is NetworkResult.Error -> {
                    _createError.value = result.message
                }
                is NetworkResult.Exception -> {
                    _createError.value = "Error de red al procesar IA"
                }
            }
            _isCreating.value = false
        }
    }

    fun dismissError() {
        _createError.value = null
    }
}
