package com.gamma.mobile.utils

import com.gamma.mobile.domain.model.Obligacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class VencimientoUtilsTest {

    private fun crearCalendar(dia: Int, mes: Int, anio: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, anio)
            set(Calendar.MONTH, mes - 1)
            set(Calendar.DAY_OF_MONTH, dia)
        }
    }

    @Test
    fun `calcularEstadoVencimiento returns Vencio con fecha exacta cuando diaVencimiento ya paso`() {
        // Hoy: 22 de Octubre de 2026. Vencimiento: día 15
        val hoy = crearCalendar(dia = 22, mes = 10, anio = 2026)
        val obligacion = Obligacion(
            id = "1",
            nombre = "Préstamo Vencido",
            montoInicial = 5000000L,
            saldoActual = 5000000L,
            estado = "Activo",
            diaVencimiento = 15
        )

        val info = obligacion.obtenerEstadoVencimiento(hoy)

        assertNotNull(info)
        assertTrue(info!!.esVencido)
        assertFalse(info.esHoy)
        assertEquals("Venció el 15/10/2026", info.texto)
    }

    @Test
    fun `calcularEstadoVencimiento returns Vence hoy cuando diaVencimiento coincide con dia actual`() {
        // Hoy: 22 de Octubre de 2026. Vencimiento: día 22
        val hoy = crearCalendar(dia = 22, mes = 10, anio = 2026)
        val obligacion = Obligacion(
            id = "2",
            nombre = "Tarjeta Hoy",
            montoInicial = 2000000L,
            saldoActual = 2000000L,
            estado = "Activo",
            diaVencimiento = 22
        )

        val info = obligacion.obtenerEstadoVencimiento(hoy)

        assertNotNull(info)
        assertFalse(info!!.esVencido)
        assertTrue(info.esHoy)
        assertEquals("Vence hoy (22/10/2026)", info.texto)
    }

    @Test
    fun `calcularEstadoVencimiento returns Vence el con fecha futura y marca proximo dentro de 7 dias`() {
        // Hoy: 22 de Octubre de 2026. Vencimiento: día 25 (faltan 3 días)
        val hoy = crearCalendar(dia = 22, mes = 10, anio = 2026)
        val obligacion = Obligacion(
            id = "3",
            nombre = "Cuota Próxima",
            montoInicial = 3000000L,
            saldoActual = 3000000L,
            estado = "Activo",
            diaVencimiento = 25
        )

        val info = obligacion.obtenerEstadoVencimiento(hoy)

        assertNotNull(info)
        assertFalse(info!!.esVencido)
        assertTrue(info.esProximo)
        assertEquals("Vence el 25/10/2026", info.texto)
    }

    @Test
    fun `calcularEstadoVencimiento does not mark saldada debt as vencido`() {
        val hoy = crearCalendar(dia = 22, mes = 10, anio = 2026)
        val obligacion = Obligacion(
            id = "4",
            nombre = "Préstamo Saldado",
            montoInicial = 1000000L,
            saldoActual = 0L,
            estado = "Saldado",
            diaVencimiento = 10
        )

        val info = obligacion.obtenerEstadoVencimiento(hoy)

        assertNotNull(info)
        assertFalse(info!!.esVencido)
        assertEquals("Vencía el 10/10/2026", info.texto)
    }

    @Test
    fun `calcularEstadoVencimiento handles paused debt without error state`() {
        val hoy = crearCalendar(dia = 22, mes = 10, anio = 2026)
        val obligacion = Obligacion(
            id = "5",
            nombre = "Deuda Pausada",
            montoInicial = 4000000L,
            saldoActual = 2000000L,
            estado = "Pausada",
            diaVencimiento = 12
        )

        val info = obligacion.obtenerEstadoVencimiento(hoy)

        assertNotNull(info)
        assertFalse(info!!.esVencido)
        assertEquals("Pausada (vencía el 12/10/2026)", info.texto)
    }

    @Test
    fun `calcularEstadoVencimiento handles future fechaInicio without treating as expired`() {
        // Hoy: 22 de Octubre de 2026. Préstamo inicia el 15/11/2026 (mes que viene)
        val hoy = crearCalendar(dia = 22, mes = 10, anio = 2026)
        val obligacion = Obligacion(
            id = "6",
            nombre = "Préstamo Nuevo",
            montoInicial = 10000000L,
            saldoActual = 10000000L,
            estado = "Activo",
            diaVencimiento = 15,
            fechaInicio = "15/11/2026"
        )

        val info = obligacion.obtenerEstadoVencimiento(hoy)

        assertNotNull(info)
        assertFalse(info!!.esVencido)
        assertEquals("1° Venc.: 15/11/2026", info.texto)
    }

    @Test
    fun `calcularEstadoVencimiento returns null when diaVencimiento is zero and fechaInicio is blank`() {
        val obligacion = Obligacion(id = "7", nombre = "Sin Vencimiento", diaVencimiento = 0)
        assertNull(obligacion.obtenerEstadoVencimiento())
    }
}
