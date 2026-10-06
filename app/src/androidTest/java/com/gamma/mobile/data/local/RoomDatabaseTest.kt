package com.gamma.mobile.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gamma.mobile.data.local.dao.MovimientoDao
import com.gamma.mobile.data.local.entity.MovimientoEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {

    private lateinit var db: GammaDatabase
    private lateinit var dao: MovimientoDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, GammaDatabase::class.java
        ).build()
        dao = db.movimientoDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeMovimientoAndReadInList() = runBlocking {
        val movimiento = MovimientoEntity("1", "2026-05-01", "Ingreso", "Ventas", 1000, "Virtual")
        dao.insertAll(listOf(movimiento))

        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals(movimiento.id, all[0].id)
    }

    @Test
    fun deleteAllClearsTable() = runBlocking {
        val movimiento = MovimientoEntity("1", "2026-05-01", "Ingreso", "Ventas", 1000, "Virtual")
        dao.insertAll(listOf(movimiento))
        dao.deleteAll()

        val all = dao.getAll()
        assertEquals(0, all.size)
    }
}
