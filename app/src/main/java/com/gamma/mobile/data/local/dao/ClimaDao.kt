package com.gamma.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gamma.mobile.data.local.entity.ClimaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClimaDao {

    @Query("SELECT * FROM clima_cache WHERE id = :id LIMIT 1")
    fun getClimaFlow(id: String = "current"): Flow<ClimaEntity?>

    @Query("SELECT * FROM clima_cache WHERE id = :id LIMIT 1")
    suspend fun getClima(id: String = "current"): ClimaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClima(entity: ClimaEntity)

    @Query("DELETE FROM clima_cache")
    suspend fun clear()
}
