package com.gamma.mobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clima_cache")
data class ClimaEntity(
    @PrimaryKey
    val id: String = "current",
    @ColumnInfo(name = "temperatura_actual")
    val temperaturaActual: Double,
    @ColumnInfo(name = "temperatura_min")
    val temperaturaMin: Double,
    @ColumnInfo(name = "temperatura_max")
    val temperaturaMax: Double,
    @ColumnInfo(name = "condicion")
    val condicion: String,
    @ColumnInfo(name = "alerta_paraguas_activar")
    val alertaParaguasActivar: Boolean,
    @ColumnInfo(name = "probabilidad_lluvia")
    val probabilidadLluvia: Int,
    @ColumnInfo(name = "motivo_paraguas")
    val motivoParaguas: String,
    @ColumnInfo(name = "actualizado_at")
    val actualizadoAt: String,
    @ColumnInfo(name = "cached_timestamp")
    val cachedTimestamp: Long = System.currentTimeMillis()
)
