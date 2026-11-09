package com.gamma.mobile.domain.model

data class FlujoCaja(
    val saldoActual: Long = 0L,
    val saldoProyectado30d: Long = 0L,
    val totalIngresosProyectados: Long = 0L,
    val totalGastosFijos: Long = 0L,
    val totalGastosVariablesEstimados: Long = 0L,
    val diasCriticos: List<DiaCritico> = emptyList(),
    val dias: List<FlujoCajaItemDiario> = emptyList()
)

data class FlujoCajaItemDiario(
    val fecha: String = "",
    val saldoInicial: Long = 0L,
    val ingresos: Long = 0L,
    val egresosFijos: Long = 0L,
    val egresosVariables: Long = 0L,
    val saldoFinal: Long = 0L,
    val compromisos: List<String> = emptyList()
)

data class DiaCritico(
    val fecha: String = "",
    val saldo: Long = 0L,
    val motivo: String = ""
)
