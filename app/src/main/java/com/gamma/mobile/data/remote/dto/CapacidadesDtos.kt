package com.gamma.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CapacidadesResponseDto(
    @SerializedName(value = "version_api", alternate = ["versionApi"])
    val versionApi: String = "",
    @SerializedName(value = "total_rutas", alternate = ["totalRutas"])
    val totalRutas: Int = 0,
    @SerializedName("modulos")
    val modulos: List<String> = emptyList(),
    @SerializedName(value = "capacidades_clave", alternate = ["capacidadesClave"])
    val capacidadesClave: Map<String, Boolean> = emptyMap(),
    @SerializedName("rutas")
    val rutas: List<RutaEndpointInfoDto> = emptyList()
)

data class RutaEndpointInfoDto(
    @SerializedName("modulo")
    val modulo: String = "",
    @SerializedName("ruta")
    val ruta: String = "",
    @SerializedName("metodos")
    val metodos: List<String> = emptyList(),
    @SerializedName(value = "nombre_endpoint", alternate = ["nombreEndpoint"])
    val nombreEndpoint: String? = null,
    @SerializedName("descripcion")
    val descripcion: String? = null,
    @SerializedName("autenticacion")
    val autenticacion: Boolean = true,
    @SerializedName("parametros")
    val parametros: List<String> = emptyList()
)
