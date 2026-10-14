# Checklist de Tareas Atómicas: v004 - Refinamiento Operativo

## Fase 0: Especificación y Arquitectura
- [x] Tarea 0.1: Análisis estático del ordenamiento de fechas en `TransaccionesViewModel` y causa de distorsión en agosto.
- [x] Tarea 0.2: Redacción de `spec.md`, `plan.md` y `tasks.md` en `docs/specs/v004-daily-dashboard-refinement/`.
- [ ] **GATE DE COMPUERTA DE PLANIFICACIÓN**: Aprobación del usuario antes de iniciar implementación (Fase 2).

---

## Fase 2: Implementación Modular Guiada por Tareas

### Tarea 1: Backend (`Gamma_bot`) - Margen Libre, Posición IVA real y Avisos Calendar
- **Objetivo:** Calcular la posición neta de IVA del período no cerrado a partir de las facturas con IVA 10% y 5% de `Libro_Diario`, calcular el margen libre según compromisos activos, y combinar eventos de Google Calendar en `GET /avisos`.
- **Archivos a modificar:**
  - `src/api/blueprint.py`
  - `src/logic/financiero.py`
- **Criterio de Aceptación / Prueba:**
  - `pytest tests/test_mobile_api.py` pasa exitosamente.
  - El endpoint `/dashboard/summary` devuelve valores no nulos para `margen_libre` y `posicion_iva`.
  - El endpoint `/avisos` incluye eventos de Google Calendar cuando el servicio está activo.
- **Estado:** [x]

### Tarea 2: Backend (`Gamma_bot`) - Endpoint de Materias Oficiales y Cierre Dual
- **Objetivo:** Crear el endpoint `GET /asistencia/materias` leyendo de `Config_bot` y soportar parámetro de tipo (`horas` vs `contable`) en el cierre.
- **Archivos a modificar:**
  - `src/api/blueprint.py`
- **Criterio de Aceptación / Prueba:**
  - `GET /asistencia/materias` devuelve la lista de materias extraídas de la planilla con status 200.
  - Tests unitarios en `pytest` cubren el nuevo endpoint.
- **Estado:** [x]

### Tarea 3: Mobile (`GammaMobile`) - Orden Cronológico Real en Libro Diario
- **Objetivo:** Reemplazar el ordenamiento lexicográfico de strings (`sortedByDescending { it.fecha }`) por un comparador cronológico robusto con soporte de formatos de fecha paraguayos (`dd/MM/yyyy`, `dd/MM`), asegurando que los registros de octubre y los más recientes siempre aparezcan arriba.
- **Archivos a modificar:**
  - `app/src/main/java/com/gamma/mobile/presentation/transacciones/TransaccionesViewModel.kt`
  - `app/src/main/java/com/gamma/mobile/data/repository/MovimientoRepositoryImpl.kt`
- **Criterio de Aceptación / Prueba:**
  - Test unitario validando que `"14/10/2026"` precede a `"31/08/2026"` en la lista agrupada.
  - `./gradlew testDebugUnitTest --tests *TransaccionesViewModelTest*` pasa en verde.
- **Estado:** [x]

### Tarea 4: Mobile (`GammaMobile`) - Atajo de Marcación Laboral y Tarjetas Financieras en Dashboard
- **Objetivo:** Incorporar en `DashboardScreen.kt` una tarjeta de atajo para marcación de entrada/salida visible de Lunes a Viernes, y enriquecer las tarjetas de Margen Libre e IVA con formato en Guaraníes (`₲`) y chip de Crédito/Débito fiscal.
- **Archivos a modificar:**
  - `app/src/main/java/com/gamma/mobile/presentation/dashboard/DashboardScreen.kt`
  - `app/src/main/java/com/gamma/mobile/presentation/dashboard/DashboardViewModel.kt`
- **Criterio de Aceptación / Prueba:**
  - El atajo laboral se muestra en días laborables y ejecuta la marcación.
  - `./gradlew testDebugUnitTest --tests *DashboardViewModelTest*` pasa en verde.
- **Estado:** [x]

### Tarea 5: Mobile (`GammaMobile`) - Filtros de Deudas y ExtendedFAB en Compromisos
- **Objetivo:** Agregar barra de `FilterChip` (`Todas`, `Activas`, `Saldadas`, `Simuladas`) en `ObligacionesScreen.kt` y convertir el botón `+` en un `ExtendedFloatingActionButton` con icono y texto claro ("Nueva Obligación").
- **Archivos a modificar:**
  - `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesScreen.kt`
  - `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesViewModel.kt`
- **Criterio de Aceptación / Prueba:**
  - Las deudas se filtran según el estado seleccionado.
  - El botón flotante muestra texto visible.
  - `./gradlew testDebugUnitTest --tests *ObligacionesViewModelTest*` pasa en verde.
- **Estado:** [x]

### Tarea 6: Mobile (`GammaMobile`) - Materias Oficiales y Cierre Mensual Ampliado
- **Objetivo:** Conectar `AsistenciaViewModel` con el nuevo endpoint `GET /asistencia/materias` eliminando la lista mock hardcodeada, y permitir seleccionar Cierre Laboral o Cierre Contable en la interfaz de Cierre.
- **Archivos a modificar:**
  - `app/src/main/java/com/gamma/mobile/data/remote/GammaApi.kt`
  - `app/src/main/java/com/gamma/mobile/domain/repository/AsistenciaRepository.kt`
  - `app/src/main/java/com/gamma/mobile/data/repository/AsistenciaRepositoryImpl.kt`
  - `app/src/main/java/com/gamma/mobile/presentation/asistencia/AsistenciaViewModel.kt`
  - `app/src/main/java/com/gamma/mobile/presentation/asistencia/AsistenciaScreen.kt`
- **Criterio de Aceptación / Prueba:**
  - Las materias se pueblan desde el repositorio.
  - `./gradlew testDebugUnitTest --tests *AsistenciaViewModelTest*` pasa en verde.
- **Estado:** [x]

### Tarea 7: Mobile (`GammaMobile`) - Candado de Configuración y Acordeón de Tickets en Sistema
- **Objetivo:** Proteger los campos de URL y API Key con bloqueo de solo lectura por defecto y botón de desbloqueo explícito; hacer colapsable la lista de tickets de soporte para evitar sobrecarga de scroll.
- **Archivos a modificar:**
  - `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt`
- **Criterio de Aceptación / Prueba:**
  - Los campos de configuración no pueden editarse sin pulsar "Desbloquear".
  - La sección de tickets se puede expandir/colapsar con fluidez visual.
  - `./gradlew testDebugUnitTest --tests *SistemaViewModelTest*` pasa en verde.
- **Estado:** [x]

### Tarea 8: Verificación Integral, Pruebas de Regresión y Compilación de APK
- **Objetivo:** Ejecutar la suite completa de pruebas en backend (`pytest`) y frontend (`./gradlew testDebugUnitTest`), auditar higiene estricta de git y compilar el APK debug/release actualizado.
- **Archivos a verificar:** Todo el árbol de trabajo.
- **Criterio de Aceptación / Prueba:**
  - 100% de tests unitarios pasan en ambos repositorios.
  - `git status` limpio sin archivos residuales.
  - APK compilado exitosamente en `app/build/outputs/apk/debug/app-debug.apk`.
- **Estado:** [x]
