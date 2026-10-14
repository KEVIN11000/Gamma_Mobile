package com.gamma.mobile.domain.model

import com.google.gson.annotations.SerializedName

data class DashboardSummary(
    @SerializedName("margen_libre")
    val margenLibreDisponible: Int = 0,
    @SerializedName("saldo_acumulado")
    val saldoAcumulado: Int = 0,
    @SerializedName("posicion_iva")
    val posicionIva: Int = 0,
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
    val total: Int = 0,
    @SerializedName("comprobante")
    val comprobante: String = "" // "Virtual Legal", "Física Legal", "No Legal"
)

data class Obligacion(
    @SerializedName(value = "id", alternate = ["ID_Obligacion"])
    val id: String = "",
    @SerializedName(value = "nombre", alternate = ["Nombre"])
    val nombre: String = "",
    @SerializedName(value = "monto_inicial", alternate = ["Monto_Inicial"])
    val montoInicial: Int = 0,
    @SerializedName(value = "saldo_actual", alternate = ["Saldo_Actual"])
    val saldoActual: Int = 0,
    @SerializedName(value = "estado", alternate = ["Estado"])
    val estado: String = "" // "Activo", "Simulado", "Saldado"
)
