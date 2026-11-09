package com.gamma.mobile.data.repository

import android.content.Context
import com.gamma.mobile.data.local.dao.AvisoDao
import com.gamma.mobile.data.local.entity.toEntity
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.BootstrapResponseDto
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.repository.BootstrapRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Named

class BootstrapRepositoryImpl @Inject constructor(
    private val api: GammaApi,
    private val avisoDao: AvisoDao,
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) : BootstrapRepository {

    private val prefs = context.getSharedPreferences("gamma_cache", Context.MODE_PRIVATE)

    override suspend fun syncBootstrap(): NetworkResult<BootstrapResponseDto> {
        val result = safeApiCall(dispatcher) { api.getBootstrap() }

        if (result is NetworkResult.Success) {
            val dto = result.data

            // 1. Hidratar resumen de Dashboard en SharedPreferences
            if (dto.summary != null) {
                prefs.edit()
                    .putString(PREF_SUMMARY, gson.toJson(dto.summary))
                    .putLong(PREF_LAST_SYNC, System.currentTimeMillis())
                    .apply()
            }

            // 2. Hidratar avisos en Room
            if (dto.proximosAvisos.isNotEmpty()) {
                val entities = dto.proximosAvisos.map { it.toEntity() }
                avisoDao.insertAll(entities)
            }

            // 3. Cachear periodo actual si está presente
            if (!dto.periodoActual.isNullOrBlank()) {
                prefs.edit()
                    .putString(PREF_PERIODO, dto.periodoActual)
                    .apply()
            }
        }

        return result
    }

    companion object {
        const val PREF_SUMMARY = "dashboard_summary"
        const val PREF_LAST_SYNC = "dashboard_last_sync"
        const val PREF_PERIODO = "cached_periodo_actual"
    }
}
