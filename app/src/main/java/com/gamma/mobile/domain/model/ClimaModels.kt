package com.gamma.mobile.domain.model

data class ClimaInfo(
    val temperaturaActual: Double = 0.0,
    val temperaturaMin: Double = 0.0,
    val temperaturaMax: Double = 0.0,
    val condicion: String = "Despejado",
    val alertaParaguas: ClimaAlertaParaguas = ClimaAlertaParaguas(),
    val actualizadoAt: String = ""
)

data class ClimaAlertaParaguas(
    val activar: Boolean = false,
    val probabilidadLluvia: Int = 0,
    val motivo: String = ""
)
