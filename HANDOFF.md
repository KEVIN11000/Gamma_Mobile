# HANDOFF — Gamma Mobile & Core Bot

**Fase cerrada:** v0.0.8 — Notificaciones Push Locales de Comprobantes PDF y Publicación en GitHub Releases
**Fecha de cierre:** 2026-10-17
**Estado del build/tests:** ✅ pasando — Compilación y tests verificados al 100% (`./gradlew testDebugUnitTest` con 109 tests unitarios y APK firmado `gamma-mobile-v0.0.8-release.apk` en GammaMobile; 187 tests unitarios en `pytest` en Gamma_bot).

---

## 1. Estado actual

- **Notificaciones Push Locales de Descarga de Comprobantes (`GammaMobile`):**
  - `ComprobanteNotificationHelper` operativo en `app/src/main/java/com/gamma/mobile/utils/ComprobanteNotificationHelper.kt`, cubierto por tests unitarios en `app/src/test/java/com/gamma/mobile/utils/ComprobanteNotificationHelperTest.kt`.
  - Canal `"comprobantes_channel"` ("Comprobantes y Reportes PDF") registrado con `NotificationCompat` y `PendingIntent` (`FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`) conectado a `FileProvider` (`com.gamma.mobile.fileprovider`) para abrir el PDF directamente al tocar la notificación.
  - Emisión de notificación integrada en `SistemaViewModel.abrirPdfConCache` y en `AsistenciaScreen.CierreTabContent`.
  - Solicitud no intrusiva del permiso runtime `android.permission.POST_NOTIFICATIONS` en Android 13+ (`API 33+`) en `MainActivity.onCreate`.
- **Publicación de Release `v0.0.8` en GitHub Releases (`GammaMobile`):**
  - Commit `0b1a5f8` y tag `v0.0.8` publicados en `origin/main` (`KEVIN11000/Gamma_Mobile`).
  - Release `v0.0.8` publicado en `https://github.com/KEVIN11000/Gamma_Mobile/releases/tag/v0.0.8`.
  - Binario firmado `gamma-mobile-v0.0.8-release.apk` (33,295,986 bytes, APK Signature Scheme v2) subido y disponible en `https://github.com/KEVIN11000/Gamma_Mobile/releases/download/v0.0.8/gamma-mobile-v0.0.8-release.apk`.
- **Sincronización con Backend (`Gamma_bot`):**
  - Endpoint `GET /api/v1/health` en `Main-stable` y PythonAnywhere (`kevin11000.pythonanywhere.com`) apuntando a `latest_app_version: "0.0.8"` y al enlace de descarga directa del release `v0.0.8`.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Implementar las notificaciones de descarga mediante `NotificationManager` local y `ComprobanteNotificationHelper` desacoplado con validación defensiva de `POST_NOTIFICATIONS`.
  **Por qué:** Evita dependencias externas (como Firebase Cloud Messaging) y garantiza que la ausencia de permisos de notificación en Android 13+ no bloquee ni interrumpa la apertura del comprobante PDF en primer plano.
  **Alternativas descartadas:** Integrar FCM para eventos originados en el propio dispositivo o lanzar excepciones si el usuario deniega el permiso de notificaciones.
- **Decisión:** Unificar el almacenamiento de comprobantes descargados en `AsistenciaScreen.CierreTabContent` dentro de `context.cacheDir/comprobantes/`.
  **Por qué:** Mantiene paridad con `SistemaViewModel.abrirPdfConCache` y asegura que el `PendingIntent` de la notificación apunte a una ruta estable servida por `FileProvider`.
  **Alternativas descartadas:** Guardar archivos sueltos en la raíz de `cacheDir`.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `app/src/main/java/com/gamma/mobile/utils/ComprobanteNotificationHelper.kt` | Nuevo helper de notificaciones push locales para comprobantes PDF con canal, validación de archivo y `PendingIntent` vía `FileProvider`. |
| `app/src/test/java/com/gamma/mobile/utils/ComprobanteNotificationHelperTest.kt` | Nuevas pruebas unitarias para construcción de payload y validación de archivos PDF. |
| `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaViewModel.kt` | Disparo de notificación local en `abrirPdfConCache` al obtener el comprobante. |
| `app/src/main/java/com/gamma/mobile/presentation/asistencia/AsistenciaScreen.kt` | Guardado en `cacheDir/comprobantes` y disparo de notificación local en `CierreTabContent`. |
| `app/src/main/java/com/gamma/mobile/MainActivity.kt` | Solicitud de permiso runtime `POST_NOTIFICATIONS` en Android 13+ (`TIRAMISU`). |
| `Gamma_bot (src/com/handlers/base.py, tests/test_handlers_base.py)` | Actualización del comando `/app` de Telegram a `v0.0.8` (`gamma-mobile-v0.0.8-release.apk`) y test unitario `test_comando_app`. |
| `docs/specs/notificaciones-descarga-y-release-v008/` | Artefactos SDD (`spec.md`, `plan.md`, `tasks.md`) completados al 100%. |
| `docs/history/fase-v008-deploy.md` | Archivo histórico del handoff anterior. |
| `HANDOFF.md` | Documento de cierre de la fase `v0.0.8`. |

## 4. Pendientes explícitos para la próxima fase

- Sin pendientes abiertos para la versión `v0.0.8` (ciclo cerrado al 100%).

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Si el usuario deniega manualmente el permiso `POST_NOTIFICATIONS` en Android 13+, el sistema operativo omitirá la notificación en la barra de estado.
  **Impacto:** El usuario no verá el aviso en la bandeja de notificaciones al descargar un comprobante.
  **Mitigación sugerida:** `ComprobanteNotificationHelper` comprueba el permiso antes de notificar y el visor PDF se abre igualmente de forma automática en pantalla tras la descarga.

## 6. Cómo verificar que este handoff sigue vigente

```bash
# En Gamma_bot:
& "D:\Program Files\Anaconda\python.exe" -m pytest

# En GammaMobile:
./gradlew testDebugUnitTest
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\34.0.0\apksigner.bat" verify --verbose "app/build/outputs/apk/release/gamma-mobile-v0.0.8-release.apk"
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
