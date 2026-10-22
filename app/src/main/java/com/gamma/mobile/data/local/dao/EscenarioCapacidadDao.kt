package com.gamma.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gamma.mobile.data.local.entity.EscenarioCapacidadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EscenarioCapacidadDao {
    @Query("SELECT * FROM escenarios_capacidad ORDER BY fecha_actualizacion DESC")
    fun getAllFlow(): Flow<List<EscenarioCapacidadEntity>>

    @Query("SELECT * FROM escenarios_capacidad ORDER BY fecha_actualizacion DESC")
    suspend fun getAll(): List<EscenarioCapacidadEntity>

    @Query("SELECT * FROM escenarios_capacidad WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EscenarioCapacidadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(escenario: EscenarioCapacidadEntity)

    @Query("DELETE FROM escenarios_capacidad WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM escenarios_capacidad")
    suspend fun deleteAll()
}
