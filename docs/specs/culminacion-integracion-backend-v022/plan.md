# Plan de Arquitectura y Diseño Técnico (Plan) — Versión v0.2.2
## Módulo: Culminación de Integración Backend Core (`Gamma_bot` v0.2.0) en GammaMobile

- **Fecha:** 22 de Octubre de 2026
- **Versión de Plan:** 2.2.0
- **Fase SDD:** 0 (Especificación y Arquitectura - Modo Planificador)
- **Estado:** Pendiente de Aprobación por Usuario (Compuerta de Planificación)

---

## 1. Contratos de Datos y DTOs

### 1.1 Cronograma de Vencimientos (`VencimientoCronogramaItem`)
```kotlin
data class VencimientoCronogramaItem(
    @SerializedName(value = "id", alternate = ["id_obligacion", "ID_Obligacion"])
    val id: String = "",
    @SerializedName(value = "nombre", alternate = ["Nombre"])
    val nombre: String = "",
    @SerializedName(value = "entidad", alternate = ["Entidad"])
    val entidad: String = "",
    @SerializedName(value = "dia_vencimiento", alternate = ["diaVencimiento", "Dia_Vencimiento"])
    val diaVencimiento: Int = 0,
    @SerializedName(value = "cuota_gs", alternate = ["cuotaReferenciaGs", "cuota_referencia_gs"])
    val cuotaGs: Long = 0L,
    @SerializedName(value = "saldo_actual", alternate = ["saldoActual", "Saldo_Actual"])
    val saldoActual: Long = 0L,
    @SerializedName("vencido")
    val vencido: Boolean = false,
    @SerializedName(value = "dias_restantes", alternate = ["diasRestantes"])
    val diasRestantes: Int = 0,
    @SerializedName(value = "fecha_vencimiento_exacta", alternate = ["fechaVencimientoExacta"])
    val fechaVencimientoExacta: String = ""
)
```

### 1.2 Deuda Foco (`DeudaFocoResponse`)
```kotlin
data class DeudaFocoResponse(
    @SerializedName(value = "id_deuda", alternate = ["idDeuda", "id"])
    val idDeuda: String? = null,
    @SerializedName(value = "nombre", alternate = ["Nombre"])
    val nombre: String = "",
    @SerializedName(value = "entidad", alternate = ["Entidad"])
    val entidad: String = "",
    @SerializedName(value = "saldo_actual", alternate = ["saldoActual"])
    val saldoActual: Long = 0L,
    @SerializedName(value = "cuota_gs", alternate = ["cuotaGs", "cuota_referencia_gs"])
    val cuotaGs: Long = 0L,
    @SerializedName("criterio")
    val criterio: String = "saldo", // "saldo" (bola de nieve) o "prioridad" (avalancha)
    @SerializedName(value = "ahorro_estimado_meses", alternate = ["ahorroEstimadoMeses"])
    val ahorroEstimadoMeses: Int = 0,
    @SerializedName(value = "explicacion", alternate = ["motivo", "motivoRecomendacion"])
    val explicacion: String = ""
)
```

---

## 2. Declaraciones en `GammaApi.kt`

```kotlin
    @GET("obligaciones/vencimientos")
    suspend fun getCronogramaVencimientos(): Response<List<VencimientoCronogramaItem>>

    @GET("obligaciones/foco")
    suspend fun getDeudaFoco(
        @Query("criterio") criterio: String
    ): Response<DeudaFocoResponse>

    @POST("obligaciones/proyectar-capacidad")
    suspend fun proyectarCapacidad(
        @Body request: CapacidadPagoRequest
    ): Response<CapacidadPagoResponse>
```

---

## 3. Arquitectura y Resiliencia en Repositorio (`ObligacionRepository`)

### Métodos a añadir en `ObligacionRepository`:
1. `suspend fun getCronogramaVencimientos(forceRefresh: Boolean = false): NetworkResult<List<VencimientoCronogramaItem>>`
2. `suspend fun getDeudaFoco(criterio: String): NetworkResult<DeudaFocoResponse>`
3. `suspend fun proyectarCapacidad(request: CapacidadPagoRequest): NetworkResult<CapacidadPagoResponse>`

### Estrategia de Fallback Offline (Offline-First):
- Si la petición de `getCronogramaVencimientos()` falla por falta de conectividad o HTTP error, el repositorio recupera las obligaciones locales de `ObligacionDao`, calcula dinámicamente con `VencimientoUtils` el estado de vencimiento, días restantes y orden cronológico, retornando un `NetworkResult.Success` con datos locales.
- Si `getDeudaFoco(criterio)` falla en red, el repositorio calcula la deuda foco localmente:
  * Criterio `"saldo"` (Bola de Nieve): obligación activa de menor saldo actual mayor a cero.
  * Criterio `"prioridad"` (Avalancha): obligación activa de mayor cuota o saldo actual.

---

## 4. Diseño de Capa de Presentación (UI / Compose)

### 4.1 En `ObligacionesScreen.kt`:
- **Carrusel / Agenda de Vencimientos (`CronogramaVencimientosSection`):**
  - Fila horizontal con scroll (`LazyRow`) o acordeón con chips de fecha (`"Día 10"`, `"Día 25"`).
  - Badges visuales de alerta según proximidad:
    * 🔴 Rojo: `"Venció hace X días"`
    * 🟡 Ámbar: `"Vence hoy"` o `"En 3 días"`
    * 🟢 Azul/Verde: `"En 15 días"`
- **Tarjeta de Deuda Foco (`DeudaFocoCard`):**
  - Muestra un banner distinguido: *"Estrategia de Desendeudamiento"*.
  - Selector de Segmented Button o Chips de Filtro:
    * `[❄️ Bola de Nieve]` vs `[⚡ Avalancha / Prioridad]`
  - Explicación concisa y botón: `[Abonar a esta deuda]`, que pre-carga el modal de abono existente.

### 4.2 En `CapacidadPagoViewModel.kt`:
- Incorporación de sincronización con `proyectarCapacidad(request)`.
- El recálculo local sigue siendo inmediato para asegurar 0ms de feedback en sliders y switches, disparando en paralelo la validación remota con debounce cuando los valores se estabilizan.

---

## 5. Estrategia de Pruebas Unitarias y Gates

1. **`ObligacionRepositoryTest`:**
   - Test de llamada exitosa a `getCronogramaVencimientos()`.
   - Test de fallback offline de cronograma cuando `GammaApi` lanza excepción.
   - Test de cálculo y llamada a `getDeudaFoco()` con criterio `"saldo"` y `"prioridad"`.
   - Test de `proyectarCapacidad()`.
2. **`ObligacionesViewModelTest`:**
   - Test de carga de cronograma y deuda foco.
   - Test de cambio de criterio de estrategia (`setEstrategiaFoco`).
3. **`CapacidadPagoViewModelTest`:**
   - Test de sincronización y fallback de proyección.
4. **Verificación Visual:**
   - Previews de `@Preview` de `CronogramaVencimientosSection` y `DeudaFocoCard`.
   - Ejecución de `./gradlew testDebugUnitTest` y `./gradlew assembleDebug`.
