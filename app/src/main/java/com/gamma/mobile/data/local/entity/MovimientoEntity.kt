package com.gamma.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gamma.mobile.domain.model.Movimiento

@Entity(tableName = "movimientos")
data class MovimientoEntity(
    @PrimaryKey val id: String,
    val fecha: String,
    val tipo: String,
    val categoria: String,
    val total: Long,
    val comprobante: String,
    val syncStatus: String = "SYNCED"
)

fun MovimientoEntity.toDomain(): Movimiento {
    return Movimiento(id, fecha, tipo, categoria, total, comprobante)
}

fun Movimiento.toEntity(status: String = "SYNCED"): MovimientoEntity {
    return MovimientoEntity(id, fecha, tipo, categoria, total, comprobante, status)
}
