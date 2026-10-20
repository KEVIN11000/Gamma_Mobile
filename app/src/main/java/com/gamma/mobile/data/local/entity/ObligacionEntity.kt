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
    val estado: String,
    val entidad: String = "",
    val tipo: String = "Deuda",
    val cuotaReferenciaGs: Long = 0L,
    val cuotasTotales: Int = 0,
    val cuotasRestantes: Int = 0,
    val diaVencimiento: Int = 0,
    val fechaInicio: String = "",
    val observaciones: String = ""
)

fun ObligacionEntity.toDomain(): Obligacion {
    return Obligacion(
        id = id,
        nombre = nombre,
        montoInicial = montoInicial,
        saldoActual = saldoActual,
        estado = estado,
        entidad = entidad,
        tipo = tipo,
        cuotaReferenciaGs = cuotaReferenciaGs,
        cuotasTotales = cuotasTotales,
        cuotasRestantes = cuotasRestantes,
        diaVencimiento = diaVencimiento,
        fechaInicio = fechaInicio,
        observaciones = observaciones
    )
}

fun Obligacion.toEntity(): ObligacionEntity {
    return ObligacionEntity(
        id = id,
        nombre = nombre,
        montoInicial = montoInicial,
        saldoActual = saldoActual,
        estado = estado,
        entidad = entidad,
        tipo = tipo,
        cuotaReferenciaGs = cuotaReferenciaGs,
        cuotasTotales = cuotasTotales,
        cuotasRestantes = cuotasRestantes,
        diaVencimiento = diaVencimiento,
        fechaInicio = fechaInicio,
        observaciones = observaciones
    )
}
