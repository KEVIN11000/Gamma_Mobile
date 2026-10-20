# HANDOFF — Gamma Mobile

**Fase cerrada:** v0.1.0 — Remediación Integral UI/UX, Contratos Backend, Blindaje de Cuotas y Previews Compose
**Fecha de cierre:** 2026-10-20
**Estado del build/tests:** ✅ pasando — Compilación y tests verificados al 100% (`./gradlew testDebugUnitTest` 32/32 tareas exitosas, `./gradlew assembleDebug` limpio en 34s).

---

## 1. Estado actual

- **Blindaje de Cuotas Google Sheets en Dashboard:** En `DashboardViewModel.onScreenVisible()` se utiliza `loadDashboard(forceRefresh = false, silent = true)`, evitando saturar la cuota de 60 req/min del backend al alternar solapas; el recálculo se delega a `DashboardSummaryCalculator` (cubierto en `DashboardViewModelTest.kt`).
- **Homologación de Contratos con Backend Pydantic:** Modelo `Movimiento` y `MovimientoEntity` integran `proveedorCliente`, `nroFactura`, `tasaIva` y `clientId` con retrocompatibilidad posicional (cubierto en `MovimientoRepositoryTest.kt`).
- **Resiliencia en Worker de Sincronización:** `SyncMovimientosWorker` captura códigos HTTP 400..499 y marca el registro como `FAILED_CLIENT_VALIDATION` en vez de reintentar infinitamente en segundo plano (cubierto en `SyncMovimientosWorkerTest.kt`).
- **Saneamiento de Mojibake en UI:** En `NuevaTransaccionScreen` se corrigieron los strings corruptos (`₲`, `"Física Legal"`, acentuación y leyendas de desglose de IVA).
- **Mapeo SIFEN de Comprobantes Legales:** `SifenScannerViewModel` mapea RUC emisor a `proveedorCliente`, número de factura y discrimina tasa de IVA 10%/5%/Exento.
- **Ergonomía Táctil y Anti-Solapamiento:** `ObligacionesScreen` cuenta con `contentPadding.bottom = 152.dp` y `AvisosScreen` con `136.dp`, liberando las tarjetas y botones de acción del solapamiento con los FABs; eliminado `Spacer(96.dp)` residual en el root de `DashboardScreen`.
- **Visor SIFEN con Recorte Nítido:** `ScannerOverlay` en `SifenScannerScreen` utiliza `Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }` para que `BlendMode.Clear` perfore la máscara translúcida sin pintar un marco negro opaco.
- **Persistencia Local Inmediata de Avisos:** `AvisoDao` incluye métodos `@Insert` y `deleteById`, sincronizados en `AvisoRepositoryImpl` para reflejar altas y bajas en Room en modo offline.
- **Sincronización Estética de StatusBar:** En `Theme.kt`, `window.statusBarColor` toma `colorScheme.background.toArgb()` fundiéndose limpiamente con los TopAppBar en Material 3.
- **Contrato de Abono a Obligaciones:** `GammaApi` expone `@POST("obligaciones/abonar")` con DTOs `AbonoRequest` y `AbonoResponse`; `ObligacionesViewModel.abonarObligacion` amortiza saldo localmente y despacha el egreso contable (cubierto en `ObligacionesViewModelTest.kt`).
- **Suite de Previews en Jetpack Compose:** Incorporadas funciones `@Preview` en `DashboardScreen.kt`, `ObligacionesScreen.kt` y `NuevaTransaccionScreen.kt` con soporte de tema sobrio.
- **Especificaciones SDD v0.2.0 Listas:** Artefactos preparados en `docs/specs/proyector-capacidad-pago-v020/` (`spec.md`, `plan.md`, `tasks.md` y `requerimientos_gamma_bot_v0.2.0.md`).

## 2. Decisiones tomadas en esta fase

- **Decisión:** Mantener intacto el código fuente de `Gamma_bot` durante la sesión de `GammaMobile`.
  **Por qué:** Principio de gobernanza multi-repositorio y aislamiento de responsabilidades; todos los requerimientos de backend se aislaron en `docs/specs/proyector-capacidad-pago-v020/requerimientos_gamma_bot_v0.2.0.md`.
  **Alternativas descartadas:** Editar scripts en caliente en PythonAnywhere durante el desarrollo móvil.
- **Decisión:** Implementar `CompositingStrategy.Offscreen` en el Canvas del escáner SIFEN.
  **Por qué:** Evita que `BlendMode.Clear` horade directamente hasta la superficie base negra de la ventana Android en lugar de la capa translúcida de Compose.
  **Alternativas descartadas:** Dibujar cuatro rectángulos opacos independientes alrededor del visor.
- **Decisión:** Marcar `FAILED_CLIENT_VALIDATION` ante errores HTTP 400..499 en `SyncMovimientosWorker`.
  **Por qué:** Evita bucles infinitos de reintento en WorkManager ante datos rechazados por validaciones de esquema Pydantic.
  **Alternativas descartadas:** Seguir devolviendo `Result.retry()` indiscriminadamente.
- **Decisión:** Preservar la firma posicional de los primeros 6 parámetros en el constructor de `Movimiento`.
  **Por qué:** Mantiene compatibilidad total hacia atrás con los tests unitarios existentes sin romper inicializaciones previas.
  **Alternativas descartadas:** Reordenar parámetros o forzar constructores con named arguments en toda la suite de tests.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `app/src/main/java/com/gamma/mobile/data/local/dao/AvisoDao.kt` | Agregados métodos `insert` y `deleteById`. |
| `app/src/main/java/com/gamma/mobile/data/local/dao/MovimientoDao.kt` | Parámetro `status` en `updateSyncStatus`. |
| `app/src/main/java/com/gamma/mobile/data/local/entity/MovimientoEntity.kt` | Campos `proveedorCliente`, `nroFactura`, `tasaIva`. |
| `app/src/main/java/com/gamma/mobile/data/remote/GammaApi.kt` | Endpoint `@POST("obligaciones/abonar")`. |
| `app/src/main/java/com/gamma/mobile/data/repository/AvisoRepositoryImpl.kt` | Persistencia inmediata en Room para altas y bajas de avisos. |
| `app/src/main/java/com/gamma/mobile/data/repository/ObligacionRepositoryImpl.kt` | Implementación de `abonarObligacion`. |
| `app/src/main/java/com/gamma/mobile/data/worker/SyncMovimientosWorker.kt` | Gestión de errores HTTP 400..499 con `FAILED_CLIENT_VALIDATION`. |
| `app/src/main/java/com/gamma/mobile/domain/model/Models.kt` | Campos homologados en `Movimiento` y DTOs `AbonoRequest`/`AbonoResponse`. |
| `app/src/main/java/com/gamma/mobile/domain/repository/ObligacionRepository.kt` | Declaración de `abonarObligacion`. |
| `app/src/main/java/com/gamma/mobile/presentation/avisos/AvisosScreen.kt` | Padding inferior `136.dp` anti-solapamiento. |
| `app/src/main/java/com/gamma/mobile/presentation/dashboard/DashboardScreen.kt` | Retiro de Spacer redundante y adición de `@Preview`. |
| `app/src/main/java/com/gamma/mobile/presentation/dashboard/DashboardViewModel.kt` | `onScreenVisible` invoca `forceRefresh = false`. |
| `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesScreen.kt` | Padding inferior `152.dp` anti-solapamiento y `@Preview`. |
| `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesViewModel.kt` | Conexión de llamada a `abonarObligacion`. |
| `app/src/main/java/com/gamma/mobile/presentation/sifen/SifenScannerScreen.kt` | `CompositingStrategy.Offscreen` en Canvas. |
| `app/src/main/java/com/gamma/mobile/presentation/sifen/SifenScannerViewModel.kt` | Mapeo de proveedor, factura e IVA. |
| `app/src/main/java/com/gamma/mobile/presentation/transacciones/NuevaTransaccionScreen.kt` | Saneamiento de mojibake y `@Preview`. |
| `app/src/main/java/com/gamma/mobile/presentation/transacciones/NuevaTransaccionViewModel.kt` | Integración de campos de proveedor y factura. |
| `app/src/main/java/com/gamma/mobile/ui/theme/Theme.kt` | Sincronización de `statusBarColor` con `colorScheme.background`. |
| `docs/specs/proyector-capacidad-pago-v020/` | Documentos SDD y requerimientos backend v0.2.0. |

## 4. Pendientes explícitos para la próxima fase

- [ ] [Backend Gamma_bot: Implementar `InMemorySheetsCache` en `src/repositories/sheets_repository.py` con TTL de 180s/300s — Requerimiento 1 de v0.2.0]
- [ ] [Backend Gamma_bot: Soporte para estado `Pausado` en deudas y asiento contable de abonos en `Libro_Diario` — Requerimiento 2 de v0.2.0]
- [ ] [Backend Gamma_bot: Auto-detección dinámica de período en `AgenteAutonomoHoras` y captura de HTTP 429 — Requerimiento 3 de v0.2.0]
- [ ] [Backend Gamma_bot: Crear endpoint `GET /api/v1/cierres` para historial consolidado de cierres — Requerimiento 4 de v0.2.0]
- [ ] [Backend Gamma_bot: Exponer `GET /obligaciones/vencimientos` y `GET /obligaciones/foco` — Requerimiento 5 de v0.2.0]
- [ ] [Backend Gamma_bot: Endpoint de cálculo `POST /obligaciones/proyectar-capacidad` — Requerimiento 6 de v0.2.0]
- [ ] [Mobile GammaMobile: Soporte de estado `Pausada` en `Obligacion` y selector en `ObligacionesScreen` — Tarea 7 de v0.2.0]
- [ ] [Mobile GammaMobile: Pantalla `HistorialCierresScreen` con visualización y descarga de comprobantes PDF — Tareas 8 a 10 de v0.2.0]
- [ ] [Mobile GammaMobile: Proyector de Capacidad de Pago (`CapacidadPagoScreen`) con semáforo y simulación What-If — Tarea 11 de v0.2.0]

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Cuotas de Google Sheets saturadas mientras `Gamma_bot` no implemente `InMemorySheetsCache`.
  **Impacto:** Llamadas a `/dashboard/summary` pueden devolver `HTTP 429` si se fuerza recarga manual repetitiva.
  **Mitigación sugerida:** Implementar de inmediato el Requerimiento 1 en la sesión de backend.
- **Riesgo:** Cierres mensuales previos no cuentan con endpoint de listado hasta desplegar `GET /api/v1/cierres`.
  **Impacto:** La app móvil no puede renderizar la vista de historial de cierres hasta que el endpoint esté activo en PythonAnywhere.
  **Mitigación sugerida:** Implementar Requerimiento 4 en `Gamma_bot` antes de cerrar la UI móvil de cierres.

## 6. Cómo verificar que este handoff sigue vigente

```bash
./gradlew testDebugUnitTest && ./gradlew assembleDebug
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*