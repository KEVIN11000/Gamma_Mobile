# HANDOFF — Gamma Mobile

**Fase cerrada:** v0.2.1 (Mobile) — Proyector de Capacidad de Pago con Gastos Ad-Hoc y Escenarios Persistentes en Room v5
**Fecha de cierre:** 2026-10-22
**Estado del build/tests:** ✅ pasando — Compilación y tests verificados al 100% (`./gradlew testDebugUnitTest` 50+ tests exitosos, `./gradlew assembleDebug` limpio en 32s).

---

## 1. Estado actual

- **Pausado de Deudas y Filtro en UI (v0.2.0):** `Obligacion` y `ObligacionEntity` integran la propiedad `esPausada`. `ObligacionesScreen` cuenta con solapa *"Pausadas"*, banner de compromiso mensual activo, botón *"Pausar este mes"* / *"Reanudar"* y exclusión inmediata de cuotas pausadas del compromiso total.
- **Detección Dinámica de Vencimientos Pasados:** `VencimientoUtils` calcula el vencimiento exacto contra la fecha actual (`Calendar`), mostrando en rojo `"⚠️ Venció el DD/MM/AAAA"` si ya expiró en el mes, `"Vence hoy"` o `"Vence el DD/MM/AAAA"`.
- **Persistencia Local de Cierres con Room v4:** Tabla `cierres` persistida vía `CierreEntity`, `CierreDao` y migración `MIGRATION_3_4` en `GammaDatabase` (versión 4).
- **Historial de Cierres y Descarga de PDF:** Pantalla `HistorialCierresScreen` con solapas *Todos*, *Contables* y *Laborales*, tarjetas métricas y visor nativo de comprobantes PDF vía `FileProvider` e `Intent.ACTION_VIEW`.
- **Proyector de Capacidad con Gastos Ad-Hoc (v0.2.1):** Sección *"Gastos Adicionales / Imprevistos"* en `CapacidadPagoScreen` con diálogo modal de entrada rápida, tarjetas de gastos con Switch de inclusión/pausa y botón de eliminación.
- **Persistencia de Escenarios en Room v5 (`MIGRATION_4_5`):** Tabla `escenarios_capacidad` gestionada por `EscenarioCapacidadDao` y `EscenarioCapacidadRepository`. El usuario puede nombrar, guardar, listar en un `ModalBottomSheet`, restaurar y eliminar escenarios completos (ingreso extra, deudas pausadas, abonos y gastos ad-hoc).
- **Semáforo M3 y Resumen Métrico Consolidado:** Línea dedicada *"Gastos Proyectados: Gs. X"* en la tarjeta métrica, con recálculo dinámico en vivo del Semáforo (*Superávit*, *Capacidad Ajustada*, *Déficit*) y DTI proyectado.

---

## 2. Decisiones tomadas en esta fase

- **Decisión:** Mantener los gastos ad-hoc estrictamente en el dominio del simulador/proyector, descartando la conversión automática a asientos contables en el Libro Diario.
  **Por qué:** Consensuado en el `/grill-me` para preservar el aislamiento contable y evitar que pruebas hipotéticas ("What-If") contaminen los registros fiscales y contables de la empresa.
- **Decisión:** Persistencia de Escenarios en Room v5 con `MIGRATION_4_5`.
  **Por qué:** Permite a los usuarios guardar múltiples hipótesis ("Vacaciones", "Fin de Año", "Mes Austero") sin requerir conectividad de red ni cuotas de servidor.
- **Decisión:** Switch individual de inclusión en cada tarjeta de gasto ad-hoc.
  **Por qué:** Ofrece al usuario la ergonomía de evaluar inmediatamente el impacto de postergar un gasto sin tener que borrarlo y volver a escribir el concepto y monto.

---

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `app/src/main/java/com/gamma/mobile/data/local/GammaDatabase.kt` | Bump a versión 5 con `MIGRATION_4_5` y registro de `EscenarioCapacidadEntity`. |
| `app/src/main/java/com/gamma/mobile/data/local/dao/EscenarioCapacidadDao.kt` | DAO con Flow, queries de ordenación por fecha y borrado por ID. |
| `app/src/main/java/com/gamma/mobile/data/local/entity/EscenarioCapacidadEntity.kt` | Entidad Room con serialización JSON para escenarios de proyección. |
| `app/src/main/java/com/gamma/mobile/data/repository/EscenarioCapacidadRepositoryImpl.kt` | Repositorio con mapeo seguro Gson para deudas, abonos y gastos ad-hoc. |
| `app/src/main/java/com/gamma/mobile/domain/model/Models.kt` | `GastoSimulado`, `EscenarioCapacidad` y campo `totalGastosProyectados` en `CapacidadPagoResponse`. |
| `app/src/main/java/com/gamma/mobile/domain/repository/EscenarioCapacidadRepository.kt` | Interfaz de repositorio para escenarios. |
| `app/src/main/java/com/gamma/mobile/di/AppModule.kt` | Inyección de `Gson` y binding de `EscenarioCapacidadRepository`. |
| `app/src/main/java/com/gamma/mobile/di/DatabaseModule.kt` | Proveedor `@Provides` de `EscenarioCapacidadDao` y migración `MIGRATION_4_5`. |
| `app/src/main/java/com/gamma/mobile/presentation/capacidad/CapacidadPagoScreen.kt` | Sección de gastos ad-hoc, modal de adición, fila métrica y `EscenariosBottomSheet`. |
| `app/src/main/java/com/gamma/mobile/presentation/capacidad/CapacidadPagoViewModel.kt` | Lógica de gestión de gastos ad-hoc, guardado, carga y recálculo reactivo. |
| `docs/specs/proyector-gastos-ad-hoc/` | Especificaciones SDD v0.2.1 (`spec.md`, `plan.md`, `tasks.md`). |

---

## 4. Pendientes explícitos para la próxima fase (Sesión Backend `Gamma_bot`)

- [ ] [Backend Gamma_bot: Implementar `InMemorySheetsCache` en `src/repositories/sheets_repository.py` con TTL de 180s/300s — Requerimiento 1 de v0.2.0]
- [ ] [Backend Gamma_bot: Soporte para estado `Pausado` en deudas y asiento contable de abonos en `Libro_Diario` — Requerimiento 2 de v0.2.0]
- [ ] [Backend Gamma_bot: Auto-detección dinámica de período en `AgenteAutonomoHoras` y captura de HTTP 429 — Requerimiento 3 de v0.2.0]
- [ ] [Backend Gamma_bot: Crear endpoint `GET /api/v1/cierres` para historial consolidado de cierres — Requerimiento 4 de v0.2.0]
- [ ] [Backend Gamma_bot: Exponer `GET /obligaciones/vencimientos` y `GET /obligaciones/foco` — Requerimiento 5 de v0.2.0]
- [ ] [Backend Gamma_bot: Endpoint de cálculo `POST /obligaciones/proyectar-capacidad` — Requerimiento 6 de v0.2.0]

---

## 5. Cómo verificar que este handoff sigue vigente

```bash
./gradlew testDebugUnitTest && ./gradlew assembleDebug
```
(Debe compilar limpiamente en verde y superar 50+ pruebas unitarias sin errores).
