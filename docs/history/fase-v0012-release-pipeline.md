# HANDOFF — Gamma Mobile

**Fase cerrada:** Depuración e2e en Dispositivo Móvil y Pipeline de Releases Oficiales (v0.0.12-release-pipeline)
**Fecha de cierre:** 2026-11-05
**Estado del build/tests:** ✅ pasando — 220 tests unitarios (`.\gradlew.bat testDebugUnitTest`) y `assembleRelease` completados con éxito. Auditoría criptográfica de firma verificada con apksigner (SHA-256: `2db8d51c88ad21c640a3bffb9fe04c50603e81182f8e1b7e54b3a568a580ffe7`).

---

## 1. Estado actual

- **Conectividad e2e verificada:** Conexión auditada en vivo en dispositivo físico (HONOR Magic6 Lite) vía ADB inalámbrico (`10.51.80.190:39481`), confirmando sincronización exitosa en todas las pantallas (Dashboard, Diario con transacciones reales de Google Sheets, Compromisos con Deuda Foco, Tickets de Soporte y Logs de Sistema).
- **Pipeline de Releases Oficiales:** Creado `.github/workflows/release.yml` con compuerta de auditoría criptográfica inviolable, prohibición estricta de silent fallbacks efímeros y publicación automática de APKs en GitHub Releases.
- **Soporte de Keystore Dinámico:** `app/build.gradle.kts` soporta la variable `KEYSTORE_FILE` para inyección segura de certificados en CI runners y máquinas locales.
- **Instrumentación de Red:** Logging en vivo de peticiones HTTP en modo Debug (`GammaNetwork`) y captura automática de fallos de red en `AppLogBuffer` para diagnóstico en pantalla.
- **220 tests unitarios pasando** en Gradle.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Implementar compuerta criptográfica obligatoria en GitHub Actions (`apksigner verify --verbose --print-certs`).
  **Por qué:** Impedir caídas silenciosas a debug keystores o certificados efímeros en CI/CD que provoquen incompatibilidad de actualización (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`).
- **Decisión:** Mantener el almacén de firmas `gamma_release.jks` fuera del árbol de Git, protegido por `.gitignore` e inyectado como secreto Base64 en CI (`RELEASE_KEYSTORE_BASE64`).
  **Por qué:** Cumplir con la política de seguridad estricta y fuga cero de credenciales del proyecto.
- **Decisión:** Instrumentar `SafeApiCall` con `AppLogBuffer` local sin dependencias en stubs de Android.
  **Por qué:** Permitir que los usuarios y desarrolladores auditen fallos de red directamente en la pantalla de Sistema de la app sin requerir una PC conectada, manteniendo 100% de compatibilidad con pruebas unitarias JVM puras.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `.github/workflows/release.yml` | Workflow de CI/CD para compilación, auditoría criptográfica SHA-256 y publicación de releases. |
| `app/build.gradle.kts` | Soporte para `KEYSTORE_FILE` en `signingConfigs.release`. |
| `app/src/main/java/com/gamma/mobile/di/NetworkModule.kt` | Interceptor de red en modo Debug para trazas de peticiones HTTP. |
| `app/src/main/java/com/gamma/mobile/data/remote/SafeApiCall.kt` | Registro automático de errores y excepciones de red en `AppLogBuffer`. |
| `docs/ESPECIFICACIONES_API_MOBILE.md` | Catálogo maestro de contratos de API y 25 endpoints del cliente móvil. |
| `docs/history/fase-v0012-deudas-estabilidad.md` | Histórico de handoff archivado. |
| `HANDOFF.md` | Registro de estado del pipeline de releases y depuración móvil. |

## 4. Pendientes explícitos para la próxima fase

- [ ] Instalar el APK Release oficial (`app-release.apk`) en el dispositivo físico tras desinstalar la versión debug previa.
- [ ] Ejecutar `git commit` y `git push origin main` en `GammaMobile` previa autorización textual del usuario.
- [ ] Adoptar el consumo del endpoint de hidratación global atómica `GET /api/v1/bootstrap` en `GammaMobile` (ya disponible en backend).
- [ ] Diseñar endpoint de resolución de proveedores por RUC KuDE (`POST /api/v1/sifen/consultar-ruc`) en `Gamma_bot`.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Conflicto de firmas en dispositivos que mantengan instalada la versión debug previa al intentar actualizar desde GitHub Releases.
  **Impacto:** Error de sistema Android `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
  **Mitigación:** Desinstalación manual previa de la app debug antes de instalar la versión Release oficial firmada.

## 6. Cómo verificar que este handoff sigue vigente

```bash
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleRelease
git status
```
