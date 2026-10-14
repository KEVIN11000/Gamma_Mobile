# Bug Fix: Defectos Críticos de Release v0.0.2 (TCK-001 & TCK-002)

- **Slug**: release-v002-fixes
- **Fixed**: 2026-10-14T15:20:00Z
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Se corrigieron de forma integral y coordinada los cinco defectos críticos reportados en el release v0.0.2 (TCK-001 y TCK-002) y la omisión de notificaciones en tickets ("agujero negro"). Se alinearon los contratos de datos de Libro Diario y Obligaciones entre backend y app móvil, se reemplazó el mock de avisos por IA con inferencia real y fallback determinista, se reactivó la notificación de tickets vía Telegram y Vigía, y se dotó a la pantalla de Sistema de feedback visual reactivo, manejo de estados de conexión y versionado dinámico `0.0.2`.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `E:\PG\vcp-s\GammaMobile\app\build.gradle.kts` | modified | Se actualizó `versionName` a `"0.0.2+${getGitCommitHash()}"`. |
| `E:\PG\vcp-s\GammaMobile\app\src\main\java\com\gamma/mobile\presentation\sistema\SistemaScreen.kt` | modified | Se añadió `LinearProgressIndicator` cuando `isLoading == true`, se deshabilitó el botón de recarga durante el proceso y se asignaron colores condicionales (verde para éxito, rojo para error) al mensaje de configuración. Fallback actualizado a `0.0.2+debug`. |
| `E:\PG\vcp-s\GammaMobile\app\src\main\java\com\gamma/mobile\presentation\sistema\SistemaViewModel.kt` | modified | `guardarConfiguracion` ahora invoca `cargarDatosSistema(isTestingConnection = true)`, reportando explícitamente éxito (`✅ Conexión establecida exitosamente con el servidor`) o error (`❌ Error al conectar con el servidor`). |
| `E:\PG\vcp-s\GammaMobile\app\src\main\java\com\gamma/mobile\domain\model\Models.kt` | modified | Se añadieron alias alternativos `@SerializedName("tipo", alternate = ["movimiento"])` en `Movimiento` y alias `alternate` en `Obligacion` (`ID_Obligacion`, `Nombre`, `Monto_Inicial`, `Saldo_Actual`, `Estado`) para tolerar discrepancias de naming entre capas. |
| `E:\PG\vcp-s\Gamma_bot\src\api\blueprint.py` | modified | 1) `GET /movimientos`: ajustado para leer el esquema oficial de 16 columnas (`neto` col 4, `iva` col 5, `total` col 6, `categoria` col 7, `comprobante` col 8) retornando tanto `"tipo"` como `"movimiento"`.<br>2) `POST /movimientos`: inserción de 16 columnas conforme a `ENCABEZADOS_LIBRO_DIARIO`.<br>3) `GET /obligaciones`: normalización explícita a claves `snake_case`.<br>4) `POST /ia/interpretar/aviso`: extracción real mediante IA con fallback regex para fechas agendadas.<br>5) `POST /soporte/tickets`: notificación automática vía Telegram al admin y registro en el subsistema Vigía. |
| `E:\PG\vcp-s\Gamma_bot\tests\test_mobile_api.py` | modified | Se incorporó el test `test_contracts_parity_and_remediation` que verifica en un solo ciclo los contratos de `/movimientos`, `/obligaciones`, `/ia/interpretar/aviso` y `/soporte/tickets`. |

## Diff Highlights

### 1. Paridad de Columnas en Libro Diario (`Gamma_bot/src/api/blueprint.py`)
```python
# Mapeo según ENCABEZADOS_LIBRO_DIARIO (16 columnas)
# 0: Fecha, 1: Movimiento, 2: Proveedor/Cliente, 3: Nro Factura, 4: Neto, 5: IVA, 6: Total, 7: Categoría, 8: Comprobante
fecha = row[0] if len(row) > 0 else ""
mov = row[1] if len(row) > 1 else ""
prov = row[2] if len(row) > 2 else ""
nro_fac = row[3] if len(row) > 3 else ""
neto = float(row[4].replace(".", "").replace(",", ".")) if len(row) > 4 and row[4] else 0.0
iva = float(row[5].replace(".", "").replace(",", ".")) if len(row) > 5 and row[5] else 0.0
total = float(row[6].replace(".", "").replace(",", ".")) if len(row) > 6 and row[6] else 0.0
cat = row[7] if len(row) > 7 else "General"
comp = row[8] if len(row) > 8 else "Factura"
```

### 2. Notificación en Soporte / Tickets (`Gamma_bot/src/api/blueprint.py`)
```python
# Notificar por Telegram a la administración
try:
    from src.bot import bot
    from src.config import CHAT_ID
    msg_telegram = (
        f"🚨 *Nuevo Ticket Móvil Recibido*\n\n"
        f"📌 *ID:* `{ticket_id}`\n"
        f"👤 *Usuario:* {req_obj.usuario}\n"
        f"🏷 *Tipo:* {req_obj.tipo}\n"
        f"📝 *Mensaje:* {req_obj.mensaje}\n"
        f"📱 *Dispositivo:* {req_obj.dispositivo or 'Desconocido'}\n"
        f"⏰ *Fecha:* {ticket_data['fecha']}"
    )
    bot.send_message(CHAT_ID, msg_telegram, parse_mode="Markdown")
except Exception as e_tg:
    logger.warning(f"No se pudo despachar el ticket por Telegram: {e_tg}")
```

### 3. Feedback Reactivo en `SistemaViewModel.kt`
```kotlin
fun guardarConfiguracion(url: String, key: String) {
    viewModelScope.launch {
        sistemaRepository.guardarConfiguracion(url, key)
        _uiState.update { 
            it.copy(
                apiUrl = url, 
                apiKey = key,
                configMessage = "Configuración guardada. Probando conexión..."
            ) 
        }
        cargarDatosSistema(isTestingConnection = true)
    }
}
```

## Tests Added or Updated

- `Gamma_bot/tests/test_mobile_api.py::test_contracts_parity_and_remediation`:
  - Valida que `GET /movimientos` entregue claves `tipo`, `categoria`, `comprobante` y números correctos.
  - Valida que `GET /obligaciones` devuelva claves normalizadas en `snake_case` (`id`, `nombre`, `monto_inicial`, `saldo_actual`, `estado`).
  - Valida que `POST /ia/interpretar/aviso` extraiga fechas agendadas y calcule días restantes sin asignar `now()`.
  - Valida que `POST /soporte/tickets` registre el ticket y despache la alerta por Telegram y Vigía.

## Local Verification

- **GammaMobile**:
  - Comando: `./gradlew testDebugUnitTest`
  - Resultado: **BUILD SUCCESSFUL** (32 tasks ejecutadas, 0 fallos).
- **Gamma_bot**:
  - Comando: `pytest tests/test_mobile_api.py -v`
  - Resultado: **10 passed in 2.68s** (100% de la suite de API móvil superada).
  - Comando: `pytest -q`
  - Resultado: **163 passed in 8.91s** (100% de la suite completa de backend sin regresiones).

## Deviations from Assessment

- Ninguna desviación respecto a la propuesta de remediación del assessment. Se cumplieron todos los puntos planificados y adicionalmente se resolvió el ticket black hole en `POST /soporte/tickets`.

## Follow-ups

- Ejecutar `/speckit-bug-test` para formalizar el reporte de verificación `test.md`.
- Presentar el reporte consolidado al usuario antes de cualquier operación de Git (`git push`) o despliegue a producción.
