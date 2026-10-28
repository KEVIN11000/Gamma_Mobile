package com.gamma.mobile.data.repository

import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.model.AnaliticasHorasResponse
import com.gamma.mobile.domain.model.AsistenciaMarcaRequest
import com.gamma.mobile.domain.model.AsistenciaMarcaResponse
import com.gamma.mobile.domain.model.AsistenciaMateriaRequest
import com.gamma.mobile.domain.model.AsistenciaMateriaResponse
import com.gamma.mobile.domain.model.PeriodosResponse
import com.gamma.mobile.domain.repository.AsistenciaRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
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

    override suspend fun getMaterias(): NetworkResult<List<String>> {
        return safeApiCall(dispatcher) {
            api.getMaterias()
        }
    }

    override suspend fun getPeriodos(): NetworkResult<PeriodosResponse> {
        return safeApiCall(dispatcher) {
            api.getPeriodos()
        }
    }

    override suspend fun getAnaliticas(hoja: String?): NetworkResult<AnaliticasHorasResponse> {
        return safeApiCall(dispatcher) {
            api.getAnaliticas(hoja)
        }
    }

    override suspend fun descargarReporteExcel(
        hoja: String?,
        descuento: Double,
        saldoAnterior: Double,
        incluirIva: Boolean
    ): NetworkResult<ByteArray> {
        return withContext(dispatcher) {
            try {
                val response = api.descargarReporteExcel(hoja, descuento, saldoAnterior, incluirIva)
                if (response.isSuccessful) {
                    val bytes = response.body()?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        NetworkResult.Success(bytes)
                    } else {
                        NetworkResult.Error(response.code(), "El archivo Excel recibido está vacío")
                    }
                } else {
                    NetworkResult.Error(response.code(), response.message())
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }

    override suspend fun descargarReportePdfAsistencia(
        hoja: String?,
        descuento: Double,
        saldoAnterior: Double,
        incluirIva: Boolean
    ): NetworkResult<ByteArray> {
        return withContext(dispatcher) {
            try {
                val response = api.descargarReportePdfAsistencia(hoja, descuento, saldoAnterior, incluirIva)
                if (response.isSuccessful) {
                    val bytes = response.body()?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        NetworkResult.Success(bytes)
                    } else {
                        NetworkResult.Error(response.code(), "El archivo PDF recibido está vacío")
                    }
                } else {
                    NetworkResult.Error(response.code(), response.message())
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }
}
