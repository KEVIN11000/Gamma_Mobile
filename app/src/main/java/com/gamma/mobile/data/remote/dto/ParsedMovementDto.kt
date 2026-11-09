package com.gamma.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ParseMovementRequestDto(
    @SerializedName("texto")
    val texto: String
)

data class ParsedMovementDto(
    @SerializedName("valido")
    val valido: Boolean = true,
    @SerializedName("monto")
    val monto: Long = 0L,
    @SerializedName("moneda")
    val moneda: String = "PYG",
    @SerializedName(value = "tipo_movimiento", alternate = ["tipoMovimiento", "tipo"])
    val tipoMovimiento: String = "Gasto",
    @SerializedName("categoria")
    val categoria: String = "Varios",
    @SerializedName(value = "proveedor_cliente", alternate = ["proveedorCliente", "comercio"])
    val proveedorCliente: String = "Varios",
    @SerializedName("descripcion")
    val descripcion: String? = null,
    @SerializedName("confianza")
    val confianza: Double = 1.0
)
