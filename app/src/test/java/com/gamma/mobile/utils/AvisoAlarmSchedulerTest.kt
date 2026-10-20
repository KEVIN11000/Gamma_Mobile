package com.gamma.mobile.utils

import com.gamma.mobile.domain.model.Aviso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class AvisoAlarmSchedulerTest {

    private val zoneUtc = ZoneId.of("UTC")

    @Test
    fun `parseFechaEventoToMillis parses correctly valid formats`() {
        val millis = AvisoAlarmScheduler.parseFechaEventoToMillis("10/10/2026 15:30", zoneUtc)
        assertNotNull(millis)

        val instant = Instant.ofEpochMilli(millis!!)
        assertEquals("2026-10-10T15:30:00Z", instant.toString())
    }

    @Test
    fun `calcularDisparos schedules milestones and exact event in the future`() {
        // Evento: 10/10/2026 15:00 UTC
        val aviso = Aviso(
            id = "test-1",
            titulo = "Vencimiento Tarjeta",
            fechaEvento = "10/10/2026 15:00",
            repeticion = Aviso.REPETICION_MENSUAL,
            hitosMinutos = listOf(1440, 60, 15) // 1 día, 1 hora, 15 min
        )

        // Supongamos que "ahora" es 08/10/2026 10:00 UTC (los 3 hitos + evento están en el futuro)
        val nowMillis = Instant.parse("2026-10-08T10:00:00Z").toEpochMilli()

        val disparos = AvisoAlarmScheduler.calcularDisparos(aviso, nowMillis, zoneUtc)

        // 3 hitos + 1 evento exacto = 4 disparos
        assertEquals(4, disparos.size)

        // El primer hito debe ser 1440 minutos (1 día) antes: 09/10/2026 15:00 UTC
        val primerHito = disparos.first { it.hitoMinutosPrevios == 1440 }
        assertEquals(Instant.parse("2026-10-09T15:00:00Z").toEpochMilli(), primerHito.triggerMillis)

        // El último disparo es a la hora exacta del evento (0 min antes)
        val eventoDisparo = disparos.first { it.hitoMinutosPrevios == 0 }
        assertEquals(Instant.parse("2026-10-10T15:00:00Z").toEpochMilli(), eventoDisparo.triggerMillis)
    }

    @Test
    fun `calcularDisparos omits milestones that already elapsed in the past`() {
        val aviso = Aviso(
            id = "test-2",
            titulo = "Reunión",
            fechaEvento = "10/10/2026 15:00",
            hitosMinutos = listOf(1440, 60, 15)
        )

        // Supongamos que "ahora" es 10/10/2026 14:10 UTC (el de 1440m y el de 60m ya pasaron)
        val nowMillis = Instant.parse("2026-10-10T14:10:00Z").toEpochMilli()

        val disparos = AvisoAlarmScheduler.calcularDisparos(aviso, nowMillis, zoneUtc)

        // Solo deben quedar el de 15 min (a las 14:45) y el de la hora exacta (a las 15:00)
        assertEquals(2, disparos.size)
        assertTrue(disparos.any { it.hitoMinutosPrevios == 15 })
        assertTrue(disparos.any { it.hitoMinutosPrevios == 0 })
    }

    @Test
    fun `calcularSiguienteRepeticion calculates next dates correctly`() {
        val baseFecha = "10/10/2026 09:00"

        val diaria = AvisoAlarmScheduler.calcularSiguienteRepeticion(baseFecha, Aviso.REPETICION_DIARIA, zoneUtc)
        assertEquals("11/10/2026 09:00", diaria)

        val semanal = AvisoAlarmScheduler.calcularSiguienteRepeticion(baseFecha, Aviso.REPETICION_SEMANAL, zoneUtc)
        assertEquals("17/10/2026 09:00", semanal)

        val mensual = AvisoAlarmScheduler.calcularSiguienteRepeticion(baseFecha, Aviso.REPETICION_MENSUAL, zoneUtc)
        assertEquals("10/11/2026 09:00", mensual)

        val anual = AvisoAlarmScheduler.calcularSiguienteRepeticion(baseFecha, Aviso.REPETICION_ANUAL, zoneUtc)
        assertEquals("10/10/2027 09:00", anual)

        val ninguna = AvisoAlarmScheduler.calcularSiguienteRepeticion(baseFecha, Aviso.REPETICION_NINGUNA, zoneUtc)
        assertNull(ninguna)
    }
}
