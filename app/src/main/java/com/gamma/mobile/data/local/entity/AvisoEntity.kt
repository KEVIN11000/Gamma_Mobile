package com.gamma.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gamma.mobile.domain.model.Aviso

@Entity(tableName = "avisos")
data class AvisoEntity(
    @PrimaryKey val id: String,
    val titulo: String,
    val diasRestantes: Int
)

fun AvisoEntity.toDomain(): Aviso {
    return Aviso(id, titulo, diasRestantes)
}

fun Aviso.toEntity(): AvisoEntity {
    return AvisoEntity(id, titulo, diasRestantes)
}
