package com.gamma.mobile.presentation.asistencia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.repository.AsistenciaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AsistenciaUiState {
    data object Idle : AsistenciaUiState
    data object Loading : AsistenciaUiState
    data class Success(val mensaje: String, val hora: String, val detalle: String) : AsistenciaUiState
    data class Error(val message: String) : AsistenciaUiState
}

@HiltViewModel
class AsistenciaViewModel @Inject constructor(
    private val repository: AsistenciaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AsistenciaUiState>(AsistenciaUiState.Idle)
    val uiState: StateFlow<AsistenciaUiState> = _uiState.asStateFlow()

    private val _modo = MutableStateFlow("normal") // "normal" o "directo"
    val modo: StateFlow<String> = _modo.asStateFlow()

    private val _materia = MutableStateFlow("")
    val materia: StateFlow<String> = _materia.asStateFlow()

    val materiasDisponibles: List<String> = listOf(
        "Algoritmos y Estructuras de Datos",
        "Base de Datos",
        "Inteligencia Artificial",
        "Ingeniería de Software",
        "Sistemas Operativos",
        "Redes de Computadoras",
        "Programación Web",
        "Matemática Discreta",
        "Física",
        "Cálculo"
    )

    fun setModo(nuevoModo: String) {
        _modo.value = nuevoModo
    }

    fun setMateria(nuevaMateria: String) {
        _materia.value = nuevaMateria
    }

    fun marcarAsistencia(modoParaMarcar: String = _modo.value) {
        viewModelScope.launch {
            _uiState.value = AsistenciaUiState.Loading
            when (val result = repository.marcarAsistencia(modoParaMarcar)) {
                is NetworkResult.Success -> {
                    val data = result.data
                    val tipoTexto = if (data.tipo.isNotEmpty()) data.tipo.replaceFirstChar { it.uppercase() } else "Marcación"
                    val horaTexto = if (data.hora.isNotEmpty()) " a las ${data.hora}" else ""
                    val detalle = "$tipoTexto registrado$horaTexto (Modo $modoParaMarcar)"
                    _uiState.value = AsistenciaUiState.Success(
                        mensaje = data.mensaje.ifEmpty { "Marcación registrada con éxito" },
                        hora = data.hora,
                        detalle = detalle
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = AsistenciaUiState.Error(result.message.ifEmpty { "Error ${result.code} al marcar asistencia" })
                }
                is NetworkResult.Exception -> {
                    _uiState.value = AsistenciaUiState.Error(result.e.localizedMessage ?: "Error de red al marcar asistencia")
                }
            }
        }
    }

    fun marcarMateria(materiaParaMarcar: String = _materia.value) {
        val mat = materiaParaMarcar.trim()
        if (mat.isEmpty()) {
            _uiState.value = AsistenciaUiState.Error("Debe ingresar el nombre de la materia")
            return
        }
        viewModelScope.launch {
            _uiState.value = AsistenciaUiState.Loading
            when (val result = repository.marcarMateria(mat)) {
                is NetworkResult.Success -> {
                    val data = result.data
                    val horaTexto = if (data.hora.isNotEmpty()) " a las ${data.hora}" else ""
                    val detalle = "Materia '$mat' registrada$horaTexto"
                    _uiState.value = AsistenciaUiState.Success(
                        mensaje = data.mensaje.ifEmpty { "Asistencia a materia registrada" },
                        hora = data.hora,
                        detalle = detalle
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = AsistenciaUiState.Error(result.message.ifEmpty { "Error ${result.code} al registrar materia" })
                }
                is NetworkResult.Exception -> {
                    _uiState.value = AsistenciaUiState.Error(result.e.localizedMessage ?: "Error de red al registrar materia")
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = AsistenciaUiState.Idle
    }
}
