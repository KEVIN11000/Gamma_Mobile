package com.gamma.mobile.presentation.dashboard

import com.gamma.mobile.domain.model.Aviso
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class AvisoUiModel(
    val id: String,
    val titulo: String,
    val diasRestantes: Int,
    val textoTiempo: String,
    val esCritico: Boolean // <= 1 day
)

fun Aviso.toUiModel(clock: Clock = Clock.systemDefaultZone()): AvisoUiModel? {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    val eventDate = try {
        LocalDate.parse(this.fechaEvento, formatter)
    } catch (e: Exception) {
        return null // Formato inválido
    }

    val today = LocalDate.now(clock)
    val daysBetween = ChronoUnit.DAYS.between(today, eventDate).toInt()

    if (daysBetween < 0) return null // Eventos pasados excluidos

    val textoTiempo = when (daysBetween) {
        0 -> "Hoy"
        1 -> "Mañana"
        else -> "En $daysBetween días"
    }

    return AvisoUiModel(
        id = this.id,
        titulo = this.titulo,
        diasRestantes = daysBetween,
        textoTiempo = textoTiempo,
        esCritico = daysBetween <= 1
    )
}

fun List<Aviso>.filterAndSortForDashboard(clock: Clock = Clock.systemDefaultZone()): List<AvisoUiModel> {
    return this.mapNotNull { it.toUiModel(clock) }
        .filter { it.diasRestantes <= 7 }
        .sortedBy { it.diasRestantes }
}
