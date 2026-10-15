# Checklist de Tareas Atómicas: Versionado de Distribuibles (v0.0.4)

## Fase 0: Especificación y Arquitectura
- [x] Tarea 0.1: Análisis de releases en GitHub, nombres de distribuibles y endpoints de salud.
- [x] Tarea 0.2: Redacción de `spec.md`, `plan.md` y `tasks.md` en `docs/specs/v004-distribuibles-versioning/`.
- [x] **GATE DE COMPUERTA DE PLANIFICACIÓN**: Aprobación explícita del usuario recibida ("si, aplica").

---

## Fase 2: Implementación Modular Guiada por Tareas

### Tarea 1: Centralización del Versionado y Configuración en Gradle
- **Objetivo:** Crear el archivo `VERSION` con `0.0.4` y configurar `app/build.gradle.kts` para calcular dinámicamente `versionName` y renombrar los archivos APK distribuibles a `gamma-mobile-v${appVersion}-${variant.name}.apk`.
- **Archivos a modificar/crear:**
  - `VERSION`
  - `app/build.gradle.kts`
  - `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt`
- **Criterio de Aceptación / Prueba:**
  - `./gradlew assembleDebug` compila y genera `app/build/outputs/apk/debug/gamma-mobile-v0.0.4-debug.apk`.
  - `./gradlew testDebugUnitTest` pasa al 100%.
- **Estado:** [x]

### Tarea 2: Compilación y Firma de Release Versionada
- **Objetivo:** Ejecutar `./gradlew assembleRelease` utilizando el keystore local `gamma_release.jks` y validar la presencia de `gamma-mobile-v0.0.4-release.apk` debidamente firmado, generando además copia `app-release.apk` para retrocompatibilidad.
- **Archivos a verificar:**
  - `app/build/outputs/apk/release/gamma-mobile-v0.0.4-release.apk`
  - `app/build/outputs/apk/release/output-metadata.json`
- **Criterio de Aceptación / Prueba:**
  - `output-metadata.json` contiene `versionName: "0.0.4+..."` y `outputFile: "gamma-mobile-v0.0.4-release.apk"`.
- **Estado:** [x]

### Tarea 3: Actualización de Endpoint de Salud en Backend (`Gamma_bot`)
- **Objetivo:** Actualizar en `Gamma_bot/src/api/blueprint.py` los campos `latest_app_version`, `latest_app_version_code` y `download_url` apuntando al release `v0.0.4` de `Gamma_Mobile`.
- **Archivos a modificar:**
  - `../Gamma_bot/src/api/blueprint.py`
  - `../Gamma_bot/tests/test_mobile_api.py` (si valida versión explícita)
- **Criterio de Aceptación / Prueba:**
  - `pytest tests/test_mobile_api.py` pasa exitosamente.
  - Endpoint `/health` responde con la versión `0.0.4` y URL correcta.
- **Estado:** [x]

### Tarea 4: Generación de Notas de Release y Auditoría Git
- **Objetivo:** Preparar el manifiesto de Release para GitHub (`v0.0.4`), auditar el estado de Git con `git status` y `git diff --cached --stat` sin commitear secretos ni archivos residuales.
- **Criterio de Aceptación / Prueba:**
  - Git limpio según gobernanza.
  - Reporte consolidado presentado al usuario para el Gate de Autorización de Despliegue.
- **Estado:** [x]
