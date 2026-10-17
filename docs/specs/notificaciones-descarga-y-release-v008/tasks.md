# Checklist de Tareas Atómicas de Implementación (Tasks)
## Módulo: Notificaciones Push Locales de Descarga de Comprobantes PDF y Publicación de Release v0.0.8

- **Fecha:** 17 de Octubre de 2026
- **Versión de Tareas:** 1.0.0
- **Fase:** 0 (Especificación y Arquitectura)
- **Estado:** En Revisión (Gate Obligatorio de Planificación)

---

### Tareas Atómicas de Construcción

- [x] **Tarea 1: Implementar `ComprobanteNotificationHelper` y sus pruebas unitarias**
  - **Archivos:**
    - `app/src/main/java/com/gamma/mobile/utils/ComprobanteNotificationHelper.kt`
    - `app/src/test/java/com/gamma/mobile/utils/ComprobanteNotificationHelperTest.kt`
  - **Acción:**
    - Crear `ComprobanteNotificationHelper` con construcción pura de payload (`buildPayload`) y despacho seguro de notificación local (`mostrarNotificacionDescarga`) con `NotificationChannel`, verificación de permiso `POST_NOTIFICATIONS` en API 33+, y `PendingIntent` vía `FileProvider`.
    - Crear tests unitarios en `ComprobanteNotificationHelperTest.kt` verificando la generación de payloads, títulos, mensajes e identificadores.
  - **Criterio de Aceptación:** `./gradlew testDebugUnitTest` pasa al 100% incluyendo los nuevos tests de `ComprobanteNotificationHelperTest`.

- [x] **Tarea 2: Integrar notificación local al completar descarga de comprobantes en `SistemaViewModel`, `AsistenciaScreen` y permiso runtime en `MainActivity`**
  - **Archivos:**
    - `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaViewModel.kt`
    - `app/src/main/java/com/gamma/mobile/presentation/asistencia/AsistenciaScreen.kt`
    - `app/src/main/java/com/gamma/mobile/MainActivity.kt`
  - **Acción:**
    - En `SistemaViewModel.abrirPdfConCache`, invocar `ComprobanteNotificationHelper.mostrarNotificacionDescarga(context, file, periodo)` cuando se descarga un comprobante desde el backend.
    - En `AsistenciaScreen.CierreTabContent`, guardar el PDF descargado en `File(context.cacheDir, "comprobantes")`, invocar `ComprobanteNotificationHelper.mostrarNotificacionDescarga(context, tempFile, hoja)` yresetear o mantener la apertura segura vía `FileProvider`.
    - En `MainActivity.onCreate`, solicitar el permiso `Manifest.permission.POST_NOTIFICATIONS` en Android 13+ (`TIRAMISU`) si aún no está concedido.
  - **Criterio de Aceptación:** Compilación limpia y `./gradlew testDebugUnitTest` pasando al 100% sin regresiones.

- [x] **Tarea 3: Compilar y verificar APK Release firmado `v0.0.8` y preparar publicación en GitHub Releases**
  - **Archivos:**
    - `app/build/outputs/apk/release/gamma-mobile-v0.0.8-release.apk`
    - `HANDOFF.md`
  - **Acción:**
    - Recompilar `./gradlew assembleRelease` con las notificaciones locales integradas y verificar firma v2 con `apksigner`.
    - Presentar el Gate de Autorización de Despliegue (Sección 5) y, tras aprobación del usuario, publicar el tag `v0.0.8` y subir el binario `gamma-mobile-v0.0.8-release.apk` a GitHub Releases.
  - **Criterio de Aceptación:** APK firmado verificado con `apksigner`, árbol de Git limpio y release `v0.0.8` listo/publicado según autorización.
