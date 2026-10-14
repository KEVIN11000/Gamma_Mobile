# Plan de Arquitectura y Contratos Técnicos (Plan) — Release Fixes

## 1. Contratos de Datos y DTOs

### 1.1. Contrato de Movimientos / Libro Diario (`GET /api/v1/movimientos`)
El backend devolverá el siguiente payload JSON normalizado, respetando las columnas canónicas de `Libro_Diario` definidas en `logic/constants.py` (`ENCABEZADOS_LIBRO_DIARIO`):
- `row[0]`: Fecha
- `row[1]`: Movimiento / Tipo (e.g. "Ingreso", "Egreso", "Gasto")
- `row[2]`: Proveedor/Cliente
- `row[3]`: Nro Factura
- `row[4]`: Neto (monto gravado)
- `row[5]`: IVA (monto impuesto)
- `row[6]`: Total
- `row[7]`: Categoría
- `row[8]`: Comprobante ("Virtual Legal", "Física Legal", "No Legal")

```json
[
  {
    "id": "mov-0",
    "fecha": "14/10/2026",
    "tipo": "Gasto",
    "movimiento": "Gasto",
    "proveedor_cliente": "Supermercado",
    "nro_factura": "001-001-12345",
    "categoria": "Alimentación",
    "comprobante": "Virtual Legal",
    "total": 150000,
    "monto_gravado": 136364,
    "monto_iva": 13636,
    "tasa_iva": "10%"
  }
]
```
En el cliente Android (`com.gamma.mobile.domain.model.Movimiento`):
Garantizar compatibilidad tanto con `tipo` como con `movimiento` para tolerancia a variantes de payload.

### 1.2. Contrato de Obligaciones / Deudas (`GET /api/v1/obligaciones`)
El backend normalizará las claves del diccionario extraído de `Obligaciones_Maestro` a snake_case antes de serializar a JSON:

```python
{
    "id": row.get("ID_Obligacion") or row.get("id", ""),
    "nombre": row.get("Nombre") or row.get("nombre", ""),
    "tipo": row.get("Tipo") or row.get("tipo", ""),
    "monto_inicial": safe_int(row.get("Monto_Inicial") or row.get("monto_inicial")),
    "saldo_actual": safe_int(row.get("Saldo_Actual") or row.get("saldo_actual")),
    "estado": row.get("Estado") or row.get("estado", "Activo"),
    "cuota_referencia_gs": safe_int(row.get("Cuota_Referencia_Gs") or row.get("Cuotas")),
    "fecha_inicio": row.get("Fecha_Inicio") or "",
    "observaciones": row.get("Observaciones") or ""
}
```

### 1.3. Contrato de Interpretación de Avisos (`POST /api/v1/ia/interpretar/aviso`)
Reemplazar la respuesta hardcodeada por una llamada a `AIService.analizar_aviso_con_ia(texto)` que extraiga:
- `titulo`: Breve resumen del evento.
- `fecha_evento`: Fecha y hora en formato `DD/MM/YYYY HH:MM`.
- `dias_restantes`: Cálculo relativo entre `fecha_evento` y la fecha actual en Paraguay (`get_now_py()`).

---

## 2. Cambios en la App Android (`GammaMobile`)

### 2.1. `SistemaScreen.kt` & `SistemaViewModel.kt`
- En `SistemaViewModel`:
  - En `guardarConfiguracion(url, key)`:
    - Ejecutar test de conexión contra `repository.getHealthStatus()`.
    - Si responde `Success`: asignar `configMessage = "✅ Conexión establecida exitosamente con el backend"`.
    - Si responde `Error`: asignar `configMessage = "❌ Error ${result.code}: ${result.message}"`.
    - Si responde `Exception`: asignar `configMessage = "❌ Error de red: ${result.e.localizedMessage}"`.
  - En `SistemaScreen`:
    - Incorporar un `LinearProgressIndicator` superior o un indicador flotante cuando `uiState.isLoading == true`.
    - Proporcionar feedback visual inmediato al pulsar el botón de recarga (TopAppBar refresh).

### 2.2. Gestión de Versión Centralizada
- Crear archivo `VERSION` en la raíz de `GammaMobile` con valor `0.0.2`.
- En `app/build.gradle.kts`:
  - Leer el valor de `VERSION` para configurar `versionName = "${rootProject.file("VERSION").readText().trim()}+${getGitCommitHash()}"`.
  - Asegurar que `BuildConfig.VERSION_NAME` reporte `0.0.2+hash`.

---

## 3. Plan de Verificación y Resiliencia
- Pruebas unitarias en Android:
  - `./gradlew testDebugUnitTest` para validar que los mappers y DTOs deserialicen correctamente payloads mixtos de `Movimiento` y `Obligacion`.
- Pruebas unitarias en Backend:
  - `pytest tests/test_mobile_api.py` para validar los endpoints `/api/v1/movimientos`, `/api/v1/obligaciones` y `/api/v1/ia/interpretar/aviso`.
