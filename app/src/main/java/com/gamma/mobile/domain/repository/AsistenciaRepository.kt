package com.gamma.mobile.domain.repository

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.AsistenciaMarcaResponse
import com.gamma.mobile.domain.model.AsistenciaMateriaResponse

interface AsistenciaRepository {
    suspend fun marcarAsistencia(modo: String): NetworkResult<AsistenciaMarcaResponse>
    suspend fun marcarMateria(materia: String): NetworkResult<AsistenciaMateriaResponse>
}
