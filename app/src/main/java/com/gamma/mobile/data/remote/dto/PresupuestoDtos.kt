package com.gamma.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PresupuestoFijosResponseDto(
    @SerializedName(value = "salario_base", alternate = ["salarioBase"])
    val salarioBase: Long = 0L,
    @SerializedName(value = "total_gastos_fijos", alternate = ["totalGastosFijos"])
    val totalGastosFijos: Long = 0L,
    @SerializedName("categorias")
    val categorias: Map<String, List<GastoFijoItemDto>> = emptyMap()
)

data class GastoFijoItemDto(
    @SerializedName("concepto")
    val concepto: String = "",
    @SerializedName("monto")
    val monto: Long = 0L,
    @SerializedName(value = "concepto_id", alternate = ["conceptoId"])
    val conceptoId: String? = null
)

data class GastoFijoCreateDto(
    @SerializedName("categoria")
    val categoria: String,
    @SerializedName("concepto")
    val concepto: String,
    @SerializedName("monto")
    val monto: Long
)

data class GastoFijoUpdateDto(
    @SerializedName(value = "nuevo_monto", alternate = ["nuevoMonto"])
    val nuevoMonto: Long
)

data class SalarioBaseUpdateDto(
    @SerializedName(value = "nuevo_salario", alternate = ["nuevoSalario"])
    val nuevoSalario: Long
)

data class TechoPresupuestarioItemDto(
    @SerializedName("categoria")
    val categoria: String = "",
    @SerializedName(value = "limite_mensual", alternate = ["limiteMensual"])
    val limiteMensual: Long = 0L,
    @SerializedName("consumido")
    val consumido: Long = 0L,
    @SerializedName("porcentaje")
    val porcentaje: Double = 0.0,
    @SerializedName(value = "umbral_alerta", alternate = ["umbralAlerta"])
    val umbralAlerta: Int = 80,
    @SerializedName(value = "alerta_emitida", alternate = ["alertaEmitida"])
    val alertaEmitida: Boolean = false,
    @SerializedName("estado")
    val estado: String = "NORMAL",
    @SerializedName("mensaje")
    val mensaje: String = ""
)

data class TechoPresupuestarioUpdateDto(
    @SerializedName(value = "limite_mensual", alternate = ["limiteMensual"])
    val limiteMensual: Long? = null,
    @SerializedName(value = "umbral_alerta", alternate = ["umbralAlerta"])
    val umbralAlerta: Int? = null
)

data class PresupuestoAccionResponseDto(
    @SerializedName("status")
    val status: String = "ok",
    @SerializedName("mensaje")
    val mensaje: String? = null,
    @SerializedName("concepto")
    val concepto: String? = null,
    @SerializedName("categoria")
    val categoria: String? = null,
    @SerializedName("monto")
    val monto: Long? = null,
    @SerializedName(value = "nuevo_monto", alternate = ["nuevoMonto"])
    val nuevoMonto: Long? = null,
    @SerializedName(value = "nuevo_salario", alternate = ["nuevoSalario"])
    val nuevoSalario: Long? = null
)
