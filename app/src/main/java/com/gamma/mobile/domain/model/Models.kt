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
    val comprobante: String = "", // "Virtual Legal", "Física Legal", "No Legal"
    @SerializedName(value = "proveedor_cliente", alternate = ["proveedorCliente"])
    val proveedorCliente: String = "Varios",
    @SerializedName(value = "nro_factura", alternate = ["nroFactura"])
    val nroFactura: String = "S/N",
    @SerializedName(value = "tasa_iva", alternate = ["tasaIva"])
    val tasaIva: String = "10%",
    @SerializedName(value = "client_id", alternate = ["clientId"])
    val clientId: String = id
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

    val esPausada: Boolean
        get() = estado.trim().lowercase() in listOf("pausado", "pausada") && saldoActual > 0L
}

fun List<Obligacion>.calcularCompromisoMensual(): Long =
    filter { it.esActiva && !it.esPausada }.sumOf { it.cuotaReferenciaGs }

data class AbonoRequest(
    @SerializedName(value = "id_obligacion", alternate = ["idObligacion"])
    val idObligacion: String,
    @SerializedName("monto")
    val monto: Long,
    @SerializedName(value = "nro_factura", alternate = ["nroFactura"])
    val nroFactura: String = "S/N",
    @SerializedName(value = "tasa_iva", alternate = ["tasaIva"])
    val tasaIva: String = "10%"
)

data class AbonoResponse(
    @SerializedName("status")
    val status: String = "ok",
    @SerializedName(value = "id_obligacion", alternate = ["idObligacion"])
    val idObligacion: String = "",
    @SerializedName(value = "saldo_actual", alternate = ["saldoActual"])
    val saldoActual: Long = 0L,
    @SerializedName("estado")
    val estado: String = "Activo"
)

data class CierreHistorialItem(
    @SerializedName("id")
    val id: String = "",
    @SerializedName("tipo")
    val tipo: String = "contable", // "contable" o "laboral"
    @SerializedName("periodo")
    val periodo: String = "",
    @SerializedName("fecha")
    val fecha: String = "",
    @SerializedName(value = "total_monto", alternate = ["totalMonto"])
    val totalMonto: Long = 0L,
    @SerializedName(value = "total_horas", alternate = ["totalHoras"])
    val totalHoras: Double = 0.0,
    @SerializedName("resumen")
    val resumen: String = "",
    @SerializedName(value = "pdf_url", alternate = ["pdfUrl"])
    val pdfUrl: String? = null,
    @SerializedName(value = "pdf_disponible", alternate = ["pdfDisponible"])
    val pdfDisponible: Boolean = false
)

typealias Cierre = CierreHistorialItem

enum class SemaforoViabilidad {
    SUPERAVIT,
    AJUSTADO,
    DEFICIT
}

data class CapacidadPagoRequest(
    @SerializedName("deudas_pausadas_simuladas")
    val deudasPausadasSimuladas: List<String> = emptyList(),
    @SerializedName("abonos_extraordinarios")
    val abonosExtraordinarios: Map<String, Long> = emptyMap(),
    @SerializedName("ajuste_ingreso_extra")
    val ajusteIngresoExtra: Long = 0L
)

data class CapacidadPagoResponse(
    @SerializedName("compromiso_total_mes")
    val compromisoTotalMes: Long = 0L,
    @SerializedName("fondo_disponible_mes")
    val fondoDisponibleMes: Long = 0L,
    @SerializedName("total_gastos_proyectados")
    val totalGastosProyectados: Long = 0L,
    @SerializedName("capacidad_neta")
    val capacidadNeta: Long = 0L,
    @SerializedName("semaforo")
    val semaforo: String = "SUPERAVIT", // "SUPERAVIT", "AJUSTADO", "DEFICIT"
    @SerializedName("dti_proyectado")
    val dtiProyectado: Double = 0.0,
    @SerializedName("meses_ahorrados_por_abonos")
    val mesesAhorradosPorAbonos: Map<String, Int> = emptyMap()
) {
    val semaforoEnum: SemaforoViabilidad
        get() = when (semaforo.trim().uppercase()) {
            "DEFICIT" -> SemaforoViabilidad.DEFICIT
            "AJUSTADO" -> SemaforoViabilidad.AJUSTADO
            else -> SemaforoViabilidad.SUPERAVIT
        }
}

data class GastoSimulado(
    @SerializedName("id")
    val id: String = java.util.UUID.randomUUID().toString(),
    @SerializedName("concepto")
    val concepto: String = "",
    @SerializedName("monto")
    val monto: Long = 0L,
    @SerializedName("incluido")
    val incluido: Boolean = true
)

data class EscenarioCapacidad(
    val id: String = java.util.UUID.randomUUID().toString(),
    val nombre: String = "",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis(),
    val ingresoExtra: Long = 0L,
    val deudasPausadasIds: Set<String> = emptySet(),
    val abonosExtraordinarios: Map<String, Long> = emptyMap(),
    val gastosAdHoc: List<GastoSimulado> = emptyList()
)
