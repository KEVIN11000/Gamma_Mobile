# Checklist de Tareas Atómicas: Verificación de Actualizaciones vía GitHub

## Fase 0: Especificación y Arquitectura
- [x] Tarea 0.1: Análisis de la API pública de GitHub Releases y diseño de contratos.
- [x] Tarea 0.2: Redacción de `spec.md`, `plan.md` y `tasks.md` en `docs/specs/inapp-github-update-checker/`.
- [ ] **GATE DE COMPUERTA DE PLANIFICACIÓN**: Aprobación del usuario antes de iniciar la implementación (Fase 2).

---

## Fase 2: Implementación Modular Guiada por Tareas

### Tarea 1: DTOs, API Service y Caso de Uso de Comparación Semver
- **Objetivo:** Crear los modelos `GitHubReleaseDto`, el endpoint Retrofit en interfaz dedicada `GitHubReleaseApi`, el comparador semántico `SemverUtils` y el caso de uso `CheckAppUpdateUseCase`.
- **Archivos a crear/modificar:**
  - `app/src/main/java/com/gamma/mobile/data/remote/GitHubReleaseApi.kt`
  - `app/src/main/java/com/gamma/mobile/domain/model/UpdateModels.kt`
  - `app/src/main/java/com/gamma/mobile/domain/util/SemverUtils.kt`
  - `app/src/main/java/com/gamma/mobile/domain/usecase/CheckAppUpdateUseCase.kt`
  - `app/src/main/java/com/gamma/mobile/di/NetworkModule.kt`
  - `app/src/test/java/com/gamma/mobile/domain/util/SemverUtilsTest.kt`
- **Criterio de Aceptación / Prueba:**
  - Pruebas unitarias de `SemverUtilsTest` pasando al 100%.
  - `./gradlew testDebugUnitTest --tests *SemverUtilsTest*` en verde.
- **Estado:** [x]

### Tarea 2: Integración en `SistemaViewModel` y `SistemaRepository`
- **Objetivo:** Inyectar `CheckAppUpdateUseCase` en `SistemaViewModel`, extender `SistemaUiState` con `UpdateStatus`, y ejecutar la comprobación de versión durante `cargarDatosSistema()`.
- **Archivos a modificar:**
  - `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaViewModel.kt`
  - `app/src/test/java/com/gamma/mobile/presentation/sistema/SistemaViewModelTest.kt`
- **Criterio de Aceptación / Prueba:**
  - `SistemaViewModelTest` valida la transición a `UpdateAvailable` y `UpToDate`.
  - `./gradlew testDebugUnitTest --tests *SistemaViewModelTest*` en verde.
- **Estado:** [x]

### Tarea 3: UI Reactiva en `SistemaScreen.kt` (Banner y Badge de Estado)
- **Objetivo:** Reflejar el estado de actualización en la tarjeta de versión de `SistemaScreen.kt`: si está al día, badge verde con confirmación; si hay actualización, banner destacado con botón directo para descargar el APK desde GitHub.
- **Archivos a modificar:**
  - `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt`
- **Criterio de Aceptación / Prueba:**
  - `./gradlew testDebugUnitTest assembleDebug` pasa sin errores.
  - La UI muestra el estado reactivo con fluidez y sin bloqueos de scroll.
- **Estado:** [x]

### Tarea 4: Verificación Integral y Auditoría Git
- **Objetivo:** Ejecutar la suite completa de pruebas unitarias (`./gradlew testDebugUnitTest`), compilar release (`./gradlew assembleRelease`) y realizar la auditoría limpia de git.
- **Criterio de Aceptación / Prueba:**
  - 100% tests unitarios pasando.
  - Árbol de Git limpio.
- **Estado:** [x]
