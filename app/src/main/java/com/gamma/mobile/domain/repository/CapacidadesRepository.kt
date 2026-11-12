package com.gamma.mobile.domain.repository

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.CapacidadesResponseDto
import kotlinx.coroutines.flow.StateFlow

interface CapacidadesRepository {
    /**
     * Flow reactivo de capacidades clave negociadas con el backend.
     */
    val capacidadesFlow: StateFlow<Map<String, Boolean>>

    /**
     * Sincroniza dinámicamente el catálogo de capacidades y rutas desde GET /api/v1/capacidades.
     */
    suspend fun syncCapacidades(): NetworkResult<CapacidadesResponseDto>

    /**
     * Consulta si una capacidad o feature toggle puntual está habilitado.
     */
    fun isCapacidadHabilitada(clave: String, defaultSiNoExiste: Boolean = true): Boolean

    /**
     * Retorna el mapa completo de capacidades activas.
     */
    fun getCapacidadesClave(): Map<String, Boolean>

    /**
     * Lista de módulos activos soportados por el backend.
     */
    fun getModulos(): List<String>

    /**
     * Versión del API backend descubierta durante el último handshake.
     */
    fun getVersionApi(): String
}
