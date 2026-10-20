package com.gamma.mobile.data.repository

import com.gamma.mobile.data.local.dao.AvisoDao
import com.gamma.mobile.data.local.dao.SyncStateDao
import com.gamma.mobile.data.local.entity.SyncStateEntity
import com.gamma.mobile.data.local.entity.toDomain
import com.gamma.mobile.data.local.entity.toEntity
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.repository.AvisoRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Named

class AvisoRepositoryImpl @Inject constructor(
    private val api: GammaApi,
    private val avisoDao: AvisoDao,
    private val syncStateDao: SyncStateDao,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) : AvisoRepository {

    override suspend fun getAvisos(forceRefresh: Boolean): NetworkResult<List<Aviso>> {
        val syncState = syncStateDao.getSyncState(SYNC_KEY)
        val isCacheValid = syncState != null && (System.currentTimeMillis() - syncState.lastSyncTimestamp) < CACHE_TTL

        if (!forceRefresh && isCacheValid) {
            val localData = avisoDao.getAll().map { it.toDomain() }
            if (localData.isNotEmpty()) {
                return NetworkResult.Success(localData)
            }
        }

        return when (val response = safeApiCall(dispatcher) { api.getAvisos() }) {
            is NetworkResult.Success -> {
                val remoteData = response.data
                avisoDao.deleteAll()
                avisoDao.insertAll(remoteData.map { it.toEntity() })
                syncStateDao.insert(SyncStateEntity(SYNC_KEY, System.currentTimeMillis()))
                NetworkResult.Success(remoteData)
            }
            is NetworkResult.Error, is NetworkResult.Exception -> {
                val localData = avisoDao.getAll().map { it.toDomain() }
                if (localData.isNotEmpty()) {
                    NetworkResult.Success(localData)
                } else {
                    if (response is NetworkResult.Error) response else response as NetworkResult.Exception
                }
            }
        }
    }

    override suspend fun createAviso(aviso: Aviso): NetworkResult<Unit> {
        avisoDao.insert(aviso.toEntity())
        return safeApiCall(dispatcher) { api.createAviso(aviso) }.also {
            if (it is NetworkResult.Success) {
                syncStateDao.insert(SyncStateEntity(SYNC_KEY, 0L))
            }
        }
    }

    override suspend fun updateAviso(aviso: Aviso): NetworkResult<Unit> {
        // Persistencia local primero: repetición e hitos deben sobrevivir aunque el backend aún no exponga PUT /avisos/{id}.
        avisoDao.insert(aviso.toEntity())
        return safeApiCall(dispatcher) { api.updateAviso(aviso.id, aviso) }
    }

    override suspend fun deleteAviso(id: String): NetworkResult<Unit> {
        avisoDao.deleteById(id)
        return safeApiCall(dispatcher) { api.deleteAviso(id) }.also {
            if (it is NetworkResult.Success) {
                syncStateDao.insert(SyncStateEntity(SYNC_KEY, 0L))
            }
        }
    }

    override suspend fun interpretarAvisoIa(texto: String): NetworkResult<Aviso> {
        return safeApiCall(dispatcher) { api.interpretarAvisoIa(com.gamma.mobile.data.remote.IaTextRequest(texto)) }
    }

    companion object {
        private const val SYNC_KEY = "avisos"
        private const val CACHE_TTL = 30 * 60 * 1000L // 30 minutes
    }
}
