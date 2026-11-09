package com.gamma.mobile.data.repository

import com.gamma.mobile.data.local.dao.ClimaDao
import com.gamma.mobile.data.local.entity.ClimaEntity
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.ClimaResponseDto
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.model.ClimaAlertaParaguas
import com.gamma.mobile.domain.model.ClimaInfo
import com.gamma.mobile.domain.repository.ClimaRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

class ClimaRepositoryImpl @Inject constructor(
    private val api: GammaApi,
    private val dao: ClimaDao,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) : ClimaRepository {

    override fun getClimaFlow(): Flow<ClimaInfo?> {
        return dao.getClimaFlow().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getClimaCached(): ClimaInfo? {
        return dao.getClima()?.toDomain()
    }

    override suspend fun refreshClima(): NetworkResult<ClimaInfo> {
        val result = safeApiCall(dispatcher) {
            api.getClima()
        }

        return when (result) {
            is NetworkResult.Success -> {
                val dto = result.data
                dao.insertClima(dto.toEntity())
                NetworkResult.Success(dto.toDomain())
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
            is NetworkResult.Exception -> NetworkResult.Exception(result.e)
        }
    }

    private fun ClimaEntity.toDomain(): ClimaInfo {
        return ClimaInfo(
            temperaturaActual = temperaturaActual,
            temperaturaMin = temperaturaMin,
            temperaturaMax = temperaturaMax,
            condicion = condicion,
            alertaParaguas = ClimaAlertaParaguas(
                activar = alertaParaguasActivar,
                probabilidadLluvia = probabilidadLluvia,
                motivo = motivoParaguas
            ),
            actualizadoAt = actualizadoAt
        )
    }

    private fun ClimaResponseDto.toDomain(): ClimaInfo {
        return ClimaInfo(
            temperaturaActual = temperaturaActual,
            temperaturaMin = temperaturaMin,
            temperaturaMax = temperaturaMax,
            condicion = condicion,
            alertaParaguas = ClimaAlertaParaguas(
                activar = alertaParaguas.activar,
                probabilidadLluvia = alertaParaguas.probabilidadLluvia,
                motivo = alertaParaguas.motivo
            ),
            actualizadoAt = actualizadoAt
        )
    }

    private fun ClimaResponseDto.toEntity(id: String = "current"): ClimaEntity {
        return ClimaEntity(
            id = id,
            temperaturaActual = temperaturaActual,
            temperaturaMin = temperaturaMin,
            temperaturaMax = temperaturaMax,
            condicion = condicion,
            alertaParaguasActivar = alertaParaguas.activar,
            probabilidadLluvia = alertaParaguas.probabilidadLluvia,
            motivoParaguas = alertaParaguas.motivo,
            actualizadoAt = actualizadoAt,
            cachedTimestamp = System.currentTimeMillis()
        )
    }
}
