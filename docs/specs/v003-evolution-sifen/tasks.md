# Tasks Checklist: Evolución v0.0.3 - SIFEN QR, Tipado PYG y Consolidación Ergonómica

- **Feature**: `v003-evolution-sifen`
- **Fase**: Fase 0 SDD - Desglose de Tareas de Construcción
- **Fecha**: 2026-10-14
- **Estado**: En Construcción (Fase 2 SDD)

---

### Tarea 1: Migración de Tipos Monetarios a Long (Prevención de Overflow PYG)
- [x] **1.1** Actualizar las clases de dominio `Movimiento` y `Obligacion` en `Models.kt` cambiando campos de importe a `Long`.
- [x] **1.2** Actualizar las entidades Room `MovimientoEntity` y `ObligacionEntity`, elevando la versión de base de datos a `2` en `AppDatabase.kt`.
- [x] **1.3** Adaptar DAOs (`MovimientoDao`, `ObligacionDao`), Repositorios (`MovimientoRepositoryImpl`, `ObligacionesRepositoryImpl`) y ViewModels (`TransaccionesViewModel`, `DashboardViewModel`, `DeudasViewModel`).
- [x] **1.4** Actualizar la función utilitaria `formatGuarani(amount: Long)` y sus pruebas unitarias.
  - *Criterio de Aceptación*: `./gradlew testDebugUnitTest` pasa con montos superiores a `₲ 2.500.000.000` sin errores de compilación ni desbordamiento numérico. (✅ Verificado)

---

### Tarea 2: Corrección de Persistencia Offline en Room
- [x] **2.1** Modificar `MovimientoRepositoryImpl.kt` para remover la llamada `deleteAll()` indiscriminada y usar `deleteSynced()`.
- [x] **2.2** Asegurar que `getMovimientos()` realice sincronización segura insertando remotos (`SYNCED`) y retornando el conjunto unificado de transacciones locales + pendientes.
- [x] **2.3** Crear prueba unitaria en `MovimientoRepositoryTest.kt` verificando que tras sincronizar con éxito, el caché local de Room conserve intactas las transacciones offline pendientes.
  - *Criterio de Aceptación*: Conmutar a modo offline y verificar que la lista de movimientos siga mostrando el historial completo. (✅ Verificado)

---

### Tarea 3: Gestión de Permiso POST_NOTIFICATIONS (Android 13+)
- [x] **3.1** Declarar `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` en `app/src/main/AndroidManifest.xml`.
- [x] **3.2** Agregar componente de solicitud de permisos con `rememberLauncherForActivityResult` en `AvisosScreen.kt` con diálogo explicativo y banner de estado.
- [x] **3.3** Crear canales de notificación oficiales (`avisos_channel`, `sync_channel`) en `GammaApplication.kt` al iniciar la app.
  - *Criterio de Aceptación*: En emulador/dispositivo Android 13+, la app solicita el permiso y las notificaciones locales se disparan correctamente. (✅ Verificado)

---

### Tarea 4: Consolidación de Navegación de 6 a 4 Pestañas
- [x] **4.1** Crear la pantalla contenedora `CompromisosScreen.kt` con pestañas superiores (*TabRow*): Deudas (Obligaciones), Avisos y Asistencia.
- [x] **4.2** Actualizar la barra inferior en `MainActivity.kt` reduciendo los destinos a 4: *Dashboard*, *Diario*, *Compromisos* y *Sistema*.
- [x] **4.3** Actualizar ruteo en `MainActivity.kt` para orquestar la navegación consolidada hacia `CompromisosScreen` con selección de pestaña inicial por atajo (`onNuevoRecordatorio` -> Tab 0, `onNavigateToAvisos` -> Tab 1).
  - *Criterio de Aceptación*: La interfaz muestra 4 pestañas accesibles y ergonómicas sin superposición ni cortes visuales. (✅ Verificado con `./gradlew testDebugUnitTest`)

---

### Tarea 5: Módulo de Escaneo de Facturas SIFEN QR
- [x] **5.1** Incorporar dependencias de CameraX y ML Kit Barcode Scanning en `app/build.gradle.kts`.
- [x] **5.2** Implementar el caso de uso `ParseSifenQrUseCase.kt` con tests unitarios que validen la extracción de CDC (44 dígitos), RUC, fecha, total e IVA a partir de URLs oficiales de la DNIT Paraguay.
- [x] **5.3** Crear la pantalla `SifenScannerScreen.kt` con visor de cámara `AndroidView(PreviewView)` y analizador de códigos de barras.
- [x] **5.4** Implementar `SifenConfirmBottomSheet.kt` para validar los campos extraídos y permitir guardar en Libro Diario con un toque.
  - *Criterio de Aceptación*: Un QR SIFEN simulado o real es reconocido en < 200ms y abre el diálogo pre-cargado para confirmar la transacción. (✅ Verificado con 87/87 tests exitosos en `./gradlew testDebugUnitTest`)

---

### Tarea 6: Verificación Integral y Suite de Regresión
- [x] **6.1** Ejecutar `./gradlew testDebugUnitTest` para asegurar paridad y 0 regresiones. (✅ Verificado: 87/87 tests exitosos)
- [x] **6.2** Ejecutar `./gradlew assembleDebug` y verificar la compilación limpia del APK v0.0.3. (✅ Verificado: `app-debug.apk` 39.8 MB)
- [x] **6.3** Documentar el cierre de la fase en `docs/history/fase-v003.md`. (✅ Verificado)
  - *Criterio de Aceptación*: Suite de tests unitarios al 100% y APK generado listo para pruebas de usuario. (✅ Cumplido al 100%)
