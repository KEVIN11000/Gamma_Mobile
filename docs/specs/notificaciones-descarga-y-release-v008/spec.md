# Especificación de Requerimiento (Spec)
## Módulo: Notificaciones Push Locales de Descarga de Comprobantes PDF y Publicación de Release v0.0.8

- **Fecha:** 17 de Octubre de 2026
- **Versión:** 1.0.0
- **Fase:** 0 (Especificación y Arquitectura)
- **Estado:** En Revisión (Gate Obligatorio de Planificación)

---

### 1. Contexto y Justificación
Tras el cierre del ticket `TCK-004`, la sincronización en producción de `Gamma_bot` y la generación firmada del APK `gamma-mobile-v0.0.8-release.apk`, el documento `HANDOFF.md` registra dos pendientes explícitos para completar el ciclo de la versión `v0.0.8`:
1. **Frontend (Notificación Push Local en Android):** Cuando el usuario descarga un comprobante PDF (Cierre Contable en `SistemaScreen` o Reporte de Cierre Mensual en `AsistenciaScreen`), la descarga se ejecuta de forma asíncrona en `Dispatchers.IO`. Incorporar una notificación push local del sistema operativo al finalizar la descarga permite confirmar visualmente la disponibilidad del archivo en el dispositivo e interactuar con la notificación (`PendingIntent`) para abrir el PDF directamente con el visor del sistema en cualquier momento.
2. **Infraestructura (GitHub Release `v0.0.8`):** Publicar formalmente el tag `v0.0.8` y subir el binario firmado `gamma-mobile-v0.0.8-release.apk` a GitHub Releases (`KEVIN11000/Gamma_Mobile`) para que el verificador autónomo de actualizaciones (`CheckAppUpdateUseCase`) y el canal de distribución reflejen la versión `v0.0.8`.

---

### 2. Alcance (In-Scope)
- **Notificaciones Push Locales de Comprobantes (`ComprobanteNotificationHelper`):**
  - Creación y registro del canal de notificaciones `"comprobantes_channel"` (`"Comprobantes y Reportes PDF"`, importancia `IMPORTANCE_DEFAULT` / `IMPORTANCE_HIGH`).
  - Emisión de notificación local con `NotificationCompat.Builder` al completarse exitosamente la descarga o guardado en caché de un comprobante PDF:
    * Título descriptivo (ej. `"Comprobante descargado"`).
    * Cuerpo con el nombre o período del comprobante (ej. `"El reporte de Octubre 2026 está listo para abrir."`).
    * Acción al pulsar (`contentIntent` con `PendingIntent.getActivity` y `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`) que abre el archivo PDF local mediante `FileProvider` (`com.gamma.mobile.fileprovider`) y `FLAG_GRANT_READ_URI_PERMISSION`.
    * `setAutoCancel(true)` para descartar la notificación al tocarla.
  - Verificación defensiva de permiso `android.permission.POST_NOTIFICATIONS` en Android 13+ (`TIRAMISU`, API 33+): si el permiso no está concedido, la descarga y apertura del PDF continúan normalmente sin lanzar `SecurityException`.
  - Solicitud no intrusiva del permiso `POST_NOTIFICATIONS` en `MainActivity` para dispositivos Android 13+ (`API 33+`).
  - Integración en ambos flujos de descarga de comprobantes:
    1. `SistemaViewModel.abrirPdfConCache` (Cierre Contable en pestaña Sistema).
    2. `AsistenciaScreen.CierreTabContent` / `CierreViewModel` (Reporte PDF de Cierre Laboral/Contable en pestaña Compromisos -> Asistencia).
- **Publicación de Release `v0.0.8` en GitHub Releases:**
  - Creación del tag Git `v0.0.8` y publicación del Release `v0.0.8` en `KEVIN11000/Gamma_Mobile` adjuntando el artefacto firmado `gamma-mobile-v0.0.8-release.apk` (ejecutado tras la autorización explícita del usuario en el Gate de Despliegue).

---

### 3. Casos de Uso y Estados de Interfaz (UX)

#### CU-01: Descarga de Comprobante en `SistemaScreen` con Notificación Local
- **Actor:** Usuario administrativo en Gamma Mobile.
- **Entradas:** Selección de período (ej. `"Octubre 2026"`) y pulsación en `"Ver Comprobante PDF 📄"`.
- **Estados Visuales:**
  - `Loading` (`isDescargandoPdf = true`): Botón deshabilitado con `CircularProgressIndicator` y texto `"Descargando Comprobante..."`.
  - `Success`: Archivo persistido en `cacheDir/comprobantes/cierre_contable_<periodo>.pdf`, emisión de notificación push local en la barra de estado de Android con `PendingIntent` de apertura, y lanzamiento del visor PDF.
  - `Error`: Mensaje en `Toast` / `cierreMensaje` indicando el motivo del fallo sin emitir notificación falsa.

#### CU-02: Descarga de Reporte PDF en `AsistenciaScreen` (`CierreTabContent`) con Notificación Local
- **Actor:** Usuario administrativo en Gamma Mobile.
- **Entradas:** Pulsación en `"Ver / Descargar Reporte PDF"` tras ejecutar cierre mensual.
- **Estados Visuales:**
  - `Loading` (`CierreUiState.DownloadingPdf`): Indicador `"Descargando reporte PDF..."`.
  - `Success` (`CierreUiState.PdfDownloaded`): Escritura del PDF en caché, emisión de notificación local interactiva y apertura del visor PDF.
  - `Error` (`CierreUiState.Error`): Diálogo de alerta con detalle del error.

#### CU-03: Publicación y Detección de Release `v0.0.8`
- **Actor:** Administrador / Pipeline de Release.
- **Entradas:** Binario firmado `gamma-mobile-v0.0.8-release.apk` y tag `v0.0.8`.
- **Salidas:** Release publicado en GitHub Releases con asset descargable; `CheckAppUpdateUseCase` reconoce `v0.0.8` como `UpToDate` en instalaciones `0.0.8` y como `UpdateAvailable` en versiones previas.

---

### 4. Lo que queda Excluido (No-Scope)
- Integración de servicios push externos en la nube (Firebase Cloud Messaging / FCM): las notificaciones requeridas son estrictamente locales al dispositivo Android (`NotificationManager`).
- Modificaciones en el backend `Gamma_bot` (ya se encuentra en `0.0.8` / `versionCode 45` y desplegado en producción).
- Visor PDF embebido en canvas dentro de Compose.
