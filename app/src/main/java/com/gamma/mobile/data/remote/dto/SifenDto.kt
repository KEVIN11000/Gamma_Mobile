package com.gamma.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SifenRucRequestDto(
    @SerializedName("ruc")
    val ruc: String
)

data class SifenRucResponseDto(
    @SerializedName("ruc")
    val ruc: String = "",
    @SerializedName(value = "razon_social", alternate = ["razonSocial", "nombre"])
    val razonSocial: String = "",
    @SerializedName("estado")
    val estado: String? = "ACTIVO"
)
