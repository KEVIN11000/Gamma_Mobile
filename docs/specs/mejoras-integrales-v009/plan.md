# Plan de Arquitectura y Diseño Técnico (Plan) — `GammaMobile` v0.0.9

**Fecha:** 2026-10-19  
**Feature:** `mejoras-integrales-v009`

---

## 1. Contratos de Datos, Modelos y Persistencia Local (Room)

### 1.1 Modelo `Aviso` y Entidad `AvisoEntity` (Edición, Repetición y hasta 5 Hitos)
- **`domain/model/Models.kt` (`Aviso`):**
  ```kotlin
  data class Aviso(
      @SerializedName("id") val id: String = "",
      @SerializedName("titulo") val titulo: String = "",
      @SerializedName("fecha_evento") val fechaEvento: String = "", // "dd/MM/yyyy HH:mm" o "dd/MM/yyyy"
      @SerializedName("repeticion") val repeticion: String = "NINGUNA", // "NINGUNA", "DIARIA", "SEMANAL", "MENSUAL", "ANUAL"
      @SerializedName("hitos_minutos") val hitosMinutos: List<Int> = listOf(1440) // Hasta 5 hitos en minutos previos (ej. 1440 = 1 día antes)
  )
  ```
- **`data/local/entity/AvisoEntity.kt`:**
  - Añadir columnas `repeticion: String = "NINGUNA"` y `hitosMinutosCsv: String = "1440"` (conversión `List<Int>` <-> CSV).
  - En `GammaDatabase.kt`, incrementar versión de base de datos con `fallbackToDestructiveMigration()` (ya habilitado para caché offline-first) o migración segura.
- **`data/remote/GammaApi.kt` y `AvisoRepository`:**
  - Añadir `@PUT("avisos/{id}") suspend fun updateAviso(@Path("id") id: String, @Body aviso: Aviso): Response<Unit>`.
  - En `AvisoRepositoryImpl.updateAviso(aviso)`: actualizar inmediatamente en `AvisoDao` (para que la edición y la repetición/hitos persistan siempre en el dispositivo) e intentar `api.updateAviso(aviso.id, aviso)` de forma tolerante a fallos mientras se actualiza `Gamma_bot`.

### 1.2 Modelo `Obligacion` y Entidad `ObligacionEntity` (Campos Completos y Eliminación)
- **`domain/model/Models.kt` (`Obligacion`):**
  ```kotlin
  data class Obligacion(
      @SerializedName(value = "id", alternate = ["ID_Obligacion"]) val id: String = "",
      @SerializedName(value = "entidad", alternate = ["Entidad"]) val entidad: String = "",
      @SerializedName(value = "nombre", alternate = ["Nombre"]) val nombre: String = "",
      @SerializedName(value = "tipo", alternate = ["Tipo"]) val tipo: String = "Deuda",
      @SerializedName(value = "monto_inicial", alternate = ["Monto_Inicial"]) val montoInicial: Long = 0L,
      @SerializedName(value = "saldo_actual", alternate = ["Saldo_Actual"]) val saldoActual: Long = 0L,
      @SerializedName(value = "estado", alternate = ["Estado"]) val estado: String = "Activo",
      @SerializedName(value = "cuota_referencia_gs", alternate = ["Cuota_Referencia_Gs", "Cuotas"]) val cuotaReferenciaGs: Long = 0L,
      @SerializedName(value = "cuotas_totales", alternate = ["Cuotas_Totales"]) val cuotasTotales: Int = 1,
      @SerializedName(value = "cuotas_restantes", alternate = ["Cuotas_Restantes"]) val cuotasRestantes: Int = 1,
      @SerializedName(value = "dia_vencimiento", alternate = ["Dia_Vencimiento"]) val diaVencimiento: Int = 0,
      @SerializedName(value = "fecha_inicio", alternate = ["Fecha_Inicio"]) val fechaInicio: String = "",
      @SerializedName(value = "observaciones", alternate = ["Observaciones"]) val observaciones: String = ""
  )
  ```
- **`data/local/entity/ObligacionEntity.kt` y `ObligacionDao.kt`:**
  - Ampliar columnas en `ObligacionEntity` para almacenar todos los campos nuevos.
  - Añadir `@Query("DELETE FROM obligaciones WHERE id = :id") suspend fun deleteById(id: String)` en `ObligacionDao`.
- **`data/remote/GammaApi.kt` y `ObligacionRepository`:**
  - Añadir `@DELETE("obligaciones/{id}") suspend fun deleteObligacion(@Path("id") id: String): Response<Unit>`.
  - En `ObligacionRepositoryImpl`:
    - `deleteObligacion(id)`: elimina en `obligacionDao.deleteById(id)` y llama a `api.deleteObligacion(id)`.
    - `updateObligacion(obligacion)`: persiste en `obligacionDao.insertAll(listOf(obligacion.toEntity()))` siempre (incluso si el servidor aún no tiene `PUT /obligaciones/{id}`).

---

## 2. Arquitectura Modular por Componente

### 2.1 Dashboard Reactivo y Marcación Validada (`DashboardViewModel`, `DashboardRepositoryImpl`, `DashboardScreen`)
1. **Marcación Laboral:**
   - Añadir selector compacto de modo (`"normal"` / `"directo"`) en la tarjeta de Marcación Laboral de `DashboardScreen`.
   - En `DashboardViewModel.marcarAsistencia(modo)`:
     - Evaluar si `res.data.mensaje` contiene `"❌"` o `"⚠️"` o está vacío; exponer `asistenciaIsError: StateFlow<Boolean>` para renderizar el banner en rojo si hubo advertencia/error y en verde solo si fue válido.
2. **Reactividad de Balance Actual, Margen Libre y Posición IVA:**
   - En `DashboardRepositoryImpl`:
     - Inyectar `MovimientoDao` y `ObligacionDao`.
     - Calcular dinámicamente a partir de los movimientos locales/remotos (`MovimientoDao.getAll()`) y obligaciones activas (`ObligacionDao.getAll()`):
       - Ingresos totales vs Gastos totales -> Saldo Acumulado dinámico.
       - IVA Débito (10%/5% en Ingresos) vs IVA Crédito (10%/5% en Gastos) -> Posición IVA y flag `esDebitoFiscal`.
       - Descuento de cuotas de obligaciones activas -> Margen Libre actualizado.
     - Si `forceRefresh = true`, sincronizar primero `movimientos` y `obligaciones` además de `/dashboard/summary`, asegurando que el botón `Refrescar` y el retorno desde `NuevaTransaccion` actualicen inmediatamente los 3 indicadores sin depender de cachés obsoletos.
3. **Corrección de Solapamiento del Botón "Transacción" (Punto 6):**
   - Añadir `Spacer(modifier = Modifier.height(96.dp))` al final de `DashboardContent` y ajustar el layout del `Scaffold` para que el `ExtendedFloatingActionButton` ("Transacción") tenga separación clara respecto a la tarjeta de Marcación Laboral y al `NavigationBar` inferior ("Diario").

### 2.2 Navegación y `BackHandler` (`MainActivity.kt`, `SifenScannerScreen.kt`, `NuevaTransaccionScreen.kt`)
- En `GammaApp` (`MainActivity.kt`):
  - Registrar `var previousScreen by remember { mutableStateOf(Screen.Dashboard) }` al navegar hacia `NuevaTransaccion` o `SifenScanner`.
  - Cuando el usuario abre `SifenScanner` desde `NuevaTransaccion`, `previousScreen` es `Screen.NuevaTransaccion`; cuando lo abre desde `Transacciones`, `previousScreen` es `Screen.Transacciones`.
- En `SifenScannerScreen.kt` y `NuevaTransaccionScreen.kt`:
  - Añadir `androidx.activity.compose.BackHandler`:
    - Si `uiState.scannedInvoice != null` en `SifenScannerScreen`, el botón "Atrás" cierra el `BottomSheet` (`viewModel.dismissInvoice()`).
    - Si no hay `BottomSheet`, el botón "Atrás" invoca `onBack()`, regresando a `previousScreen` sin cerrar la aplicación.

### 2.3 Libro Diario: Rango de Fechas, Vista de Planilla y Exportación PDF Local (`TransaccionesScreen`, `TransaccionesViewModel`, `LocalDiarioPdfGenerator`)
1. **Eliminación de botón QR redundante (Punto 8):**
   - Quitar el `IconButton` de `QrCodeScanner` en el `TopAppBar` de `TransaccionesScreen`.
2. **Filtros de Rango de Fechas y Selector de Vista (Punto 7):**
   - En `TransaccionesViewModel`:
     - Añadir `fechaDesde: LocalDate?` y `fechaHasta: LocalDate?` en el estado.
     - Parsear fechas de movimientos (`dd/MM/yyyy` o `dd/MM` asumiendo año en curso) y filtrar el listado según `filtroActual` (`TODOS`, `INGRESO`, `GASTO`) **y** el rango `[fechaDesde, fechaHasta]`.
     - Exponer `movimientosFiltradosLista: List<Movimiento>` con el conjunto exacto de movimientos visibles actualmente.
   - En `TransaccionesScreen`:
     - En la cabecera (debajo de los chips `Todos`, `Ingresos`, `Gastos`), incluir barra de filtro de fechas (`Desde`, `Hasta`, botón limpiar) con `DatePickerDialog`.
     - En el `TopAppBar`, incluir:
       1. Botón de alternancia de vista (`Vista Tarjetas` / `Vista Planilla`).
       2. Botón **`Exportar PDF`** (`Icons.Default.PictureAsPdf` o `Share` / `Print`) que exporta el conjunto de datos actual mostrado en la vista.
     - Implementar `PlanillaMovimientosTable(movimientos: List<Movimiento>)`: tabla tipo hoja de cálculo con cabecera fija (`Fecha | Tipo | Categoría | Comprobante | Monto`), filas con bordes/alternancia de color y fila resumen al final (`Total Ingresos`, `Total Gastos`, `Balance Neto`).
3. **Generador PDF 100% Local (`utils/LocalDiarioPdfGenerator.kt`):**
   - Utiliza `android.graphics.pdf.PdfDocument` nativo de Android (sin enviar nada al servidor).
   - Recibe `movimientos: List<Movimiento>`, `filtroTipo: String`, `rangoFechasTexto: String`.
   - Dibuja encabezado institucional (`"GAMMA MOBILE — REPORTE DE LIBRO DIARIO"`), filtro aplicado, rango de fechas, tabla paginada de movimientos y cuadro de totales (Ingresos, Gastos, Neto).
   - Escribe el archivo en `File(context.cacheDir, "comprobantes/reporte_diario_${timestamp}.pdf")`, dispara `ComprobanteNotificationHelper.mostrarNotificacionDescarga` y lanza el `Intent.ACTION_VIEW` con `FileProvider`.

### 2.4 Alarmas Exactas No Ancladas con Hasta 5 Hitos y Edición/Repetición de Eventos (`AvisoAlarmScheduler`, `AvisoAlarmReceiver`, `AvisosScreen`)
1. **`utils/AvisoAlarmScheduler.kt` y `receiver/AvisoAlarmReceiver.kt`:**
   - `AvisoAlarmScheduler` programa en `AlarmManager` (`setExactAndAllowWhileIdle` con fallback a `setAndAllowWhileIdle` si `canScheduleExactAlarms()` es false en Android 12+) un `PendingIntent` único por cada `(aviso.id, hitoMinutos)` más el hito `0` (hora exacta del evento).
   - Al dispararse `AvisoAlarmReceiver`:
     - Construye una notificación en `"avisos_channel"` con `setAutoCancel(true)` y `setOngoing(false)` (nunca anclada).
     - Si el disparo corresponde al hito `0` y `aviso.repeticion != "NINGUNA"`, calcula la próxima fecha (`+1 día`, `+1 semana`, `+1 mes`, `+1 año`) y reprograma automáticamente el siguiente ciclo.
2. **Formulario Unificado Crear/Editar Aviso en `AvisosScreen.kt`:**
   - Permite:
     - Escribir una frase y pulsar `"Autocompletar con IA"` (que llama al endpoint de IA y rellena los campos del formulario sin cerrarlo).
     - Editar manualmente `Título`, `Fecha (DD/MM/YYYY)` y `Hora (HH:MM)`.
     - Seleccionar `Repetición`: `No repetir`, `Diario`, `Semanal`, `Mensual`, `Anual`.
     - Configurar hasta **5 hitos previos** (chips/botones para agregar/quitar hitos como `Al momento`, `30 min antes`, `1 hora antes`, `3 horas antes`, `1 día antes`, `3 días antes`, `7 días antes`, o valor personalizado en minutos/horas/días, con tope máximo de 5 hitos).
   - Al tocar una tarjeta de `AvisoItem` existente, se abre este mismo diálogo pre-llenado con los datos del aviso para editarlo y guardar cambios.

### 2.5 Deudas: Eliminar, Cancelar Simuladas, Simulador Ampliado y Nueva Obligación Completa (`ObligacionesScreen.kt`, `ObligacionesViewModel.kt`)
1. **Tarjetas de Deuda (`ObligacionItem`):**
   - Si `obligacion.esSimulada`: mostrar botones **`Aprobar`** y **`Cancelar`** (con confirmación que invoca `viewModel.eliminarObligacion(obligacion)`).
   - En todas las tarjetas: icono de papelera (`Icons.Default.Delete`) en la esquina superior derecha de la tarjeta que abre diálogo de confirmación para eliminar el registro de deuda.
2. **Simulador de Cuotas Ampliado (`SimuladorCuotasModal`):**
   - Campos en contenedor amplio con `verticalScroll`:
     - `Concepto / Nombre`
     - `Entidad / Acreedor`
     - `Monto Total del Préstamo/Obligación (₲)`
     - `Cantidad de Cuotas`
     - `Monto de cada Cuota (₲)` (se auto-calcula al cambiar Monto o Cuotas, y si el usuario edita manualmente el monto de la cuota, usa ese monto de cuota para evaluar la viabilidad contra el Margen Libre).
   - Al calcular, muestra el tarjeta de viabilidad (`VERDE`, `AMARILLO`, `ROJO`) y habilita el botón **`"Usar en Nueva Obligación"`**, que cierra el simulador y abre `CrearObligacionModal` con todos esos datos pre-cargados.
3. **Formulario `CrearObligacionModal` Completo:**
   - Recibe datos iniciales opcionales desde el Simulador.
   - Campos: `Entidad / Acreedor`, `Nombre / Concepto`, `Tipo` (`Deuda`, `Préstamo`, `Tarjeta`, `Gasto Fijo`), `Monto Total (₲)`, `Cantidad de Cuotas`, `Monto de cada Cuota (₲)`, `Día de Vencimiento (1-31)`, `Fecha Primer Vencimiento (DD/MM/YYYY)`, `Estado Inicial` (`Simulado` / `Activo`), `Observaciones`.
   - Eliminar el archivo sin uso `SimuladorModal.kt`.

### 2.6 Sistema y Soporte (`SistemaScreen.kt`, `SistemaViewModel.kt`, `DeviceAuthHelper.kt`, `AppLogBuffer.kt`)
1. **Autenticación con Bloqueo del Celular (Punto 11):**
   - Crear `utils/DeviceAuthHelper.kt` usando `KeyguardManager.createConfirmDeviceCredentialIntent(title, description)` / `BiometricPrompt` lanzado con `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())`.
   - En `SistemaScreen.kt`, al pulsar el botón `"Desbloquear para Editar"`, el icono del candado o el botón `"Ver"` de la API Key, invocar la autenticación del bloqueo del dispositivo antes de cambiar `isConfigLocked = false` o `showKeyText = true`.
2. **Eliminación de Cierre Contable Mensual (Punto 13):**
   - Remover el bloque de UI `"Cierre Contable Mensual"` de `SistemaScreen.kt`.
3. **Tickets de Soporte Solo Lectura de Estado + Creación (Punto 14):**
   - Remover `selectedTicketForAction` y el diálogo `"Atender Ticket"` de `SistemaScreen.kt`, así como el texto `"Toca para atender ✏️"`.
   - Mantener los chips de filtro (`Todos`, `Abierto`, `En proceso`, `Cerrado`), la visualización completa de cada tarjeta (ID, origen, badge de estado, asunto, descripción, resolución y fecha) y el botón `+ Nuevo` para crear tickets.
4. **Registros y Logs Dinámicos (Punto 15):**
   - Crear singleton `utils/AppLogBuffer.kt` que registre eventos en tiempo real de la app (sincronizaciones HTTP, respuestas de health/dashboard, marcaciones, descargas PDF, errores de red) con `timestamp` (`dd/MM/yyyy HH:mm:ss`), `nivel` y `mensaje`.
   - En `SistemaViewModel.cargarDatosSistema()`, fusionar los logs del servidor (`/api/v1/sistema/logs`) con las entradas de `AppLogBuffer`, agregar botón de refresco directo en la tarjeta `"Registros y Logs"` y permitir ver hasta los últimos 20 registros con su hora exacta.
