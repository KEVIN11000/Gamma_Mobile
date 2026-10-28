# Checklist de Tareas Atómicas: Flujo de Confirmación y Notificación de Marcación de Asistencia

## Fase 0: Especificación y Arquitectura (Planificación)
- [x] Tarea 0.1: Análisis de causa raíz de falta de confirmación en planilla y diseño de flujo en dos etapas.
- [x] Tarea 0.2: Elaboración de especificación técnica (`spec.md`), arquitectura de componentes (`plan.md`) y checklist atómico (`tasks.md`).
- [ ] **COMPUERTA DE PLANIFICACIÓN (GATE OBLIGATORIO)**: Detenerse y solicitar aprobación del usuario antes de iniciar la implementación en código.

---

## Fase 2: Construcción Modular Guiada por Tareas (Modo Constructor)

### Tarea 1: Extensión de Contratos de Asistencia (`AsistenciaModels.kt`)
- **Objetivo**: Añadir los campos opcionales `planilla`, `fila` y `confirmado_en_planilla` en `AsistenciaMarcaResponse`.
- **Archivos involucrados**:
  - `app/src/main/java/com/gamma/mobile/domain/model/AsistenciaModels.kt`
- **Criterio de Aceptación / Prueba**:
  - Compilación limpia con `.\gradlew compileDebugKotlin`.
- **Estado**: [x]

### Tarea 2: Creación de `MarcacionNotificationHelper` y Lógica de Evaluación
- **Objetivo**: Implementar `MarcacionNotificationHelper` para la emisión segura de notificaciones nativas de marcación confirmada y actualizar `evaluarMarcacion()` en `MarcacionResultado.kt`.
- **Archivos involucrados**:
  - `app/src/main/java/com/gamma/mobile/utils/MarcacionNotificationHelper.kt`
  - `app/src/main/java/com/gamma/mobile/presentation/dashboard/MarcacionResultado.kt`
- **Criterio de Aceptación / Prueba**:
  - Pruebas unitarias de evaluación pasando con `.\gradlew testDebugUnitTest --tests *DashboardViewModelTest*`.
- **Estado**: [x]

### Tarea 3: Implementación del Flujo de Dos Etapas en `DashboardViewModel` y `DashboardScreen`
- **Objetivo**: Reflejar de inmediato la hora de envío (`"⏳ Marcación enviada al servidor a las HH:mm..."`) y al recibir respuesta del servidor cambiar el banner a confirmación en planilla (`"✅ Marcación registrada en la planilla de horas a las HH:mm (Entrada/Salida)"`), disparando la notificación nativa.
- **Archivos involucrados**:
  - `app/src/main/java/com/gamma/mobile/presentation/dashboard/DashboardViewModel.kt`
  - `app/src/main/java/com/gamma/mobile/presentation/dashboard/DashboardScreen.kt`
  - `app/src/test/java/com/gamma/mobile/presentation/dashboard/DashboardViewModelTest.kt`
- **Criterio de Aceptación / Prueba**:
  - `.\gradlew testDebugUnitTest --tests *DashboardViewModelTest*` pasa al 100%.
- **Estado**: [x]

### Tarea 4: Actualización de Estado en `AsistenciaViewModel` y `AsistenciaScreen`
- **Objetivo**: Integrar el estado `AsistenciaUiState.Enviando` y la confirmación de registro en planilla en la pantalla dedicada de Asistencia y RRHH.
- **Archivos involucrados**:
  - `app/src/main/java/com/gamma/mobile/presentation/asistencia/AsistenciaViewModel.kt`
  - `app/src/main/java/com/gamma/mobile/presentation/asistencia/AsistenciaScreen.kt`
  - `app/src/test/java/com/gamma/mobile/presentation/asistencia/AsistenciaViewModelTest.kt`
- **Criterio de Aceptación / Prueba**:
  - `.\gradlew testDebugUnitTest --tests *AsistenciaViewModelTest*` pasa al 100%.
- **Estado**: [x]

### Tarea 5: Verificación Visual de UI (Gate de Ergonomía Compose)
- **Objetivo**: Validar el renderizado visual de los dos estados del banner de marcación en Compose (etapa de envío vs etapa de confirmación en planilla) asegurando legibilidad y contraste.
- **Archivos involucrados**:
  - `app/src/main/java/com/gamma/mobile/presentation/dashboard/DashboardScreen.kt`
- **Criterio de Aceptación / Prueba**:
  - Reality check / preview verificado sin desbordamiento ni regresiones visuales.
- **Estado**: [x]

### Tarea 6: Validación Integral y Suite Completa de Tests
- **Objetivo**: Ejecutar la suite completa de pruebas unitarias y compilar el paquete debug.
- **Archivos involucrados**:
  - Todo el proyecto.
- **Criterio de Aceptación / Prueba**:
  - `.\gradlew testDebugUnitTest` en verde sin fallos.
  - `.\gradlew assembleDebug` compila exitosamente.
- **Estado**: [x]

### Tarea 7: Redacción y Exportación de Requerimientos Backend para `Gamma_bot`
- **Objetivo**: Documentar los ajustes específicos en `AgenteAutonomoHoras` (verificación dinámica de `periodo_actual.txt` y retorno de metadatos de planilla en `blueprint.py`) en un archivo dedicado para la sesión del backend.
- **Archivos involucrados**:
  - `docs/specs/asistencia-confirmacion-planilla/requerimientos_backend_asistencia.md`
- **Criterio de Aceptación / Prueba**:
  - Documento validado y listo para ejecución en sesión dedicada de `Gamma_bot`.
- **Estado**: [x]
