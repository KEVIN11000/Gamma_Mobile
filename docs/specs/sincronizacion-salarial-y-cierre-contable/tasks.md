# Checklist de Tareas Atómicas de Implementación (Tasks)
## Módulo: Sincronización Salarial, Cierre Contable y Sincronización de Tickets GitHub

- **Fecha:** 15 de Octubre de 2026
- **Versión de Tareas:** 1.0.0
- **Fase:** 0 (Especificación y Arquitectura)

---

### Tareas Atómicas de Construcción

- [x] **Tarea 1: Implementar `upsert_salario_presupuesto_base` en `SheetsRepository` (`Gamma_bot`)**
  - **Archivos:** `src/repositories/sheets_repository.py`
  - **Acción:** Crear método para actualizar fila existente de sueldo/salario en `Presupuesto_Base` o agregarla si no existe.
  - **Criterio de Aceptación:** Test unitario en `tests/test_sheets_repository.py` validando actualización de fila existente e inserción de nueva fila sin corromper encabezados.

- [x] **Tarea 2: Conectar pipeline de auto-sincronización salarial en `registrar_movimiento` (`Gamma_bot`)**
  - **Archivos:** `src/logic/financiero.py`
  - **Acción:** En `registrar_movimiento`, al confirmar un ingreso con categoría o motivo salarial, invocar `repo.upsert_salario_presupuesto_base()` con el monto registrado.
  - **Criterio de Aceptación:** Test de integración/unitario en `tests/test_financiero.py` confirmando que tras registrar un ingreso salarial, se llama a `upsert_salario_presupuesto_base`.

- [x] **Tarea 3: Implementar validación de balance previo al corte en `execute_monthly_closing` (`Gamma_bot`)**
  - **Archivos:** `src/logic/financiero.py`
  - **Acción:** Agregar verificación de consistencia de débitos y créditos antes de ejecutar el reset de `Libro_Diario`. Si la validación falla o falta backup en Drive, abortar la operación sin limpiar la hoja.
  - **Criterio de Aceptación:** Test unitario en `tests/test_financiero.py` donde un balance corrupto o fallo de drive impide la limpieza de `Libro_Diario` y levanta excepción informativa.

- [x] **Tarea 4: Actualizar y verificar comando de cierre contable en Telegram y resolución de TCK-005 (`Gamma_bot`)**
  - **Archivos:** `src/com/handlers/deudas.py`, `src/data/tickets.json`
  - **Acción:** Refactorizar `callback_cierre_anio` para capturar balance validado, mostrar reporte formateado al usuario en Telegram y marcar `TCK-005` como `Cerrado`.
  - **Criterio de Aceptación:** Test unitario en `tests/test_handlers_deudas.py` verificando el flujo completo de cierre y suite general en verde.

- [x] **Tarea 5: Implementar servicio de sincronización de tickets con GitHub Issues (`Gamma_bot`)**
  - **Archivos:** `src/services/github_service.py`, `src/api/blueprint.py`
  - **Acción:** Crear `GitHubIssueSyncService` y conectarlo a `PATCH /api/v1/soporte/tickets/<id>` para sincronizar el cierre de tickets hacia GitHub cuando `GITHUB_TOKEN` esté disponible.
  - **Criterio de Aceptación:** Tests unitarios con mocks en `tests/test_mobile_api.py` verificando que al invocar PATCH se despacha la llamada de cierre a GitHub sin bloquear la API si no hay conexión.

- [x] **Tarea 6: Revalidación integral end-to-end y auditoría de tests**
  - **Archivos:** Todo el ecosistema (`GammaMobile` y `Gamma_bot`)
  - **Acción:** Ejecución completa de `./gradlew testDebugUnitTest` en GammaMobile y `pytest tests/` en Gamma_bot.
  - **Criterio de Aceptación:** 100% de tests en verde en ambos proyectos y sin archivos temporales residuales.
