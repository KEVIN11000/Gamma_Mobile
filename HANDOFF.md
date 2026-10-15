# HANDOFF — Gamma Mobile & Core Bot

**Fase cerrada:** Sincronización Salarial Idempotente, Validación de Cierre Contable y Sincronización de Tickets GitHub (v0.0.6)
**Fecha de cierre:** 2026-10-15
**Estado del build/tests:** ✅ pasando — Compilación y suites unitarias verificadas (`./gradlew testDebugUnitTest` en GammaMobile; 179 tests en `pytest tests/` en Gamma_bot).

---

## 1. Estado actual

- **Fallback dinámico salarial para Margen de Ataque (TCK-004):**
  - Implementado `obtener_ultimo_salario_libro_diario()` en `SheetsRepository` con búsqueda cronológica inversa.
  - `calcular_margen_de_ataque()` en `financiero.py` utiliza dinámicamente el último sueldo percibido de `Libro_Diario` cuando `Presupuesto_Base` carece de ingreso fijo registrado.
  - Ticket `TCK-004` auditado y cerrado.
- **Auto-sincronización reactiva de Salario hacia Presupuesto Base:**
  - Implementado `upsert_salario_presupuesto_base()` en `SheetsRepository` para actualizar atómicamente la celda `Monto_Mensual_Gs` o insertar la fila salarial con encabezados canónicos si no existe.
  - Conectado en `AgenteFinanciero.registrar_movimiento()` y `SheetsRepository.insert_movimiento_diario()` de forma no bloqueante ante ingresos de salario/sueldo.
- **Validación de Balance y Protección Previa al Corte Mensual (TCK-005):**
  - Auditoría de consistencia contable en `validar_consistencia_movimientos_mes()`: aborta el cierre si existen montos negativos inválidos o descuadre `Neto + IVA != Total`.
  - Protección de datos: Si el balance es inconsistente o la subida del backup a Google Drive falla, `execute_monthly_closing()` se cancela inmediatamente sin alterar ni limpiar `Libro_Diario`.
  - Mensajes interactivos de Telegram en `deudas.py` formatean el resumen verificado con Margen Libre, Saldo Acumulado transferido y enlace a Drive.
  - Ticket `TCK-005` auditado y cerrado.
- **Sincronización de Tickets con GitHub Issues:**
  - Servicio `GitHubIssueSyncService` en `src/services/github_service.py` conectado a los endpoints `POST` y `PATCH` de `/api/v1/soporte/tickets`.
  - Despacha creación de issues y actualización de estado (`closed` / `open`) con nota de resolución en GitHub sin bloquear la API ante caídas de red o ausencia de token.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Priorizar `Presupuesto_Base` y usar `Libro_Diario` como fallback reactivo antes de forzar al usuario a configuraciones manuales.
  **Por qué:** Evita que usuarios novatos o migraciones vean márgenes libres en 0 o proyecciones vacías.
  **Alternativas descartadas:** Dejar margen libre en 0 hasta que el usuario editara la planilla manualmente.
- **Decisión:** Política de no-limpieza de `Libro_Diario` ante cualquier anomalía de backup o balance en el cierre mensual.
  **Por qué:** Prevenir pérdida accidental de registros contables si falla la cuota de Drive o si hay inconsistencias humanas en el libro.
  **Alternativas descartadas:** Limpiar el libro diario e ignorar fallos en Drive.
- **Decisión:** Desacoplamiento total con timeout de 5s y modo pasivo en `GitHubIssueSyncService`.
  **Por qué:** Garantiza paridad operativa en entornos locales de desarrollo y en producción aunque `GITHUB_TOKEN` no esté configurado o GitHub sufra latencia.
  **Alternativas descartadas:** Hacer que el endpoint de tickets falle con 500 si GitHub no responde.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `docs/specs/sincronizacion-salarial-y-cierre-contable/` | Artefactos SDD: `spec.md`, `plan.md`, `tasks.md` completados. |
| `.specify/bugs/tck-004-fallback-margen-salario/` | Artefactos de corrección: `assessment.md` y `fix.md` de TCK-004. |
| `Gamma_bot (src/repositories/sheets_repository.py)` | `obtener_ultimo_salario_libro_diario()`, `upsert_salario_presupuesto_base()` y auto-sync en `insert_movimiento_diario()`. |
| `Gamma_bot (src/logic/financiero.py)` | Fallback TCK-004, auto-sync salarial en `registrar_movimiento()` y validación previa en `execute_monthly_closing()`. |
| `Gamma_bot (src/com/handlers/deudas.py)` | Reporte formateado con balance estructurado en callbacks de cierre mensual. |
| `Gamma_bot (src/services/github_service.py)` | Servicio `GitHubIssueSyncService` para integración con la API de GitHub. |
| `Gamma_bot (src/api/blueprint.py)` | Despacho a GitHub en endpoints POST y PATCH de soporte. |
| `Gamma_bot (src/data/tickets.json)` | Cierre de `TCK-004` y `TCK-005`. |
| `Gamma_bot (Tests unitarios)` | Nuevas suites en `test_financiero.py`, `test_sheets_repository.py`, `test_handlers_deudas.py` y `test_mobile_api.py`. |

## 4. Pendientes explícitos para la próxima fase

- [ ] Sincronizar y recargar worker WSGI en PythonAnywhere (`TCK-006`).
- [ ] Configurar webhook receptor en `Gamma_bot` para recibir eventos de cierre de issues desde GitHub (`issues.closed`) hacia `tickets.json`.
- [ ] Incorporar visualización de comprobante PDF del cierre contable mensual generado directamente desde la pantalla de Sistema de Gamma Mobile.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Cuotas de API de Google Sheets en períodos con alto volumen de transacciones salariales concurrentes.
  **Impacto:** Rate limits temporales de Google.
  **Mitigación sugerida:** El mecanismo de retry en `_retry_operation` absorbe reintentos con backoff exponencial.

## 6. Cómo verificar que este handoff sigue vigente

```bash
# En GammaMobile:
./gradlew testDebugUnitTest

# En Gamma_bot:
pytest tests/
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
