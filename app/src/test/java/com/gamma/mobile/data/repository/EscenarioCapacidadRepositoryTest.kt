package com.gamma.mobile.data.repository

import com.gamma.mobile.data.local.dao.EscenarioCapacidadDao
import com.gamma.mobile.data.local.entity.EscenarioCapacidadEntity
import com.gamma.mobile.domain.model.EscenarioCapacidad
import com.gamma.mobile.domain.model.GastoSimulado
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EscenarioCapacidadRepositoryTest {

    private lateinit var fakeDao: FakeEscenarioCapacidadDao
    private lateinit var repository: EscenarioCapacidadRepositoryImpl
    private val gson = Gson()

    @Before
    fun setup() {
        fakeDao = FakeEscenarioCapacidadDao()
        repository = EscenarioCapacidadRepositoryImpl(fakeDao, gson)
    }

    @Test
    fun `guardarEscenario serializes domain model into entity and inserts into dao`() = runBlocking {
        val escenario = EscenarioCapacidad(
            id = "esc-1",
            nombre = "Escenario Vacaciones",
            fechaCreacion = 1000L,
            fechaActualizacion = 1000L,
            ingresoExtra = 700000L,
            deudasPausadasIds = setOf("d1", "d2"),
            abonosExtraordinarios = mapOf("d1" to 500000L),
            gastosAdHoc = listOf(
                GastoSimulado(id = "g1", concepto = "Vuelo", monto = 1200000L, incluido = true),
                GastoSimulado(id = "g2", concepto = "Hotel", monto = 800000L, incluido = false)
            )
        )

        repository.guardarEscenario(escenario)

        assertEquals(1, fakeDao.entities.size)
        val entity = fakeDao.entities[0]
        assertEquals("esc-1", entity.id)
        assertEquals("Escenario Vacaciones", entity.nombre)
        assertEquals(700000L, entity.ingresoExtra)
        assertTrue(entity.deudasPausadasJson.contains("d1"))
        assertTrue(entity.deudasPausadasJson.contains("d2"))
        assertTrue(entity.abonosExtraJson.contains("500000"))
        assertTrue(entity.gastosAdHocJson.contains("Vuelo"))
    }

    @Test
    fun `getEscenarios deserializes entity flow into domain models accurately`() = runBlocking {
        val entity = EscenarioCapacidadEntity(
            id = "esc-2",
            nombre = "Escenario Fin de Año",
            fechaCreacion = 2000L,
            fechaActualizacion = 3000L,
            ingresoExtra = 1500000L,
            deudasPausadasJson = "[\"d3\"]",
            abonosExtraJson = "{\"d3\": 400000}",
            gastosAdHocJson = "[{\"id\":\"g1\",\"concepto\":\"Cena familiar\",\"monto\":600000,\"incluido\":true}]"
        )
        fakeDao.emit(listOf(entity))

        val escenarios = repository.getEscenarios().first()
        assertEquals(1, escenarios.size)

        val esc = escenarios[0]
        assertEquals("esc-2", esc.id)
        assertEquals("Escenario Fin de Año", esc.nombre)
        assertEquals(1500000L, esc.ingresoExtra)
        assertEquals(setOf("d3"), esc.deudasPausadasIds)
        assertEquals(400000L, esc.abonosExtraordinarios["d3"])
        assertEquals(1, esc.gastosAdHoc.size)
        assertEquals("Cena familiar", esc.gastosAdHoc[0].concepto)
        assertEquals(600000L, esc.gastosAdHoc[0].monto)
        assertTrue(esc.gastosAdHoc[0].incluido)
    }

    @Test
    fun `getEscenarios gracefully handles invalid json without crashing`() = runBlocking {
        val corruptedEntity = EscenarioCapacidadEntity(
            id = "corrupted",
            nombre = "Corrupto",
            fechaCreacion = 1000L,
            fechaActualizacion = 1000L,
            ingresoExtra = 0L,
            deudasPausadasJson = "invalid json",
            abonosExtraJson = "{invalid}",
            gastosAdHocJson = "not a list"
        )
        fakeDao.emit(listOf(corruptedEntity))

        val escenarios = repository.getEscenarios().first()
        assertEquals(1, escenarios.size)
        assertTrue(escenarios[0].deudasPausadasIds.isEmpty())
        assertTrue(escenarios[0].abonosExtraordinarios.isEmpty())
        assertTrue(escenarios[0].gastosAdHoc.isEmpty())
    }

    @Test
    fun `eliminarEscenario calls dao deleteById`() = runBlocking {
        repository.eliminarEscenario("esc-to-delete")
        assertEquals(listOf("esc-to-delete"), fakeDao.deletedIds)
    }
}

class FakeEscenarioCapacidadDao : EscenarioCapacidadDao {
    val entities = mutableListOf<EscenarioCapacidadEntity>()
    val deletedIds = mutableListOf<String>()
    private val flow = MutableStateFlow<List<EscenarioCapacidadEntity>>(emptyList())

    fun emit(list: List<EscenarioCapacidadEntity>) {
        entities.clear()
        entities.addAll(list)
        flow.value = list
    }

    override fun getAllFlow(): Flow<List<EscenarioCapacidadEntity>> = flow

    override suspend fun getAll(): List<EscenarioCapacidadEntity> = entities

    override suspend fun getById(id: String): EscenarioCapacidadEntity? = entities.find { it.id == id }

    override suspend fun insert(escenario: EscenarioCapacidadEntity) {
        entities.removeAll { it.id == escenario.id }
        entities.add(escenario)
        flow.value = entities.toList()
    }

    override suspend fun deleteById(id: String): Int {
        deletedIds.add(id)
        val removed = entities.removeAll { it.id == id }
        flow.value = entities.toList()
        return if (removed) 1 else 0
    }

    override suspend fun deleteAll() {
        entities.clear()
        flow.value = emptyList()
    }
}
