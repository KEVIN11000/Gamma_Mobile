package com.gamma.mobile.domain.model

import com.google.gson.annotations.SerializedName

data class CierreRequest(
    @SerializedName("hoja")
    val hoja: String,
    @SerializedName("aplicar_descuento")
    val aplicarDescuento: Boolean = false
)

data class CierreResponse(
    @SerializedName("resumen")
    val resumen: String = "",
    @SerializedName("pdf_url")
    val pdfUrl: String? = null,
    @SerializedName("total_horas")
    val totalHoras: Double = 0.0,
    @SerializedName("total_monto")
    val totalMonto: Int = 0
)
