package com.gamma.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gamma.mobile.domain.model.Obligacion

@Entity(tableName = "obligaciones")
data class ObligacionEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val montoInicial: Long,
    val saldoActual: Long,
    val estado: String
)

fun ObligacionEntity.toDomain(): Obligacion {
    return Obligacion(id, nombre, montoInicial, saldoActual, estado)
}

fun Obligacion.toEntity(): ObligacionEntity {
    return ObligacionEntity(id, nombre, montoInicial, saldoActual, estado)
}
