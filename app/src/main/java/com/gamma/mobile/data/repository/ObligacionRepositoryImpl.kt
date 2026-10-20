package com.gamma.mobile.data.repository

import com.gamma.mobile.data.local.dao.ObligacionDao
import com.gamma.mobile.data.local.dao.SyncStateDao
import com.gamma.mobile.data.local.entity.SyncStateEntity
import com.gamma.mobile.data.local.entity.toDomain
import com.gamma.mobile.data.local.entity.toEntity
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.model.Obligacion
import com.gamma.mobile.domain.repository.ObligacionRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Named

class ObligacionRepositoryImpl @Inject constructor(
    private val api: GammaApi,
    private val obligacionDao: ObligacionDao,
    private val syncStateDao: SyncStateDao,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) : ObligacionRepository {

    override suspend fun getObligaciones(forceRefresh: Boolean): NetworkResult<List<Obligacion>> {
        val syncState = syncStateDao.getSyncState(SYNC_KEY)
        val isCacheValid = syncState != null && (System.currentTimeMillis() - syncState.lastSyncTimestamp) < CACHE_TTL

        if (!forceRefresh && isCacheValid) {
            val localData = obligacionDao.getAll().map { it.toDomain() }
            if (localData.isNotEmpty()) {
                return NetworkResult.Success(localData)
            }
        }

        return when (val response = safeApiCall(dispatcher) { api.getObligaciones() }) {
            is NetworkResult.Success -> {
                obligacionDao.deleteAll()
                obligacionDao.insertAll(response.data.map { it.toEntity() })
                syncStateDao.insert(SyncStateEntity(SYNC_KEY, System.currentTimeMillis()))
                NetworkResult.Success(response.data)
            }
            else -> {
                val localData = obligacionDao.getAll().map { it.toDomain() }
                if (localData.isNotEmpty()) NetworkResult.Success(localData) else response
            }
        }
    }

    override suspend fun createObligacion(obligacion: Obligacion): NetworkResult<Unit> {
        val result = safeApiCall(dispatcher) { api.createObligacion(obligacion) }
        if (result is NetworkResult.Success) {
            obligacionDao.insertAll(listOf(obligacion.toEntity()))
        }
        return result
    }

    override suspend fun updateObligacion(obligacion: Obligacion): NetworkResult<Unit> {
        // Persistencia local siempre: el backend aún no expone PUT /obligaciones/{id} (ver requerimientos_gamma_bot.md).
        obligacionDao.insertAll(listOf(obligacion.toEntity()))
        return safeApiCall(dispatcher) { api.updateObligacion(obligacion.id, obligacion) }
    }

    override suspend fun deleteObligacion(id: String): NetworkResult<Unit> {
        val result = safeApiCall(dispatcher) { api.deleteObligacion(id) }
        if (result is NetworkResult.Success) {
            obligacionDao.deleteById(id)
        }
        return result
    }

    override suspend fun abonarObligacion(idObligacion: String, monto: Long): NetworkResult<Unit> {
        val result = safeApiCall(dispatcher) {
            api.abonarObligacion(com.gamma.mobile.domain.model.AbonoRequest(idObligacion = idObligacion, monto = monto))
        }
        if (result is NetworkResult.Success) {
            syncStateDao.insert(SyncStateEntity(SYNC_KEY, 0L))
        }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(Unit)
            is NetworkResult.Error -> result
            is NetworkResult.Exception -> result
        }
    }

    companion object {
        private const val SYNC_KEY = "obligaciones"
        private const val CACHE_TTL = 30 * 60 * 1000L // 30 minutes
    }
}
