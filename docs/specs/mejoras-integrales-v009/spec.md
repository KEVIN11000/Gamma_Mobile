# Especificación Técnica (Spec) — Actualización General y Mejoras Integrales v0.0.9 (`GammaMobile`)

**Fecha:** 2026-10-19  
**Proyecto:** `GammaMobile` (App Android Nativa — Jetpack Compose / Hilt / Room / Retrofit)  
**Alcance estricto:** Exclusivamente código y recursos de `GammaMobile`. Queda prohibido modificar `Gamma_bot` en esta sesión (las adecuaciones de servidor están decantadas en el artefacto `requerimientos_gamma_bot.md`).

---

## 1. Alcance y Casos de Uso (15 Puntos de Mejora)

### CU-01: Marcación Laboral Validada en Dashboard (Punto 1)
- **Situación actual:** En `DashboardScreen`, el botón `"Marcar"` llama a `marcarAsistencia("normal")` sin permitir elegir el modo (`"normal"` o `"directo"`) y acepta cualquier respuesta HTTP 200 sin validar si el mensaje devuelto por el backend indica error o advertencia (`❌` / `⚠️`), ni verificar el estado de la respuesta.
- **Comportamiento esperado:**
  - La tarjeta de Marcación Laboral en `DashboardScreen` debe permitir seleccionar el modo (`"normal"` / `"directo"`) o confirmar la marcación invocando a `AsistenciaRepository.marcarAsistencia(modo)`.
  - `DashboardViewModel` debe validar rigurosamente la respuesta: si `res.data.mensaje` contiene indicadores de rechazo (`❌`, `⚠️`, `"error"`) o si la respuesta no trae hora/confirmación válida, debe mostrar estado de error visual diferenciado (color de error) en lugar de un `"✅"` falso.

### CU-02 y CU-03: Reactividad Dinámica de Balance Actual, Margen Libre y Posición IVA (Puntos 2 y 3a)
- **Situación actual:**
  1. `DashboardRepositoryImpl` retiene un caché de 15 minutos en `SharedPreferences`, pero además, cuando se registran movimientos en Libro Diario o abonos/obligaciones en Deudas, el resumen del Dashboard no se recalcula localmente ni se invalida su caché.
  2. Si el backend devuelve `0` o datos estancados, o si hay movimientos nuevos sincronizados o locales en Room (`MovimientoDao` / `ObligacionDao`), `Balance Actual`, `Margen Libre` y `Posición IVA` no reflejan los cambios en tiempo real.
- **Comportamiento esperado:**
  - Al pulsar el botón `Refrescar` en `DashboardScreen`, o al volver al Dashboard tras registrar una transacción/obligación, `DashboardViewModel` debe forzar la recarga (`forceRefresh = true`) tanto del resumen remoto como de los movimientos y obligaciones locales/remotos.
  - Implementar cálculo reactivo en `DashboardRepositoryImpl` / `DashboardViewModel` que combine el resumen del backend con el cálculo dinámico sobre los movimientos (`MovimientoDao`) y obligaciones activas (`ObligacionDao`) cuando corresponda, garantizando que `Balance Actual (Saldo Acumulado)`, `Margen Libre` y `Posición IVA` (Crédito/Débito fiscal 10% y 5%) se actualicen instantáneamente ante cualquier nuevo ingreso, gasto o abono, y al pulsar el botón de actualizar.

### CU-04: Edición de Tarjetas de Evento/Aviso, Opciones de Repetición y Sincronización Calendar (Puntos 3b y 12)
- **Situación actual:**
  - En `AvisosScreen`, al tocar una tarjeta de evento (`AvisoItem`), solo se abre un `AlertDialog` estático de lectura (`"Detalle del Aviso"`). No permite editar el evento.
  - Al crear un aviso con IA, se guarda directamente sin mostrar una vista previa editable.
  - El modelo `Aviso` no soporta opciones de repetición (`No repetir`, `Diario`, `Semanal`, `Mensual`, `Anual`) ni hitos personalizados.
- **Comportamiento esperado:**
  - Ampliar el modelo `Aviso` y la entidad Room `AvisoEntity` para incluir:
    - `repeticion`: `"NINGUNA"`, `"DIARIA"`, `"SEMANAL"`, `"MENSUAL"`, `"ANUAL"` (por defecto `"NINGUNA"`).
    - `hitosMinutos`: lista de hasta 5 hitos previos en minutos antes del evento (ej. `[1440, 180, 60]`), serializada en Room y JSON.
  - Unificar un modal/formulario completo de **Crear / Editar Aviso**:
    - Permite entrada asistida por IA: al pulsar `"Interpretar con IA"`, pre-llena los campos editables (Título, Fecha `DD/MM/YYYY`, Hora `HH:MM`) en el mismo formulario para que el usuario revise, ajuste repetición/hitos y confirme antes de guardar.
    - Al tocar cualquier tarjeta de evento existente en `AvisosScreen`, abre este mismo formulario en modo **Edición**, permitiendo modificar Título, Fecha, Hora, Repetición y hasta 5 hitos previos, persistiendo los cambios vía `AvisoRepository.updateAviso(aviso)` (en Room + endpoint `PUT /api/v1/avisos/{id}` con fallback local si el backend aún no expone `PUT`) y reprogramando sus alarmas locales.

### CU-05: Notificaciones Exactas en Segundo Plano No Ancladas con Hasta 5 Hitos Previos (Punto 4)
- **Situación actual:** `AlarmaAvisosWorker` usa un `PeriodicWorkRequest` cada 24 horas que Android pospone o congela cuando la app no está activa.
- **Comportamiento esperado:**
  - Implementar `AvisoAlarmScheduler` usando `AlarmManager` + `BroadcastReceiver` (`AvisoAlarmReceiver`) registrado en `AndroidManifest.xml` (incluyendo reprogramación en `BOOT_COMPLETED`).
  - Cada evento programa alarmas exactas/con ventana precisa tanto para la **fecha y hora exacta del evento** como para los **hasta 5 hitos previos personalizados** por el usuario (ej. 1 día antes, 3 horas antes, 30 minutos antes, etc.).
  - La notificación emitida debe ser **estándar y descartable** (`setAutoCancel(true)`, ` setOngoing(false)` — **nunca anclada** en la barra de notificaciones), mostrando el título del evento y cuánto falta, y al tocarla debe abrir la app.
  - Si el evento tiene configurada una opción de repetición (`Diaria`, `Semanal`, `Mensual`, `Anual`), al dispararse la alarma de la fecha/hora principal se calcula y programa automáticamente la siguiente ocurrencia.

### CU-06: Navegación Correcta al Retroceder desde Escáner de Facturas SIFEN y Nueva Transacción (Punto 5)
- **Situación actual:** En `MainActivity.kt`, la navegación usa una variable `var currentScreen by remember { mutableStateOf(Screen.Dashboard) }` sin `BackHandler` del sistema. Por ello, cuando el usuario entra a `SifenScanner` o `NuevaTransaccion` y pulsa el botón físico/gesto "Atrás" de Android, `ComponentActivity` cierra la aplicación y sale de la app en lugar de volver a la pantalla anterior. Además, el botón superior de retroceso en `SifenScannerScreen` tenía `onBack = { currentScreen = Screen.Transacciones }` fijo aunque se hubiera abierto desde `NuevaTransaccion`.
- **Comportamiento esperado:**
  - Mantener seguimiento de la pantalla previa (`previousScreen` o pila de navegación en `GammaApp`) e integrar `androidx.activity.compose.BackHandler` en `SifenScannerScreen`, `NuevaTransaccionScreen` y pantallas secundarias.
  - Si el usuario tiene el `BottomSheet` de factura escaneada abierto y pulsa "Atrás", cierra primero el `BottomSheet`; si pulsa "Atrás" en la cámara de `SifenScannerScreen` (o el botón de flecha superior), regresa exactamente a la pantalla desde donde vino (`Transacciones` o `NuevaTransaccion`) sin cerrar la app.

### CU-07: Corrección de Solapamiento de Toque en el Botón "Transacción" del Dashboard (Punto 6)
- **Situación actual:** En `DashboardScreen`, el `Scaffold` interno define un `ExtendedFloatingActionButton` ("Transacción") mientras está dentro del `Scaffold` principal de `MainActivity` (que contiene el `NavigationBar` inferior con el ítem `"Diario"` en la segunda posición) y el contenido de `DashboardContent` hace scroll sin `contentPadding` inferior suficiente ni separación de insets, provocando problemas de superposición táctil/visual en la zona inferior.
- **Comportamiento esperado:**
  - Eliminar el anidamiento conflictivo de `Scaffold` / `WindowInsets` entre `MainActivity` y `DashboardScreen`, asegurar la jerarquía y `zIndex` del `ExtendedFloatingActionButton`, y agregar `Spacer(modifier = Modifier.height(88.dp))` al final del `Column` scrolleable de `DashboardContent` para que la tarjeta de Marcación Laboral y las demás tarjetas nunca empujen ni interfieran con el área táctil del botón `"Transacción"` ni con la barra inferior (`"Diario"`).

### CU-08: Filtro por Rango de Fechas, Vista de Planilla/Tabla y Exportación PDF Local en Libro Diario (Punto 7)
- **Situación actual:** `TransaccionesScreen` solo filtra por chip (`Todos`, `Ingresos`, `Gastos`), muestra únicamente tarjetas agrupadas y no permite filtrar por rango de fechas ni alternar a vista de planilla ni exportar a PDF local.
- **Comportamiento esperado:**
  - **Filtro de Rango de Fechas en Cabecera:** Junto a los chips `Todos`, `Ingresos`, `Gastos`, incorporar controles de fecha (`Desde` — `Hasta` con `DatePickerDialog` y botón para limpiar rango) que filtren reactivamente la lista de movimientos en `TransaccionesViewModel`.
  - **Selector de Vista (`Tarjetas` vs `Planilla/Tabla`):** Permitir alternar en la misma pantalla entre la vista de tarjetas actual y una **Vista de Planilla/Tabla** estructurada con columnas (`Fecha`, `Tipo`, `Categoría`, `Comprobante`, `Total ₲`) y fila de totales al pie.
  - **Botón de Exportar PDF Local en TopAppBar:** Un botón en la barra superior de `TransaccionesScreen` que tome **exactamente el conjunto de datos actual mostrado en la vista (filtrado o no por tipo y rango de fechas)**, genere un documento PDF 100% en local en el dispositivo usando `android.graphics.pdf.PdfDocument` (`LocalDiarioPdfGenerator`), lo guarde en `context.cacheDir/comprobantes/`, dispare la notificación local vía `ComprobanteNotificationHelper` y abra el visor PDF con `FileProvider` **sin almacenar ni pedir nada al servidor**.

### CU-09: Eliminación del Botón QR Redundante en la Esquina Superior Derecha (Punto 8)
- **Situación actual:** `TransaccionesScreen` tiene dos botones para lo mismo: un `IconButton` con icono QR en la esquina superior derecha del `TopAppBar` (líneas 62–66) y el `ExtendedFloatingActionButton` `"Escanear Factura"`.
- **Comportamiento esperado:** Eliminar el `IconButton` de QR en la esquina superior derecha de `TransaccionesScreen`, dejando ese espacio limpio para los controles de vista/exportación PDF y manteniendo el FAB `"Escanear Factura"`.

### CU-10: Botón de Eliminar Deuda y Botón de Cancelar en Simuladas (Punto 9)
- **Situación actual:** `ObligacionItem` en `ObligacionesScreen.kt` solo muestra `"Aprobar"` (si es simulada) o `"Abonar"` (si es activa). No existe opción para eliminar una deuda ni para cancelar una obligación simulada.
- **Comportamiento esperado:**
  - En las obligaciones con estado `Simulado`/`Simulada`: mostrar junto al botón `"Aprobar"` un botón `"Cancelar"` (que elimine/descarte la simulación previa confirmación).
  - En todas las tarjetas de deuda (`Activas`, `Saldadas`, `Todas`): incluir un icono/botón de eliminar (`Icons.Default.Delete`) con diálogo de confirmación (`"¿Eliminar registro de deuda?"`) que invoque `viewModel.eliminarObligacion(obligacion)` (borrando de Room y llamando a `DELETE /api/v1/obligaciones/{id}`).

### CU-11: Ampliación del Simulador de Cuotas y del Formulario de Nueva Obligación (Punto 10)
- **Situación actual:**
  - `SimuladorCuotasModal` solo tiene 2 campos (`Monto del préstamo` y `Cantidad de cuotas`) en un diálogo pequeño.
  - `CrearObligacionModal` solo pide `Nombre` y `Monto`.
  - Además, existe un archivo huérfano `SimuladorModal.kt` sin uso.
- **Comportamiento esperado:**
  - **Simulador de Cuotas Ampliado:** Ventana amplia/scrolleable donde el usuario pueda ingresar o ajustar:
    - `Concepto / Nombre del proyecto o deuda` (opcional para pre-llenado)
    - `Monto Total (₲)`
    - `Cantidad de Cuotas`
    - `Monto de cada Cuota (₲)` (calculado automáticamente como `Monto / Cuotas`, pero **editable** por si la entidad aplica interés fijo en la cuota, recalculando el total comprometido)
    - Visualización clara de: Cuota mensual resultante, Margen Libre actual, Nuevo Margen proyectado y Nivel de Riesgo (`VERDE`, `AMARILLO`, `ROJO`).
    - Botón de acción directa: **`"Crear Obligación con estos datos"`**, que cierra el simulador y abre el formulario de `Nueva Obligación` **pre-llenado** con el concepto, monto total, cantidad de cuotas y monto de cuota mensual.
  - **Formulario de Nueva Obligación Completo:** Ventana amplia/scrolleable con los campos:
    - `Entidad / Acreedor` (ej. Banco Itaú, Cooperativa, ANDE)
    - `Nombre / Concepto` (ej. Préstamo Personal, Tarjeta)
    - `Tipo` (`Deuda`, `Préstamo`, `Tarjeta`, `Gasto Fijo`)
    - `Monto Total (₲)`
    - `Cantidad de Cuotas`
    - `Monto de cada Cuota Referencia (₲)` (auto-calculado al ingresar monto y cuotas, pero editable manualmente)
    - `Día de Vencimiento Mensual (1–31)` / `Fecha de Inicio o Primer Vencimiento (DD/MM/YYYY)`
    - `Estado Inicial` (`Simulado` o `Activo`)
    - `Observaciones` (opcional)
  - Mostrar estos datos enriquecidos (entidad, cuota de referencia, cuotas restantes/totales, día de vencimiento) en cada tarjeta `ObligacionItem`.

### CU-12: Autenticación con Bloqueo del Celular para Editar Conexión / Ver API Key (Punto 11)
- **Situación actual:** En `SistemaScreen`, pulsar `"Desbloquear para Editar"`, el icono del candado o el botón `"Ver"` de la API Key desbloquea inmediatamente los campos en texto plano sin ninguna verificación de seguridad.
- **Comportamiento esperado:**
  - Integrar autenticación del dispositivo mediante `BiometricPrompt` / `KeyguardManager.createConfirmDeviceCredentialIntent` (`BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL`: huella digital, reconocimiento facial, PIN, patrón o contraseña del teléfono).
  - Requerir superar el bloqueo del celular antes de:
    1. Desbloquear la edición de URL / API Key (`isConfigLocked = false`).
    2. Mostrar la API Key en texto plano (`showKeyText = true`).
  - Si el dispositivo no tiene ningún bloqueo de pantalla configurado en Android (`!keyguardManager.isDeviceSecure`), permitir el desbloqueo mostrando un `Toast` / aviso preventivo recomendando configurar un bloqueo de pantalla en el dispositivo.

### CU-13: Eliminación de "Cierre Contable Mensual" en Sistema y Soporte (Punto 13)
- **Situación actual:** `SistemaScreen.kt` (líneas 381–543) incluye una tarjeta de `"Cierre Contable Mensual"`, la cual está duplicada y fuera de lugar (ya que los cierres pertenecen a `Compromisos -> Asistencia -> Cierre Mensual` y la exportación por rango de fechas estará en `Libro Diario`).
- **Comportamiento esperado:** Eliminar la tarjeta `"Cierre Contable Mensual"` de `SistemaScreen.kt` (manteniendo intacta la funcionalidad en `AsistenciaScreen`).

### CU-14: Tickets de Soporte en Modo Solo Lectura de Estado + Creación (Punto 14)
- **Situación actual:** En `SistemaScreen.kt`, tocar un ticket abre un diálogo `"Atender TCK-XXX"` que permite cambiar su estado (`Abierto`, `En proceso`, `Cerrado`) y escribir una resolución desde el teléfono.
- **Comportamiento esperado:**
  - En `SistemaScreen`, la sección `"Tickets de Soporte"` debe listar los tickets, permitir filtrarlos por chip de estado (`Todos`, `Abierto`, `En proceso`, `Cerrado`), visualizar claramente su estado actual, fecha, canal de origen y resolución (si ya fue resuelto), y permitir **agregar un nuevo ticket** (`+ Nuevo`).
  - Eliminar por completo el diálogo `"Atender Ticket"` y cualquier acción de cambio de estado desde la app (eliminando también la leyenda `"Toca para atender ✏️"` de las tarjetas).

### CU-15: Actualización Dinámica de Registros y Logs (Punto 15)
- **Situación actual:** La tarjeta `"Registros y Logs"` en `SistemaScreen` solo muestra `uiState.logs.take(5)` sin botón de refresco propio, sin marca de tiempo visible en cada fila, sin scroll/expansión y sin capturar los eventos de actividad de red/sincronización de la propia sesión.
- **Comportamiento esperado:**
  - En `SistemaScreen` y `SistemaViewModel`:
    - Mostrar `timestamp`, `nivel` (con badge/color según `INFO`, `WARN`, `ERROR`) y `mensaje` de cada log.
    - Agregar botón de actualización rápida en la cabecera de la tarjeta `"Registros y Logs"` y auto-refresco periódico mientras la pantalla de Sistema está visible (o al ejecutar acciones en Sistema).
    - Combinar los logs remotos devueltos por `GET /api/v1/sistema/logs` con un buffer reactivo local de eventos del cliente (`AppLogBuffer`: intentos de conexión, sincronización de endpoints, descargas PDF, errores HTTP) para que el panel de Registros y Logs refleje actividad real en vivo incluso mientras el endpoint de `Gamma_bot` siga devolviendo las 2 líneas base.

---

## 2. Exclusiones Explícitas (No-Scope)
- **No tocar código de `Gamma_bot`:** Ningún archivo `.py` del repositorio `Gamma_bot` será modificado en esta fase. Todos los requerimientos de backend quedan documentados en `requerimientos_gamma_bot.md`.
- **No exportar archivos CSV:** En el Libro Diario no se generarán archivos `.csv`; se implementará la **Vista de Planilla/Tabla** interactiva en pantalla y la **Exportación/Descarga de PDF 100% local**.
- **No usar notificaciones persistentes/ancladas (`ongoing`):** Ninguna notificación de avisos o comprobantes quedará anclada en la barra de estado.
- **No almacenar reportes del Libro Diario en el servidor:** El PDF por rango de fechas del Libro Diario se construye y guarda exclusivamente en el almacenamiento local del dispositivo.
