package com.gamma.mobile.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gamma.mobile.data.local.dao.EscenarioCapacidadDao
import com.gamma.mobile.data.local.entity.EscenarioCapacidadEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EscenarioCapacidadDaoTest {

    private lateinit var db: GammaDatabase
    private lateinit var dao: EscenarioCapacidadDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            GammaDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.escenarioCapacidadDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert and getAll returns items ordered by fecha_actualizacion DESC`() = runBlocking {
        val escenarioAntiguo = EscenarioCapacidadEntity(
            id = "e1",
            nombre = "Escenario Antiguo",
            fechaCreacion = 1000L,
            fechaActualizacion = 1000L,
            ingresoExtra = 500000L,
            deudasPausadasJson = "[\"d1\"]",
            abonosExtraJson = "{\"d2\": 300000}",
            gastosAdHocJson = "[]"
        )
        val escenarioReciente = EscenarioCapacidadEntity(
            id = "e2",
            nombre = "Escenario Reciente",
            fechaCreacion = 2000L,
            fechaActualizacion = 3000L,
            ingresoExtra = 1000000L,
            deudasPausadasJson = "[]",
            abonosExtraJson = "{}",
            gastosAdHocJson = "[{\"id\":\"g1\",\"concepto\":\"Pintura\",\"monto\":400000,\"incluido\":true}]"
        )

        dao.insert(escenarioAntiguo)
        dao.insert(escenarioReciente)

        val items = dao.getAll()
        assertEquals(2, items.size)
        assertEquals("e2", items[0].id)
        assertEquals("Escenario Reciente", items[0].nombre)
        assertEquals("e1", items[1].id)

        val flowItems = dao.getAllFlow().first()
        assertEquals(2, flowItems.size)
        assertEquals("e2", flowItems[0].id)
    }

    @Test
    fun `getById returns correct scenario or null`() = runBlocking {
        val escenario = EscenarioCapacidadEntity(
            id = "e-target",
            nombre = "Target",
            fechaCreacion = 1000L,
            fechaActualizacion = 2000L,
            ingresoExtra = 0L,
            deudasPausadasJson = "[]",
            abonosExtraJson = "{}",
            gastosAdHocJson = "[]"
        )
        dao.insert(escenario)

        val found = dao.getById("e-target")
        assertNotNull(found)
        assertEquals("Target", found?.nombre)

        val notFound = dao.getById("inexistente")
        assertNull(notFound)
    }

    @Test
    fun `deleteById removes correct scenario`() = runBlocking {
        val escenario1 = EscenarioCapacidadEntity(
            id = "e1",
            nombre = "E1",
            fechaCreacion = 1000L,
            fechaActualizacion = 1000L,
            ingresoExtra = 0L,
            deudasPausadasJson = "[]",
            abonosExtraJson = "{}",
            gastosAdHocJson = "[]"
        )
        val escenario2 = EscenarioCapacidadEntity(
            id = "e2",
            nombre = "E2",
            fechaCreacion = 2000L,
            fechaActualizacion = 2000L,
            ingresoExtra = 0L,
            deudasPausadasJson = "[]",
            abonosExtraJson = "{}",
            gastosAdHocJson = "[]"
        )

        dao.insert(escenario1)
        dao.insert(escenario2)

        val deletedCount = dao.deleteById("e1")
        assertEquals(1, deletedCount)

        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals("e2", all[0].id)
    }

    @Test
    fun `deleteAll clears all scenarios`() = runBlocking {
        val escenario = EscenarioCapacidadEntity(
            id = "e1",
            nombre = "E1",
            fechaCreacion = 1000L,
            fechaActualizacion = 1000L,
            ingresoExtra = 0L,
            deudasPausadasJson = "[]",
            abonosExtraJson = "{}",
            gastosAdHocJson = "[]"
        )
        dao.insert(escenario)
        dao.deleteAll()

        assertTrue(dao.getAll().isEmpty())
    }
}
