# Checklist de Tareas Atómicas de Implementación (Tasks)
## Módulo: Visualización de Comprobante PDF de Cierre Contable Mensual en Gamma Mobile

- **Fecha:** 15 de Octubre de 2026
- **Versión de Tareas:** 1.0.0
- **Fase:** 0 (Especificación y Arquitectura)

---

### Tareas Atómicas de Construcción

- [x] **Tarea 1: Extender `SistemaRepository` y `SistemaRepositoryImpl` para Cierres Contables**
  - **Archivos:** 
    - `app/src/main/java/com/gamma/mobile/domain/repository/SistemaRepository.kt`
    - `app/src/main/java/com/gamma/mobile/data/repository/SistemaRepositoryImpl.kt`
  - **Acción:**
    - Agregar `suspend fun ejecutarCierreContable(hoja: String = "Período Actual"): NetworkResult<CierreResponse>` a la interfaz y su implementación usando `api.ejecutarCierre(CierreRequest(hoja = hoja, tipo = "contable"))`.
  - **Criterio de Aceptación:** Método implementado y compilando correctamente dentro del árbol de dependencias de Hilt.

- [x] **Tarea 2: Extender `SistemaUiState` y `SistemaViewModel` con lógica de Cierre y PDF**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaViewModel.kt`
  - **Acción:**
    - Incorporar en `SistemaUiState`: `cierreContable: CierreResponse? = null`, `isEjecutandoCierre: Boolean = false`, `cierreMensaje: String? = null`.
    - Implementar en `SistemaViewModel`:
      * `fun ejecutarCierreContable(hoja: String = "Período Actual")`
      * `fun obtenerUrlAbsolutaPdf(pdfUrlRelativa: String?): String?`
  - **Criterio de Aceptación:** ViewModel maneja los estados de carga, éxito y error al invocar el repositorio.

- [x] **Tarea 3: Implementar la tarjeta interactiva de Cierre Contable en `SistemaScreen.kt`**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt`
  - **Acción:**
    - Agregar Card "Cierre Contable Mensual" con indicador de progreso.
    - Mostrar resumen contable, total en Guaraníes y botón de apertura "Ver Comprobante PDF 📄".
    - Manejar el Intent de Android de forma segura con `ACTION_VIEW` para abrir el archivo PDF en el visor nativo.
  - **Criterio de Aceptación:** Vista renderizada correctamente sin advertencias en Compose.

- [x] **Tarea 4: Revalidación integral con pruebas unitarias en Android**
  - **Archivos:** `app/src/test/java/com/gamma/mobile/presentation/sistema/SistemaViewModelTest.kt` (o equivalente)
  - **Acción:**
    - Verificar que los tests unitarios cubran la ejecución de cierre contable en el ViewModel.
    - Ejecutar `./gradlew testDebugUnitTest`.
  - **Criterio de Aceptación:** `./gradlew testDebugUnitTest` finaliza con BUILD SUCCESSFUL y 100% de tests aprobados.
