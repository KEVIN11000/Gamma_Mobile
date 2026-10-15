# Bug Assessment: Fallback dinámico de lectura de salario en Margen de Ataque

- **Slug**: tck-004-fallback-margen-salario
- **Created**: 2026-10-15
- **Source**: Ticket TCK-004 (origen: app, tickets.json)
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

> **ID:** TCK-004  
> **Asunto:** Fallback dinámico de lectura de salario en Margen de Ataque  
> **Descripción:** Calcular margen contra último ingreso percibido si Presupuesto Base carece de sueldo.  
> **Origen:** app  
> **Estado:** Abierto  

## Symptom

Al invocar `calcular_margen_de_ataque()` (utilizado en el endpoint `/api/v1/sistema/health`, en `/api/v1/financiero/metricas`, y en el cálculo de proyección de deudas `proyectar_cola_deudas()` y `simular_impacto_abono()`), si la hoja `Presupuesto_Base` no tiene una fila configurada de tipo ingreso con monto salarial (`ingresos_base == 0`), el cálculo arroja `0` como margen libre disponible (`0 - gastos_fijos - cuotas`). Esto ocurre a pesar de que el usuario ya haya registrado ingresos salariales válidos en su `Libro_Diario`.

## Reproduction

1. Configurar una hoja `Presupuesto_Base` sin filas de tipo ingreso (`total_ingreso = 0`) pero con gastos fijos registrados (ej. 500.000 Gs).
2. Registrar en `Libro_Diario` un movimiento de tipo `Ingreso` con categoría `"Salario"` o `"Sueldo"` por 5.000.000 Gs.
3. Invocar `calcular_margen_de_ataque()`.
4. **Resultado observado:** Devuelve `0` porque `ingresos_base` se toma exclusivamente de `Presupuesto_Base` (0 - 500.000 < 0).
5. **Resultado esperado:** Debe aplicar un fallback dinámico leyendo el último ingreso salarial percibido en `Libro_Diario` (5.000.000 Gs) y calcular `5.000.000 - 500.000 = 4.500.000 Gs`.

## Suspected Code Paths

- `Gamma_bot/src/logic/financiero.py:480` (`calcular_margen_de_ataque`): Toma `ingresos_base` directamente de `repo.obtener_presupuesto_base_completo()` sin verificar si es 0 ni consultar `Libro_Diario`.
- `Gamma_bot/src/repositories/sheets_repository.py`: No cuenta con un método específico para recuperar de forma reversa (del más reciente al más antiguo) el último ingreso por salario/sueldo registrado en `Libro_Diario`.

## Root Cause Hypothesis

`calcular_margen_de_ataque()` asume rígidamente que `Presupuesto_Base` siempre está pre-poblado por el usuario con su sueldo base mensual presupuestado. En la práctica, muchos usuarios solo registran sus ingresos diarios/mensuales a través de la app o el bot en `Libro_Diario`. Cuando `Presupuesto_Base` carece de filas de ingreso, el cálculo del margen colapsa a 0, impidiendo la proyección correcta del plan bola de nieve de amortización y las métricas de salud financiera. (Confianza: Alta).

## Proposed Remediation

**Preferred**:
1. Implementar en `SheetsRepository` el método `obtener_ultimo_salario_libro_diario() -> int`. Este método leerá los registros de `Libro_Diario` en orden cronológico inverso (desde el más reciente) buscando el primer movimiento con `Movimiento` que contenga `"ingreso"` y cuya `Categoría` o `Proveedor/Cliente` contenga `"salario"` o `"sueldo"`, extrayendo el monto positivo (`Total` o `Neto`).
2. En `calcular_margen_de_ataque()` (`src/logic/financiero.py`), si `ingresos_base <= 0`, invocar `repo.obtener_ultimo_salario_libro_diario()`. Si retorna un valor `> 0`, usarlo como sustituto de `ingresos_base`.
3. Preservar intacta la fórmula: `margen = ingresos_base - gastos_fijos - suma_cuotas` (`return margen if margen > 0 else 0`).
4. Actualizar el estado de `TCK-004` en `src/data/tickets.json` a `"Cerrado"` con nota de resolución explicativa.

**Alternatives**:
- *Alternativa 1:* Obligar al usuario a cargar manualmente su sueldo en `Presupuesto_Base`. Descartada porque empeora la UX y rompe la promesa de autonomía y auto-aprendizaje del bot.

**Files likely to change**:
- `E:/PG/vcp-s/Gamma_bot/src/repositories/sheets_repository.py`
- `E:/PG/vcp-s/Gamma_bot/src/logic/financiero.py`
- `E:/PG/vcp-s/Gamma_bot/src/data/tickets.json`
- `E:/PG/vcp-s/Gamma_bot/tests/test_financiero.py`
- `E:/PG/vcp-s/Gamma_bot/tests/test_sheets_repository.py`

**Tests to add or update**:
- Test unitario en `test_financiero.py`: verificar que `calcular_margen_de_ataque()` activa el fallback dinámico cuando `obtener_presupuesto_base_completo` devuelve 0 ingresos y `obtener_ultimo_salario_libro_diario` retorna un salario válido.
- Test unitario en `test_sheets_repository.py`: verificar que `obtener_ultimo_salario_libro_diario()` recupera correctamente el salario más reciente ignorando egresos u otras categorías.

## Risks & Considerations

- **Riesgo:** Movimientos con montos mal formateados (strings con puntos o comas).
  *Mitigación:* Se utiliza `safe_int()` estándar del proyecto para la conversión de montos.
- **Riesgo:** Rendimiento al consultar `Libro_Diario`.
  *Mitigación:* La lectura ya se realiza vía cache/batch en memoria por fila de hoja de cálculo en `_get_records`.

## Open Questions

- Ninguna. El comportamiento deseado está completamente acotado por el ticket TCK-004 y el handoff.
