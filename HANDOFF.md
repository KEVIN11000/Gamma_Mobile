# HANDOFF — Gamma Mobile

**Fase cerrada:** Gestión, Atención y Verificación de Tickets de Soporte y Normalización de Estados de Deuda
**Fecha de cierre:** 2026-10-15
**Estado del build/tests:** ✅ pasando — Compilación y suites de tests unitarios verificados exitosamente (`./gradlew testDebugUnitTest assembleDebug` en GammaMobile en 29s; 164 tests de `pytest tests/` en Gamma_bot).

---

## 1. Estado actual

- **Ciclo de vida y atención de tickets de soporte:**
  - Endpoint `PATCH /api/v1/soporte/tickets/<ticket_id>` en `Gamma_bot` validado con Pydantic (`TicketUpdate`) y persistencia atómica en `src/data/tickets.json`.
  - Capa de datos en Android (`GammaApi.kt`, `SistemaRepository`, `SistemaRepositoryImpl`) con llamada tipada a `PATCH`.
  - Lógica de estado en `SistemaViewModel.kt` con función `actualizarEstadoTicket(ticketId, nuevoEstado, resolucion)` y soporte de actualización optimista en memoria.
  - UI interactiva en `SistemaScreen.kt` con selector modal "Atender Ticket", actualización en vivo del estado y registro opcional de resolución.
- **Diferenciación de origen y filtros cromáticos:**
  - Campo `origen` incorporado y tipado (`app`, `telegram`, `ci`, `servidor`) en frontend y backend.
  - Badges visuales con iconos representativos (`📱 App`, `✈️ Telegram`, `🤖 CI/CD`, `🚨 Servidor`).
  - Chips de filtrado reactivo en `SistemaScreen.kt` (`Todos`, `Abiertos`, `En proceso`, `Cerrados`) con contadores dinámicos.
  - Sincronización Telegram -> `tickets.json` implementada en `src/com/handlers/soporte.py`, garantizando una única fuente de verdad compartida.
- **Aislamiento y saneamiento de datos:**
  - Aislamiento riguroso de `TICKETS_FILE` en `tests/test_mobile_api.py` mediante monkeypatch a `tmp_path`, impidiendo que las ejecuciones de test contaminen `src/data/tickets.json`.
  - Registros reales de incidentes (TCK-001 a TCK-006) restaurados y persistidos con sus respectivos orígenes.
- **Normalización integral de estados de deuda (Obligaciones):**
  - Identificación y corrección de discrepancia en `Gamma_bot`: corrección del bug en `blueprint.py` donde `update_obligacion_saldo` recibía argumentos posicionales desfasados (`nuevo_estado` sobreescribía `cuotas_restantes`); ahora se pasa con kwargs explícitos y estado unificado `"Saldada"` en paridad con `financiero.py`.
  - Tolerancia gramatical en `SheetsRepository.get_obligaciones_activas` para aceptar tanto `"activo"` como `"activa"`.
  - Incorporación en `Models.kt` (Android) de helpers robustos `esActiva`, `esSaldada` y `esSimulada` que normalizan variaciones de género (`Activo`/`Activa`, `Saldado`/`Saldada`, `Simulado`/`Simulada`) y saldos `<= 0L`.
  - `ObligacionesViewModel.kt` y `ObligacionesScreen.kt` actualizados para filtrar y habilitar acciones ("Abonar") basados en `esActiva`, `esSaldada` y `esSimulada`, garantizando que ninguna deuda saldada quede oculta o mal clasificada.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Desacoplamiento de almacenamiento de tests con `monkeypatch` en `test_mobile_api.py`.
  **Por qué:** Evita que ejecuciones continuas de CI o desarrollo generen tickets sintéticos repetitivos ("Fallo sincronización") en la base de datos real visible en la app.
  **Alternativas descartadas:** Limpiar el archivo manualmente tras cada ejecución de pytest.
- **Decisión:** Helpers booleanos computados en `Obligacion` (`esActiva`, `esSaldada`, `esSimulada`) en lugar de comparaciones rígidas por string.
  **Por qué:** El bot core de Telegram utiliza históricamente `"Saldada"` (femenino), mientras que la API y usuarios en hojas de cálculo pueden registrar `"Saldado"`, `"Pagado"` o dejar saldo en 0. Los helpers absorben cualquier discrepancia sin requerir migraciones destructivas en las planillas.
  **Alternativas descartadas:** Forzar un enum estricto en el backend que fallara al leer filas preexistentes de Google Sheets.
- **Decisión:** Unificación de canal de soporte Telegram en `src/data/tickets.json`.
  **Por qué:** Centraliza los incidentes reportados por usuarios vía Telegram o App en un único tablero auditable desde la pantalla de Sistema de Gamma Mobile.
  **Alternativas descartadas:** Mantener tickets de Telegram solo en memoria/logs separados.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `docs/specs/gestion-tickets-soporte/` | Artefactos SDD: `spec.md`, `plan.md`, `tasks.md` completos. |
| `Gamma_bot (src/data/tickets.json)` | Datos reales limpios con orígenes y estados auditados (TCK-001 al TCK-006). |
| `Gamma_bot (src/api/blueprint.py)` | Endpoint `PATCH /api/v1/soporte/tickets/<id>`, soporte de `origen`, corrección de kwargs en `update_obligacion_saldo` y alineación a `"Saldada"`. |
| `Gamma_bot (src/api/schemas.py)` | Esquema Pydantic `TicketUpdate` y soporte opcional de `origen` en `TicketCreate`. |
| `Gamma_bot (src/com/handlers/soporte.py)` | Fallback de soporte en Telegram ahora sincroniza y persiste con `origen="telegram"`. |
| `Gamma_bot (src/repositories/sheets_repository.py)` | `get_obligaciones_activas` tolera `"activo"` y `"activa"`. |
| `Gamma_bot (tests/test_mobile_api.py)` | Aislamiento con `tmp_path` en tickets y tests completos de `PATCH` y `origen`. |
| `GammaMobile (Models.kt)` | Helpers `esActiva`, `esSaldada`, `esSimulada` en dataclass `Obligacion`. |
| `GammaMobile (SistemaModels.kt)` | Extensión de `TicketItem` con `origen`, `resolucion` y data class `TicketUpdate`. |
| `GammaMobile (GammaApi.kt)` | Endpoint Retrofit `@PATCH("soporte/tickets/{id}")`. |
| `GammaMobile (SistemaRepository.kt / SistemaRepositoryImpl.kt)` | Implementación de `updateTicketStatus`. |
| `GammaMobile (SistemaViewModel.kt)` | Filtrado reactivo por chip y acción de actualización de tickets. |
| `GammaMobile (SistemaScreen.kt)` | Chips de filtrado, badges cromáticos, badge de origen y modal "Atender Ticket". |
| `GammaMobile (ObligacionesViewModel.kt)` | Filtrado y transiciones usando los helpers normalizados de `Obligacion`. |
| `GammaMobile (ObligacionesScreen.kt)` | Renderizado condicional de acciones según helpers normalizados. |
| `GammaMobile (Tests unitarios)` | `SistemaViewModelTest`, `ObligacionesViewModelTest`, stubs en `MovimientoRepositoryTest`, `SyncMovimientosWorkerTest` y `OnboardingViewModelTest`. |

## 4. Pendientes explícitos para la próxima fase

- [ ] Implementar pipeline en `Gamma_bot` que al detectar un ingreso en `Libro_Diario` con categoría/motivo "Salario" (o "Sueldo"), actualice o inserte automáticamente la fila salarial en la hoja `Presupuesto_Base` (`Monto_Mensual_Gs`).
- [ ] Implementar fallback dinámico en `calcular_margen_de_ataque()` para leer el último salario percibido desde `Libro_Diario` si `Presupuesto_Base` no tiene ingreso registrado (`Margen = Salario - Gastos Fijos - Cuotas Activas`).
- [ ] Corregir comando de cierre contable en el core de `Gamma_bot` para permitir el corte mensual de ingresos/egresos del Libro Diario.
- [ ] Implementar sincronización bidireccional del estado de los tickets de soporte con GitHub Issues (Abierto / Cerrado).

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Webhooks de GitHub Issues hacia `Gamma_bot` no implementados aún (la sincronización actual es local y vía API push al crear el issue).
  **Impacto:** Si un issue se cierra manualmente desde la web de GitHub, el estado en `tickets.json` no se actualizará hasta que se atienda desde la app o se configure el webhook.
  **Mitigación:** La atención manual desde la app (`PATCH`) cubre la operatoria inmediata del administrador.

## 6. Cómo verificar que este handoff sigue vigente

```bash
# En GammaMobile:
./gradlew testDebugUnitTest assembleDebug

# En Gamma_bot:
pytest tests/test_mobile_api.py
pytest tests/
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
