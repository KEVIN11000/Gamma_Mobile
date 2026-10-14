# HANDOFF — Gamma Mobile

**Fase cerrada:** Fase de Cierre Final (F-01 a F-04) & Publicación de Backend
**Fecha de cierre:** 2026-10-13
**Estado del build/tests:** ✅ pasando — Compilación, suites de tests y ensamblado release firmado verificados (`./gradlew testDebugUnitTest assembleDebug assembleRelease`).

---

## 1. Estado actual

- **Seguridad (F-01):** Política `network_security_config.xml` configurada restringiendo tráfico HTTP en texto plano exclusivamente a loopback local (`10.0.2.2`, `127.0.0.1`, `localhost`); auditoría de código limpia sin credenciales ni tokens en logs.
- **Validación Integral (F-02):** Suites completas ejecutadas con 49 tareas de Gradle en verde, reporte de lint generado (`lint-results-debug.html`), y 6/6 tests de especificación de backend pasando en pytest.
- **Release y Empaquetado (F-03):** APK firmado generado en `app/build/outputs/apk/release/app-release.apk` con versión semántica `0.0.1+3048e4c` (Code: 16), validado exitosamente con `apksigner` bajo esquema V2.
- **Canal de Actualizaciones:** Pantalla `SistemaScreen` adaptada para consultar `download_url` desde el backend y ofrecer descarga directa de nuevas versiones.
- **Backend Integrado y Subido:** Rama `feat/gamma-mobile-backend` publicada en GitHub (`KEVIN11000/Gamma_bot`), incorporando el Blueprint de Flask en `/api/v1`, autenticación `X-API-KEY` y comando de Telegram `/app` con 162/162 tests pasando.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Distribución inicial desacoplada vía GitHub Releases y Telegram.
  **Por qué:** Evita depender de tiendas comerciales en etapas tempranas, permitiendo a los usuarios actualizar la app directamente desde el bot o mediante el enlace provisto por el backend en la pestaña Sistema.
- **Decisión:** Mantenimiento de Blueprint síncrono en Flask para PythonAnywhere.
  **Por qué:** Se descartó FastAPI debido a las limitaciones del worker WSGI de PythonAnywhere, asegurando 100% de compatibilidad con la infraestructura de producción.
- **Decisión:** Keystore de release preservado en almacenamiento seguro fuera del repositorio (`~/.android/gamma_release.jks`).
  **Por qué:** Protege la clave de firma contra fugas en el control de versiones.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `app/src/main/res/xml/network_security_config.xml` | Definición de dominios permitidos para desarrollo y bloqueo de cleartext en producción. |
| `app/src/main/AndroidManifest.xml` | Vinculación del `networkSecurityConfig`. |
| `app/build.gradle.kts` | Configuración de signingConfig para la variante `release` y generación dinámica de versión. |
| `app/src/main/java/com/gamma/mobile/domain/model/SistemaModels.kt` | Campos `latestAppVersion`, `latestAppVersionCode` y `downloadUrl` en `HealthStatus`. |
| `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt` | Botón interactivo para descarga de actualización de APK. |
| `docs/specs/gamma-mobile/tasks.md` | Marcadas F-01, F-02, F-03 y F-04 como completadas al 100%. |
| `Gamma_bot (src/api/*)` | Blueprint `/api/v1`, DTOs con Pydantic y seguridad `X-API-KEY`. |
| `Gamma_bot (src/flask_app.py)` | Registro del Blueprint `/api/v1` en la aplicación Flask principal. |
| `Gamma_bot (src/com/handlers/base.py)` | Comando `/app` en Telegram para descarga del APK. |

## 4. Pendientes explícitos para la próxima fase

- [ ] Revisión de Pull Request y fusión de `feat/gamma-mobile-backend` a `Main-stable` en `Gamma_bot`.
- [ ] Creación del primer GitHub Release en `Gamma_bot` (o `GammaMobile`) adjuntando `app-release.apk`.
- [ ] Verificación en vivo de conexión entre la app instalada en dispositivo físico/emulador y el servidor en PythonAnywhere (`kevin11000.pythonanywhere.com/api/v1/health`).

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Permiso de instalación de fuentes desconocidas en dispositivos Android.
  **Impacto:** Los usuarios deben conceder permiso a la app o navegador para actualizar APKs descargados fuera de Google Play.
  **Mitigación sugerida:** El diálogo y comando de Telegram instruyen claramente cómo habilitar la instalación de la actualización.

## 6. Cómo verificar que este handoff sigue vigente

```bash
./gradlew testDebugUnitTest assembleDebug
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
