# Checklist de Tareas Atómicas de Implementación (Tasks)
## Módulo: Proyector de Capacidad de Pago con Gastos Ad-Hoc y Escenarios Persistentes (v0.2.1)

- **Fecha:** 22 de Octubre de 2026
- **Versión de Tareas:** 1.0.0
- **Fase SDD:** 0 (Especificación y Arquitectura)
- **Estado:** Pendiente de Aprobación por Usuario (Compuerta de Planificación)

---

- [x] **Tarea 1: Modelos de Dominio para Gastos Ad-Hoc y Escenarios de Capacidad**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/domain/model/Models.kt`
  - **Acción:** Definir las clases `data class GastoSimulado`, `data class EscenarioCapacidad` y agregar `val totalGastosProyectados: Long` a `CapacidadPagoResponse`.
  - **Criterio de Aceptación:** Compilación limpia y modelos serializables con Gson.

- [x] **Tarea 2: Persistencia Local en Room v5 con `EscenarioCapacidadDao` y Migración `MIGRATION_4_5`**
  - **Archivos:** `EscenarioCapacidadEntity.kt`, `EscenarioCapacidadDao.kt`, `GammaDatabase.kt`, `DatabaseModule.kt`, `EscenarioCapacidadDaoTest.kt`
  - **Acción:** Crear tabla `escenarios_capacidad`, implementar operaciones CRUD en DAO, registrar `MIGRATION_4_5` con incremento a Database v5 y proveer DAO en Hilt.
  - **Criterio de Aceptación:** Test unitario `EscenarioCapacidadDaoTest` ejecutando inserción, listado ordenado y eliminación en base en memoria de Room.

- [x] **Tarea 3: Implementación y Binding del Repositorio de Escenarios**
  - **Archivos:** `EscenarioCapacidadRepository.kt`, `EscenarioCapacidadRepositoryImpl.kt`, `AppModule.kt`, `EscenarioCapacidadRepositoryTest.kt`
  - **Acción:** Implementar métodos `getEscenarios(): Flow`, `guardarEscenario()` y `eliminarEscenario()` con mapeo seguro de JSON (`Gson`), enlazar mediante `@Binds` en `AppModule`.
  - **Criterio de Aceptación:** Pruebas unitarias en `EscenarioCapacidadRepositoryTest` verificando serialización/deserialización de deudas pausadas, abonos y lista de gastos ad-hoc.

- [x] **Tarea 4: Lógica de Simulación de Gastos y Escenarios en `CapacidadPagoViewModel`**
  - **Archivos:** `CapacidadPagoViewModel.kt`, `CapacidadPagoViewModelTest.kt`
  - **Acción:** Agregar funciones `agregarGastoAdHoc`, `toggleGastoAdHoc`, `eliminarGastoAdHoc`, `guardarEscenarioActual`, `cargarEscenario`, `eliminarEscenario`, e incorporar `totalGastos` en `recalcular()`.
  - **Criterio de Aceptación:** Pruebas unitarias verificando que los gastos ad-hoc impactan la capacidad neta, actualizan el semáforo y se restauran íntegramente al cargar un escenario guardado.

- [x] **Tarea 5: Interfaz de Usuario para Gastos Ad-Hoc y Escenarios en `CapacidadPagoScreen`**
  - **Archivos:** `CapacidadPagoScreen.kt`
  - **Acción:**
    - Incorporar sección *"Gastos Adicionales / Imprevistos"* con botón `+ Agregar Gasto` y diálogo modal de entrada.
    - Renderizar tarjetas individuales de gastos con Switch de inclusión/pausa y botón eliminar.
    - Añadir fila *"Gastos Proyectados"* en `MetricasCapacidadCard`.
    - Añadir botón de acción en `TopAppBar` que despliega `EscenariosBottomSheet` para listar, guardar con nombre, cargar o eliminar escenarios.
  - **Criterio de Aceptación:** Verificación visual de interacción fluida, prevención de solapamientos con el scroll y actualización inmediata de cifras.

- [x] **Tarea 6: Previews en Jetpack Compose, Gate Visual y Validación Final**
  - **Archivos:** `CapacidadPagoScreen.kt`, artefacto visual HTML.
  - **Acción:**
    - Incorporar Previews para estados con gastos ad-hoc y diálogo de escenarios.
    - Generar el artefacto visual interactivo en el directorio de brain.
    - Ejecutar `./gradlew testDebugUnitTest` y `./gradlew assembleDebug` en terminal.
  - **Criterio de Aceptación:** 100% de tests unitarios pasando y compilación limpia del APK sin advertencias.
