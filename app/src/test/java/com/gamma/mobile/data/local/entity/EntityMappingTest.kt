package com.gamma.mobile.data.local.entity

import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.Obligacion
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityMappingTest {

    @Test
    fun aviso_roundTrip_preservesRepeticionAndHitos() {
        val aviso = Aviso(
            id = "a1",
            titulo = "Pagar ANDE",
            fechaEvento = "25/10/2026 09:00",
            repeticion = Aviso.REPETICION_MENSUAL,
            hitosMinutos = listOf(1440, 180, 60)
        )

        val back = aviso.toEntity().toDomain()

        assertEquals(aviso, back)
    }

    @Test
    fun aviso_toEntity_limitsHitosToFiveSortedDescendingWithoutDuplicates() {
        val aviso = Aviso(id = "a2", hitosMinutos = listOf(10, 60, 60, 1440, 30, 180, 4320, 5))

        val hitos = aviso.toEntity().toDomain().hitosMinutos

        assertEquals(listOf(4320, 1440, 180, 60, 30), hitos)
        assertEquals(Aviso.MAX_HITOS, hitos.size)
    }

    @Test
    fun aviso_unknownRepeticion_isNormalizedToNinguna() {
        val entity = AvisoEntity(id = "a3", titulo = "x", fechaEvento = "", repeticion = "QUINCENAL", hitosMinutosCsv = "")

        val domain = entity.toDomain()

        assertEquals(Aviso.REPETICION_NINGUNA, domain.repeticion)
        assertTrue(domain.hitosMinutos.isEmpty())
    }

    @Test
    fun aviso_corruptedCsv_ignoresInvalidTokens() {
        val entity = AvisoEntity(id = "a4", titulo = "x", fechaEvento = "", hitosMinutosCsv = "60,abc, ,1440")

        assertEquals(listOf(1440, 60), entity.toDomain().hitosMinutos)
    }

    @Test
    fun aviso_gsonWithoutNewFields_usesDefaults() {
        val aviso = Gson().fromJson("""{"id":"1","titulo":"Viejo","fecha_evento":"01/01/2027 10:00"}""", Aviso::class.java)

        assertEquals(Aviso.REPETICION_NINGUNA, aviso.repeticion)
        assertEquals(Aviso.DEFAULT_HITOS_MINUTOS, aviso.hitosMinutos)
    }

    @Test
    fun aviso_gsonSerialization_usesSnakeCaseKeys() {
        val json = Gson().toJson(Aviso(id = "1", repeticion = Aviso.REPETICION_DIARIA, hitosMinutos = listOf(60)))

        assertTrue(json.contains("\"repeticion\":\"DIARIA\""))
        assertTrue(json.contains("\"hitos_minutos\":[60]"))
    }

    @Test
    fun obligacion_roundTrip_preservesAllFields() {
        val obligacion = Obligacion(
            id = "OBL-1",
            nombre = "Prestamo personal",
            montoInicial = 12_000_000L,
            saldoActual = 9_000_000L,
            estado = "Activo",
            entidad = "Banco Itau",
            tipo = "Préstamo",
            cuotaReferenciaGs = 1_000_000L,
            cuotasTotales = 12,
            cuotasRestantes = 9,
            diaVencimiento = 15,
            fechaInicio = "15/07/2026",
            observaciones = "Debito automatico"
        )

        assertEquals(obligacion, obligacion.toEntity().toDomain())
    }

    @Test
    fun obligacion_positionalConstructor_keepsLegacyFieldOrder() {
        val obligacion = Obligacion("1", "Deuda", 1000, 500, "Activo")

        assertEquals("Deuda", obligacion.nombre)
        assertEquals(1000L, obligacion.montoInicial)
        assertEquals(500L, obligacion.saldoActual)
        assertEquals("Activo", obligacion.estado)
        assertEquals("Deuda", obligacion.tipo)
        assertEquals(0L, obligacion.cuotaReferenciaGs)
    }

    @Test
    fun obligacion_gsonAcceptsBackendSheetKeys() {
        val json = """{"ID_Obligacion":"OBL-9","Nombre":"Tarjeta","Monto_Inicial":100,"Saldo_Actual":50,"Estado":"Activo",
            "Entidad":"Cooperativa","Cuota_Referencia_Gs":10,"Cuotas_Totales":10,"Cuotas_Restantes":5,"Dia_Vencimiento":20,
            "Fecha_Inicio":"01/01/2026","Observaciones":"obs"}"""

        val o = Gson().fromJson(json, Obligacion::class.java)

        assertEquals("OBL-9", o.id)
        assertEquals("Cooperativa", o.entidad)
        assertEquals(10L, o.cuotaReferenciaGs)
        assertEquals(10, o.cuotasTotales)
        assertEquals(5, o.cuotasRestantes)
        assertEquals(20, o.diaVencimiento)
        assertEquals("01/01/2026", o.fechaInicio)
        assertEquals("obs", o.observaciones)
    }

    @Test
    fun obligacion_esPausada_evaluatesCorrectly() {
        val oPausada = Obligacion("1", "Deuda 1", 1000, 1000, "Pausada")
        val oPausado = Obligacion("2", "Deuda 2", 1000, 1000, "Pausado")
        val oActiva = Obligacion("3", "Deuda 3", 1000, 1000, "Activo")
        val oSaldada = Obligacion("4", "Deuda 4", 1000, 0, "Pausada")

        assertTrue(oPausada.esPausada)
        assertTrue(oPausado.esPausada)
        org.junit.Assert.assertFalse(oActiva.esPausada)
        org.junit.Assert.assertFalse(oSaldada.esPausada)
    }
}
