package com.gamma.mobile.data.repository

import android.content.Context
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.CapacidadesResponseDto
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.repository.CapacidadesRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Named

class CapacidadesRepositoryImpl @Inject constructor(
    private val api: GammaApi,
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) : CapacidadesRepository {

    private val prefs = context.getSharedPreferences("gamma_cache", Context.MODE_PRIVATE)

    private val _capacidadesFlow = MutableStateFlow<Map<String, Boolean>>(cargarCapacidadesIniciales())
    override val capacidadesFlow: StateFlow<Map<String, Boolean>> = _capacidadesFlow.asStateFlow()

    private fun cargarCapacidadesIniciales(): Map<String, Boolean> {
        val json = prefs.getString(PREF_CAPACIDADES_CLAVE, null) ?: return DEFAULT_CAPACIDADES
        return try {
            val type = object : TypeToken<Map<String, Boolean>>() {}.type
            gson.fromJson<Map<String, Boolean>>(json, type) ?: DEFAULT_CAPACIDADES
        } catch (_: Exception) {
            DEFAULT_CAPACIDADES
        }
    }

    override suspend fun syncCapacidades(): NetworkResult<CapacidadesResponseDto> {
        val result = safeApiCall(dispatcher) { api.getCapacidades() }
        if (result is NetworkResult.Success) {
            val dto = result.data
            prefs.edit()
                .putString(PREF_VERSION_API, dto.versionApi)
                .putString(PREF_MODULOS, gson.toJson(dto.modulos))
                .putString(PREF_CAPACIDADES_CLAVE, gson.toJson(dto.capacidadesClave))
                .putLong(PREF_LAST_SYNC, System.currentTimeMillis())
                .apply()

            _capacidadesFlow.value = dto.capacidadesClave
        }
        return result
    }

    override fun isCapacidadHabilitada(clave: String, defaultSiNoExiste: Boolean): Boolean {
        return _capacidadesFlow.value[clave] ?: defaultSiNoExiste
    }

    override fun getCapacidadesClave(): Map<String, Boolean> {
        return _capacidadesFlow.value
    }

    override fun getModulos(): List<String> {
        val json = prefs.getString(PREF_MODULOS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun getVersionApi(): String {
        return prefs.getString(PREF_VERSION_API, "") ?: ""
    }

    companion object {
        const val PREF_CAPACIDADES_CLAVE = "capacidades_clave"
        const val PREF_MODULOS = "capacidades_modulos"
        const val PREF_VERSION_API = "capacidades_version_api"
        const val PREF_LAST_SYNC = "capacidades_last_sync"

        val DEFAULT_CAPACIDADES = mapOf(
            "presupuesto_fijos_delete" to true,
            "presupuesto_techos_put" to true,
            "presupuesto_salario_put" to true,
            "asistencia_excel_export" to true,
            "asistencia_pdf_a4_optimizado" to true,
            "obligaciones_deuda_foco" to true,
            "obligaciones_abonar_post" to true,
            "obligaciones_eliminar_delete" to true,
            "cierres_historicos_get" to true,
            "capacidades_discovery" to true
        )
    }
}
