package com.gamma.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gamma.mobile.data.local.dao.AvisoDao
import com.gamma.mobile.data.local.dao.CierreDao
import com.gamma.mobile.data.local.dao.ClimaDao
import com.gamma.mobile.data.local.dao.EscenarioCapacidadDao
import com.gamma.mobile.data.local.dao.FlujoCajaDao
import com.gamma.mobile.data.local.dao.MovimientoDao
import com.gamma.mobile.data.local.dao.ObligacionDao
import com.gamma.mobile.data.local.dao.SyncStateDao
import com.gamma.mobile.data.local.entity.AvisoEntity
import com.gamma.mobile.data.local.entity.CierreEntity
import com.gamma.mobile.data.local.entity.ClimaEntity
import com.gamma.mobile.data.local.entity.EscenarioCapacidadEntity
import com.gamma.mobile.data.local.entity.FlujoCajaEntity
import com.gamma.mobile.data.local.entity.MovimientoEntity
import com.gamma.mobile.data.local.entity.ObligacionEntity
import com.gamma.mobile.data.local.entity.SyncStateEntity

@Database(
    entities = [
        MovimientoEntity::class,
        AvisoEntity::class,
        ObligacionEntity::class,
        SyncStateEntity::class,
        CierreEntity::class,
        EscenarioCapacidadEntity::class,
        FlujoCajaEntity::class,
        ClimaEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class GammaDatabase : RoomDatabase() {
    abstract fun movimientoDao(): MovimientoDao
    abstract fun avisoDao(): AvisoDao
    abstract fun obligacionDao(): ObligacionDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun cierreDao(): CierreDao
    abstract fun escenarioCapacidadDao(): EscenarioCapacidadDao
    abstract fun flujoCajaDao(): FlujoCajaDao
    abstract fun climaDao(): ClimaDao

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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `escenarios_capacidad` (
                        `id` TEXT NOT NULL,
                        `nombre` TEXT NOT NULL,
                        `fecha_creacion` INTEGER NOT NULL,
                        `fecha_actualizacion` INTEGER NOT NULL,
                        `ingreso_extra` INTEGER NOT NULL,
                        `deudas_pausadas_json` TEXT NOT NULL,
                        `abonos_extra_json` TEXT NOT NULL,
                        `gastos_adhoc_json` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `flujo_caja` (
                        `id` TEXT NOT NULL,
                        `saldo_actual` INTEGER NOT NULL,
                        `saldo_proyectado_30d` INTEGER NOT NULL,
                        `total_ingresos_proyectados` INTEGER NOT NULL,
                        `total_gastos_fijos` INTEGER NOT NULL,
                        `total_gastos_variables_estimados` INTEGER NOT NULL,
                        `dias_criticos_json` TEXT NOT NULL,
                        `dias_json` TEXT NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `clima_cache` (
                        `id` TEXT NOT NULL,
                        `temperatura_actual` REAL NOT NULL,
                        `temperatura_min` REAL NOT NULL,
                        `temperatura_max` REAL NOT NULL,
                        `condicion` TEXT NOT NULL,
                        `alerta_paraguas_activar` INTEGER NOT NULL,
                        `probabilidad_lluvia` INTEGER NOT NULL,
                        `motivo_paraguas` TEXT NOT NULL,
                        `actualizado_at` TEXT NOT NULL,
                        `cached_timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
