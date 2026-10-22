package com.gamma.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gamma.mobile.data.local.dao.AvisoDao
import com.gamma.mobile.data.local.dao.CierreDao
import com.gamma.mobile.data.local.dao.MovimientoDao
import com.gamma.mobile.data.local.dao.ObligacionDao
import com.gamma.mobile.data.local.dao.SyncStateDao
import com.gamma.mobile.data.local.entity.AvisoEntity
import com.gamma.mobile.data.local.entity.CierreEntity
import com.gamma.mobile.data.local.entity.MovimientoEntity
import com.gamma.mobile.data.local.entity.ObligacionEntity
import com.gamma.mobile.data.local.entity.SyncStateEntity

@Database(
    entities = [
        MovimientoEntity::class,
        AvisoEntity::class,
        ObligacionEntity::class,
        SyncStateEntity::class,
        CierreEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class GammaDatabase : RoomDatabase() {
    abstract fun movimientoDao(): MovimientoDao
    abstract fun avisoDao(): AvisoDao
    abstract fun obligacionDao(): ObligacionDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun cierreDao(): CierreDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cierres` (
                        `id` TEXT NOT NULL,
                        `tipo` TEXT NOT NULL,
                        `periodo` TEXT NOT NULL,
                        `fecha` TEXT NOT NULL,
                        `totalMonto` INTEGER NOT NULL,
                        `totalHoras` REAL NOT NULL,
                        `resumen` TEXT NOT NULL,
                        `pdfUrl` TEXT,
                        `pdfDisponible` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
