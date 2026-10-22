package com.gamma.mobile.data.repository

import com.gamma.mobile.data.local.dao.CierreDao
import com.gamma.mobile.data.local.dao.SyncStateDao
import com.gamma.mobile.data.local.entity.SyncStateEntity
import com.gamma.mobile.data.local.entity.toDomain
import com.gamma.mobile.data.local.entity.toEntity
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.model.CierreHistorialItem
import com.gamma.mobile.domain.model.CierreRequest
import com.gamma.mobile.domain.model.CierreResponse
import com.gamma.mobile.domain.repository.CierreRepository
import com.gamma.mobile.domain.repository.CierresRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class CierreRepositoryImpl @Inject constructor(
    private val api: GammaApi,
    private val cierreDao: CierreDao,
    private val syncStateDao: SyncStateDao,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) : CierreRepository, CierresRepository {

    override suspend fun ejecutarCierre(hoja: String, aplicarDescuento: Boolean, tipo: String): NetworkResult<CierreResponse> {
        return safeApiCall(dispatcher) {
            api.ejecutarCierre(CierreRequest(hoja = hoja, tipo = tipo, aplicarDescuento = aplicarDescuento))
        }
    }

    override suspend fun getCierres(forceRefresh: Boolean): NetworkResult<List<CierreHistorialItem>> {
        val syncState = syncStateDao.getSyncState(SYNC_KEY)
        val isCacheValid = syncState != null && (System.currentTimeMillis() - syncState.lastSyncTimestamp) < CACHE_TTL

        if (!forceRefresh && isCacheValid) {
            val localData = cierreDao.getAll().map { it.toDomain() }
            if (localData.isNotEmpty()) {
                return NetworkResult.Success(localData)
            }
        }

        return when (val response = safeApiCall(dispatcher) { api.getCierres() }) {
            is NetworkResult.Success -> {
                cierreDao.deleteAll()
                cierreDao.insertAll(response.data.map { it.toEntity() })
                syncStateDao.insert(SyncStateEntity(SYNC_KEY, System.currentTimeMillis()))
                NetworkResult.Success(response.data)
            }
            else -> {
                val localData = cierreDao.getAll().map { it.toDomain() }
                if (localData.isNotEmpty()) NetworkResult.Success(localData) else response
            }
        }
    }

    override suspend fun descargarReporteCierre(filename: String): NetworkResult<ByteArray> {
        return withContext(dispatcher) {
            try {
                val response = api.descargarReporteCierre(filename)
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

    override suspend fun descargarPdf(url: String): NetworkResult<ByteArray> {
        return withContext(dispatcher) {
            try {
                val response = api.descargarArchivoPdf(url)
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

    companion object {
        private const val SYNC_KEY = "cierres"
        private const val CACHE_TTL = 30 * 60 * 1000L // 30 minutes
    }
}
