package com.gamma.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FlujoCajaResponseDto(
    @SerializedName(value = "saldo_actual", alternate = ["saldoActual"])
    val saldoActual: Long = 0L,
    @SerializedName(value = "saldo_proyectado_30d", alternate = ["saldoProyectado30d"])
    val saldoProyectado30d: Long = 0L,
    @SerializedName(value = "total_ingresos_proyectados", alternate = ["totalIngresosProyectados"])
    val totalIngresosProyectados: Long = 0L,
    @SerializedName(value = "total_gastos_fijos", alternate = ["totalGastosFijos"])
    val totalGastosFijos: Long = 0L,
    @SerializedName(value = "total_gastos_variables_estimados", alternate = ["totalGastosVariablesEstimados"])
    val totalGastosVariablesEstimados: Long = 0L,
    @SerializedName(value = "dias_criticos", alternate = ["diasCriticos"])
    val diasCriticos: List<DiaCriticoDto> = emptyList(),
    @SerializedName(value = "dias", alternate = ["items"])
    val dias: List<FlujoCajaItemDiarioDto> = emptyList()
)

data class FlujoCajaItemDiarioDto(
    @SerializedName("fecha")
    val fecha: String = "",
    @SerializedName(value = "saldo_inicial", alternate = ["saldoInicial"])
    val saldoInicial: Long = 0L,
    @SerializedName("ingresos")
    val ingresos: Long = 0L,
    @SerializedName(value = "egresos_fijos", alternate = ["egresosFijos"])
    val egresosFijos: Long = 0L,
    @SerializedName(value = "egresos_variables", alternate = ["egresosVariables"])
    val egresosVariables: Long = 0L,
    @SerializedName(value = "saldo_final", alternate = ["saldoFinal"])
    val saldoFinal: Long = 0L,
    @SerializedName("compromisos")
    val compromisos: List<String> = emptyList()
)

data class DiaCriticoDto(
    @SerializedName("fecha")
    val fecha: String = "",
    @SerializedName("saldo")
    val saldo: Long = 0L,
    @SerializedName("motivo")
    val motivo: String = ""
)
