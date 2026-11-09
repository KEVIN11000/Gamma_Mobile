package com.gamma.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ClimaResponseDto(
    @SerializedName(value = "temperatura_actual", alternate = ["temperaturaActual"])
    val temperaturaActual: Double = 0.0,
    @SerializedName(value = "temperatura_min", alternate = ["temperaturaMin"])
    val temperaturaMin: Double = 0.0,
    @SerializedName(value = "temperatura_max", alternate = ["temperaturaMax"])
    val temperaturaMax: Double = 0.0,
    @SerializedName("condicion")
    val condicion: String = "Despejado",
    @SerializedName(value = "alerta_paraguas", alternate = ["alertaParaguas"])
    val alertaParaguas: ClimaAlertaParaguasDto = ClimaAlertaParaguasDto(),
    @SerializedName(value = "actualizado_at", alternate = ["actualizadoAt"])
    val actualizadoAt: String = ""
)

data class ClimaAlertaParaguasDto(
    @SerializedName("activar")
    val activar: Boolean = false,
    @SerializedName(value = "probabilidad_lluvia", alternate = ["probabilidadLluvia"])
    val probabilidadLluvia: Int = 0,
    @SerializedName("motivo")
    val motivo: String = ""
)
