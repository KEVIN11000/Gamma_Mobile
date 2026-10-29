# Traspaso Backend (Gamma Mobile -> Gamma_bot)
## Requerimientos de Backend para Estabilidad y Paridad con Gamma Mobile

- **Fecha:** 29 de Octubre de 2026
- **Origen:** Gamma Mobile (Sesión de Remediación y Estabilidad `v0.0.12` / `remediacion-estabilidad-v012`)
- **Destino:** Repositorio `Gamma_bot` (`E:\PG\vcp-s\Gamma_bot`) para desarrollo en su sesión dedicada.
- **Principio:** Cero modificaciones en el backend durante la sesión de la app móvil; todos los requerimientos del servidor quedan formalizados en este documento.

---

### Módulo 1: Alineación de Contratos y Esquemas (Pydantic vs Mobile)

1. **Soporte de Alias `"tipo"` en `POST /api/v1/movimientos` (`src/api/schemas.py`)**:
   - **Problema:** Android envía `"tipo": "Ingreso"`. `MovimientoCreate` solo reconoce `"movimiento"`, descartando `"tipo"` y asignando `TipoMovimiento.GASTO` por defecto. En consecuencia, todos los ingresos móviles se registran como gastos en Google Sheets.
   - **Solución Requerida:** En `src/api/schemas.py`, configurar en `MovimientoCreate`:
     ```python
     movimiento: TipoMovimiento = Field(default=TipoMovimiento.GASTO, alias="tipo")
     ```
     O permitir tanto `tipo` como `movimiento` en el validador antes de crear la fila.

2. **Desempaquetado de `deuda_foco` en `GET /api/v1/obligaciones/foco` (`src/api/blueprint.py`)**:
   - **Problema:** El endpoint devuelve `{"criterio": "...", "deuda_foco": {...}}`, mientras que la app móvil consumía el DTO plano esperando los campos en la raíz.
   - **Solución Requerida:** Mantener el objeto `deuda_foco` o exponer los campos planos en la raíz para garantizar compatibilidad con versiones previas.

3. **Paridad de Clave en IA y OCR (`src/logic/ai_service.py`)**:
   - **Problema:** El servicio de IA retorna `"tipo_movimiento"`, mientras que la app móvil consume `"tipo"`.
   - **Solución Requerida:** Retornar ambas claves (`datos["tipo"] = tipo` y `datos["tipo_movimiento"] = tipo`) en la respuesta JSON.

---

### Módulo 2: Respuestas Informativas y Códigos HTTP

1. **Marcación Laboral Informativa en `POST /api/v1/asistencia/marcar` (`src/api/blueprint.py`)**:
   - **Problema:** Cuando `AgenteAutonomoHoras` retorna `"ℹ️ Ya registraste tu entrada hoy"`, el endpoint devuelve `HTTP 400 Bad Request`, provocando que la app móvil muestre una alerta de error crítico.
   - **Solución Requerida:** Responder con `HTTP 200 OK` y payload estructurado:
     ```json
     {
       "tipo": "info",
       "mensaje": "ℹ️ Ya registraste tu entrada hoy a las HH:mm",
       "confirmado_en_planilla": true
     }
     ```

2. **Invalidación Reactiva de Caché (`src/repositories/sheets_repository.py`)**:
   - **Problema:** `_sheets_cache` tiene un TTL de 180 segundos. Tras registrar transacciones desde la app, `GET /dashboard/summary` sigue retornando el balance antiguo durante 3 minutos.
   - **Solución Requerida:** Llamar a `_sheets_cache.clear()` al insertar movimientos en `POST /api/v1/movimientos`.

3. **Cálculo de Analíticas MTD y Devengado Laboral (`src/logic/logic.py`)**:
   - **Problema:** El devengado de horas y salario computa días con sólo marcación de entrada como si fueran jornadas completas.
   - **Solución Requerida:** En `AgenteAutonomoHoras.obtener_analiticas_horas()`, condicionar el cálculo de horas trabajadas y salario devengado acumulado a que el día tenga registrada su salida efectiva.

---

### Módulo 3: Ejecución Real de Cierres Contables

1. **Reemplazo de Stub en `POST /api/v1/cierres/ejecutar` (`src/api/blueprint.py`)**:
   - **Problema:** El endpoint genera un PDF sintético con ReportLab hardcodeando 160h y 4.5M Gs sin interactuar con Google Sheets ni ejecutar `execute_monthly_closing()`.
   - **Solución Requerida:** Integrar la llamada real a `agente_excel.ejecutar_cierre_periodo_manual()` o `execute_monthly_closing()`.

2. **Sincronización de Cierres Laborales en Historial (`src/api/blueprint.py`)**:
   - **Problema:** No existe un endpoint que liste los cierres laborales archivados en Google Sheets para que la app móvil los consulte.
   - **Solución Requerida:** Implementar `GET /api/v1/asistencia/cierres` retornando la lista de períodos laborales cerrados y sus PDFs.

---

### Módulo 4: Deduplicación e Idempotencia

1. **Recepción de `clientId` o `id` en `POST /api/v1/movimientos` (`src/api/blueprint.py`)**:
   - **Problema:** Si ocurre un timeout de red tras escribir en Google Sheets, el Worker de Android reintenta y duplica el asiento contable en la planilla.
   - **Solución Requerida:** Verificar si el `client_id` recibido en el header `X-Client-Id` o en el cuerpo ya fue registrado en las últimas 24 horas antes de hacer `append_row()`.

---

### Módulo 5: Realineación de Columnas y Migración en `Obligaciones_Maestro`

1. **Desfasaje Horizontal por Sobrescritura de Encabezados (`src/repositories/sheets_repository.py:94-113`)**:
   - **Problema:** En `_ensure_headers`, al detectar que la fila 1 difiere de `ENCABEZADOS_OBLIGACIONES_MAESTRO` (que incluye la columna `Tipo` en la posición 2), el código ejecutó `ws.update_cell(1, col_idx, header)` sobrescribiendo los nombres de las columnas en la fila 1 pero **sin desplazar las celdas de las filas de datos preexistentes**.
   - **Impacto Observado:**
     * La cabecera `Entidad` quedó encima de `Monto_Inicial` (por eso en la app móvil y en la API la entidad devuelve números como `1500000`).
     * La cabecera `Saldo_Actual` quedó encima de `Estado` (el texto `"Activo"` no puede convertirse a entero con `safe_int`, devolviendo `0`).
     * La cabecera `Cuota_Referencia_Gs` quedó encima de saldos acumulados, inflando la cuota por mes.
   - **Solución Requerida:**
     * Crear un script de migración en `Gamma_bot` que lea las filas de datos de `Obligaciones_Maestro`, detecte si la columna 2 contiene el nombre en vez del tipo, e inserte el valor por defecto `"Deuda"` en la columna 2 desplazando los datos hacia la derecha para que coincidan 1:1 con `ENCABEZADOS_OBLIGACIONES_MAESTRO`.
     * En `_ensure_headers`, asegurar que nunca se modifiquen los encabezados sin verificar previamente que la longitud y estructura de las filas de datos coincide con el esquema proyectado.



---

### Módulo 6: Duplicación de `saldo_inicial` en Resumen de Dashboard

1. **Doble Suma de `saldo_inicial` en `GET /api/v1/dashboard/summary` (`src/api/blueprint.py:105` vs `src/logic/financiero.py:407`)**:
   - **Problema:** En `src/logic/financiero.py:407`:
     ```python
     flujo_neto = saldo_inicial + total_ingresos - total_gastos
     ```
     `flujo_neto` ya incluye acumulado el `saldo_inicial` de las filas tipo `Saldo Inicial / Traspaso` de `Libro_Diario`.
     Luego, en `src/api/blueprint.py:105`:
     ```python
     saldo_acumulado = balance.get("saldo_inicial", 0) + flujo_neto
     ```
     El endpoint suma `saldo_inicial` por segunda vez:
     `saldo_acumulado = saldo_inicial + (saldo_inicial + total_ingresos - total_gastos) = 2 * saldo_inicial + ingresos - gastos`.
   - **Evidencia Empírica en Vivo:**
     - En `Libro_Diario` existe un único asiento de `Saldo Inicial / Traspaso` de **`1.431.300 ₲`**.
     - En la pantalla de la app móvil (`phone_screen_balance.png`), el Dashboard muestra **`2.862.600 ₲`** (exactamente `1.431.300 * 2`).
   - **Solución Requerida:**
     - Corregir la definición contable en `src/logic/financiero.py`:
       `flujo_neto = total_ingresos - total_gastos` (flujo neto operativo del período).
       `saldo_acumulado = saldo_inicial + flujo_neto` (saldo disponible al corte).
     - O en `src/api/blueprint.py:105`, fijar `saldo_acumulado = flujo_neto` si `flujo_neto` ya integra el saldo inicial.
