# Lista de Tareas Atómicas (Tasks) — `GammaMobile` v0.0.9

**Regla de ejecución (Fase 2 SDD):** Se implementará **UNA sola tarea por turno**, verificando que `./gradlew testDebugUnitTest` compile y pase al 100% antes de marcar `[x]` y avanzar a la siguiente tarea.

---

- [x] **Tarea 1: Modelos de Dominio, Entidades Room y Contratos de Repositorio (`Aviso` y `Obligacion`)**
  - **Nota de implementación:** los campos nuevos se agregaron **al final** de `Aviso`/`Obligacion` (no intercalados como en `plan.md`) para conservar la compatibilidad de los constructores posicionales usados en los tests existentes. `updateObligacion`/`updateAviso` persisten en Room siempre; `deleteObligacion` borra en Room solo tras éxito remoto.
  - **Alcance:**
    - Ampliar `Aviso` en `Models.kt` y `AvisoEntity.kt` con `repeticion` (`NINGUNA`, `DIARIA`, `SEMANAL`, `MENSUAL`, `ANUAL`) y `hitosMinutos: List<Int>` (hasta 5 hitos previos).
    - Ampliar `Obligacion` en `Models.kt` y `ObligacionEntity.kt` con `entidad`, `tipo`, `cuotaReferenciaGs`, `cuotasTotales`, `cuotasRestantes`, `diaVencimiento`, `fechaInicio`, `observaciones`.
    - Añadir `deleteById` en `ObligacionDao.kt`, `updateAviso` en `GammaApi.kt`/`AvisoRepository`/`AvisoRepositoryImpl`, y `deleteObligacion` en `GammaApi.kt`/`ObligacionRepository`/`ObligacionRepositoryImpl`.
    - Incrementar versión de `GammaDatabase.kt`.
  - **Criterio de aceptación:** `./gradlew testDebugUnitTest` compila y pasa todos los tests existentes y nuevos tests de mapeo Entity <-> Domain.

- [x] **Tarea 2: Navegación con `BackHandler`, Corrección de Solapamiento en Dashboard, Marcación Validada y Reactividad Financiera (Puntos 1, 2, 3a, 5 y 6)**
  - **Alcance:**
    - En `MainActivity.kt`, gestionar `previousScreen` al abrir `NuevaTransaccion` y `SifenScanner`, y refrescar Dashboard al regresar de `NuevaTransaccion`.
    - En `SifenScannerScreen.kt` y `NuevaTransaccionScreen.kt`, agregar `BackHandler` para que el botón/gesto "Atrás" cierre el `BottomSheet` o regrese a la pantalla previa sin salir de la aplicación.
    - En `DashboardScreen.kt` y `DashboardViewModel.kt`, agregar selector de modo (`Normal` / `Directo`) en la tarjeta de Marcación Laboral, validar errores/advertencias en la respuesta (`asistenciaIsError`), y corregir el solapamiento táctil del FAB `"Transacción"` con la tarjeta de marcación y la barra inferior (`Spacer` inferior y jerarquía limpia).
    - En `DashboardRepositoryImpl.kt` y `DashboardViewModel.kt`, integrar recálculo dinámico de `Balance Actual`, `Margen Libre` y `Posición IVA` sincronizado con los movimientos y obligaciones locales/remotos.
  - **Criterio de aceptación:** Tests unitarios en `DashboardViewModelTest` y `DashboardRepositoryImplTest` verificando reactividad de saldos, validación de marcación y `./gradlew testDebugUnitTest` en verde.

- [x] **Tarea 3: Libro Diario — Eliminación de QR Redundante, Filtro por Rango de Fechas, Vista de Planilla y Exportación PDF 100% Local (Puntos 7 y 8)**
  - **Alcance:**
    - Eliminar el botón QR de la esquina superior derecha en `TransaccionesScreen.kt`.
    - En `TransaccionesViewModel.kt`, añadir filtrado reactivo por rango de fechas (`fechaDesde`, `fechaHasta`) combinado con el filtro de tipo (`Todos`, `Ingresos`, `Gastos`), exponiendo la lista filtrada actual.
    - En `TransaccionesScreen.kt`, agregar los selectores de rango de fechas en la cabecera, el selector para alternar entre **Vista de Tarjetas** y **Vista de Planilla/Tabla**, y el botón **Exportar PDF** en la barra superior.
    - Crear `utils/LocalDiarioPdfGenerator.kt` usando `android.graphics.pdf.PdfDocument` para generar localmente el PDF del conjunto de datos actual en `cacheDir/comprobantes/`, notificar con `ComprobanteNotificationHelper` y abrir con `FileProvider`.
  - **Criterio de aceptación:** Tests unitarios de filtrado por rango de fechas en `TransaccionesViewModelTest` y generación de PDF local, con `./gradlew testDebugUnitTest` en verde.

- [x] **Tarea 4: Avisos y Eventos — Edición de Tarjetas, Repetición, Hasta 5 Hitos Previos y Alarmas en Segundo Plano No Ancladas (Puntos 3b, 4 y 12)**
  - **Alcance:**
    - Crear `utils/AvisoAlarmScheduler.kt` y `receiver/AvisoAlarmReceiver.kt` (registrado en `AndroidManifest.xml`) con `AlarmManager` para emitir notificaciones descartables (`setAutoCancel(true)`, `setOngoing(false)`) en la hora exacta y en los hasta 5 hitos previos, con reprogramación automática según la repetición (`DIARIA`, `SEMANAL`, `MENSUAL`, `ANUAL`).
    - En `AvisosViewModel.kt` y `AvisosScreen.kt`, implementar el formulario unificado de **Crear / Editar Aviso** (con pre-llenado editable desde IA, edición al tocar cualquier tarjeta existente, selector de repetición y configurador de hasta 5 hitos previos).
  - **Criterio de aceptación:** Tests unitarios en `AvisosViewModelTest` y test de cálculo de programación de hitos/repetición, con `./gradlew testDebugUnitTest` en verde.

- [x] **Tarea 5: Deudas y Obligaciones — Eliminar Deuda, Cancelar Simuladas, Simulador Ampliado y Nueva Obligación Completa (Puntos 9 y 10)**
  - **Alcance:**
    - En `ObligacionesScreen.kt` y `ObligacionesViewModel.kt`:
      - Añadir icono/botón de eliminar con confirmación en las tarjetas de deuda y botón `"Cancelar"` en las obligaciones simuladas.
      - Ampliar `SimuladorCuotasModal` con campos de Concepto, Entidad, Monto Total, Cantidad de Cuotas y Monto de cada Cuota (auto-calculado y editable), más botón para pasar los datos pre-llenados al formulario de Nueva Obligación.
      - Ampliar `CrearObligacionModal` con todos los campos (`Entidad`, `Nombre/Concepto`, `Tipo`, `Monto Total`, `Cantidad de Cuotas`, `Monto de Cuota`, `Día de Vencimiento`, `Fecha Primer Vencimiento`, `Estado Inicial`, `Observaciones`) y mostrar los detalles en `ObligacionItem`.
      - Eliminar el archivo huérfano `SimuladorModal.kt`.
  - **Criterio de aceptación:** Tests unitarios en `ObligacionesViewModelTest` (creación completa, eliminación, cancelación de simuladas y simulación con cuota editable) y `./gradlew testDebugUnitTest` en verde.

- [x] **Tarea 6: Sistema y Soporte — Bloqueo del Celular para Editar/Ver API Key, Retiro de Cierre Mensual, Tickets Solo Lectura de Estado y Logs Dinámicos (Puntos 11, 13, 14 y 15)**
  - **Alcance:**
    - Crear `utils/DeviceAuthHelper.kt` para requerir el bloqueo del celular (`KeyguardManager` / `DEVICE_CREDENTIAL` / biometría) al pulsar `"Desbloquear para Editar"`, el icono del candado o `"Ver"` API Key en `SistemaScreen.kt`.
    - Eliminar la tarjeta `"Cierre Contable Mensual"` de `SistemaScreen.kt`.
    - Eliminar el diálogo `"Atender Ticket"` y la edición de estado en `SistemaScreen.kt`, dejando la visualización de tickets, filtros por estado y creación de nuevo ticket.
    - Crear `utils/AppLogBuffer.kt` e integrarlo en `SistemaViewModel.kt` y `SistemaScreen.kt` con botón de actualización rápida y marcas de tiempo para que `"Registros y Logs"` se actualice dinámicamente.
  - **Criterio de aceptación:** Tests unitarios en `SistemaViewModelTest` y suite completa `./gradlew testDebugUnitTest` pasando al 100%.
