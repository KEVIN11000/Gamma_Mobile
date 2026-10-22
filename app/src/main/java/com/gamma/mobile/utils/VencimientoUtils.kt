package com.gamma.mobile.utils

import com.gamma.mobile.domain.model.Obligacion
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class EstadoVencimientoInfo(
    val texto: String,
    val esVencido: Boolean,
    val esHoy: Boolean = false,
    val esProximo: Boolean = false
)

object VencimientoUtils {

    fun calcularEstadoVencimiento(
        obligacion: Obligacion,
        calendar: Calendar = Calendar.getInstance()
    ): EstadoVencimientoInfo? {
        if (obligacion.diaVencimiento <= 0 && obligacion.fechaInicio.isBlank()) {
            return null
        }

        val diaActual = calendar.get(Calendar.DAY_OF_MONTH)
        val mesActual = calendar.get(Calendar.MONTH) + 1
        val anioActual = calendar.get(Calendar.YEAR)

        // Si la obligación tiene fecha de inicio futura comprobable, no se considera vencida este mes
        if (obligacion.fechaInicio.isNotBlank()) {
            val fechaInicioDate = parsearFecha(obligacion.fechaInicio)
            if (fechaInicioDate != null) {
                val calInicio = Calendar.getInstance().apply { time = fechaInicioDate }
                val anioInicio = calInicio.get(Calendar.YEAR)
                val mesInicio = calInicio.get(Calendar.MONTH) + 1

                if (anioInicio > anioActual || (anioInicio == anioActual && mesInicio > mesActual)) {
                    return EstadoVencimientoInfo(
                        texto = "1° Venc.: ${obligacion.fechaInicio}",
                        esVencido = false,
                        esHoy = false,
                        esProximo = false
                    )
                }
            }
        }

        if (obligacion.diaVencimiento <= 0) {
            return null
        }

        val fechaMesStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", obligacion.diaVencimiento, mesActual, anioActual)

        if (obligacion.esSaldada) {
            return EstadoVencimientoInfo(
                texto = "Vencía el $fechaMesStr",
                esVencido = false,
                esHoy = false,
                esProximo = false
            )
        }

        if (obligacion.esPausada) {
            return EstadoVencimientoInfo(
                texto = if (diaActual > obligacion.diaVencimiento) "Pausada (vencía el $fechaMesStr)" else "Pausada (vence el $fechaMesStr)",
                esVencido = false,
                esHoy = false,
                esProximo = false
            )
        }

        return when {
            diaActual > obligacion.diaVencimiento -> {
                EstadoVencimientoInfo(
                    texto = "Venció el $fechaMesStr",
                    esVencido = true,
                    esHoy = false,
                    esProximo = false
                )
            }
            diaActual == obligacion.diaVencimiento -> {
                EstadoVencimientoInfo(
                    texto = "Vence hoy ($fechaMesStr)",
                    esVencido = false,
                    esHoy = true,
                    esProximo = true
                )
            }
            else -> {
                val diasRestantes = obligacion.diaVencimiento - diaActual
                EstadoVencimientoInfo(
                    texto = "Vence el $fechaMesStr",
                    esVencido = false,
                    esHoy = false,
                    esProximo = diasRestantes <= 7
                )
            }
        }
    }

    private fun parsearFecha(fechaStr: String): java.util.Date? {
        val formatos = listOf("dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd")
        for (formato in formatos) {
            try {
                val sdf = SimpleDateFormat(formato, Locale.getDefault())
                sdf.isLenient = false
                return sdf.parse(fechaStr.trim())
            } catch (_: Exception) {}
        }
        return null
    }
}

fun Obligacion.obtenerEstadoVencimiento(calendar: Calendar = Calendar.getInstance()): EstadoVencimientoInfo? =
    VencimientoUtils.calcularEstadoVencimiento(this, calendar)
