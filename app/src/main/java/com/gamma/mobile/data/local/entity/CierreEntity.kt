package com.gamma.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gamma.mobile.domain.model.CierreHistorialItem

@Entity(tableName = "cierres")
data class CierreEntity(
    @PrimaryKey val id: String,
    val tipo: String, // "contable" o "laboral"
    val periodo: String,
    val fecha: String,
    val totalMonto: Long,
    val totalHoras: Double,
    val resumen: String,
    val pdfUrl: String?,
    val pdfDisponible: Boolean
)

fun CierreEntity.toDomain(): CierreHistorialItem {
    return CierreHistorialItem(
        id = id,
        tipo = tipo,
        periodo = periodo,
        fecha = fecha,
        totalMonto = totalMonto,
        totalHoras = totalHoras,
        resumen = resumen,
        pdfUrl = pdfUrl,
        pdfDisponible = pdfDisponible
    )
}

fun CierreHistorialItem.toEntity(): CierreEntity {
    return CierreEntity(
        id = id,
        tipo = tipo,
        periodo = periodo,
        fecha = fecha,
        totalMonto = totalMonto,
        totalHoras = totalHoras,
        resumen = resumen,
        pdfUrl = pdfUrl,
        pdfDisponible = pdfDisponible
    )
}
