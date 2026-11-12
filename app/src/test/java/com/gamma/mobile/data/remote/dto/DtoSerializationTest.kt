package com.gamma.mobile.data.remote.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoSerializationTest {

    private val gson = Gson()

    @Test
    fun testBootstrapResponseDtoDeserialization() {
        val json = """
            {
                "status": "ok",
                "health": {
                    "status": "ok",
                    "sheets": true,
                    "avisos": true,
                    "version_api": "2.0.0"
                },
                "summary": {
                    "margen_libre": 3500000,
                    "saldo_acumulado": 1200000,
                    "posicion_iva": 150000,
                    "es_debito_fiscal": false
                },
                "proximos_avisos": [
                    {
                        "id": "aviso-1",
                        "titulo": "Pago IPS",
                        "fecha_evento": "15/11/2026 09:00"
                    }
                ],
                "proximos_vencimientos": [
                    {
                        "id": "deuda-1",
                        "nombre": "Préstamo BNF",
                        "cuota_gs": 450000,
                        "saldo_actual": 4500000,
                        "dias_restantes": 5
                    }
                ],
                "periodo_actual": "Noviembre 2026",
                "timestamp": "2026-11-09T10:00:00"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, BootstrapResponseDto::class.java)
        assertEquals("ok", dto.status)
        assertNotNull(dto.health)
        assertEquals("2.0.0", dto.health?.versionApi)
        assertNotNull(dto.summary)
        assertEquals(3500000L, dto.summary?.margenLibreDisponible)
        assertEquals(1, dto.proximosAvisos.size)
        assertEquals("Pago IPS", dto.proximosAvisos[0].titulo)
        assertEquals(1, dto.proximosVencimientos.size)
        assertEquals("Préstamo BNF", dto.proximosVencimientos[0].nombre)
        assertEquals("Noviembre 2026", dto.periodoActual)
    }

    @Test
    fun testFlujoCajaResponseDtoDeserialization() {
        val json = """
            {
                "saldo_actual": 2500000,
                "saldo_proyectado_30d": 1800000,
                "total_ingresos_proyectados": 8000000,
                "total_gastos_fijos": 4500000,
                "total_gastos_variables_estimados": 4200000,
                "dias_criticos": [
                    {
                        "fecha": "2026-11-20",
                        "saldo": -150000,
                        "motivo": "Cuota BNF + IPS"
                    }
                ],
                "dias": [
                    {
                        "fecha": "2026-11-10",
                        "saldo_inicial": 2500000,
                        "ingresos": 0,
                        "egresos_fijos": 500000,
                        "egresos_variables": 100000,
                        "saldo_final": 1900000,
                        "compromisos": ["Alquiler"]
                    }
                ]
            }
        """.trimIndent()

        val dto = gson.fromJson(json, FlujoCajaResponseDto::class.java)
        assertEquals(2500000L, dto.saldoActual)
        assertEquals(1800000L, dto.saldoProyectado30d)
        assertEquals(1, dto.diasCriticos.size)
        assertEquals(-150000L, dto.diasCriticos[0].saldo)
        assertEquals(1, dto.dias.size)
        assertEquals("2026-11-10", dto.dias[0].fecha)
        assertEquals(1900000L, dto.dias[0].saldoFinal)
    }

    @Test
    fun testParsedMovementDtoDeserialization() {
        val json = """
            {
                "valido": true,
                "monto": 45000,
                "moneda": "PYG",
                "tipo_movimiento": "Gasto",
                "categoria": "Combustible",
                "proveedor_cliente": "Shell",
                "descripcion": "Nafta súper",
                "confianza": 0.98
            }
        """.trimIndent()

        val dto = gson.fromJson(json, ParsedMovementDto::class.java)
        assertTrue(dto.valido)
        assertEquals(45000L, dto.monto)
        assertEquals("Gasto", dto.tipoMovimiento)
        assertEquals("Combustible", dto.categoria)
        assertEquals("Shell", dto.proveedorCliente)
        assertEquals(0.98, dto.confianza, 0.001)

        val request = ParseMovementRequestDto(texto = "Gasto 45.000 combustible en Shell")
        val reqJson = gson.toJson(request)
        assertTrue(reqJson.contains("Gasto 45.000 combustible en Shell"))
    }

    @Test
    fun testClimaResponseDtoDeserialization() {
        val json = """
            {
                "temperatura_actual": 27.5,
                "temperatura_min": 21.0,
                "temperatura_max": 33.0,
                "condicion": "Tormentas dispersas",
                "alerta_paraguas": {
                    "activar": true,
                    "probabilidad_lluvia": 70,
                    "motivo": "Alta probabilidad de tormenta por la tarde"
                },
                "actualizado_at": "2026-11-09 10:00"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, ClimaResponseDto::class.java)
        assertEquals(27.5, dto.temperaturaActual, 0.01)
        assertEquals("Tormentas dispersas", dto.condicion)
        assertTrue(dto.alertaParaguas.activar)
        assertEquals(70, dto.alertaParaguas.probabilidadLluvia)
    }

    @Test
    fun testSifenDtoDeserialization() {
        val json = """
            {
                "ruc": "80012345-6",
                "razon_social": "Supermercados Stock S.A.",
                "estado": "ACTIVO"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, SifenRucResponseDto::class.java)
        assertEquals("80012345-6", dto.ruc)
        assertEquals("Supermercados Stock S.A.", dto.razonSocial)
        assertEquals("ACTIVO", dto.estado)
    }

    @Test
    fun testDispositivoDtoSerialization() {
        val request = DispositivoCreateDto(
            deviceToken = "fcm-token-test-12345",
            platform = "android",
            aliasUsuario = "Kevin"
        )
        val json = gson.toJson(request)
        assertTrue(json.contains("fcm-token-test-12345"))
        assertTrue(json.contains("device_token"))

        val responseJson = """
            {
                "status": "ok",
                "registrado": true,
                "dispositivos_activos": 2
            }
        """.trimIndent()
        val responseDto = gson.fromJson(responseJson, DispositivoResponseDto::class.java)
        assertEquals("ok", responseDto.status)
        assertTrue(responseDto.registrado)
        assertEquals(2, responseDto.dispositivosActivos)
    }

    @Test
    fun testBootstrapResponseDtoWithResumenFinancieroKey() {
        val json = """
            {
                "status": "ok",
                "resumen_financiero": {
                    "margen_libre": 4200000,
                    "saldo_acumulado": 5100000,
                    "posicion_iva": 250000,
                    "es_debito_fiscal": true
                },
                "periodo_actual": "Noviembre 2026"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, BootstrapResponseDto::class.java)
        assertEquals("ok", dto.status)
        assertNotNull(dto.summary)
        assertEquals(4200000L, dto.summary?.margenLibreDisponible)
        assertEquals(5100000L, dto.summary?.saldoAcumulado)
        assertEquals(250000L, dto.summary?.posicionIva)
        assertEquals(true, dto.summary?.esDebitoFiscal)
    }

    @Test
    fun testCapacidadesResponseDtoDeserialization() {
        val json = """
            {
                "version_api": "2.1.4",
                "total_rutas": 42,
                "modulos": ["presupuesto", "obligaciones", "asistencia"],
                "capacidades_clave": {
                    "presupuesto_fijos_delete": true,
                    "presupuesto_techos_put": true,
                    "asistencia_excel_export": true
                },
                "rutas": [
                    {
                        "modulo": "presupuesto",
                        "ruta": "/api/v1/presupuesto/fijos",
                        "metodos": ["GET", "POST"],
                        "nombre_endpoint": "presupuesto_v1.get_gastos_fijos",
                        "descripcion": "Retorna el salario base y el desglose de gastos fijos por categoría.",
                        "autenticacion": true,
                        "parametros": []
                    }
                ]
            }
        """.trimIndent()

        val dto = gson.fromJson(json, CapacidadesResponseDto::class.java)
        assertEquals("2.1.4", dto.versionApi)
        assertEquals(42, dto.totalRutas)
        assertEquals(3, dto.modulos.size)
        assertTrue(dto.capacidadesClave["presupuesto_fijos_delete"] == true)
        assertEquals(1, dto.rutas.size)
        assertEquals("presupuesto", dto.rutas[0].modulo)
        assertEquals(listOf("GET", "POST"), dto.rutas[0].metodos)
        assertEquals("presupuesto_v1.get_gastos_fijos", dto.rutas[0].nombreEndpoint)
    }

    @Test
    fun testPresupuestoFijosResponseDtoDeserialization() {
        val json = """
            {
                "salario_base": 7500000,
                "total_gastos_fijos": 2300000,
                "categorias": {
                    "Vivienda": [
                        {
                            "concepto": "Alquiler",
                            "monto": 1500000,
                            "concepto_id": "Alquiler"
                        }
                    ],
                    "Servicios": [
                        {
                            "concepto": "Internet Fibra",
                            "monto": 180000
                        }
                    ]
                }
            }
        """.trimIndent()

        val dto = gson.fromJson(json, PresupuestoFijosResponseDto::class.java)
        assertEquals(7500000L, dto.salarioBase)
        assertEquals(2300000L, dto.totalGastosFijos)
        assertEquals(2, dto.categorias.size)
        assertEquals(1, dto.categorias["Vivienda"]?.size)
        assertEquals("Alquiler", dto.categorias["Vivienda"]?.first()?.concepto)
        assertEquals(1500000L, dto.categorias["Vivienda"]?.first()?.monto)
    }

    @Test
    fun testTechoPresupuestarioItemDtoDeserialization() {
        val json = """
            {
                "categoria": "Supermercado",
                "limite_mensual": 1200000,
                "consumido": 950000,
                "porcentaje": 79.2,
                "umbral_alerta": 80,
                "alerta_emitida": false,
                "estado": "NORMAL",
                "mensaje": "Consumo dentro de lo proyectado"
            }
        """.trimIndent()

        val dto = gson.fromJson(json, TechoPresupuestarioItemDto::class.java)
        assertEquals("Supermercado", dto.categoria)
        assertEquals(1200000L, dto.limiteMensual)
        assertEquals(950000L, dto.consumido)
        assertEquals(79.2, dto.porcentaje, 0.01)
        assertEquals(80, dto.umbralAlerta)
        assertEquals(false, dto.alertaEmitida)
        assertEquals("NORMAL", dto.estado)
    }

    @Test
    fun testObligacionUpdateDtoSerialization() {
        val updateDto = ObligacionUpdateDto(
            nombre = "Préstamo GNB Refinanciado",
            saldoActual = 14500000L,
            cuotasRestantes = 20,
            cuotaReferenciaGs = 890000L,
            estado = "Activo",
            esGastoFijo = true
        )
        val json = gson.toJson(updateDto)
        assertTrue(json.contains("Préstamo GNB Refinanciado"))
        assertTrue(json.contains("\"saldo_actual\":14500000"))
        assertTrue(json.contains("\"cuotas_restantes\":20"))
        assertTrue(json.contains("\"cuota_referencia_gs\":890000"))
        assertTrue(json.contains("\"estado\":\"Activo\""))
        assertTrue(json.contains("\"es_gasto_fijo\":true"))
    }
}
