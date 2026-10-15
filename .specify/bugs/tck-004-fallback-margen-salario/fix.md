# Bug Fix: Fallback dinámico de lectura de salario en Margen de Ataque

- **Slug**: tck-004-fallback-margen-salario
- **Fixed**: 2026-10-15
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Se implementó el método `obtener_ultimo_salario_libro_diario()` en `SheetsRepository` y se integró como fallback dinámico en `calcular_margen_de_ataque()`, resolviendo el ticket TCK-004 para que el margen libre y la proyección de amortización utilicen el último salario percibido en `Libro_Diario` cuando `Presupuesto_Base` no tiene ingreso registrado.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `Gamma_bot/src/repositories/sheets_repository.py` | modified | Agregado método `obtener_ultimo_salario_libro_diario` con búsqueda inversa cronológica. |
| `Gamma_bot/src/logic/financiero.py` | modified | Fallback en `calcular_margen_de_ataque()` cuando `ingresos_base <= 0`. |
| `Gamma_bot/tests/test_sheets_repository.py` | modified | Agregados tests `test_obtener_ultimo_salario_libro_diario` y `test_obtener_ultimo_salario_libro_diario_none_found`. |
| `Gamma_bot/tests/test_financiero.py` | modified | Agregados tests `test_calcular_margen_de_ataque_fallback_libro_diario` y `test_calcular_margen_de_ataque_fallback_zero_when_no_salary`. |
| `Gamma_bot/src/data/tickets.json` | modified | Actualizado estado de `TCK-004` a `Cerrado` con detalle de resolución. |

## Diff Highlights

```python
# Gamma_bot/src/logic/financiero.py
def calcular_margen_de_ataque() -> int:
    repo = SheetsRepository()
    ingresos_base, gastos_fijos = repo.obtener_presupuesto_base_completo()
    deudas_activas = repo.get_obligaciones_activas()

    suma_cuotas = sum(safe_int(d.get("Cuota_Referencia_Gs", 0)) for d in deudas_activas)

    # Fallback dinámico si Presupuesto_Base no tiene ingresos registrados (TCK-004)
    if ingresos_base <= 0:
        salario_fallback = repo.obtener_ultimo_salario_libro_diario()
        if salario_fallback > 0:
            ingresos_base = salario_fallback

    margen = ingresos_base - gastos_fijos - suma_cuotas
    return margen if margen > 0 else 0
```

## Tests Added or Updated

- `tests/test_financiero.py::test_calcular_margen_de_ataque_fallback_libro_diario` — Verifica que cuando `Presupuesto_Base` tiene 0 ingresos, toma el salario percibido de `Libro_Diario` y calcula el margen correcto descontando costos fijos y cuotas.
- `tests/test_financiero.py::test_calcular_margen_de_ataque_fallback_zero_when_no_salary` — Asegura que si tampoco hay salario en `Libro_Diario`, el margen retorna 0 de forma segura.
- `tests/test_sheets_repository.py::test_obtener_ultimo_salario_libro_diario` — Valida la búsqueda cronológica inversa para recuperar el salario más reciente ignorando egresos u otras categorías.
- `tests/test_sheets_repository.py::test_obtener_ultimo_salario_libro_diario_none_found` — Valida el retorno de 0 cuando no existen ingresos de salario en la hoja.

## Local Verification

- Commands run: `pytest tests/` → 168 passed in 8.37s (todos en verde).
- Suite focalizada: `pytest tests/test_financiero.py tests/test_sheets_repository.py` → 33 passed in 2.79s.

## Deviations from Assessment

None.

## Follow-ups

- Ejecutar `/speckit-bug-test slug=tck-004-fallback-margen-salario` para auditoría formal de no regresión.
- Abordar el siguiente requerimiento solicitado por el usuario: `/speckit-plan` para los pendientes heredados.
