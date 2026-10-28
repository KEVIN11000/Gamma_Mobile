package com.gamma.mobile.domain.model

import com.google.gson.annotations.SerializedName

data class AsistenciaMarcaRequest(
    @SerializedName("modo")
    val modo: String // "normal" o "directo"
)

data class AsistenciaMarcaResponse(
    @SerializedName("tipo")
    val tipo: String = "", // "entrada" o "salida"
    @SerializedName("hora")
    val hora: String = "",
    @SerializedName("mensaje")
    val mensaje: String = "",
    @SerializedName("planilla")
    val planilla: String? = null,
    @SerializedName("fila")
    val fila: Int? = null,
    @SerializedName("confirmado_en_planilla")
    val confirmadoEnPlanilla: Boolean = false
)

data class AsistenciaMateriaRequest(
    @SerializedName("materia")
    val materia: String = ""
)

data class AsistenciaMateriaResponse(
    @SerializedName("materia")
    val materia: String? = null,
    @SerializedName("hora")
    val hora: String = "",
    @SerializedName("mensaje")
    val mensaje: String = ""
)

data class PeriodosResponse(
    @SerializedName("periodo_laboral")
    val periodoLaboral: String = "",
    @SerializedName("periodo_contable")
    val periodoContable: String = "",
    @SerializedName("sincronizados")
    val sincronizados: Boolean = true
)

data class AnaliticasHorasResponse(
    @SerializedName("periodo")
    val periodo: String = "",
    @SerializedName("monto_por_hora")
    val montoPorHora: Int = 0,
    @SerializedName("dias_trabajados")
    val diasTrabajados: Int = 0,
    @SerializedName("total_horas_decimal")
    val totalHorasDecimal: Double = 0.0,
    @SerializedName("total_horas_formato")
    val totalHorasFormato: String = "0h 00m",
    @SerializedName("salario_acumulado_gs")
    val salarioAcumuladoGs: Long = 0L,
    @SerializedName("horas_perdidas_decimal")
    val horasPerdidasDecimal: Double = 0.0,
    @SerializedName("horas_extras_decimal")
    val horasExtrasDecimal: Double = 0.0,
    @SerializedName("balance_neto_decimal")
    val balanceNetoDecimal: Double = 0.0,
    @SerializedName("horas_perdidas_formato")
    val horasPerdidasFormato: String = "0h 00m",
    @SerializedName("horas_extras_formato")
    val horasExtrasFormato: String = "0h 00m",
    @SerializedName("balance_neto_formato")
    val balanceNetoFormato: String = "0h 00m",
    @SerializedName("promedio_horas_por_dia")
    val promedioHorasPorDia: Double = 0.0
)
