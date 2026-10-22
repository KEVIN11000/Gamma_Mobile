package com.gamma.mobile.domain.repository

import com.gamma.mobile.domain.model.EscenarioCapacidad
import kotlinx.coroutines.flow.Flow

interface EscenarioCapacidadRepository {
    fun getEscenarios(): Flow<List<EscenarioCapacidad>>
    suspend fun guardarEscenario(escenario: EscenarioCapacidad)
    suspend fun eliminarEscenario(id: String)
}
