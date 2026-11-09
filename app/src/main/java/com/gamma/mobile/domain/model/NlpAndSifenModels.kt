package com.gamma.mobile.domain.model

data class ParsedMovement(
    val valido: Boolean = true,
    val monto: Long = 0L,
    val moneda: String = "PYG",
    val tipoMovimiento: String = "Gasto",
    val categoria: String = "Varios",
    val proveedorCliente: String = "Varios",
    val descripcion: String? = null,
    val confianza: Double = 1.0
)

data class SifenRucInfo(
    val ruc: String = "",
    val razonSocial: String = "",
    val estado: String? = "ACTIVO"
)
