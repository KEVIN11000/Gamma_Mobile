package com.gamma.mobile.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gamma.mobile.data.local.dao.ClimaDao
import com.gamma.mobile.data.local.dao.FlujoCajaDao
import com.gamma.mobile.data.local.entity.ClimaEntity
import com.gamma.mobile.data.local.entity.FlujoCajaEntity
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
class FlujoCajaAndClimaDaoTest {

    private lateinit var db: GammaDatabase
    private lateinit var flujoCajaDao: FlujoCajaDao
    private lateinit var climaDao: ClimaDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            GammaDatabase::class.java
        ).allowMainThreadQueries().build()
        flujoCajaDao = db.flujoCajaDao()
        climaDao = db.climaDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testFlujoCajaDaoInsertAndQuery() = runBlocking {
        val entity = FlujoCajaEntity(
            id = "current",
            saldoActual = 2500000L,
            saldoProyectado30d = 1800000L,
            totalIngresosProyectados = 8000000L,
            totalGastosFijos = 4500000L,
            totalGastosVariablesEstimados = 4200000L,
            diasCriticosJson = "[{\"fecha\":\"2026-11-20\",\"saldo\":-150000,\"motivo\":\"Cuota BNF\"}]",
            diasJson = "[{\"fecha\":\"2026-11-10\",\"saldo_final\":1900000}]",
            updatedAt = 1000L
        )

        flujoCajaDao.insertFlujoCaja(entity)

        val retrieved = flujoCajaDao.getFlujoCaja("current")
        assertNotNull(retrieved)
        assertEquals(2500000L, retrieved?.saldoActual)
        assertEquals(1800000L, retrieved?.saldoProyectado30d)
        assertTrue(retrieved?.diasCriticosJson?.contains("Cuota BNF") == true)

        val flowValue = flujoCajaDao.getFlujoCajaFlow("current").first()
        assertEquals(retrieved, flowValue)

        flujoCajaDao.clear()
        assertNull(flujoCajaDao.getFlujoCaja("current"))
    }

    @Test
    fun testClimaDaoInsertAndQuery() = runBlocking {
        val entity = ClimaEntity(
            id = "current",
            temperaturaActual = 28.5,
            temperaturaMin = 22.0,
            temperaturaMax = 34.0,
            condicion = "Parcialmente nublado",
            alertaParaguasActivar = true,
            probabilidadLluvia = 65,
            motivoParaguas = "Lluvia moderada esperada",
            actualizadoAt = "2026-11-09 11:00",
            cachedTimestamp = 2000L
        )

        climaDao.insertClima(entity)

        val retrieved = climaDao.getClima("current")
        assertNotNull(retrieved)
        assertEquals(28.5, retrieved!!.temperaturaActual, 0.01)
        assertTrue(retrieved.alertaParaguasActivar)
        assertEquals(65, retrieved.probabilidadLluvia)
        assertEquals("Parcialmente nublado", retrieved.condicion)

        val flowValue = climaDao.getClimaFlow("current").first()
        assertEquals(retrieved, flowValue)

        climaDao.clear()
        assertNull(climaDao.getClima("current"))
    }
}
