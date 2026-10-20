package com.gamma.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gamma.mobile.data.local.entity.ObligacionEntity

@Dao
interface ObligacionDao {
    @Query("SELECT * FROM obligaciones")
    suspend fun getAll(): List<ObligacionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(obligaciones: List<ObligacionEntity>)

    @Query("DELETE FROM obligaciones")
    suspend fun deleteAll()

    @Query("DELETE FROM obligaciones WHERE id = :id")
    suspend fun deleteById(id: String)
}
