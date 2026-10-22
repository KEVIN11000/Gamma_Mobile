package com.gamma.mobile.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gamma.mobile.data.local.dao.CierreDao
import com.gamma.mobile.data.local.entity.CierreEntity
import com.gamma.mobile.data.local.entity.toDomain
import com.gamma.mobile.data.local.entity.toEntity
import com.gamma.mobile.domain.model.CierreHistorialItem
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
class CierreDaoTest {

    private lateinit var db: GammaDatabase
    private lateinit var dao: CierreDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            GammaDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.cierreDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insertAll and getAll returns items ordered by fecha DESC`() = runBlocking {
        val cierre1 = CierreEntity(
            id = "c1",
            tipo = "contable",
            periodo = "09/2026",
            fecha = "2026-09-30 20:00",
            totalMonto = 15000000L,
            totalHoras = 0.0,
            resumen = "Cierre Septiembre",
            pdfUrl = "https://example.com/c1.pdf",
            pdfDisponible = true
        )
        val cierre2 = CierreEntity(
            id = "c2",
            tipo = "laboral",
            periodo = "10/2026",
            fecha = "2026-10-15 18:00",
            totalMonto = 8000000L,
            totalHoras = 160.0,
            resumen = "Cierre Quincenal",
            pdfUrl = null,
            pdfDisponible = false
        )

        dao.insertAll(listOf(cierre1, cierre2))

        val all = dao.getAll()
        assertEquals(2, all.size)
        // Order by fecha DESC: c2 (2026-10-15) before c1 (2026-09-30)
        assertEquals("c2", all[0].id)
        assertEquals("c1", all[1].id)
    }

    @Test
    fun `getByTipo returns only requested category`() = runBlocking {
        val cContable = CierreEntity("1", "contable", "10/2026", "2026-10-20", 5000L, 0.0, "Contable", null, false)
        val cLaboral = CierreEntity("2", "laboral", "10/2026", "2026-10-21", 7000L, 40.0, "Laboral", null, false)

        dao.insertAll(listOf(cContable, cLaboral))

        val contables = dao.getByTipo("contable")
        assertEquals(1, contables.size)
        assertEquals("1", contables[0].id)

        val laborales = dao.getByTipo("laboral")
        assertEquals(1, laborales.size)
        assertEquals("2", laborales[0].id)
    }

    @Test
    fun `getById and deleteById works correctly`() = runBlocking {
        val c = CierreEntity("c100", "contable", "10/2026", "2026-10-22", 12000L, 0.0, "Test", "url", true)
        dao.insert(c)

        val retrieved = dao.getById("c100")
        assertNotNull(retrieved)
        assertEquals("c100", retrieved?.id)

        dao.deleteById("c100")
        assertNull(dao.getById("c100"))
    }

    @Test
    fun `deleteAll clears all records`() = runBlocking {
        val list = listOf(
            CierreEntity("1", "contable", "08/2026", "2026-08-31", 1000L, 0.0, "R1", null, false),
            CierreEntity("2", "laboral", "08/2026", "2026-08-31", 2000L, 80.0, "R2", null, false)
        )
        dao.insertAll(list)
        assertEquals(2, dao.getAll().size)

        dao.deleteAll()
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun `domain and entity mapping preserves all properties`() {
        val domain = CierreHistorialItem(
            id = "c-domain",
            tipo = "laboral",
            periodo = "10/2026",
            fecha = "2026-10-22",
            totalMonto = 18500000L,
            totalHoras = 176.5,
            resumen = "Resumen de nómina",
            pdfUrl = "https://gamma.bot/pdf/laboral_10_2026.pdf",
            pdfDisponible = true
        )

        val entity = domain.toEntity()
        val backToDomain = entity.toDomain()

        assertEquals(domain, backToDomain)
        assertEquals(176.5, entity.totalHoras, 0.001)
        assertEquals(18500000L, entity.totalMonto)
        assertTrue(entity.pdfDisponible)
    }
}
