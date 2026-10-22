package com.gamma.mobile.data.repository

import com.gamma.mobile.data.local.dao.EscenarioCapacidadDao
import com.gamma.mobile.data.local.entity.EscenarioCapacidadEntity
import com.gamma.mobile.domain.model.EscenarioCapacidad
import com.gamma.mobile.domain.model.GastoSimulado
import com.gamma.mobile.domain.repository.EscenarioCapacidadRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class EscenarioCapacidadRepositoryImpl @Inject constructor(
    private val dao: EscenarioCapacidadDao,
    private val gson: Gson
) : EscenarioCapacidadRepository {

    private val setStringType = object : TypeToken<Set<String>>() {}.type
    private val mapStringLongType = object : TypeToken<Map<String, Long>>() {}.type
    private val listGastoType = object : TypeToken<List<GastoSimulado>>() {}.type

    override fun getEscenarios(): Flow<List<EscenarioCapacidad>> {
        return dao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun guardarEscenario(escenario: EscenarioCapacidad) {
        val now = System.currentTimeMillis()
        val entity = EscenarioCapacidadEntity(
            id = escenario.id.ifBlank { java.util.UUID.randomUUID().toString() },
            nombre = escenario.nombre,
            fechaCreacion = if (escenario.fechaCreacion > 0L) escenario.fechaCreacion else now,
            fechaActualizacion = now,
            ingresoExtra = escenario.ingresoExtra,
            deudasPausadasJson = gson.toJson(escenario.deudasPausadasIds),
            abonosExtraJson = gson.toJson(escenario.abonosExtraordinarios),
            gastosAdHocJson = gson.toJson(escenario.gastosAdHoc)
        )
        dao.insert(entity)
    }

    override suspend fun eliminarEscenario(id: String) {
        dao.deleteById(id)
    }

    private fun EscenarioCapacidadEntity.toDomain(): EscenarioCapacidad {
        val deudasPausadas: Set<String> = try {
            gson.fromJson(deudasPausadasJson, setStringType) ?: emptySet()
        } catch (_: Exception) {
            emptySet()
        }

        val abonosExtra: Map<String, Long> = try {
            gson.fromJson(abonosExtraJson, mapStringLongType) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }

        val gastos: List<GastoSimulado> = try {
            gson.fromJson(gastosAdHocJson, listGastoType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        return EscenarioCapacidad(
            id = id,
            nombre = nombre,
            fechaCreacion = fechaCreacion,
            fechaActualizacion = fechaActualizacion,
            ingresoExtra = ingresoExtra,
            deudasPausadasIds = deudasPausadas,
            abonosExtraordinarios = abonosExtra,
            gastosAdHoc = gastos
        )
    }
}
