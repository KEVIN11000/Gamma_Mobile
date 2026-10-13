package com.gamma.mobile.data.repository

import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.model.AsistenciaMarcaRequest
import com.gamma.mobile.domain.model.AsistenciaMarcaResponse
import com.gamma.mobile.domain.model.AsistenciaMateriaRequest
import com.gamma.mobile.domain.model.AsistenciaMateriaResponse
import com.gamma.mobile.domain.repository.AsistenciaRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Named

class AsistenciaRepositoryImpl @Inject constructor(
    private val api: GammaApi,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) : AsistenciaRepository {

    override suspend fun marcarAsistencia(modo: String): NetworkResult<AsistenciaMarcaResponse> {
        return safeApiCall(dispatcher) {
            api.marcarAsistencia(AsistenciaMarcaRequest(modo = modo))
        }
    }

    override suspend fun marcarMateria(materia: String): NetworkResult<AsistenciaMateriaResponse> {
        return safeApiCall(dispatcher) {
            api.marcarMateria(AsistenciaMateriaRequest(materia = materia))
        }
    }
}
