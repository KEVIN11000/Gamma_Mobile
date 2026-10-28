# Requerimientos Técnicos para el Backend Core (`Gamma_bot`)
## Módulo: Corrección de Hoja Activa en `AgenteAutonomoHoras` y Metadatos de Confirmación de Planilla

- **Fecha:** 27 de Octubre de 2026
- **Destino:** Repositorio `Gamma_bot` (`E:\PG\vcp-s\Gamma_bot`) para su desarrollo en sesión dedicada.
- **Aislamiento:** Este documento decanta los requerimientos técnicos del servidor para garantizar que la marcación escriba efectivamente en la planilla activa y confirme sus metadatos a la app móvil.

---

### 1. Diagnóstico de la Causa Raíz en el Backend

El usuario reporta que al marcar desde la app móvil, el servidor devuelve respuesta positiva pero **no escribe en la planilla de horas visible**.

Al auditar `Gamma_bot/src/logic/logic.py` (`AgenteAutonomoHoras`):
1. **Desfasaje de `periodo_actual.txt`:**
   En líneas 238-263 de `logic.py`:
   ```python
   path_txt = base_path / "periodo_actual.txt"
   nombre_hoja = path_txt.read_text(encoding="utf-8").strip()
   ```
   Si el archivo `periodo_actual.txt` existe en el servidor y contiene un período antiguo (por ejemplo, `"Mayo 2026"` o `"Septiembre 2026"`), `AgenteAutonomoHoras` **busca o crea la fila en esa hoja antigua**, no en la hoja del mes en curso (`"Octubre 2026"`). El usuario revisa la pestaña de Octubre y la encuentra vacía.
2. **Falta de retorno de metadatos de la hoja:**
   El endpoint `POST /api/v1/asistencia/marcar` (`src/api/blueprint.py`) solo devuelve:
   `{"tipo": tipo, "hora": hora, "mensaje": resultado}`
   No informa en qué pestaña de Google Sheets ni en qué fila escribió, impidiendo que el cliente móvil verifique si se escribió en la hoja esperada.

---

### 2. Soluciones Requeridas en `Gamma_bot`

#### Requerimiento 1: Validación y Sincronización Automática de Período en `AgenteAutonomoHoras`
- **Archivo:** `src/logic/logic.py`
- **Acción:**
  En la propiedad `ws` de `AgenteAutonomoHoras`:
  - Obtener el nombre del mes y año actual según la zona horaria oficial (`get_now_py()`):
    ```python
    meses_es = [
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Setiembre", "Octubre", "Noviembre", "Diciembre"
    ]
    ahora = get_now_py()
    periodo_actual_calendario = f"{meses_es[ahora.month - 1]} {ahora.year}"
    ```
  - Si `periodo_actual.txt` no existe o su contenido difiere del mes/año calendario actual (y no fue forzado explícitamente), actualizar automáticamente `periodo_actual.txt` con `periodo_actual_calendario`.
  - Asegurar que `self._ws = self.wb.worksheet(nombre_hoja)` apunte a la hoja del mes corriente.

#### Requerimiento 2: Retornar Metadatos de Planilla en `ejecutar_marcado_para_bot` y `blueprint.py`
- **Archivos:** `src/logic/logic.py` y `src/api/blueprint.py`
- **Acción:**
  1. En `ejecutar_marcado_para_bot(modo)`:
     Retornar un diccionario estructurado (o tupla) con:
     - `mensaje`: Texto amigable del resultado (`"✅ Entrada registrado a las 08:30."`).
     - `planilla`: Nombre de la hoja donde se escribió (`self.ws.title`).
     - `fila`: Número de fila modificada en Sheets.
     - `confirmado`: `True` si la celda fue escrita exitosamente en Sheets.
  2. En `src/api/blueprint.py` (`POST /asistencia/marcar`):
     Incluir estos campos en el payload JSON:
     ```json
     {
       "tipo": "entrada",
       "hora": "08:30",
       "mensaje": "✅ Entrada registrado a las 08:30.",
       "planilla": "Octubre 2026",
       "fila": 24,
       "confirmado_en_planilla": true
     }
     ```

---

### 3. Comandos de Validación en la Sesión de `Gamma_bot`

```bash
# 1. Probar marcación y respuesta enriquecida
pytest tests/test_mobile_api.py -k "asistencia" -v

# 2. Verificar que periodo_actual.txt contenga el período correcto
python -c "from logic.logic import get_now_py; print(get_now_py())"
```
