package com.gamma.mobile.data.repository

import android.content.Context
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.GastoFijoCreateDto
import com.gamma.mobile.data.remote.dto.GastoFijoUpdateDto
import com.gamma.mobile.data.remote.dto.PresupuestoAccionResponseDto
import com.gamma.mobile.data.remote.dto.PresupuestoFijosResponseDto
import com.gamma.mobile.data.remote.dto.SalarioBaseUpdateDto
import com.gamma.mobile.data.remote.dto.TechoPresupuestarioItemDto
import com.gamma.mobile.data.remote.dto.TechoPresupuestarioUpdateDto
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.repository.PresupuestoRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Named

class PresupuestoRepositoryImpl @Inject constructor(
    private val api: GammaApi,
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) : PresupuestoRepository {

    private val prefs = context.getSharedPreferences("gamma_cache", Context.MODE_PRIVATE)

    override suspend fun getGastosFijos(forceRefresh: Boolean): NetworkResult<PresupuestoFijosResponseDto> {
        val cached = getCachedGastosFijos()
        if (!forceRefresh && cached != null) {
            return NetworkResult.Success(cached)
        }

        val result = safeApiCall(dispatcher) { api.getGastosFijos() }
        if (result is NetworkResult.Success) {
            prefs.edit()
                .putString(PREF_GASTOS_FIJOS, gson.toJson(result.data))
                .putLong(PREF_LAST_SYNC_FIJOS, System.currentTimeMillis())
                .apply()
        } else if (cached != null) {
            return NetworkResult.Success(cached)
        }
        return result
    }

    override suspend fun createGastoFijo(request: GastoFijoCreateDto): NetworkResult<PresupuestoAccionResponseDto> {
        val result = safeApiCall(dispatcher) { api.createGastoFijo(request) }
        if (result is NetworkResult.Success) {
            // Invalidar caché para forzar refresco
            prefs.edit().remove(PREF_GASTOS_FIJOS).apply()
        }
        return result
    }

    override suspend fun updateGastoFijo(concepto: String, request: GastoFijoUpdateDto): NetworkResult<PresupuestoAccionResponseDto> {
        val result = safeApiCall(dispatcher) { api.updateGastoFijo(concepto, request) }
        if (result is NetworkResult.Success) {
            prefs.edit().remove(PREF_GASTOS_FIJOS).apply()
        }
        return result
    }

    override suspend fun deleteGastoFijo(concepto: String): NetworkResult<PresupuestoAccionResponseDto> {
        val result = safeApiCall(dispatcher) { api.deleteGastoFijo(concepto) }
        if (result is NetworkResult.Success) {
            prefs.edit().remove(PREF_GASTOS_FIJOS).apply()
        }
        return result
    }

    override suspend fun updateSalarioBase(request: SalarioBaseUpdateDto): NetworkResult<PresupuestoAccionResponseDto> {
        val result = safeApiCall(dispatcher) { api.updateSalarioBase(request) }
        if (result is NetworkResult.Success) {
            prefs.edit().remove(PREF_GASTOS_FIJOS).apply()
        }
        return result
    }

    override suspend fun getTechosPresupuestarios(forceRefresh: Boolean): NetworkResult<List<TechoPresupuestarioItemDto>> {
        val cached = getCachedTechos()
        if (!forceRefresh && cached != null) {
            return NetworkResult.Success(cached)
        }

        val result = safeApiCall(dispatcher) { api.getTechosPresupuestarios() }
        if (result is NetworkResult.Success) {
            prefs.edit()
                .putString(PREF_TECHOS, gson.toJson(result.data))
                .putLong(PREF_LAST_SYNC_TECHOS, System.currentTimeMillis())
                .apply()
        } else if (cached != null) {
            return NetworkResult.Success(cached)
        }
        return result
    }

    override suspend fun updateTechoPresupuestario(categoria: String, request: TechoPresupuestarioUpdateDto): NetworkResult<PresupuestoAccionResponseDto> {
        val result = safeApiCall(dispatcher) { api.updateTechoPresupuestario(categoria, request) }
        if (result is NetworkResult.Success) {
            prefs.edit().remove(PREF_TECHOS).apply()
        }
        return result
    }

    private fun getCachedGastosFijos(): PresupuestoFijosResponseDto? {
        val json = prefs.getString(PREF_GASTOS_FIJOS, null) ?: return null
        return try {
            gson.fromJson(json, PresupuestoFijosResponseDto::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private fun getCachedTechos(): List<TechoPresupuestarioItemDto>? {
        val json = prefs.getString(PREF_TECHOS, null) ?: return null
        return try {
            val type = object : TypeToken<List<TechoPresupuestarioItemDto>>() {}.type
            gson.fromJson<List<TechoPresupuestarioItemDto>>(json, type)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val PREF_GASTOS_FIJOS = "cached_presupuesto_fijos"
        const val PREF_LAST_SYNC_FIJOS = "presupuesto_fijos_last_sync"
        const val PREF_TECHOS = "cached_presupuesto_techos"
        const val PREF_LAST_SYNC_TECHOS = "presupuesto_techos_last_sync"
    }
}
