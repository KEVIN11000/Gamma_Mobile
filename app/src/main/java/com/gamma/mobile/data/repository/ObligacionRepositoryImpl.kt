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

        // TODO: M-05 GammaApi is missing getObligaciones() endpoint. 
        // We simulate a fallback to local cache.
        val localData = obligacionDao.getAll().map { it.toDomain() }
        return if (localData.isNotEmpty()) {
            NetworkResult.Success(localData)
        } else {
            NetworkResult.Error(404, "Endpoint no implementado en el mock_server aún")
        }
    }

    companion object {
        private const val SYNC_KEY = "obligaciones"
        private const val CACHE_TTL = 30 * 60 * 1000L // 30 minutes
    }
}
