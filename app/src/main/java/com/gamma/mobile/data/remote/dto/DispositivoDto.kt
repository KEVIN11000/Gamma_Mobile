package com.gamma.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DispositivoCreateDto(
    @SerializedName(value = "device_token", alternate = ["deviceToken"])
    val deviceToken: String,
    @SerializedName("platform")
    val platform: String = "android",
    @SerializedName(value = "alias_usuario", alternate = ["aliasUsuario"])
    val aliasUsuario: String = "default"
)

data class DispositivoResponseDto(
    @SerializedName("status")
    val status: String = "ok",
    @SerializedName("registrado")
    val registrado: Boolean = true,
    @SerializedName(value = "dispositivos_activos", alternate = ["dispositivosActivos"])
    val dispositivosActivos: Int = 1
)
