# HANDOFF — Gamma Mobile

**Fase cerrada:** v0.2.0 (Mobile) — Proyector de Capacidad de Pago, Historial de Cierres (Room v4 & PDF), Pausado de Deudas y Timeline de Vencimientos Exactos
**Fecha de cierre:** 2026-10-22
**Estado del build/tests:** ✅ pasando — Compilación y tests verificados al 100% (`./gradlew testDebugUnitTest` 40+ tests exitosos, `./gradlew assembleDebug` limpio en 38s).

---

## 1. Estado actual

- **Pausado de Deudas y Filtro en UI (Tarea 7):** `Obligacion` y `ObligacionEntity` integran la propiedad `esPausada`. `ObligacionesScreen` cuenta con solapa *"Pausadas"*, banner de compromiso mensual activo, botón *"Pausar este mes"* / *"Reanudar"* y exclusión inmediata de cuotas pausadas del compromiso total.
- **Detección Dinámica de Vencimientos Pasados:** `VencimientoUtils` calcula el vencimiento exacto contra la fecha actual (`Calendar`), mostrando en rojo `"⚠️ Venció el DD/MM/AAAA"` si ya expiró en el mes, `"Vence hoy"` o `"Vence el DD/MM/AAAA"`.
- **Persistencia Local de Cierres con Room v4 (Tarea 8):** Tabla `cierres` persistida vía `CierreEntity`, `CierreDao` y migración `MIGRATION_3_4` en `GammaDatabase` (versión 4), provista mediante Hilt en `DatabaseModule`.
- **Repositorio de Cierres y Endpoints (Tarea 9):** Declarados `@GET("cierres")` y `@Streaming @GET("cierres/reportes/{filename}")` en `GammaApi`. Implementado `CierresRepository` / `CierreRepositoryImpl` con sincronización de caché (TTL 30 min) y fallback offline en Room.
- **Historial de Cierres y Descarga de PDF (Tarea 10):** Pantalla `HistorialCierresScreen` con solapas *Todos*, *Contables* y *Laborales*, tarjetas métricas de totales y horas, visualización/descarga de PDF vía `FileProvider` e `Intent.ACTION_VIEW`, integrada en las solapas de `AsistenciaScreen` y `Screen.HistorialCierres`.
- **Proyector de Capacidad de Pago (Tarea 11):** `CapacidadPagoViewModel` y `CapacidadPagoScreen` con semáforo Material 3 reactivo (*Superávit Holgado*, *Capacidad Ajustada* con buffer <15%, *Déficit de Cobertura*), resumen numérico (Fondo disponible, compromisos, margen remanente, DTI proyectado), ajuste de ingresos extraordinarios (aguinaldos/comisiones) y simulador interactivo de deudas (switch de pausa temporal y abonos extraordinarios con cálculo automático de meses ahorrados).
- **Previews en Jetpack Compose y Calidad Final (Tarea 12):** Previews en Compose con temas sobrios para `CapacidadPagoScreenPreview`, `CapacidadPagoDeficitPreview`, `HistorialCierresScreenPreview` y `ObligacionItemPreview`.

---

## 2. Decisiones tomadas en esta fase

- **Decisión:** Motor de cálculo local offline en `CapacidadPagoViewModel` a partir de `DashboardRepository.getSummary()` y `ObligacionRepository.getObligaciones()`.
  **Por qué:** Permite que la app calcule al instante las simulaciones y el semáforo M3 sin latencia de red ni dependencia bloqueante del backend en PythonAnywhere.
  **Alternativas descartadas:** Esperar a que el servidor implemente `POST /obligaciones/proyectar-capacidad` para poder usar la pantalla.
- **Decisión:** Almacenar comprobantes PDF en `context.cacheDir/cierres_reportes/` y abrirlos con `FileProvider`.
  **Por qué:** Evita requerir permisos de almacenamiento externo (`WRITE_EXTERNAL_STORAGE`) en Android 10+ y garantiza sandboxing seguro de archivos.
  **Alternativas descartadas:** Guardar en `Environment.getExternalStoragePublicDirectory()`.
- **Decisión:** Migración explícita `MIGRATION_3_4` en Room sin `fallbackToDestructiveMigration()`.
  **Por qué:** Protege la integridad de los movimientos contables, obligaciones y avisos existentes en el dispositivo del usuario.
  **Alternativas descartadas:** Recrear la base de datos perdiendo el caché local.
- **Decisión:** Navegación hacia `Screen.CapacidadPago` accesible desde el banner de compromiso mensual activo en `ObligacionesScreen`.
  **Por qué:** Ofrece una vía contextual inmediata para simular el impacto en el margen libre al revisar las deudas del mes.

---

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `app/src/main/java/com/gamma/mobile/data/local/GammaDatabase.kt` | Bump a versión 4 con `MIGRATION_3_4` y registro de `CierreEntity`. |
| `app/src/main/java/com/gamma/mobile/data/local/dao/CierreDao.kt` | DAO de Room para operaciones CRUD sobre historial de cierres. |
| `app/src/main/java/com/gamma/mobile/data/local/entity/CierreEntity.kt` | Entidad Room y conversores bidireccionales de dominio. |
| `app/src/main/java/com/gamma/mobile/data/remote/GammaApi.kt` | Endpoints `@GET("cierres")` y `@Streaming @GET("cierres/reportes/{filename}")`. |
| `app/src/main/java/com/gamma/mobile/data/repository/CierreRepositoryImpl.kt` | Sincronización en Room con TTL y descarga de PDFs. |
| `app/src/main/java/com/gamma/mobile/data/repository/CierresRepositoryImpl.kt` | Adaptador hacia la interfaz plural `CierresRepository`. |
| `app/src/main/java/com/gamma/mobile/domain/model/Models.kt` | `esPausada`, `calcularCompromisoMensual`, `SemaforoViabilidad`, `CapacidadPagoRequest`, `CapacidadPagoResponse`. |
| `app/src/main/java/com/gamma/mobile/domain/repository/CierreRepository.kt` | Declaración de `getCierres` y `descargarComprobantePdf`. |
| `app/src/main/java/com/gamma/mobile/domain/repository/CierresRepository.kt` | Interfaz unificada de repositorio de cierres. |
| `app/src/main/java/com/gamma/mobile/presentation/capacidad/CapacidadPagoScreen.kt` | Pantalla del simulador con semáforo M3, métricas, abonos y deudas. |
| `app/src/main/java/com/gamma/mobile/presentation/capacidad/CapacidadPagoViewModel.kt` | ViewModel con motor reactivo de simulación y cálculo de viabilidad. |
| `app/src/main/java/com/gamma/mobile/presentation/cierre/CierresViewModel.kt` | ViewModel para gestión de cierres y descarga de PDF. |
| `app/src/main/java/com/gamma/mobile/presentation/cierre/HistorialCierresScreen.kt` | Pantalla de historial de cierres con filtros y visor de PDF. |
| `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesScreen.kt` | Solapa Pausadas, banner con acceso al proyector y tarjeta de vencimiento. |
| `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesViewModel.kt` | Manejo de deudas pausadas, reanudación y compromiso mensual activo. |
| `app/src/main/java/com/gamma/mobile/utils/VencimientoUtils.kt` | Utilidad de cálculo de estado de vencimiento (vencido, vence hoy, futuro). |
| `app/src/main/java/com/gamma/mobile/ScreenBackStack.kt` | Soporte de retroceso para pantallas secundarias (`CapacidadPago`, `HistorialCierres`). |
| `app/src/main/java/com/gamma/mobile/MainActivity.kt` | Rutas `Screen.CapacidadPago` y `Screen.HistorialCierres`. |
| `docs/specs/proyector-capacidad-pago-v020/tasks.md` | Checklist de tareas completadas del Bloque B. |

---

## 4. Pendientes explícitos para la próxima fase (Sesión Backend `Gamma_bot`)

- [ ] [Backend Gamma_bot: Implementar `InMemorySheetsCache` en `src/repositories/sheets_repository.py` con TTL de 180s/300s — Requerimiento 1 de v0.2.0]
- [ ] [Backend Gamma_bot: Soporte para estado `Pausado` en deudas y asiento contable de abonos en `Libro_Diario` — Requerimiento 2 de v0.2.0]
- [ ] [Backend Gamma_bot: Auto-detección dinámica de período en `AgenteAutonomoHoras` y captura de HTTP 429 — Requerimiento 3 de v0.2.0]
- [ ] [Backend Gamma_bot: Crear endpoint `GET /api/v1/cierres` para historial consolidado de cierres — Requerimiento 4 de v0.2.0]
- [ ] [Backend Gamma_bot: Exponer `GET /obligaciones/vencimientos` y `GET /obligaciones/foco` — Requerimiento 5 de v0.2.0]
- [ ] [Backend Gamma_bot: Endpoint de cálculo `POST /obligaciones/proyectar-capacidad` — Requerimiento 6 de v0.2.0]

---

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Cuotas de Google Sheets en PythonAnywhere pueden saturarse con consultas repetitivas de dashboard si no se implementa el caché en memoria de backend.
- **Riesgo:** El historial de cierres remoto (`GET /api/v1/cierres`) en móvil operará en modo vacío/fallback de Room hasta que el endpoint esté desplegado en `Gamma_bot`.

---

## 6. Cómo verificar que este handoff sigue vigente

```bash
./gradlew testDebugUnitTest && ./gradlew assembleDebug
```
(Debe compilar en verde y pasar 40+ tests unitarios sin fallos).
