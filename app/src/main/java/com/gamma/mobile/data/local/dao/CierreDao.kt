package com.gamma.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gamma.mobile.data.local.entity.CierreEntity

@Dao
interface CierreDao {
    @Query("SELECT * FROM cierres ORDER BY fecha DESC")
    suspend fun getAll(): List<CierreEntity>

    @Query("SELECT * FROM cierres WHERE tipo = :tipo ORDER BY fecha DESC")
    suspend fun getByTipo(tipo: String): List<CierreEntity>

    @Query("SELECT * FROM cierres WHERE id = :id")
    suspend fun getById(id: String): CierreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cierres: List<CierreEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cierre: CierreEntity)

    @Query("DELETE FROM cierres WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM cierres")
    suspend fun deleteAll()
}
