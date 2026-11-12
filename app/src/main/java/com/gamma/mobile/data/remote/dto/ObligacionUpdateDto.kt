package com.gamma.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ObligacionUpdateDto(
    @SerializedName("entidad")
    val entidad: String? = null,
    @SerializedName("nombre")
    val nombre: String? = null,
    @SerializedName("tipo")
    val tipo: String? = null,
    @SerializedName(value = "monto_inicial", alternate = ["montoInicial"])
    val montoInicial: Long? = null,
    @SerializedName(value = "saldo_actual", alternate = ["saldoActual"])
    val saldoActual: Long? = null,
    @SerializedName(value = "cuotas_totales", alternate = ["cuotasTotales"])
    val cuotasTotales: Int? = null,
    @SerializedName(value = "cuotas_restantes", alternate = ["cuotasRestantes"])
    val cuotasRestantes: Int? = null,
    @SerializedName(value = "cuota_referencia_gs", alternate = ["cuotaReferenciaGs", "cuotas"])
    val cuotaReferenciaGs: Long? = null,
    @SerializedName(value = "dia_vencimiento", alternate = ["diaVencimiento"])
    val diaVencimiento: Int? = null,
    @SerializedName(value = "fecha_inicio", alternate = ["fechaInicio"])
    val fechaInicio: String? = null,
    @SerializedName("estado")
    val estado: String? = null,
    @SerializedName("observaciones")
    val observaciones: String? = null,
    @SerializedName(value = "es_gasto_fijo", alternate = ["esGastoFijo"])
    val esGastoFijo: Boolean? = null
)
