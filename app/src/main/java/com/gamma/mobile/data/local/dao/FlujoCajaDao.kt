package com.gamma.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gamma.mobile.data.local.entity.FlujoCajaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlujoCajaDao {

    @Query("SELECT * FROM flujo_caja WHERE id = :id LIMIT 1")
    fun getFlujoCajaFlow(id: String = "current"): Flow<FlujoCajaEntity?>

    @Query("SELECT * FROM flujo_caja WHERE id = :id LIMIT 1")
    suspend fun getFlujoCaja(id: String = "current"): FlujoCajaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlujoCaja(entity: FlujoCajaEntity)

    @Query("DELETE FROM flujo_caja")
    suspend fun clear()
}
