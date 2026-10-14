package com.gamma.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gamma.mobile.data.local.entity.MovimientoEntity

@Dao
interface MovimientoDao {
    @Query("SELECT * FROM movimientos")
    suspend fun getAll(): List<MovimientoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movimientos: List<MovimientoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movimiento: MovimientoEntity)

    @Query("SELECT * FROM movimientos WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSync(): List<MovimientoEntity>

    @Query("UPDATE movimientos SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun updateSyncStatus(id: String)

    @Query("DELETE FROM movimientos WHERE syncStatus = 'SYNCED'")
    suspend fun deleteSynced()

    @Query("DELETE FROM movimientos")
    suspend fun deleteAll()
}
