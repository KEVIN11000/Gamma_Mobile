# Plan de Arquitectura y Diseño Técnico: Integración Global Backend v2.0.0 (v0.0.13)

- **ID de Característica:** `v013-backend-v2-integracion-global`
- **Fase SDD:** 0 (Especificación y Arquitectura)
- **Fecha:** 2026-11-09

---

## 1. Contratos de Datos y DTOs en Kotlin

### 1.1 Modelos para Hidratación Atómica (`GET /api/v1/bootstrap`)
```kotlin
package com.gamma.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.gamma.mobile.domain.model.*

data class BootstrapResponseDto(
    @SerializedName("status") val status: String,
    @SerializedName("health") val health: HealthStatusDto?,
    @SerializedName("summary") val summary: DashboardSummaryDto?,
    @SerializedName("proximos_avisos") val proximosAvisos: List<AvisoDto> = emptyList(),
    @SerializedName("proximos_vencimientos") val proximosVencimientos: List<VencimientoCronogramaItemDto> = emptyList(),
    @SerializedName("periodo_actual") val periodoActual: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)
```

### 1.2 Modelos para Flujo de Caja Proyectado a 30 Días (`GET /api/v1/proyecciones/flujo-caja`)
```kotlin
package com.gamma.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FlujoCajaResponseDto(
    @SerializedName("saldo_actual") val saldoActual: Long,
    @SerializedName("saldo_proyectado_30d") val saldoProyectado30d: Long,
    @SerializedName("total_ingresos_proyectados") val totalIngresosProyectados: Long,
    @SerializedName("total_gastos_fijos") val totalGastosFijos: Long,
    @SerializedName("total_gastos_variables_estimados") val totalGastosVariablesEstimados: Long,
    @SerializedName("dias_criticos") val diasCriticos: List<DiaCriticoDto> = emptyList(),
    @SerializedName("dias") val dias: List<FlujoCajaItemDiarioDto> = emptyList()
)

data class FlujoCajaItemDiarioDto(
    @SerializedName("fecha") val fecha: String,
    @SerializedName("saldo_inicial") val saldoInicial: Long,
    @SerializedName("ingresos") val ingresos: Long,
    @SerializedName("egresos_fijos") val egresosFijos: Long,
    @SerializedName("egresos_variables") val egresosVariables: Long,
    @SerializedName("saldo_final") val saldoFinal: Long,
    @SerializedName("compromisos") val compromisos: List<String> = emptyList()
)

data class DiaCriticoDto(
    @SerializedName("fecha") val fecha: String,
    @SerializedName("saldo") val saldo: Long,
    @SerializedName("motivo") val motivo: String
)
```

### 1.3 Modelos para Parser Conversacional NLP Determinista (`POST /api/v1/movimientos/parse`)
```kotlin
package com.gamma.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ParseMovementRequestDto(
    @SerializedName("texto") val texto: String
)

data class ParsedMovementDto(
    @SerializedName("valido") val valido: Boolean,
    @SerializedName("monto") val monto: Long,
    @SerializedName("moneda") val moneda: String = "PYG",
    @SerializedName("tipo_movimiento") val tipoMovimiento: String,
    @SerializedName("categoria") val categoria: String,
    @SerializedName("proveedor_cliente") val proveedorCliente: String,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("confianza") val confianza: Double = 1.0
)
```

### 1.4 Modelos para Clima y Alerta de Paraguas (`GET /api/v1/clima`)
```kotlin
package com.gamma.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ClimaResponseDto(
    @SerializedName("temperatura_actual") val temperaturaActual: Double,
    @SerializedName("temperatura_min") val temperaturaMin: Double,
    @SerializedName("temperatura_max") val temperaturaMax: Double,
    @SerializedName("condicion") val condicion: String,
    @SerializedName("alerta_paraguas") val alertaParaguas: ClimaAlertaParaguasDto,
    @SerializedName("actualizado_at") val actualizadoAt: String
)

data class ClimaAlertaParaguasDto(
    @SerializedName("activar") val activar: Boolean,
    @SerializedName("probabilidad_lluvia") val probabilidadLluvia: Int,
    @SerializedName("motivo") val motivo: String
)
```

### 1.5 Modelos para Dispositivos Push y Consulta RUC SIFEN
```kotlin
package com.gamma.mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DispositivoCreateDto(
    @SerializedName("device_token") val deviceToken: String,
    @SerializedName("platform") val platform: String = "android",
    @SerializedName("alias_usuario") val aliasUsuario: String = "default"
)

data class DispositivoResponseDto(
    @SerializedName("status") val status: String,
    @SerializedName("registrado") val registrado: Boolean,
    @SerializedName("dispositivos_activos") val dispositivosActivos: Int = 1
)

data class SifenRucRequestDto(
    @SerializedName("ruc") val ruc: String
)

data class SifenRucResponseDto(
    @SerializedName("ruc") val ruc: String,
    @SerializedName("razon_social") val razonSocial: String,
    @SerializedName("estado") val estado: String? = "ACTIVO"
)
```

---

## 2. Capa de Red: Extensión de `GammaApi.kt` e Interceptores

### 2.1 Nuevas declaraciones en `GammaApi.kt`
```kotlin
// Bootstrap
@GET("bootstrap")
suspend fun getBootstrap(): Response<BootstrapResponseDto>

// Proyecciones
@GET("proyecciones/flujo-caja")
suspend fun getFlujoCaja(
    @Query("saldo_inicial") saldoInicial: Long? = null,
    @Query("gasto_diario") gastoDiario: Long? = null,
    @Query("umbral_reserva") umbralReserva: Long? = null
): Response<FlujoCajaResponseDto>

// Parser NLP
@POST("movimientos/parse")
suspend fun parseMovimientoNlp(
    @Body request: ParseMovementRequestDto
): Response<ParsedMovementDto>

// Clima
@GET("clima")
suspend fun getClima(): Response<ClimaResponseDto>

// Dispositivos Push
@POST("dispositivos/registrar")
suspend fun registrarDispositivo(
    @Body request: DispositivoCreateDto
): Response<DispositivoResponseDto>

// SIFEN RUC
@POST("sifen/consultar-ruc")
suspend fun consultarRucSifen(
    @Body request: SifenRucRequestDto
): Response<SifenRucResponseDto>

// Sincronización Diferencial
@GET("movimientos")
suspend fun getMovimientosDelta(
    @Query("since") since: String
): Response<List<Movimiento>>
```

### 2.2 Interceptores OkHttp de Resiliencia

1. **`IdempotencyInterceptor.kt`:**
   * Detecta peticiones HTTP mutantes (`POST`, `PUT`, `DELETE`).
   * Si no se incluye cabecera `X-Client-Id`, genera un `UUID.randomUUID().toString()` y lo inyecta para prevenir duplicaciones de transacción ante reintentos automáticos de red.

2. **`RateLimitInterceptor.kt`:**
   * Intercepta respuestas con código `HTTP 429 Too Many Requests`.
   * Lee la cabecera `Retry-After`. Si está presente (número entero de segundos), registra un log estructurado en `AppLogBuffer` y emite una señal de espera al `NetworkResult.Error`.

3. **`PayloadSizeGuardInterceptor.kt`:**
   * Verifica que las peticiones multipart/adjuntos no excedan $64\text{ KB}$ antes de saturar el enlace móvil.

---

## 3. Capa de Persistencia Local (Room Database v5)

Para mantener la resiliencia offline-first sin *destructive loading*:
1. **Entidad `FlujoCajaEntity`:** Cachea el último resumen de flujo de caja y la serie temporal en JSON (`TypeConverter`).
2. **Entidad `ClimaEntity`:** Cachea la temperatura y estado de lluvia con timestamp de última actualización.
3. Migración de base de datos a `RoomDatabase v5` con `fallbackToDestructiveMigrationOnDowngrade` o migración manual limpia para tablas de cache.

---

## 4. Capa de Dominio y Casos de Uso

* `GetBootstrapDataUseCase`: Ejecuta `getBootstrap()`, hidrata Room (`DashboardSummary`, `Aviso`, `Obligacion`) en una única transacción atómica y emite estado unificado.
* `GetFlujoCajaUseCase`: Consulta Room primero y actualiza desde `GET proyecciones/flujo-caja`.
* `ParseMovimientoNlpUseCase`: Invoca `POST movimientos/parse` y retorna `ParsedMovement`.
* `GetClimaUseCase`: Consulta `GET clima` y entrega el estado meteorológico y alerta de paraguas.
* `ConsultarRucSifenUseCase`: Invoca `POST sifen/consultar-ruc` para autocompletar razón social.

---

## 5. Capa de Presentación (Jetpack Compose)

1. **`DashboardScreen`:**
   * Cabecera enriquecida: Badge de clima con chip condicional de paraguas (*"Llevar paraguas: 60% prob."* con icono `Umbrella`).
   * Nueva tarjeta *"Flujo de Caja Proyectado a 30 Días"*: Muestra saldo actual, saldo proyectado a fin de mes y semáforo visual con alerta si hay días críticos.
2. **`NuevaTransaccionScreen`:**
   * Incorporación de botón rápido *"Parseo Rápido"* (NLP) junto al botón de *"Gemini IA"*, permitiendo alternar entre el parser ultrarrápido determinista ($< 5\text{ ms}$) y el parser LLM exhaustivo.
3. **Escáner QR KuDE:**
   * Al leer el RUC del comprobante electrónico, invoca transparentemente `ConsultarRucSifenUseCase` para precargar el nombre comercial del proveedor en pantalla.

---

## 6. Estrategia de Testing y Verificación Empírica

* **Tests de DTOs y Mappers:** Serialización y deserialización Gson con payloads reales de Backend v2.0.0.
* **Tests de Interceptores:** `IdempotencyInterceptorTest` y `RateLimitInterceptorTest` con `MockWebServer`.
* **Tests de ViewModels:**
  * `DashboardViewModelTest`: Verificar hidratación exitosa vía Bootstrap y fallback a Room ante error de red.
  * `TransaccionesViewModelTest`: Validar parseo NLP determinista y asignación de campos.
* **Comprobación de Suites:** Ejecución obligatoria de `./gradlew testDebugUnitTest` garantizando que los 220 tests existentes sigan en verde y sumen los nuevos tests.
