# Especificación de Requerimiento (Spec) — Versionado de Distribuibles y Sincronización de Releases (v0.0.4)

## 1. Contexto y Problema
Actualmente, el proyecto Gamma Mobile presenta una desincronización entre el estado de desarrollo (`v0.0.4`), los artefactos distribuibles generados por Gradle y los canales de distribución pública y actualización in-app:

1. **Releases en GitHub desactualizados:**
   - La API de GitHub (`/repos/KEVIN11000/Gamma_Mobile/releases`) solo contiene los releases `v0.0.1` y `v0.0.2`.
   - Los tags git `v0.0.3` y `v0.0.4` existen en el repositorio, pero no se publicaron como Releases formales en GitHub ni contienen binarios APK adjuntos.
2. **Nombres de distribuibles genéricos (sin versionado):**
   - El Android Gradle Plugin compila por defecto con nombres estáticos: `app-release.apk` y `app-debug.apk`.
   - Los distribuibles no reflejan el nombre de la app ni la versión en el nombre de archivo, dificultando su distribución externa y verificación visual por parte de testers y usuarios.
3. **`versionName` desfasado en Gradle:**
   - En `app/build.gradle.kts`, `versionName` quedó fijado en `"0.0.3+${getGitCommitHash()}"`, provocando que el APK de la fase `v0.0.4` reporte internamente una versión desactualizada.
   - En `SistemaScreen.kt`, el fallback por defecto sigue indicando `"0.0.2+debug"`.
4. **Canal In-App de actualización apuntando a versión antigua:**
   - El endpoint `GET /api/v1/health` en `Gamma_bot/src/api/blueprint.py` sirve la versión `0.0.2+1746768` con URL hacia `v0.0.2/app-release.apk`.
   - Al pulsar "Descargar actualización oficial (APK)" en la app móvil, el usuario descarga la versión `v0.0.2` en lugar de `v0.0.4`.

---

## 2. Alcance (In-Scope)
- **Centralización del Versionado (`GammaMobile`):**
  - Creación del archivo `VERSION` en la raíz del proyecto como Fuente Única de Verdad (SSOT) conteniendo `0.0.4`.
  - Lectura dinámica del archivo `VERSION` en `app/build.gradle.kts` para calcular `versionName = "$appVersion+${getGitCommitHash()}"`.
  - Actualización del fallback de versión en `SistemaScreen.kt` a `0.0.4+debug`.
- **Versionado Automático en Distribuibles APK:**
  - Configuración en `app/build.gradle.kts` para renombrar los artefactos de salida compilados a:
    - Release: `gamma-mobile-v0.0.4-release.apk`
    - Debug: `gamma-mobile-v0.0.4-debug.apk`
  - Generación de copia o compatibilidad con `app-release.apk` para no romper automatizaciones previas.
- **Sincronización del Endpoint In-App en Backend (`Gamma_bot`):**
  - Actualizar `GET /api/v1/health` en `Gamma_bot/src/api/blueprint.py`:
    - `latest_app_version`: `"0.0.4"`
    - `latest_app_version_code`: commit count actual (`29` o superior).
    - `download_url`: Enlace oficial al APK del Release `v0.0.4`.
- **Preparación de Release `v0.0.4`:**
  - Compilación verificada del APK release firmado con `gamma_release.jks`.
  - Generación de notas de release completas y comando/script para publicación en GitHub Releases.

---

## 3. No-Scope (Exclusiones Explícitas)
- Modificación de lógica de negocio o contratos contables/asistencia que ya fueron validados en v0.0.4.
- `git push` o publicación directa a GitHub sin confirmación explícita del usuario (Regla de Gobernanza Human-in-the-Loop).

---

## 4. Criterios de Aceptación
1. `./gradlew assembleRelease` y `./gradlew assembleDebug` generan artefactos con la nomenclatura `gamma-mobile-v0.0.4-[variant].apk` en sus respectivas carpetas de salida.
2. `output-metadata.json` en `app/build/outputs/apk/release/` refleja `versionName: "0.0.4+..."`.
3. Todos los tests unitarios (`./gradlew testDebugUnitTest` y `pytest tests/test_mobile_api.py`) continúan pasando al 100%.
4. El endpoint `GET /api/v1/health` en `Gamma_bot` reporta la versión `0.0.4` y la URL de descarga correspondiente.
