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
    val fechaEvento: String = "",
    @SerializedName("repeticion")
    val repeticion: String = REPETICION_NINGUNA, // NINGUNA, DIARIA, SEMANAL, MENSUAL, ANUAL
    @SerializedName("hitos_minutos")
    val hitosMinutos: List<Int> = DEFAULT_HITOS_MINUTOS // Hasta MAX_HITOS hitos previos (minutos antes del evento)
) {
    companion object {
        const val REPETICION_NINGUNA = "NINGUNA"
        const val REPETICION_DIARIA = "DIARIA"
        const val REPETICION_SEMANAL = "SEMANAL"
        const val REPETICION_MENSUAL = "MENSUAL"
        const val REPETICION_ANUAL = "ANUAL"
        val REPETICIONES_VALIDAS = listOf(
            REPETICION_NINGUNA, REPETICION_DIARIA, REPETICION_SEMANAL, REPETICION_MENSUAL, REPETICION_ANUAL
        )
        const val MAX_HITOS = 5
        val DEFAULT_HITOS_MINUTOS = listOf(1440)

        /** Normaliza repetición desconocida/nula (p.ej. respuesta de backend antiguo) a NINGUNA. */
        fun normalizarRepeticion(valor: String?): String {
            val v = valor?.trim()?.uppercase().orEmpty()
            return if (v in REPETICIONES_VALIDAS) v else REPETICION_NINGUNA
        }

        /** Hitos válidos: no negativos, sin duplicados, ordenados de mayor a menor y limitados a MAX_HITOS. */
        fun normalizarHitos(hitos: List<Int>?): List<Int> =
            (hitos ?: emptyList()).filter { it >= 0 }.distinct().sortedDescending().take(MAX_HITOS)
    }
}

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
    val estado: String = "", // "Activo", "Activa", "Simulado", "Simulada", "Saldado", "Saldada"
    @SerializedName(value = "entidad", alternate = ["Entidad"])
    val entidad: String = "",
    @SerializedName(value = "tipo", alternate = ["Tipo"])
    val tipo: String = "Deuda", // "Deuda", "Préstamo", "Tarjeta", "Gasto Fijo"
    @SerializedName(value = "cuota_referencia_gs", alternate = ["Cuota_Referencia_Gs"])
    val cuotaReferenciaGs: Long = 0L,
    @SerializedName(value = "cuotas_totales", alternate = ["Cuotas_Totales"])
    val cuotasTotales: Int = 0,
    @SerializedName(value = "cuotas_restantes", alternate = ["Cuotas_Restantes"])
    val cuotasRestantes: Int = 0,
    @SerializedName(value = "dia_vencimiento", alternate = ["Dia_Vencimiento"])
    val diaVencimiento: Int = 0,
    @SerializedName(value = "fecha_inicio", alternate = ["Fecha_Inicio"])
    val fechaInicio: String = "",
    @SerializedName(value = "observaciones", alternate = ["Observaciones"])
    val observaciones: String = ""
) {
    val esActiva: Boolean
        get() = estado.trim().lowercase() in listOf("activo", "activa") && saldoActual > 0L

    val esSaldada: Boolean
        get() = estado.trim().lowercase() in listOf("saldado", "saldada", "pagado", "pagada") || saldoActual <= 0L

    val esSimulada: Boolean
        get() = estado.trim().lowercase() in listOf("simulado", "simulada")
}
