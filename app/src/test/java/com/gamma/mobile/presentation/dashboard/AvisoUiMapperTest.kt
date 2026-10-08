package com.gamma.mobile.presentation.dashboard

import com.gamma.mobile.domain.model.Aviso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class AvisoUiMapperTest {

    private val fixedClock = Clock.fixed(Instant.parse("2026-10-06T00:00:00Z"), ZoneId.of("UTC"))

    @Test
    fun `toUiModel with past event returns null`() {
        val aviso = Aviso("1", "Pasado", "05/10/2026 23:59")
        val uiModel = aviso.toUiModel(fixedClock)
        assertNull(uiModel)
    }

    @Test
    fun `toUiModel with today event returns Hoy and esCritico`() {
        val aviso = Aviso("2", "Hoy", "06/10/2026 12:00")
        val uiModel = aviso.toUiModel(fixedClock)
        
        assertEquals(0, uiModel?.diasRestantes)
        assertEquals("Hoy", uiModel?.textoTiempo)
        assertEquals(true, uiModel?.esCritico)
    }

    @Test
    fun `toUiModel with tomorrow event returns Mañana and esCritico`() {
        val aviso = Aviso("3", "Mañana", "07/10/2026 00:00")
        val uiModel = aviso.toUiModel(fixedClock)
        
        assertEquals(1, uiModel?.diasRestantes)
        assertEquals("Mañana", uiModel?.textoTiempo)
        assertEquals(true, uiModel?.esCritico)
    }

    @Test
    fun `toUiModel exactly 7 days returns En 7 dias and not critico`() {
        val aviso = Aviso("4", "7 dias", "13/10/2026 23:59")
        val uiModel = aviso.toUiModel(fixedClock)
        
        assertEquals(7, uiModel?.diasRestantes)
        assertEquals("En 7 días", uiModel?.textoTiempo)
        assertEquals(false, uiModel?.esCritico)
    }

    @Test
    fun `filterAndSortForDashboard orders correctly and filters beyond 7 days`() {
        val avisos = listOf(
            Aviso("1", "A 8 dias", "14/10/2026 00:00"), // Should filter
            Aviso("2", "Mañana", "07/10/2026 15:00"),
            Aviso("3", "Hoy", "06/10/2026 10:00"),
            Aviso("4", "Pasado", "05/10/2026 10:00"), // Should filter
            Aviso("5", "A 7 dias", "13/10/2026 20:00")
        )

        val result = avisos.filterAndSortForDashboard(fixedClock)

        assertEquals(3, result.size)
        // Check order
        assertEquals("Hoy", result[0].titulo)
        assertEquals("Mañana", result[1].titulo)
        assertEquals("A 7 dias", result[2].titulo)
    }

    @Test
    fun `filterAndSortForDashboard with empty list returns empty`() {
        val result = emptyList<Aviso>().filterAndSortForDashboard(fixedClock)
        assertEquals(0, result.size)
    }
}
