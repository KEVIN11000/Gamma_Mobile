# Checklist de Tareas Atómicas: Analíticas MTD y Descarga Dual de Asistencia

## Fase 0: Especificación y Arquitectura (Planificación)
- [x] Tarea 0.1: Análisis de endpoints REST expuestos en `Gamma_bot` y diseño de contratos.
- [x] Tarea 0.2: Elaboración de `spec.md`, `plan.md` y checklist de tareas atómicas (`tasks.md`).
- [ ] **COMPUERTA DE PLANIFICACIÓN (GATE OBLIGATORIO)**: Detenerse y solicitar confirmación del usuario antes de iniciar la implementación en código.

---

## Fase 2: Construcción Modular Guiada por Tareas (Modo Constructor)

### Tarea 1: Modelos de Dominio y Contratos de API (`AsistenciaModels.kt` y `GammaApi.kt`)
- **Objetivo**: Declarar `PeriodosResponse`, `AnaliticasHorasResponse`, y añadir a `GammaApi.kt` las llamadas `getPeriodos()`, `getAnaliticas()`, y `descargarReporteExcel()`.
- **Archivos involucrados**:
  - `app/src/main/java/com/gamma/mobile/domain/model/AsistenciaModels.kt`
  - `app/src/main/java/com/gamma/mobile/data/remote/GammaApi.kt`
- **Criterio de Aceptación / Prueba**:
  - Compilación limpia con `.\gradlew compileDebugKotlin`.
- **Estado**: [x]

### Tarea 2: Extensión de `AsistenciaRepository` y `AsistenciaRepositoryImpl`
- **Objetivo**: Añadir y desacoplar los métodos `getPeriodos`, `getAnaliticas` y `descargarReporteExcel` con manejo seguro vía `safeApiCall` y streaming de bytes.
- **Archivos involucrados**:
  - `app/src/main/java/com/gamma/mobile/domain/repository/AsistenciaRepository.kt`
  - `app/src/main/java/com/gamma/mobile/data/repository/AsistenciaRepositoryImpl.kt`
- **Criterio de Aceptación / Prueba**:
  - Compilación limpia con `.\gradlew compileDebugKotlin`.
- **Estado**: [x]

### Tarea 3: Estado y Lógica de Negocio en `AsistenciaViewModel`
- **Objetivo**: Implementar `AnaliticasUiState` (`Loading`, `Success`, `Error`), el flujo de consulta concurrente de analíticas y períodos, y la acción de descarga de Excel.
- **Archivos involucrados**:
  - `app/src/main/java/com/gamma/mobile/presentation/asistencia/AsistenciaViewModel.kt`
  - `app/src/test/java/com/gamma/mobile/presentation/asistencia/AsistenciaViewModelTest.kt`
- **Criterio de Aceptación / Prueba**:
  - Pruebas unitarias completas pasando con `.\gradlew testDebugUnitTest --tests *AsistenciaViewModelTest*`.
- **Estado**: [x]

### Tarea 4: Soporte de Descarga Excel en `CierreViewModel` y `CierreTabContent`
- **Objetivo**: Permitir al usuario descargar y abrir el reporte `.xlsx` con `FileProvider` e Intent nativo desde la pantalla de Cierre / Reportes.
- **Archivos involucrados**:
  - `app/src/main/java/com/gamma/mobile/presentation/cierre/CierreViewModel.kt`
  - `app/src/main/java/com/gamma/mobile/presentation/asistencia/AsistenciaScreen.kt`
  - `app/src/test/java/com/gamma/mobile/presentation/cierre/CierreViewModelTest.kt`
- **Criterio de Aceptación / Prueba**:
  - Pruebas unitarias de cierre pasando con `.\gradlew testDebugUnitTest --tests *CierreViewModelTest*`.
- **Estado**: [x]

### Tarea 5: Componente Visual de Analíticas MTD en `AsistenciaScreen.kt`
- **Objetivo**: Renderizar el Card de Analíticas MTD en `MarcacionesTabContent` mostrando período, horas acumuladas, horas extras/perdidas y salario devengado.
- **Archivos involucrados**:
  - `app/src/main/java/com/gamma/mobile/presentation/asistencia/AsistenciaScreen.kt`
- **Criterio de Aceptación / Prueba**:
  - Reality check / `@Preview` de analíticas en Compose M3 renderizado correctamente.
- **Estado**: [x]

### Tarea 6: Validación Integral y Suite de Pruebas
- **Objetivo**: Ejecutar la suite completa de pruebas y compilar el APK debug.
- **Archivos involucrados**:
  - Todo el proyecto.
- **Criterio de Aceptación / Prueba**:
  - `.\gradlew testDebugUnitTest` en verde sin fallos.
  - `.\gradlew assembleDebug` compila exitosamente.
- **Estado**: [x]
