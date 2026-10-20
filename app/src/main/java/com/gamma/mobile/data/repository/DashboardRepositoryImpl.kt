package com.gamma.mobile.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.gamma.mobile.data.local.dao.MovimientoDao
import com.gamma.mobile.data.local.entity.toDomain
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.repository.DashboardRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Named

class DashboardRepositoryImpl @Inject constructor(
    private val api: GammaApi,
    @ApplicationContext private val context: Context,
    private val movimientoDao: MovimientoDao,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) : DashboardRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("gamma_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    override suspend fun getSummary(forceRefresh: Boolean): NetworkResult<DashboardSummary> {
        return when (val base = getRemoteOrCachedSummary(forceRefresh)) {
            is NetworkResult.Success -> NetworkResult.Success(aplicarMovimientosPendientes(base.data))
            else -> base
        }
    }

    /** Suma al resumen los movimientos locales aún no sincronizados (ver [DashboardSummaryCalculator]). */
    private suspend fun aplicarMovimientosPendientes(summary: DashboardSummary): DashboardSummary {
        return try {
            val pendientes = movimientoDao.getPendingSync().map { it.toDomain() }
            DashboardSummaryCalculator.aplicarPendientes(summary, pendientes)
        } catch (e: Exception) {
            summary
        }
    }

    private suspend fun getRemoteOrCachedSummary(forceRefresh: Boolean): NetworkResult<DashboardSummary> {
        val lastSync = prefs.getLong(PREF_LAST_SYNC, 0L)
        val isCacheValid = (System.currentTimeMillis() - lastSync) < CACHE_TTL

        if (!forceRefresh && isCacheValid) {
            val cachedJson = prefs.getString(PREF_SUMMARY, null)
            if (cachedJson != null) {
                try {
                    val summary = gson.fromJson(cachedJson, DashboardSummary::class.java)
                    return NetworkResult.Success(summary)
                } catch (e: Exception) {
                    // Ignore and fetch from API
                }
            }
        }

        return when (val response = safeApiCall(dispatcher) { api.getDashboardSummary() }) {
            is NetworkResult.Success -> {
                prefs.edit()
                    .putString(PREF_SUMMARY, gson.toJson(response.data))
                    .putLong(PREF_LAST_SYNC, System.currentTimeMillis())
                    .apply()
                NetworkResult.Success(response.data)
            }
            is NetworkResult.Error, is NetworkResult.Exception -> {
                val cachedJson = prefs.getString(PREF_SUMMARY, null)
                if (cachedJson != null) {
                    try {
                        val summary = gson.fromJson(cachedJson, DashboardSummary::class.java)
                        NetworkResult.Success(summary)
                    } catch (e: Exception) {
                        response // Return original error
                    }
                } else {
                    response
                }
            }
        }
    }

    companion object {
        private const val PREF_SUMMARY = "dashboard_summary"
        private const val PREF_LAST_SYNC = "dashboard_last_sync"
        private const val CACHE_TTL = 15 * 60 * 1000L // 15 minutes
    }
}
