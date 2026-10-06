package com.gamma.mobile.data.repository

import com.gamma.mobile.data.local.dao.MovimientoDao
import com.gamma.mobile.data.local.dao.SyncStateDao
import com.gamma.mobile.data.local.entity.SyncStateEntity
import com.gamma.mobile.data.local.entity.toDomain
import com.gamma.mobile.data.local.entity.toEntity
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.model.Movimiento
import com.gamma.mobile.domain.repository.MovimientoRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

class MovimientoRepositoryImpl @Inject constructor(
    private val api: GammaApi,
    private val movimientoDao: MovimientoDao,
    private val syncStateDao: SyncStateDao,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) : MovimientoRepository {

    override suspend fun getMovimientos(forceRefresh: Boolean): NetworkResult<List<Movimiento>> {
        val syncState = syncStateDao.getSyncState(SYNC_KEY)
        val isCacheValid = syncState != null && (System.currentTimeMillis() - syncState.lastSyncTimestamp) < CACHE_TTL

        if (!forceRefresh && isCacheValid) {
            val localData = movimientoDao.getAll().map { it.toDomain() }
            if (localData.isNotEmpty()) {
                return NetworkResult.Success(localData)
            }
        }

        // Fetch from API
        return when (val response = safeApiCall(dispatcher) { api.getMovimientos() }) {
            is NetworkResult.Success -> {
                val remoteData = response.data
                movimientoDao.deleteAll()
                movimientoDao.insertAll(remoteData.map { it.toEntity() })
                syncStateDao.insert(SyncStateEntity(SYNC_KEY, System.currentTimeMillis()))
                NetworkResult.Success(remoteData)
            }
            is NetworkResult.Error -> {
                // If API fails, try to return cache even if expired
                val localData = movimientoDao.getAll().map { it.toDomain() }
                if (localData.isNotEmpty()) {
                    NetworkResult.Success(localData)
                } else {
                    response
                }
            }
            is NetworkResult.Exception -> {
                // If network exception, try to return cache
                val localData = movimientoDao.getAll().map { it.toDomain() }
                if (localData.isNotEmpty()) {
                    NetworkResult.Success(localData)
                } else {
                    response
                }
            }
        }
    }

    override fun getMovimientosLocal(): Flow<List<Movimiento>> {
        // En un caso real MovimientoDao retornaría Flow<List<MovimientoEntity>>
        throw NotImplementedError("To be implemented as Flow")
    }

    companion object {
        private const val SYNC_KEY = "movimientos"
        private const val CACHE_TTL = 5 * 60 * 1000L // 5 minutes
    }
}
