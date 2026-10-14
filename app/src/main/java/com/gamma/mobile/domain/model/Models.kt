package com.gamma.mobile.domain.model

import com.google.gson.annotations.SerializedName

data class DashboardSummary(
    @SerializedName("margen_libre")
    val margenLibreDisponible: Long = 0L,
    @SerializedName("saldo_acumulado")
    val saldoAcumulado: Long = 0L,
    @SerializedName("posicion_iva")
    val posicionIva: Long = 0L,
    @SerializedName("es_debito_fiscal")
    val esDebitoFiscal: Boolean = false
)

data class Aviso(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("titulo")
    val titulo: String = "",
    @SerializedName("fecha_evento")
    val fechaEvento: String = ""
)

data class Movimiento(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("fecha")
    val fecha: String = "",
    @SerializedName(value = "tipo", alternate = ["movimiento"])
    val tipo: String = "", // "Ingreso" o "Gasto"
    @SerializedName("categoria")
    val categoria: String = "",
    @SerializedName("total")
    val total: Long = 0L,
    @SerializedName("comprobante")
    val comprobante: String = "" // "Virtual Legal", "Física Legal", "No Legal"
)

data class Obligacion(
    @SerializedName(value = "id", alternate = ["ID_Obligacion"])
    val id: String = "",
    @SerializedName(value = "nombre", alternate = ["Nombre"])
    val nombre: String = "",
    @SerializedName(value = "monto_inicial", alternate = ["Monto_Inicial"])
    val montoInicial: Long = 0L,
    @SerializedName(value = "saldo_actual", alternate = ["Saldo_Actual"])
    val saldoActual: Long = 0L,
    @SerializedName(value = "estado", alternate = ["Estado"])
    val estado: String = "" // "Activo", "Simulado", "Saldado"
)
