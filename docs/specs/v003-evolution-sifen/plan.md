# Architecture & Implementation Plan: Evolución v0.0.3 - SIFEN QR, Tipado PYG y Consolidación Ergonómica

- **Feature**: `v003-evolution-sifen`
- **Fase**: Fase 0 SDD - Plan de Arquitectura y Contratos
- **Fecha**: 2026-10-14
- **Dependencias**: Android Jetpack Compose, Room DB, Retrofit/Gson, CameraX / ML Kit Barcode Scanning

---

## 1. Technical Context & Arquitectura Modular

La aplicación evoluciona sobre una arquitectura limpia (Clean Architecture) con MVVM, Room como Single Source of Truth local y Retrofit para sincronización remota:

```
[UI Layer (Jetpack Compose)]
   ├── DashboardScreen (KPIs, IVA, Balance)
   ├── MovimientosScreen (Histórico Diario + FAB Carga/QR/Audio)
   ├── CompromisosScreen (Tabs: Deudas/Obligaciones + Avisos)
   ├── SifenScannerScreen (CameraX Preview + ML Kit Barcode Analyzer)
   └── AjustesScreen (Configuración, Diagnóstico, Tickets)
            │
[Presentation Layer (ViewModels + UiState)]
   ├── DashboardViewModel (Long calculations)
   ├── MovimientosViewModel (Paging/Room query)
   ├── CompromisosViewModel
   └── SifenScannerViewModel (CDC parser)
            │
[Domain Layer (UseCases & Models)]
   ├── Models: Movimiento (Long), Obligacion (Long), SifenInvoiceData
   └── ParseSifenQrUseCase
            │
[Data Layer (Repositories, Room DB & Remote API)]
   ├── AppDatabase (Migración v1 -> v2 con Schema Migration o Destructive fallback controlado)
   ├── MovimientoDao / ObligacionDao
   └── MovimientoRepositoryImpl (Políticas de persistencia sin purga)
```

---

## 2. Contratos de Datos y Esquemas

### 2.1 Modelo de Datos Migrado a Long (`Models.kt`)

```kotlin
data class Movimiento(
    @SerializedName("id") val id: String,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("tipo", alternate = ["movimiento"]) val tipo: String, // "INGRESO" o "EGRESO"
    @SerializedName("proveedor_cliente") val proveedorCliente: String,
    @SerializedName("nro_factura") val nroFactura: String,
    @SerializedName("monto_neto") val montoNeto: Long,       // Migrado de Int a Long
    @SerializedName("monto_iva") val montoIva: Long,         // Migrado de Int a Long
    @SerializedName("total") val total: Long,                // Migrado de Int a Long
    @SerializedName("categoria") val categoria: String,
    @SerializedName("comprobante") val comprobante: String
)

data class Obligacion(
    @SerializedName("id", alternate = ["ID_Obligacion"]) val id: String,
    @SerializedName("nombre", alternate = ["Nombre"]) val nombre: String,
    @SerializedName("monto_inicial", alternate = ["Monto_Inicial"]) val montoInicial: Long, // Migrado a Long
    @SerializedName("saldo_actual", alternate = ["Saldo_Actual"]) val saldoActual: Long,     // Migrado a Long
    @SerializedName("estado", alternate = ["Estado"]) val estado: String
)
```

### 2.2 Entidad Room y Migración de Base de Datos (`AppDatabase.kt`)

Las tablas `movimientos` y `obligaciones` migran sus columnas numéricas a `INTEGER` de SQLite (que soporta valores de 64 bits en Room representados por `Long` en Kotlin):

```kotlin
@Entity(tableName = "movimientos")
data class MovimientoEntity(
    @PrimaryKey val id: String,
    val fecha: String,
    val tipo: String,
    val proveedorCliente: String,
    val nroFactura: String,
    val montoNeto: Long,
    val montoIva: Long,
    val total: Long,
    val categoria: String,
    val comprobante: String,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)
```

**Estrategia de Migración:**
- Si la base de datos está en versión 1, elevar a versión 2 en `@Database(version = 2)`.
- Proveer migración declarativa `MIGRATION_1_2` o `fallbackToDestructiveMigration()` al tratarse de un release de desarrollo interno, reconstruyendo el caché local de inmediato mediante un refresco remoto con `/api/v1/movimientos`.

### 2.3 Contrato de Parser SIFEN QR (`SifenInvoiceData`)

Las facturas electrónicas de la DNIT Paraguay contienen códigos QR con URLs con parámetros query o formato de cadena concatenada oficial:
`https://ekuatia.set.gov.py/consultas/qr?nVersion=150&Id=01800123456001001000000122026101412345678901&dFeEmiDE=2026-10-14&dRucRec=...&dTotGralOpe=1500000&dTotIVA=136364`

```kotlin
data class SifenInvoiceData(
    val cdc: String,              // 44 caracteres numéricos
    val rucEmisor: String,
    val razonSocialEmisor: String,
    val fechaEmision: String,     // Formato YYYY-MM-DD
    val total: Long,              // ₲ Guaraníes
    val iva10: Long,
    val iva5: Long,
    val rawQrUrl: String
)
```

---

## 3. Arquitectura del Módulo de Escaneo QR

Para no sobrecargar el tamaño del APK:
- Opción preferida: `androidx.camera:camera-camera2`, `androidx.camera:camera-lifecycle`, `androidx.camera:camera-view` + `com.google.mlkit:barcode-scanning`.
- Provee detección en tiempo real a 30 FPS en el hilo de análisis de CameraX.
- Al detectar un QR que contenga prefijos `sifen.set.gov.py` o `ekuatia.set.gov.py` o un CDC de 44 caracteres:
  1. Vibra brevemente (`HapticFeedbackConstants.LONG_PRESS`).
  2. Parsea parámetros mediante `ParseSifenQrUseCase`.
  3. Muestra `ModalBottomSheet` con el detalle del gasto pre-cargado.
  4. Al pulsar "Registrar en Diario", se inserta localmente en Room con `syncStatus = PENDING` y se envía al endpoint `POST /api/v1/movimientos`.

---

## 4. Consolidación de Navegación (6 ➔ 4 Pestañas)

Estructura de rutas en `NavGraph`:

| Ruta Principal | Título UI | Ícono | Pantallas Internas / Tabs |
| :--- | :--- | :--- | :--- |
| `dashboard` | Dashboard | `Icons.Default.Dashboard` | KPIs de Margen Libre, Posición IVA, Gráficos de Balance. |
| `movimientos` | Movimientos | `Icons.Default.ReceiptLong` | Libro Diario, filtros de fecha/tipo, búsqueda, FAB de Carga / QR. |
| `compromisos` | Compromisos | `Icons.Default.Assignment` | Selector de pestañas superior: Tab 1 (Deudas), Tab 2 (Avisos). |
| `ajustes` | Ajustes | `Icons.Default.Settings` | Parámetros del servidor, Diagnóstico de red, Versión, Soporte/Tickets. |

---

## 5. Manejo de Errores y Resiliencia

1. **Fallback de Parser SIFEN**: Si el QR es de un formato de factura no estándar o no pertenece a SIFEN, la UI no se bloquea; muestra un Snackbar: *"El código QR no corresponde a una factura electrónica válida de la DNIT"*.
2. **Resiliencia de Sincronización**: Al fallar el envío de una factura escaneada por falta de conectividad, queda guardada en Room en estado `PENDING_SYNC` para ser despachada automáticamente cuando se restablezca la conexión vía `WorkManager` o al abrir la app.
3. **Manejo de Permiso de Cámara**: Si el usuario deniega el permiso de cámara, se ofrece un botón explicativo para reintentar o cargar manualmente los datos de la factura.

---

## 6. Verificación con la Constitución del Proyecto

- **Minimal Change**: Los cambios respetan el árbol de paquetes existente en `com.gamma.mobile`.
- **Zero Leaks**: No se exponen credenciales ni se introducen librerías de tracking invasivas.
- **Calidad Demostrable**: Cada componente de cálculo y parser contará con sus pruebas unitarias en `app/src/test/`.
