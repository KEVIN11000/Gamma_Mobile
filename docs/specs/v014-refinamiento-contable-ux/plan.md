# Plan de Arquitectura y Contratos — Versión 0.0.14
## Auto-Descubrimiento de Capacidades, Refinamiento Contable, Presupuesto Base y UX

**ID:** `v014-refinamiento-contable-ux`  
**Fecha:** 2026-11-12  
**Estado:** Fase 0 — Especificación y Arquitectura  

---

### 1. Contratos de Datos y Esquemas de Red

#### 1.1 Contrato de Auto-Descubrimiento de Capacidades (`CapacidadesDtos.kt`)
```kotlin
package com.gamma.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CapacidadesResponseDto(
    @SerializedName("version_api") val versionApi: String = "",
    @SerializedName("total_rutas") val totalRutas: Int = 0,
    @SerializedName("modulos") val modulos: List<String> = emptyList(),
    @SerializedName("capacidades_clave") val capacidadesClave: Map<String, Boolean> = emptyMap(),
    @SerializedName("rutas") val rutas: List<RutaEndpointInfoDto> = emptyList()
)

data class RutaEndpointInfoDto(
    @SerializedName("modulo") val modulo: String = "",
    @SerializedName("ruta") val ruta: String = "",
    @SerializedName("metodos") val metodos: List<String> = emptyList(),
    @SerializedName("nombre_endpoint") val nombreEndpoint: String = "",
    @SerializedName("descripcion") val descripcion: String = "",
    @SerializedName("autenticacion") val autenticacion: Boolean = true,
    @SerializedName("parametros") val parametros: List<String> = emptyList()
)
```

#### 1.2 Contratos de Presupuesto Base (`PresupuestoDtos.kt`)
```kotlin
package com.gamma.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PresupuestoFijosResponseDto(
    @SerializedName("salario_base") val salarioBase: Long = 0L,
    @SerializedName("total_gastos_fijos") val totalGastosFijos: Long = 0L,
    @SerializedName("categorias") val categorias: Map<String, List<GastoFijoItemDto>> = emptyMap()
)

data class GastoFijoItemDto(
    @SerializedName("concepto") val concepto: String = "",
    @SerializedName("monto") val monto: Long = 0L,
    @SerializedName("concepto_id") val conceptoId: String = ""
)

data class GastoFijoCreateRequest(
    @SerializedName("categoria") val categoria: String,
    @SerializedName("concepto") val concepto: String,
    @SerializedName("monto") val monto: Long
)

data class GastoFijoUpdateRequest(
    @SerializedName("monto") val monto: Long
)

data class SalarioBaseUpdateRequest(
    @SerializedName("nuevo_salario") val nuevoSalario: Long
)

data class TechosResponseDto(
    @SerializedName("techos") val techos: List<TechoPresupuestarioItemDto> = emptyList()
)

data class TechoPresupuestarioItemDto(
    @SerializedName("categoria") val categoria: String = "",
    @SerializedName("monto_limite") val montoLimite: Long = 0L,
    @SerializedName("consumo_actual") val consumoActual: Long = 0L,
    @SerializedName("porcentaje_consumido") val porcentajeConsumido: Double = 0.0,
    @SerializedName("estado") val estado: String = "NORMAL" // NORMAL, ALERTA, EXCEDIDO
)

data class TechoUpdateRequest(
    @SerializedName("monto_limite") val montoLimite: Long,
    @SerializedName("umbral_alerta_pct") val umbralAlertaPct: Int = 80
)
```

#### 1.3 Declaraciones en `GammaApi.kt`
```kotlin
// --- Auto-Descubrimiento de API (Feature Toggles) ---
@GET("capacidades")
suspend fun getCapacidades(): Response<CapacidadesResponseDto>

// --- Presupuesto Base ---
@GET("presupuesto/fijos")
suspend fun getGastosFijos(): Response<PresupuestoFijosResponseDto>

@POST("presupuesto/fijos")
suspend fun crearGastoFijo(@Body request: GastoFijoCreateRequest): Response<Unit>

@PUT("presupuesto/fijos/{concepto}")
suspend fun actualizarGastoFijo(
    @Path("concepto") concepto: String,
    @Body request: GastoFijoUpdateRequest
): Response<Unit>

@DELETE("presupuesto/fijos/{concepto}")
suspend fun eliminarGastoFijo(@Path("concepto") concepto: String): Response<Unit>

@PUT("presupuesto/salario-base")
suspend fun actualizarSalarioBase(@Body request: SalarioBaseUpdateRequest): Response<Unit>

@GET("presupuesto/techos")
suspend fun getTechosPresupuestarios(): Response<TechosResponseDto>

@PUT("presupuesto/techos/{categoria}")
suspend fun actualizarTecho(
    @Path("categoria") categoria: String,
    @Body request: TechoUpdateRequest
): Response<Unit>

// --- Proyecciones de Flujo de Caja ---
@GET("proyecciones/flujo-caja")
suspend fun getFlujoCaja(
    @Query("saldo_inicial") saldoInicial: Long? = null,
    @Query("gasto_diario") gastoDiario: Long? = null,
    @Query("umbral_reserva") umbralReserva: Long? = null
): Response<FlujoCajaDto>

// --- Obligaciones ---
@PUT("obligaciones/{id}")
suspend fun updateObligacion(
    @Path("id") id: String,
    @Body request: ObligacionUpdateDto
): Response<Unit>
```

#### 1.4 `BootstrapResponseDto` con Alias Simétrico
```kotlin
data class BootstrapResponseDto(
    @SerializedName("status") val status: String = "ok",
    @SerializedName("health") val health: HealthStatus? = null,
    @SerializedName(value = "resumen_financiero", alternate = ["summary", "resumenFinanciero"])
    val summary: DashboardSummary? = null,
    @SerializedName(value = "proximos_avisos", alternate = ["avisos", "proximosAvisos"])
    val proximosAvisos: List<Aviso> = emptyList(),
    @SerializedName(value = "proximos_vencimientos", alternate = ["vencimientos", "proximosVencimientos"])
    val proximosVencimientos: List<VencimientoCronogramaItem> = emptyList(),
    @SerializedName(value = "periodo_actual", alternate = ["periodoActual", "periodo"])
    val periodoActual: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)
```

---

### 2. Arquitectura de Dominio y Auto-Descubrimiento

#### 2.1 Repositorio de Capacidades (`CapacidadesRepository`)
```kotlin
interface CapacidadesRepository {
    suspend fun syncCapacidades(): NetworkResult<CapacidadesResponseDto>
    fun isCapacidadHabilitada(clave: String): Boolean
    fun getCapacidadesClaveFlow(): StateFlow<Map<String, Boolean>>
}
```
- **Persistencia:** Almacena el JSON de capacidades en `SharedPreferences("gamma_cache")` para que al abrir la app sin conexión a internet los Feature Toggles permanezcan activos con los últimos valores conocidos.

#### 2.2 Blindaje Contable en `Obligacion` (`Models.kt`)
```kotlin
// INVARIANT: Si saldoActual > 0L, la deuda está viva y NUNCA se cataloga como saldada.
val esDefinitivamenteSaldada: Boolean
    get() = saldoActual <= 0L && estado.trim().lowercase() in listOf("saldado", "saldada")

val saldoReal: Long
    get() {
        if (esDefinitivamenteSaldada) return 0L
        if (saldoActual > 0L) return saldoActual
        if (cuotaReferenciaGs > 0L) return cuotaReferenciaGs
        return 0L
    }

val estadoReal: String
    get() {
        if (esDefinitivamenteSaldada) return "Saldada"
        val raw = estado.trim().lowercase()
        if (raw in listOf("pausado", "pausada")) return "Pausada"
        if (raw in listOf("simulado", "simulada")) return "Simulado"
        if (saldoReal > 0L) return "Activo"
        return "Saldada"
    }

val cuotasPagadasReal: Int
    get() = if (cuotasTotales > 0) (cuotasTotales - cuotasRestantes).coerceAtLeast(0) else 0

val montoAbonadoReal: Long
    get() = (montoInicialReal - saldoReal).coerceAtLeast(0L)
```

---

### 3. Arquitectura de Presentación (UI/UX)

#### 3.1 Módulo `PresupuestoScreen` & `PresupuestoViewModel`
- **Ubicación:** `app/src/main/java/com/gamma/mobile/presentation/presupuesto/`
- **Gobernanza Visual por Capacidades:**
  - Si `isCapacidadHabilitada("presupuesto_fijos_delete") == true`, muestra el botón de basurero para eliminar el gasto fijo.
  - Si `isCapacidadHabilitada("presupuesto_techos_put") == true`, habilita el botón de lápiz para editar el techo de la categoría.

#### 3.2 Modal de Edición de Deudas en `ObligacionesScreen`
- Diálogo `EditarObligacionDialog`: Permite actualizar Nombre, Saldo, Cuota y Estado (`Activo`, `Pausada`). Invoca `viewModel.updateObligacion(...)` con persistencia en Room y `PUT /api/v1/obligaciones/{id}`.

#### 3.3 Estandarización de Selectores y Logs Plegables
- Selectores en `DashboardScreen` y `TransaccionesScreen`: La opción activa tiene recuadro con borde primario enfocado (1.5 dp), fondo temático e icono check (`Icons.Default.Check`).
- Acordeón de logs en `SistemaScreen`: Card colapsable con estado recordado `isLogsExpanded`.

---

### 4. Inmutabilidad y Auditoría Criptográfica Oficial del Release

#### 4.1 Parámetros Oficiales del Banco de Firmas
- **Keystore oficial:** `gamma_release.jks` / variable `RELEASE_KEYSTORE_BASE64` en GitHub Actions.
- **Alias:** `gamma_release`
- **Distinguished Name (DN):**
  `CN=Gamma Mobile, OU=Release, O=Gamma, L=Asuncion, ST=Central, C=PY`
- **Huella Criptográfica SHA-256 (Inviolable):**
  `2db8d51c88ad21c640a3bffb9fe04c50603e81182f8e1b7e54b3a568a580ffe7`

#### 4.2 Procedimiento de Verificación Pre-Release
Antes de declarar la versión completada y lista para distribución:
```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\34.0.0\apksigner.bat" verify --verbose --print-certs app/build/outputs/apk/release/gamma-mobile-v0.0.14-release.apk
```
Debe comprobarse empíricamente que la salida contenga `Verifies: true` y la huella SHA-256 exacta.
