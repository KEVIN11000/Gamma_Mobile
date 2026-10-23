# Checklist de Tareas Atómicas de Implementación (Tasks) — Versión v0.2.2
## Módulo: Culminación de Integración Backend Core (`Gamma_bot` v0.2.0) en GammaMobile

- **Fecha:** 22 de Octubre de 2026
- **Versión de Tareas:** 1.0.0
- **Fase SDD:** 0 (Especificación y Arquitectura - Modo Planificador)
- **Estado:** Pendiente de Aprobación por Usuario (Compuerta de Planificación)

---

- [x] **Tarea 1: DTOs y Contratos de Red en `Models.kt` y `GammaApi.kt`**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/domain/model/Models.kt`, `app/src/main/java/com/gamma/mobile/data/remote/GammaApi.kt`
  - **Acción:** Declarar `VencimientoCronogramaItem`, `DeudaFocoResponse` y los endpoints `@GET("obligaciones/vencimientos")`, `@GET("obligaciones/foco")`, `@POST("obligaciones/proyectar-capacidad")`.
  - **Criterio de Aceptación:** Compilación limpia sin errores de tipos o serialización.

- [x] **Tarea 2: Métodos de Cronograma, Deuda Foco y Proyector en `ObligacionRepository` e `Impl` con Fallback Offline**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/domain/repository/ObligacionRepository.kt`, `app/src/main/java/com/gamma/mobile/data/repository/ObligacionRepositoryImpl.kt`
  - **Acción:** Implementar `getCronogramaVencimientos`, `getDeudaFoco` y `proyectarCapacidad` garantizando resiliencia sin red vía cálculo local con `VencimientoUtils` y datos de `ObligacionDao`.
  - **Criterio de Aceptación:** Pruebas unitarias de repositorio pasando para casos online y offline.

- [x] **Tarea 3: Suite de Pruebas Unitarias para `ObligacionRepositoryTest`**
  - **Archivos:** `app/src/test/java/com/gamma/mobile/data/repository/ObligacionRepositoryTest.kt`
  - **Acción:** Crear batería de tests para llamadas remotas, mapeos de respuestas de vencimientos/foco y fallback cuando el backend falla.
  - **Criterio de Aceptación:** `./gradlew testDebugUnitTest --tests "*ObligacionRepositoryTest*"` ejecutando en verde al 100%.

- [x] **Tarea 4: Lógica de Cronograma y Estrategia Foco en `ObligacionesViewModel` y Pruebas Unitarias**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesViewModel.kt`, `app/src/test/java/com/gamma/mobile/presentation/obligaciones/ObligacionesViewModelTest.kt`
  - **Acción:** Exponer StateFlows `cronogramaVencimientos`, `deudaFoco`, `criterioFoco` (`"saldo"` vs `"prioridad"`), y función `cambiarCriterioFoco`. Actualizar y expandir tests unitarios existentes.
  - **Criterio de Aceptación:** 100% de tests unitarios de `ObligacionesViewModelTest` pasando.

- [x] **Tarea 5: Componentes UI de Cronograma de Vencimientos y Tarjeta de Deuda Foco en `ObligacionesScreen`**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesScreen.kt`
  - **Acción:**
    - Diseñar `CronogramaVencimientosSection` con tarjetas horizontales y etiquetas de urgencia cronológica.
    - Diseñar `DeudaFocoCard` con selector de estrategia (Bola de Nieve / Avalancha) y acción de abono directo.
  - **Criterio de Aceptación:** Verificación visual de ergonomía, accesibilidad táctil y scroll sin conflictos.

- [x] **Tarea 6: Sincronización Remota de Proyección en `CapacidadPagoViewModel`**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/presentation/capacidad/CapacidadPagoViewModel.kt`, `app/src/test/java/com/gamma/mobile/presentation/capacidad/CapacidadPagoViewModelTest.kt`
  - **Acción:** Integrar llamada a `repository.proyectarCapacidad()` manteniendo el cálculo reactivo instantáneo local para 0ms de retardo, incorporando tests unitarios.
  - **Criterio de Aceptación:** Pruebas unitarias de `CapacidadPagoViewModelTest` pasando limpiamente.

- [x] **Tarea 7: Compose `@Preview`, Gate Visual de UI y Verificación de Build Integral**
  - **Archivos:** Previews en `ObligacionesScreen.kt`, `CapacidadPagoScreen.kt`, artefactos de prueba.
  - **Acción:** Incorporar `@Preview` sobrios M3 y correr `./gradlew testDebugUnitTest` y `./gradlew assembleDebug`.
  - **Criterio de Aceptación:** 100% de tests de la suite general pasando sin errores y APK generado con éxito.
