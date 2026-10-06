package com.gamma.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gamma.mobile.data.local.entity.AvisoEntity

@Dao
interface AvisoDao {
    @Query("SELECT * FROM avisos")
    suspend fun getAll(): List<AvisoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(avisos: List<AvisoEntity>)

    @Query("DELETE FROM avisos")
    suspend fun deleteAll()
}
