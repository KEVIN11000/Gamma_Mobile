# Checklist de Tareas Atómicas de Implementación (Tasks) — Versión v0.0.13
## Módulo: Integración Global de Backend v2.0.0, Bootstrap Atómico y Resiliencia de Red

- **ID de Característica:** `v013-backend-v2-integracion-global`
- **Fecha:** 2026-11-09
- **Fase SDD:** 0 (Especificación y Arquitectura)
- **Estado:** Pendiente de Aprobación de Planificación

---

### Compuerta de Planificación (Gate Obligatorio)

- [x] **COMPUERTA DE PLANIFICACIÓN (GATE OBLIGATORIO)**: Detenerse y solicitar confirmación textual del usuario antes de iniciar la creación o modificación de archivos de código fuente (Fase 2). Aprobado por el usuario.

---

### Bloque 1: Contratos, DTOs y Capa de Red (Fase 2.1)

- [x] **Tarea 1: Implementar DTOs de Backend v2.0.0 y Bootstrap**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/data/remote/dto/BootstrapDto.kt`, `FlujoCajaDto.kt`, `ParsedMovementDto.kt`, `ClimaDto.kt`, `SifenDto.kt`, `DispositivoDto.kt`
  - **Acción:** Crear clases de datos Kotlin con anotaciones `@SerializedName` y valores por defecto seguros.
  - **Prueba / Verificación:** `app/src/test/java/com/gamma/mobile/data/remote/dto/DtoSerializationTest.kt` ejecutando `.\gradlew.bat testDebugUnitTest --tests "*.DtoSerializationTest"`. (6 tests pasando ✅)

- [x] **Tarea 2: Declarar nuevas llamadas en `GammaApi.kt`**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/data/remote/GammaApi.kt`
  - **Acción:** Añadir métodos `@GET("bootstrap")`, `@GET("proyecciones/flujo-caja")`, `@POST("movimientos/parse")`, `@GET("clima")`, `@POST("dispositivos/registrar")`, `@POST("sifen/consultar-ruc")` y `@GET("movimientos?since=")`.
  - **Prueba / Verificación:** Compilación limpia de la interfaz y tests de Retrofit con `.\gradlew.bat testDebugUnitTest --tests "*.NetworkLayerTest"`. (8 tests pasando ✅)

---

### Bloque 2: Resiliencia de Red, Idempotencia y Cuotas (Fase 2.2)

- [x] **Tarea 3: Implementar `IdempotencyInterceptor`**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/data/remote/IdempotencyInterceptor.kt`, `NetworkModule.kt`
  - **Acción:** Inyectar cabecera `X-Client-Id: <UUID>` en métodos `POST`, `PUT` y `DELETE` si no fue provista explícitamente.
  - **Prueba / Verificación:** `IdempotencyInterceptorTest.kt` verificando con `MockWebServer` que cada petición mutante lleve un UUID único. (4 tests pasando ✅)

- [x] **Tarea 4: Implementar `RateLimitInterceptor` y `PayloadSizeGuardInterceptor`**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/data/remote/RateLimitInterceptor.kt`, `PayloadSizeGuardInterceptor.kt`, `NetworkModule.kt`
  - **Acción:** Capturar respuestas `HTTP 429` leyendo `Retry-After` para registrar en `AppLogBuffer`, y prevenir envíos de cargas mayores a 64 KB.
  - **Prueba / Verificación:** Tests unitarios verificando captura de cabecera `Retry-After` sin crasheos en la app. (4 tests pasando ✅)

---

### Bloque 3: Persistencia Local y Repositorios (Fase 2.3)

- [x] **Tarea 5: Crear Entidades Room para Flujo de Caja y Clima (Database v5/v6)**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/data/local/entity/FlujoCajaEntity.kt`, `ClimaEntity.kt`, `FlujoCajaDao.kt`, `ClimaDao.kt`, `GammaDatabase.kt`, `DatabaseModule.kt`
  - **Acción:** Modelar tablas locales con timestamp de sincronización y registrar en `GammaDatabase` (bump de versión a 6 y migración `MIGRATION_5_6`).
  - **Prueba / Verificación:** `FlujoCajaAndClimaDaoTest.kt` ejecutando `.\gradlew.bat testDebugUnitTest --tests "*DaoTest*"`. (11 tests pasando ✅)

- [x] **Tarea 6: Implementar `BootstrapRepository` con Hidratación Atómica**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/domain/repository/BootstrapRepository.kt`, `app/src/main/java/com/gamma/mobile/data/repository/BootstrapRepositoryImpl.kt`
  - **Acción:** Ejecutar `getBootstrap()` y persistir en Room dentro de una transacción unificada (`@Transaction`), entregando datos a los repositorios de resumen, avisos y vencimientos.
  - **Prueba / Verificación:** `BootstrapRepositoryTest.kt` comprobando guardado atómico y fallback en Room si la red falla. (2 tests pasando ✅)

- [x] **Tarea 7: Implementar `FlujoCajaRepository` y `ClimaRepository`**
  - **Archivos:** `FlujoCajaRepositoryImpl.kt`, `ClimaRepositoryImpl.kt`, `FlujoCajaModels.kt`, `ClimaModels.kt`, `AppModule.kt`
  - **Acción:** Estrategia offline-first (servir primero desde Room, actualizar desde red en segundo plano).
  - **Prueba / Verificación:** `FlujoCajaAndClimaRepositoryTest.kt` con mocks verificando emisión reactiva de `Flow` y resiliencia offline. (4 tests pasando ✅)

---

### Bloque 4: Casos de Uso, Parser NLP y SIFEN RUC (Fase 2.4)

- [x] **Tarea 8: Casos de Uso de Parser NLP y Consulta RUC SIFEN**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/domain/usecase/ParseMovimientoNlpUseCase.kt`, `ConsultarRucSifenUseCase.kt`, `NlpAndSifenModels.kt`
  - **Acción:** Encapsular la lógica de invocación al parser determinista y a la consulta de RUC KuDE con mapeo a modelos de dominio.
  - **Prueba / Verificación:** `NlpAndSifenUseCaseTest.kt` validando asignación correcta de `monto`, `categoría`, validación de vacíos y `razón social`. (6 tests pasando ✅)

---

### Bloque 5: Capa de Presentación (Compose & ViewModels) (Fase 2.5)

- [x] **Tarea 9: Adaptar `DashboardViewModel` para Hidratación Atómica**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/presentation/dashboard/DashboardViewModel.kt`
  - **Acción:** Reemplazar llamadas separadas concurrentes por `BootstrapRepository.syncBootstrap()`, exponiendo estados de `FlujoCaja` y `Clima` con resiliencia offline-first no destructiva.
  - **Prueba / Verificación:** `DashboardViewModelTest.kt` verificando actualización fluida, fallback offline y reactividad de Room. (15 tests pasando ✅)

- [x] **Tarea 10: Componentes Visuales en `DashboardScreen` (Flujo de Caja 30D y Clima)**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/presentation/dashboard/DashboardScreen.kt`, `ClimaHeaderWidget`, `FlujoCajaCard`
  - **Acción:** Renderizar tarjeta de "Flujo de Caja Proyectado a 30 Días" (con indicador de días críticos) y chip compacto de clima con alerta de paraguas.
  - **Prueba / Verificación:** Previews `@Preview` en Compose y comprobación de legibilidad y formateo monolínea en guaraníes (`#.##0 ₲`) con verificación visual interactiva Generative UI. (@Preview Compose y Generative UI verificados ✅)

- [x] **Tarea 11: Integrar Parser Rápido NLP y Autocompletado KuDE en `NuevaTransaccionScreen`**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/presentation/transacciones/NuevaTransaccionScreen.kt`, `NuevaTransaccionViewModel.kt`, `SifenScannerViewModel.kt`, `SifenConfirmBottomSheet.kt`
  - **Acción:** Botón dual de "Rápido (NLP)" junto a "Gemini IA", y resolución automática de Razón Social tras escanear el RUC de la factura.
  - **Prueba / Verificación:** `@Preview`, Generative UI interactivo y tests de ViewModel verificando precarga automática de campos. (12 tests pasando ✅)

---

### Bloque 6: Higiene de Versión, Verificación Integral y CI/CD (Fase 3)

- [x] **Tarea 12: Bump de Versión a 0.0.13 y Verificación de Suite Completa**
  - **Archivos:** `VERSION`, `HANDOFF.md`
  - **Acción:** Incrementar versión en `VERSION` a `0.0.13`, ejecutar la suite completa de pruebas unitarias y verificar el build de release.
  - **Prueba / Verificación:**
    ```bash
    .\gradlew.bat testDebugUnitTest
    .\gradlew.bat assembleRelease
    git status
    ```
