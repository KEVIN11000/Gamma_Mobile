package com.gamma.mobile.domain.model

data class DashboardSummary(
    val margenLibreDisponible: Int,
    val saldoAcumulado: Int,
    val posicionIva: Int,
    val esDebitoFiscal: Boolean
)

data class Aviso(
    val id: String = "",
    val titulo: String,
    val diasRestantes: Int
)

data class Movimiento(
    val id: String = "",
    val fecha: String,
    val tipo: String, // "Ingreso" o "Gasto"
    val categoria: String,
    val total: Int,
    val comprobante: String // "Virtual Legal", "Física Legal", "No Legal"
)

data class Obligacion(
    val id: String,
    val nombre: String,
    val montoInicial: Int,
    val saldoActual: Int,
    val estado: String // "Activo", "Simulado", "Saldado"
)
