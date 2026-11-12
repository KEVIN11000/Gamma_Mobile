# HANDOFF — Gamma Mobile

**Fase cerrada:** Release v0.0.14 y Remediación de Seguridad (`seguridad-gamma-mobile`)
**Fecha de cierre:** 2026-11-12
**Estado del build/tests:** ✅ pasando — Suite completa `.\gradlew.bat testDebugUnitTest`, compilación `assembleRelease` y auditoría `apksigner` v2 verificadas tras los últimos cambios.

---

## 1. Estado actual

- **Refinamiento Contable y Auto-descubrimiento (`v0.0.14`):** `CapacidadesRepositoryImpl` (`/api/v1/capacidades`) con caché offline, alias en `BootstrapDto`, blindaje `saldoActual > 0L` en `Models.kt` y `PUT /api/v1/obligaciones/{id}` (`ObligacionModelTest`).
- **Módulo Presupuesto Base:** `PresupuestoScreen` y `PresupuestoViewModel` operativos para salario base, gastos fijos por categoría y techos variables con semáforos, cubiertos por `PresupuestoRepositoryTest` y `PresupuestoViewModelTest`.
- **Remediación de Seguridad (`CWE-200` / `CWE-798`):** `DEFAULT_BASE_URL` y firma local desacoplados vía `BuildConfig` + `local.properties`/CI, `allowBackup="false"` en `AndroidManifest.xml` y tests sanitizados (`SecureStorageTest`).
- **Release `v0.0.14` empaquetado localmente:** Binario `app/build/outputs/apk/release/gamma-mobile-v0.0.14-release.apk` compilado y verificado criptográficamente con el certificado oficial (`SHA-256: 2db8d51c88ad21c640a3bffb9fe04c50603e81182f8e1b7e54b3a568a580ffe7`).

## 2. Decisiones tomadas en esta fase

- **Decisión:** Desacoplar `DEFAULT_BASE_URL` vía `BuildConfig` leyendo `GAMMA_API_BASE_URL` desde `local.properties` / CI con fallback `"https://api.gamma.example.com/api/v1/"`.
  **Por qué:** Mitiga `CWE-200` sin volver privado el repositorio GitHub, preservando el Auto-Update público sin token (`GitHubReleaseApi`).
  **Evidencia (`ref`):** `test:SecureStorageTest`, `invariant:SYSTEM_MAP#INV-07`
  **Alternativas descartadas:** Pasar el repo a privado solamente (rompe `GET /repos/.../releases/latest` sin token).
- **Decisión:** Deshabilitar `android:allowBackup="false"` en `AndroidManifest.xml` y eliminar contraseñas fallback de `app/build.gradle.kts`.
  **Por qué:** Evita extracción ADB de Room (`gamma_db`) y remueve literales de firma (`CWE-798`) del build script.
  **Evidencia (`ref`):** `bug:seguridad-gamma-mobile`, `invariant:SYSTEM_MAP#INV-07`
- **Decisión:** Priorizar `saldoActual > 0L` como autoridad contable en `Models.kt` sobre el texto `estado` de Google Sheets.
  **Por qué:** Evita que desfasajes en planillas oculten deudas vivas en la pestaña `Saldadas`.
  **Evidencia (`ref`):** `spec:v014-refinamiento-contable-ux/spec.md`, `test:ObligacionModelTest`, `invariant:SYSTEM_MAP#INV-02`

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `app/build.gradle.kts` | Inyección de `BuildConfig.DEFAULT_BASE_URL` y lectura de firma desde `local.properties` / env vars |
| `app/src/main/java/com/gamma/mobile/data/repository/SecureStorageRepositoryImpl.kt` | `DEFAULT_BASE_URL` consumido desde `BuildConfig.DEFAULT_BASE_URL` (`#INV-07`) |
| `app/src/main/java/com/gamma/mobile/di/NetworkModule.kt` | `provideRetrofit` usa `SecureStorageRepositoryImpl.DEFAULT_BASE_URL` |
| `app/src/main/AndroidManifest.xml` | `android:allowBackup="false"` para proteger base de datos financiera local |
| `.github/workflows/release.yml` | Inyección de `secrets.GAMMA_API_BASE_URL` en `Build Release APK` |
| `app/src/test/java/com/gamma/mobile/data/repository/SecureStorageTest.kt` | Nuevo test de contrato para `BuildConfig.DEFAULT_BASE_URL` |
| `docs/SYSTEM_MAP.md`, `README.md`, `docs/ESPECIFICACIONES_API_MOBILE.md` | Actualización de `#INV-07` y sanitización de URLs de producción |
| `docs/specs/v014-refinamiento-contable-ux/*`, `presentation/presupuesto/*` | Feature completo `v0.0.14` (Presupuesto Base, Capacidades, Blindaje de Deudas) |

## 4. Pendientes explícitos para la próxima fase

- [ ] Autorizar y ejecutar `git push origin main` y `git push origin v0.0.14` tras aprobar el Gate de Despliegue para disparar `.github/workflows/release.yml` en GitHub Actions.
- [ ] Configurar el secreto `GAMMA_API_BASE_URL` en *GitHub Repository Settings > Secrets and variables > Actions* antes de publicar releases desde CI.
- [ ] Instalar `gamma-mobile-v0.0.14-release.apk` en el dispositivo físico (HONOR Magic6 Lite) y validar `/capacidades`, Presupuesto Base y edición de deudas en vivo.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Si no se configura `GAMMA_API_BASE_URL` en GitHub Secrets, el APK generado por CI usará el placeholder neutro `https://api.gamma.example.com/api/v1/` hasta que el usuario configure la URL en `SistemaScreen`.
  **Impacto:** Requiere ingreso manual de URL en primera instalación de APKs generados por CI (los compilados localmente con `local.properties` ya incluyen la URL real).
  **Mitigación sugerida:** Registrar `GAMMA_API_BASE_URL` en GitHub Secrets.

## 6. Cómo verificar que este handoff sigue vigente

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleRelease
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\34.0.0\apksigner.bat" verify --verbose --print-certs app/build/outputs/apk/release/gamma-mobile-v0.0.14-release.apk
git status
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
