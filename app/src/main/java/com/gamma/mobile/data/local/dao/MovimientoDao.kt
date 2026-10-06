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

    @Query("DELETE FROM movimientos")
    suspend fun deleteAll()
}
