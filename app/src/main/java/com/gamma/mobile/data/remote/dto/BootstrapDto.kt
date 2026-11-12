package com.gamma.mobile.data.remote.dto

import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.model.HealthStatus
import com.gamma.mobile.domain.model.VencimientoCronogramaItem
import com.google.gson.annotations.SerializedName

data class BootstrapResponseDto(
    @SerializedName("status")
    val status: String = "ok",
    @SerializedName("health")
    val health: HealthStatus? = null,
    @SerializedName(value = "resumen_financiero", alternate = ["summary", "resumenFinanciero"])
    val summary: DashboardSummary? = null,
    @SerializedName(value = "proximos_avisos", alternate = ["avisos", "proximosAvisos"])
    val proximosAvisos: List<Aviso> = emptyList(),
    @SerializedName(value = "proximos_vencimientos", alternate = ["vencimientos", "proximosVencimientos"])
    val proximosVencimientos: List<VencimientoCronogramaItem> = emptyList(),
    @SerializedName(value = "periodo_actual", alternate = ["periodoActual", "periodo"])
    val periodoActual: String? = null,
    @SerializedName("timestamp")
    val timestamp: String? = null
)
