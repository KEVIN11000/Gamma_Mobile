package com.gamma.mobile.domain.model

import com.google.gson.annotations.SerializedName

data class AsistenciaMarcaRequest(
    @SerializedName("modo")
    val modo: String // "normal" o "directo"
)

data class AsistenciaMarcaResponse(
    @SerializedName("tipo")
    val tipo: String = "", // "entrada" o "salida"
    @SerializedName("hora")
    val hora: String = "",
    @SerializedName("mensaje")
    val mensaje: String = ""
)

data class AsistenciaMateriaRequest(
    @SerializedName("materia")
    val materia: String = ""
)

data class AsistenciaMateriaResponse(
    @SerializedName("materia")
    val materia: String? = null,
    @SerializedName("hora")
    val hora: String = "",
    @SerializedName("mensaje")
    val mensaje: String = ""
)
