package com.gamma.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gamma.mobile.domain.model.Aviso

@Entity(tableName = "avisos")
data class AvisoEntity(
    @PrimaryKey val id: String,
    val titulo: String,
    val fechaEvento: String,
    val repeticion: String = Aviso.REPETICION_NINGUNA,
    val hitosMinutosCsv: String = Aviso.DEFAULT_HITOS_MINUTOS.joinToString(",")
)

fun AvisoEntity.toDomain(): Aviso {
    val hitos = hitosMinutosCsv
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() }
    return Aviso(
        id = id,
        titulo = titulo,
        fechaEvento = fechaEvento,
        repeticion = Aviso.normalizarRepeticion(repeticion),
        hitosMinutos = Aviso.normalizarHitos(hitos)
    )
}

fun Aviso.toEntity(): AvisoEntity {
    return AvisoEntity(
        id = id,
        titulo = titulo,
        fechaEvento = fechaEvento,
        repeticion = Aviso.normalizarRepeticion(repeticion),
        hitosMinutosCsv = Aviso.normalizarHitos(hitosMinutos).joinToString(",")
    )
}
