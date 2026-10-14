package com.gamma.mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gamma.mobile.data.local.dao.AvisoDao
import com.gamma.mobile.data.local.dao.MovimientoDao
import com.gamma.mobile.data.local.dao.ObligacionDao
import com.gamma.mobile.data.local.dao.SyncStateDao
import com.gamma.mobile.data.local.entity.AvisoEntity
import com.gamma.mobile.data.local.entity.MovimientoEntity
import com.gamma.mobile.data.local.entity.ObligacionEntity
import com.gamma.mobile.data.local.entity.SyncStateEntity

@Database(
    entities = [
        MovimientoEntity::class,
        AvisoEntity::class,
        ObligacionEntity::class,
        SyncStateEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class GammaDatabase : RoomDatabase() {
    abstract fun movimientoDao(): MovimientoDao
    abstract fun avisoDao(): AvisoDao
    abstract fun obligacionDao(): ObligacionDao
    abstract fun syncStateDao(): SyncStateDao
}
