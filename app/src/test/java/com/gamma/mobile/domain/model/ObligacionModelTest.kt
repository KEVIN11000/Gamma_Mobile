package com.gamma.mobile.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObligacionModelTest {

    @Test
    fun `prestamo GNB with estado Saldada in sheet but positive balance is treated as alive and active`() {
        // Caso exacto reportado por el usuario de la planilla de Google Sheets (Fila 3)
        val prestamoGnb = Obligacion(
            id = "5c758d49",
            tipo = "Deuda",
            nombre = "Préstamo consol GNB",
            entidad = "GNB",
            montoInicial = 16421684L,
            saldoActual = 15531684L,
            estado = "Saldada", // Erróneamente ingresado en la hoja
            cuotaReferenciaGs = 890000L,
            cuotasTotales = 24,
            cuotasRestantes = 23,
            diaVencimiento = 0,
            observaciones = "Entidad: GNB"
        )

        // 1. Invariante: Nunca debe considerarse definitivamente saldada si tiene saldo positivo
        assertFalse("No debe ser definitivamente saldada si saldoActual > 0", prestamoGnb.esDefinitivamenteSaldada)
        assertTrue("Debe clasificarse como activa", prestamoGnb.esActiva)
        assertFalse("No debe ser saldada", prestamoGnb.esSaldada)
        assertEquals("Activo", prestamoGnb.estadoReal)

        // 2. Saldos y montos
        assertEquals(15531684L, prestamoGnb.saldoReal)
        assertEquals(16421684L, prestamoGnb.montoInicialReal)
        assertEquals(890000L, prestamoGnb.cuotaReal)
        assertEquals(890000L, prestamoGnb.montoAbonadoReal)

        // 3. Cuotas pagadas y restantes
        assertEquals(1, prestamoGnb.cuotasPagadasReal)
        assertEquals(23, prestamoGnb.cuotasRestantesReal)
    }

    @Test
    fun `debt truly saldada with zero balance and estado saldada returns saldada`() {
        val deudaSaldada = Obligacion(
            id = "deuda-fin",
            tipo = "Deuda",
            nombre = "Deuda Cancelada Don Antonio",
            entidad = "Don Antonio",
            montoInicial = 140000L,
            saldoActual = 0L,
            estado = "Saldada",
            cuotaReferenciaGs = 0L,
            cuotasTotales = 2,
            cuotasRestantes = 0
        )

        assertTrue(deudaSaldada.esDefinitivamenteSaldada)
        assertTrue(deudaSaldada.esSaldada)
        assertFalse(deudaSaldada.esActiva)
        assertEquals("Saldada", deudaSaldada.estadoReal)
        assertEquals(0L, deudaSaldada.saldoReal)
        assertEquals(140000L, deudaSaldada.montoAbonadoReal)
        assertEquals(2, deudaSaldada.cuotasPagadasReal)
        assertEquals(0, deudaSaldada.cuotasRestantesReal)
    }

    @Test
    fun `debt with shifted cuota in estado retains active status and computes cuota properly`() {
        // Caso Kari (cuota desplazada en columna estado)
        val tcKari = Obligacion(
            id = "tc-kari",
            nombre = "TC Kari",
            entidad = "TC",
            estado = "400.000",
            cuotaReferenciaGs = 30092026L,
            saldoActual = 0L,
            montoInicial = 0L,
            cuotasTotales = 1,
            cuotasRestantes = 1
        )

        assertTrue(tcKari.esActiva)
        assertFalse(tcKari.esDefinitivamenteSaldada)
        assertEquals("Activo", tcKari.estadoReal)
        assertEquals(400000L, tcKari.cuotaReal)
        assertEquals(30092026L, tcKari.saldoReal)
    }

    @Test
    fun `debt paused is correctly categorized with positive balance`() {
        val deudaPausada = Obligacion(
            id = "deuda-pausa",
            nombre = "Cuota Amigo",
            entidad = "Amigo",
            montoInicial = 500000L,
            saldoActual = 300000L,
            estado = "Pausada",
            cuotaReferenciaGs = 100000L,
            cuotasTotales = 5,
            cuotasRestantes = 3
        )

        assertTrue(deudaPausada.esPausada)
        assertFalse(deudaPausada.esActiva)
        assertEquals("Pausada", deudaPausada.estadoReal)
        assertEquals(300000L, deudaPausada.saldoReal)
        assertEquals(200000L, deudaPausada.montoAbonadoReal)
        assertEquals(2, deudaPausada.cuotasPagadasReal)
        assertEquals(3, deudaPausada.cuotasRestantesReal)
    }
}
