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
    val syncStatus: String = "SYNCED",
    val proveedorCliente: String = "Varios",
    val nroFactura: String = "S/N",
    val tasaIva: String = "10%"
)

fun MovimientoEntity.toDomain(): Movimiento {
    return Movimiento(
        id = id,
        fecha = fecha,
        tipo = tipo,
        categoria = categoria,
        total = total,
        comprobante = comprobante,
        proveedorCliente = proveedorCliente,
        nroFactura = nroFactura,
        tasaIva = tasaIva,
        clientId = id
    )
}

fun Movimiento.toEntity(status: String = "SYNCED"): MovimientoEntity {
    return MovimientoEntity(
        id = id,
        fecha = fecha,
        tipo = tipo,
        categoria = categoria,
        total = total,
        comprobante = comprobante,
        syncStatus = status,
        proveedorCliente = proveedorCliente,
        nroFactura = nroFactura,
        tasaIva = tasaIva
    )
}
