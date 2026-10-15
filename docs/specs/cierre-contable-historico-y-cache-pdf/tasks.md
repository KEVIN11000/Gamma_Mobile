# Checklist de Tareas Atómicas de Implementación (Tasks)
## Módulo: Selector de Meses Históricos de Cierre Contable y Caché Local Offline de Comprobantes PDF en Gamma Mobile

- **Fecha:** 15 de Octubre de 2026
- **Versión de Tareas:** 1.0.0
- **Fase:** 0 (Especificación y Arquitectura)
- **Estado:** En Revisión (Gate Obligatorio de Planificación)

---

### Tareas Atómicas de Construcción

- [x] **Tarea 1: Registrar auditoría de validación de Webhook GitHub en Producción**
  - **Archivos:** `docs/specs/cierre-contable-historico-y-cache-pdf/spec.md`, `HANDOFF.md`
  - **Acción:**
    - Documentar la prueba de conectividad y verificación de rechazo seguro por firma HMAC realizada contra `https://kevin11000.pythonanywhere.com/api/v1/webhooks/github`.
  - **Criterio de Aceptación:** Pendiente 1 de v0.0.7 verificado empíricamente con salida del servidor.

- [x] **Tarea 2: Extender `SistemaUiState` y `SistemaViewModel` con selección de períodos y gestión de caché PDF**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaViewModel.kt`
  - **Acción:**
    - Añadir a `SistemaUiState`: `selectedPeriodoCierre: String`, `periodosDisponibles: List<String>`, `isDescargandoPdf: Boolean`.
    - Implementar `fun setPeriodoCierre(periodo: String)`.
    - Adaptar `fun ejecutarCierreContable(hoja: String = _uiState.value.selectedPeriodoCierre)`.
    - Implementar `fun abrirPdfConCache(context: Context, pdfUrlRelativa: String?, periodo: String, onError: (String) -> Unit)`.
  - **Criterio de Aceptación:** ViewModel compila y expone la lógica desacoplada para períodos y caché local de archivos.

- [x] **Tarea 3: Implementar selector de períodos y botón de apertura optimizado en `SistemaScreen.kt`**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt`
  - **Acción:**
    - En la tarjeta "Cierre Contable Mensual", insertar selector de período (Dropdown o chips con scroll horizontal) previo al botón de ejecución.
    - Conectar el botón "Ver Comprobante PDF 📄" con `viewModel.abrirPdfConCache(...)` para visualización offline y soporte local vía `FileProvider`.
    - Incorporar feedback visual si se está descargando o abriendo desde caché local.
  - **Criterio de Aceptación:** Interfaz responsiva que permite al usuario alternar períodos y abrir comprobantes cacheados sin bloqueos.

- [x] **Tarea 4: Cobertura de Pruebas Unitarias y Verificación de Regresión**
  - **Archivos:** `app/src/test/java/com/gamma/mobile/presentation/sistema/SistemaViewModelTest.kt`
  - **Acción:**
    - Implementar tests unitarios para `setPeriodoCierre`, ejecución con período personalizado y resolución de URLs.
    - Ejecutar `./gradlew testDebugUnitTest` en GammaMobile y `pytest tests/` en Gamma_bot.
  - **Criterio de Aceptación:** 100% de tests aprobados en ambos repositorios sin advertencias bloqueantes.
