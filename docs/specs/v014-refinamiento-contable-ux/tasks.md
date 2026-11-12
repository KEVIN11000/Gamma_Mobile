# Plan de Tareas (Work Breakdown Structure) — Versión 0.0.14
## Auto-Descubrimiento de Capacidades, Refinamiento Contable, Presupuesto Base y UX

**ID:** `v014-refinamiento-contable-ux`  
**Fecha:** 2026-11-12  
**Estado:** Fase 0 — Especificación y Arquitectura  

---

### Bloque 1: Contratos de Red, DTOs y Auto-Descubrimiento (`GET /capacidades`)

- [x] **Tarea 1: DTOs de Capacidades, Presupuesto, Bootstrap y Rutas de Red**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/data/remote/dto/CapacidadesDtos.kt`, `app/src/main/java/com/gamma/mobile/data/remote/dto/BootstrapDto.kt`, `app/src/main/java/com/gamma/mobile/data/remote/dto/ObligacionUpdateDto.kt`, `app/src/main/java/com/gamma/mobile/data/remote/dto/PresupuestoDtos.kt`, `app/src/main/java/com/gamma/mobile/data/remote/GammaApi.kt`, `app/src/test/java/com/gamma/mobile/data/remote/dto/DtoSerializationTest.kt`
  - **Acción:**
    - Crear `CapacidadesDtos.kt` (`CapacidadesResponseDto`, `RutaEndpointInfoDto`).
    - Declarar `@GET("capacidades")` en `GammaApi.kt`.
    - Agregar alias `@SerializedName(value = "resumen_financiero", alternate = ["summary", "resumenFinanciero"])` en `BootstrapResponseDto`.
    - Actualizar ruta de flujo de caja a `@GET("proyecciones/flujo-caja")` en `GammaApi.kt`.
    - Crear `ObligacionUpdateDto` y declarar endpoint `@PUT("obligaciones/{id}")` en `GammaApi.kt`.
    - Crear `PresupuestoDtos.kt` y declarar endpoints de presupuesto en `GammaApi.kt`.
  - **Prueba / Verificación:**
    ```bash
    .\gradlew.bat testDebugUnitTest --tests "com.gamma.mobile.data.remote.dto.DtoSerializationTest"
    ```

---

### Bloque 2: Repositorio de Capacidades y Feature Toggles en el Arranque

- [x] **Tarea 2: Implementación de `CapacidadesRepository` y Auto-Descubrimiento**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/domain/repository/CapacidadesRepository.kt`, `app/src/main/java/com/gamma/mobile/data/repository/CapacidadesRepositoryImpl.kt`, `app/src/main/java/com/gamma/mobile/di/AppModule.kt`, `app/src/main/java/com/gamma/mobile/presentation/dashboard/DashboardViewModel.kt`, `app/src/test/java/com/gamma/mobile/data/repository/CapacidadesRepositoryTest.kt`
  - **Acción:**
    - Implementar `CapacidadesRepositoryImpl` con persistencia offline en SharedPreferences (`gamma_cache`).
    - Exponer `isCapacidadHabilitada(clave: String): Boolean` y `getCapacidadesClaveFlow(): StateFlow<Map<String, Boolean>>`.
    - Integrar la llamada a `syncCapacidades()` dentro del ciclo de inicio de `DashboardViewModel.loadDashboard()`.
  - **Prueba / Verificación:**
    ```bash
    .\gradlew.bat testDebugUnitTest --tests "com.gamma.mobile.data.repository.CapacidadesRepositoryTest"
    ```

---

### Bloque 3: Lógica Contable y Blindaje de Deudas en Dominio

- [x] **Tarea 3: Blindaje Contable de Obligaciones y Normalización de Cuotas**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/domain/model/Models.kt`, `app/src/test/java/com/gamma/mobile/domain/model/ObligacionModelTest.kt`
  - **Acción:**
    - Ajustar `esDefinitivamenteSaldada` para requerir estrictamente `saldoActual <= 0L`.
    - Forzar que deudas con `saldoActual > 0L` reporten `estadoReal = "Activo"` (o "Pausada"/"Simulado" si aplica) y `saldoReal = saldoActual`.
    - Implementar propiedades utilitarias `cuotasPagadasReal` y `montoAbonadoReal`.
  - **Prueba / Verificación:**
    ```bash
    .\gradlew.bat testDebugUnitTest --tests "com.gamma.mobile.domain.model.ObligacionModelTest"
    ```

---

### Bloque 4: Repositorio y Actualización de Obligaciones

- [x] **Tarea 4: Implementación de Actualización de Obligaciones en Repositorio**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/domain/repository/ObligacionRepository.kt`, `app/src/main/java/com/gamma/mobile/data/repository/ObligacionRepositoryImpl.kt`, `app/src/test/java/com/gamma/mobile/data/repository/ObligacionRepositoryTest.kt`
  - **Acción:**
    - Refactorizar `updateObligacion` en `ObligacionRepositoryImpl` para invocar el endpoint `@PUT("obligaciones/{id}")` con `ObligacionUpdateDto` y actualizar Room atómicamente.
  - **Prueba / Verificación:**
    ```bash
    .\gradlew.bat testDebugUnitTest --tests "com.gamma.mobile.data.repository.ObligacionRepositoryTest"
    ```

---

### Bloque 5: Diálogo de Edición de Deudas en `ObligacionesScreen`

- [x] **Tarea 5: Integración del Diálogo de Edición de Deudas**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesViewModel.kt`, `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesScreen.kt`, `app/src/test/java/com/gamma/mobile/presentation/obligaciones/ObligacionesViewModelTest.kt`
  - **Acción:**
    - Agregar función `actualizarObligacion(...)` en `ObligacionesViewModel`.
    - Agregar botón de edición (icono lápiz) en las tarjetas de obligaciones en `ObligacionesScreen`.
    - Diseñar composable `EditarObligacionDialog` con validación de montos enteros.
  - **Prueba / Verificación:**
    ```bash
    .\gradlew.bat testDebugUnitTest --tests "com.gamma.mobile.presentation.obligaciones.ObligacionesViewModelTest"
    ```

---

### Bloque 6: Módulo de Presupuesto Base (Repositorio y ViewModel)

- [x] **Tarea 6: Repositorio y ViewModel de Presupuesto Base**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/domain/repository/PresupuestoRepository.kt`, `app/src/main/java/com/gamma/mobile/data/repository/PresupuestoRepositoryImpl.kt`, `app/src/main/java/com/gamma/mobile/presentation/presupuesto/PresupuestoViewModel.kt`, `app/src/main/java/com/gamma/mobile/di/AppModule.kt`, `app/src/test/java/com/gamma/mobile/data/repository/PresupuestoRepositoryTest.kt`, `app/src/test/java/com/gamma/mobile/presentation/presupuesto/PresupuestoViewModelTest.kt`
  - **Acción:**
    - Implementar `PresupuestoRepositoryImpl` (consumiendo `/presupuesto/fijos`, `/salario-base`, `/techos` con caché local).
    - Implementar `PresupuestoViewModel` con `PresupuestoUiState` enlazado a `CapacidadesRepository` para Feature Toggles.
  - **Prueba / Verificación:**
    ```bash
    .\gradlew.bat testDebugUnitTest --tests "com.gamma.mobile.presentation.presupuesto.PresupuestoViewModelTest"
    ```

---

### Bloque 7: Pantalla UI de Presupuesto Base (`PresupuestoScreen`) y Navegación

- [x] **Tarea 7: Construcción de `PresupuestoScreen` con Acordeones y Semáforos**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/presentation/presupuesto/PresupuestoScreen.kt`, `app/src/main/java/com/gamma/mobile/presentation/navigation/GammaNavGraph.kt`
  - **Acción:**
    - Diseñar `PresupuestoScreen` con Card de Salario Base editable, lista de gastos fijos por categoría con edición/borrado condicionado por `presupuesto_fijos_delete`, y monitor de techos variables con barras de progreso y edición condicionada por `presupuesto_techos_put`.
    - Integrar acceso a Presupuesto en el Drawer/Navegación principal de la app.
    - Generar artefacto interactivo HTML de UI para comprobación ergonómica visual.
  - **Prueba / Verificación:**
    ```bash
    .\gradlew.bat compileDebugKotlin
    ```

---

### Bloque 8: Desglose en Historial de Cierres y Logs Plegables en Sistema

- [x] **Tarea 8: Corrección de Etiquetas en Cierres y Logs Plegables en Sistema**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/presentation/cierre/HistorialCierresScreen.kt`, `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt`
  - **Acción:**
    - Desglosar en `HistorialCierresScreen`: Total Facturado/Ingresos, Total Gastos y Neto del Mes.
    - Transformar la Card de "Registros y Logs" en `SistemaScreen` en un acordeón expandible/plegable con indicador visual `ExpandMore`/`ExpandLess`.
  - **Prueba / Verificación:**
    ```bash
    .\gradlew.bat testDebugUnitTest --tests "com.gamma.mobile.presentation.cierre.CierresViewModelTest"
    ```

---

### Bloque 9: Estandarización Ergonómica de Selectores en Dashboard y Transacciones

- [x] **Tarea 9: Rediseño Ergonómico de Selectores**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/presentation/dashboard/DashboardScreen.kt`, `app/src/main/java/com/gamma/mobile/presentation/transacciones/TransaccionesScreen.kt`
  - **Acción:**
    - Estandarizar chips de selección (Marcación Normal/Directo y Filtros Todos/Ingresos/Gastos): la opción seleccionada tiene borde primario enfocado, fondo activo e icono `Icons.Default.Check`.
    - Generar vista previa visual de UI para constatar ergonomía.
  - **Prueba / Verificación:**
    ```bash
    .\gradlew.bat testDebugUnitTest --tests "com.gamma.mobile.presentation.dashboard.DashboardViewModelTest"
    ```

---

### Bloque 10: Bump a 0.0.14, Auditoría Criptográfica con Banco Oficial de Firmas y CI/CD

- [x] **Tarea 10: Bump de Versión a 0.0.14 y Verificación Criptográfica Oficial**
  - **Archivos:** `VERSION`, `HANDOFF.md`, `docs/SYSTEM_MAP.md`
  - **Acción:**
    - Actualizar versión en `VERSION` a `0.0.14`.
    - Ejecutar suite completa de tests unitarios: `.\gradlew.bat testDebugUnitTest`.
    - Ejecutar compilación de release: `.\gradlew.bat assembleRelease`.
    - **Auditoría criptográfica obligatoria con el banco oficial de firmas:**
      ```powershell
      & "$env:LOCALAPPDATA\Android\Sdk\build-tools\34.0.0\apksigner.bat" verify --verbose --print-certs app/build/outputs/apk/release/gamma-mobile-v0.0.14-release.apk
      ```
      Constatar que el DN y la huella SHA-256 coincidan exactamente con el certificado oficial del proyecto (`2db8d51c88ad21c640a3bffb9fe04c50603e81182f8e1b7e54b3a568a580ffe7`).
  - **Prueba / Verificación:**
    ```bash
    .\gradlew.bat testDebugUnitTest
    .\gradlew.bat assembleRelease
    git status
    ```
