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
    val mensaje: String = "",
    @SerializedName("planilla")
    val planilla: String? = null,
    @SerializedName("fila")
    val fila: Int? = null,
    @SerializedName("confirmado_en_planilla")
    val confirmadoEnPlanilla: Boolean = false
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
