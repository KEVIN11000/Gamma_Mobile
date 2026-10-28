# Checklist de Tareas Atómicas: Restauración del Flujo Visual de Actualización In-App

## Fase 0: Especificación y Arquitectura (Planificación)
- [x] Tarea 0.1: Auditoría de contratos, diagnóstico de regresión visual y análisis de la API de GitHub Releases.
- [x] Tarea 0.2: Elaboración de especificación técnica (`spec.md`), arquitectura y contratos (`plan.md`) y checklist atómico (`tasks.md`).
- [ ] **COMPUERTA DE PLANIFICACIÓN (GATE OBLIGATORIO)**: Detenerse y solicitar aprobación del usuario antes de iniciar la implementación en código.

---

## Fase 2: Implementación Modular Guiada por Tareas (Modo Constructor)

### Tarea 1: Verificación de Contratos y Cobertura Previa
- **Objetivo**: Asegurar que las pruebas unitarias de `SemverUtilsTest` y `SistemaViewModelTest` ejecuten limpiamente como baseline antes de tocar la interfaz visual.
- **Archivos involucrados**:
  - `app/src/test/java/com/gamma/mobile/domain/util/SemverUtilsTest.kt`
  - `app/src/test/java/com/gamma/mobile/presentation/sistema/SistemaViewModelTest.kt`
- **Criterio de Aceptación / Prueba**:
  - `.\gradlew testDebugUnitTest --tests *SemverUtilsTest* --tests *SistemaViewModelTest*` pasa en verde al 100%.
- **Estado**: [x]

### Tarea 2: Restauración y Refinamiento Reactivo de `Card 1` en `SistemaScreen.kt`
- **Objetivo**: Implementar la sección condicional `when (val status = uiState.updateStatus)` dentro de la tarjeta de versión en `SistemaScreen.kt`, incluyendo:
  1. Estado `UpToDate`: Sugerencia / chip con ícono `CheckCircle` confirmando que la aplicación está actualizada.
  2. Estado `UpdateAvailable`: Tarjeta con alerta informativa, indicando versión remota, nombre del artefacto APK, peso en MB, y botón con `Intent.ACTION_VIEW` para descargar la release oficial.
  3. Estado `CheckFailed`: Manejo no intrusivo con botón de fallback si `uiState.health?.downloadUrl` está disponible.
  4. Actualización del fallback de versión instalada a `"0.0.10"`.
- **Archivos involucrados**:
  - `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt`
- **Criterio de Aceptación / Prueba**:
  - Compilación sin errores con `.\gradlew compileDebugKotlin`.
- **Estado**: [x]

### Tarea 3: Verificación Visual de UI (Gate de Calidad y Ergonomía Compose)
- **Objetivo**: Validar empíricamente la ergonomía del componente (márgenes de seguridad táctil, contraste de colores con `MaterialTheme.colorScheme`, ausencia de desbordamiento horizontal y jerarquía tipográfica).
- **Archivos involucrados**:
  - `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt`
- **Criterio de Aceptación / Prueba**:
  - Renderizado / preview composable verificado y sin advertencias de layout.
- **Estado**: [x]

### Tarea 4: Validación Integral de Build y Suite de Tests
- **Objetivo**: Ejecutar la suite completa de pruebas unitarias y verificar el empaquetado de debug.
- **Archivos involucrados**:
  - Todo el proyecto.
- **Criterio de Aceptación / Prueba**:
  - `.\gradlew testDebugUnitTest` pasa con 0 fallos.
  - `.\gradlew assembleDebug` genera el artefacto APK sin advertencias críticas.
- **Estado**: [x]

### Tarea 5: Auditoría de Git y Preparación de Staging Limpio
- **Objetivo**: Verificar el estado de git con `git status` y `git diff --cached`, garantizando que únicamente los archivos solicitados estén en staging, sin commits ni push remoto (cumpliendo la instrucción "sin publicar, solo staging").
- **Criterio de Aceptación / Prueba**:
  - `git status` limpio y controlado.
  - Ningún archivo sensible (.env, certificados) tocado ni versionado.
- **Estado**: [x]
