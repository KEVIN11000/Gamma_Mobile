# Bug Assessment: Defectos Críticos de Release v0.0.2 (TCK-001 & TCK-002)

- **Slug**: release-v002-fixes
- **Created**: 2026-10-14T14:55:00Z
- **Source**: pasted text (Tickets de Soporte TCK-001 y TCK-002)
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

Se auditan de forma combinada los reportes de incidentes post-despliegue del release v0.0.2:

**TCK-001:**
1. Al pulsar 'Guardar y probar' en `SistemaScreen`, la interfaz se queda colgada en *"Configuración guardada. Probando conexión..."* sin confirmar éxito ni reportar error.
2. El botón de recarga superior no muestra animación ni progreso de carga visual.
3. Las pantallas de Dashboard, Deudas (Obligaciones) y Libro Diario muestran datos en 0 o registros mal leídos / desalineados.

**TCK-002:**
1. En la sección Avisos, al guardar un aviso mediante IA, el sistema asigna la fecha y hora actual del sistema en lugar de la fecha agendada en el texto libre.
2. La versión instalada de la aplicación móvil se muestra atascada en `0.0.1` a pesar de haber instalado el artefacto v0.0.2.

---

## Symptom

1. **Configuración colgada**: Al invocar `guardarConfiguracion(url, key)` en `SistemaScreen`, el texto auxiliar permanece indefinidamente en *"Configuración guardada. Probando conexión..."*, impidiendo saber si las credenciales fueron validadas contra `/api/v1/health`.
2. **Ausencia de feedback visual de recarga**: El botón de refresco superior en `SistemaScreen` (y ausente en otras pantallas principales) no rota ni presenta un indicador de progreso (`CircularProgressIndicator`) mientras `isLoading` es `true`.
3. **Lectura errónea y ceros en datos contables**:
   - En **Libro Diario (Diario)**, los registros muestran importes monetarios en los campos de categoría y comprobante; al aplicar filtros por "Ingresos" o "Gastos", la lista queda vacía y todos los ítems se dibujan como egresos.
   - En **Deudas (Obligaciones)**, todos los montos iniciales, saldos y estados se leen en `0` o cadenas vacías.
   - En **Dashboard**, los indicadores de Margen Libre y Saldo Acumulado reportan `0 ₲` o entran en estado `Empty` ("No hay datos registrados aún").
4. **Fecha errónea en avisos agendados por IA**: Un recordatorio redactado como *"Pagar servicio el 25 de octubre a las 10:00"* se almacena con la fecha y hora de la llamada (`now`), disparando alarmas de forma extemporánea o inmediata.
5. **Versión atascada en UI**: En `SistemaScreen`, la tarjeta de versión muestra `Versión instalada: 0.0.1+<hash>` en binarios compilados para v0.0.2.

---

## Reproduction

1. **TCK-001 (1)**: Navegar a la pestaña **Sistema** en Gamma Mobile. Modificar la URL o la API Key y pulsar *"Guardar y Probar Conexión"*. Observar que el mensaje inferior indica *"Configuración guardada. Probando conexión..."* y nunca cambia a *"Conexión exitosa"* ni borra el estado transitorio tras recibir la respuesta de red.
2. **TCK-001 (2)**: En la barra superior de **Sistema**, pulsar el ícono de refresco. Observar que el ícono permanece estático y no hay barra ni anillo de carga en la barra de herramientas.
3. **TCK-001 (3)**:
   - Ir a la pestaña **Diario**: verificar que un movimiento creado desde la bot/web tiene su importe neto en la etiqueta de categoría y su IVA en la de comprobante. Al presionar el filtro "Ingresos", el listado se vacía.
   - Ir a la pestaña **Deudas**: verificar que las tarjetas muestran saldos `0 ₲` y nombres vacíos.
   - Ir al **Dashboard**: verificar que el margen libre y saldo acumulado están en `0` o pantalla vacía.
4. **TCK-002 (1)**: Ir a la pestaña **Avisos**, pulsar el botón flotante `+` ("Crear Aviso con IA"), ingresar *"Renovar seguro el 30/11/2026 a las 09:00"* y presionar "Interpretar". Comprobar en el detalle del aviso que la `fecha_evento` es la fecha y hora exacta del minuto en que se hizo la petición.
5. **TCK-002 (2)**: Abrir la pestaña **Sistema** y observar el texto bajo "Gamma Mobile". Marca `0.0.1+...` en lugar de `0.0.2+...`.

---

## Suspected Code Paths

### Repositorio GammaMobile (Android)
- `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaViewModel.kt:64-73, 97-106` — `guardarConfiguracion` asigna `configMessage = "Configuración guardada. Probando conexión..."`, pero el método `cargarDatosSistema()` no actualiza ni limpia `configMessage` al terminar el ciclo de red.
- `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt:33-36, 177-184` — `IconButton` de refresco estático sin estado de rotación/deshabilitación; renderizado ciego de `configMessage` sin variantes de éxito/error.
- `app/src/main/java/com/gamma/mobile/domain/model/Models.kt:25-38` — Clase `Movimiento`: `@SerializedName("tipo") val tipo: String` no coincide con la llave `"movimiento"` provista por el backend.
- `app/src/main/java/com/gamma/mobile/domain/model/Models.kt:40-52` — Clase `Obligacion`: mapeo estricto `@SerializedName` en `snake_case` (`id`, `nombre`, `monto_inicial`, `saldo_actual`, `estado`) frente a claves devueltas en `PascalCase / Title_Case`.
- `app/src/main/java/com/gamma/mobile/presentation/transacciones/TransaccionesViewModel.kt:84-86` — `filtered = todosLosMovimientos.filter { it.tipo == tipoString }` evalúa contra `tipo` vacío, colapsando el filtrado.
- `app/src/main/java/com/gamma/mobile/presentation/dashboard/DashboardViewModel.kt:49-53` — Trata saldos en `0` como `DashboardUiState.Empty`.
- `app/build.gradle.kts:39` — `versionName = "0.0.1+${getGitCommitHash()}"` cadena estática con prefijo `0.0.1` hardcodeado en lugar de parametrizado o actualizado a `0.0.2`.
- `app/src/main/java/com/gamma/mobile/data/repository/SistemaRepositoryImpl.kt:40-42` — Retorna `BuildConfig.VERSION_NAME` viciado.

### Repositorio Gamma_bot (Backend Core / API)
- `src/api/blueprint.py:146-164` — Endpoint `GET /api/v1/movimientos`: indexación rota contra `Libro_Diario` (`row[4]` leído como categoría cuando corresponde a `Neto`; `row[5]` leído como comprobante cuando corresponde a `IVA`). Omite columna 7 (`Categoría`) y columna 8 (`Comprobante`). Devuelve la clave `"movimiento"` en vez de `"tipo"`.
- `src/api/blueprint.py:210-220` — Endpoint `POST /api/v1/movimientos`: inserta arreglo de 7 elementos desalineados (`nueva_fila = [fecha, req_obj.movimiento.value, req_obj.proveedor_cliente, req_obj.nro_factura or "S/N", req_obj.categoria, req_obj.comprobante.value, str(total)]`), inyectando texto en columnas contables numéricas `Neto` y `IVA`.
- `src/api/blueprint.py:314-331` — Endpoint `POST /api/v1/ia/interpretar/aviso`: stub simulado que ignora el parámetro `texto` e inyecta directamente `"fecha_evento": get_now_py().strftime("%d/%m/%Y %H:%M")`.
- `src/api/blueprint.py:334-346` — Endpoint `GET /api/v1/obligaciones`: retorna directamente el diccionario obtenido de `SheetsRepository.get_todas_obligaciones()`, el cual entrega los encabezados crudos de la planilla (`ID_Obligacion`, `Nombre`, `Monto_Inicial`, `Saldo_Actual`, `Estado`).
- `src/logic/constants.py:48-65, 84-100` — Definición canónica de `ENCABEZADOS_LIBRO_DIARIO` y `ENCABEZADOS_OBLIGACIONES_MAESTRO`.

---

## Root Cause Hypothesis

**Confianza: Alta (100% verificada empíricamente en el código fuente de ambos repositorios)**.

Las fallas reportadas no corresponden a problemas de infraestructura de red ni fallas de concurrencia, sino a cinco defectos concretos de contrato, deserialización y placeholders no completados:
1. **Desincronización de estado en `SistemaViewModel`**: `guardarConfiguracion()` establece una notificación reactiva de prueba, pero el flujo subsiguiente `cargarDatosSistema()` no tiene ramificaciones para sobreescribir `configMessage` según el resultado (`NetworkResult.Success` -> *"Conexión verificada con éxito"*; `NetworkResult.Error` -> *"Error de conexión"*).
2. **Deficiencia de señalización en UI Compose**: No existe vinculación entre `uiState.isLoading` y modificadores de animación (`rotate`, `infiniteTransition`) o componentes visuales (`LinearProgressIndicator` / `CircularProgressIndicator`) en el `TopAppBar`.
3. **Violación de contrato en `Libro_Diario`**: La hoja de cálculo utiliza un esquema canónico de 16 columnas (`Fecha`, `Movimiento`, `Proveedor/Cliente`, `Nro Factura`, `Neto`, `IVA`, `Total`, `Categoría`, `Comprobante`, ...). La API `blueprint.py` asumió erróneamente un esquema abreviado de 7 posiciones donde ubicó categoría en el índice 4 e IVA en el 5. Además, la API emite el campo como `"movimiento"` mientras la entidad móvil Gson `Movimiento` espera `"tipo"`.
4. **Incompatibilidad de Naming Strategy en Obligaciones**: Google Sheets provee encabezados en `PascalCase / Title_Case` (`Monto_Inicial`, `Saldo_Actual`), mientras que la app móvil serializa exclusivamente en `snake_case` sin fallback ni mapper intermedio en la API o en la app.
5. **Stub de IA en avisos y versión congelada en Gradle**: El endpoint `/ia/interpretar/aviso` fue dejado como un mock síncrono hardcodeado utilizando `get_now_py()`. En Android, `app/build.gradle.kts` conservó la cadena literal `"0.0.1+"` en lugar de elevarla a `"0.0.2+"`.

---

## Proposed Remediation

### Preferred (Solución Canónica Integral)

1. **Alineación de Contratos y Normalización en Backend Core (`Gamma_bot`)**:
   - En `src/api/blueprint.py` (`GET /movimientos`), ajustar la lectura de filas respetando los índices canónicos de `ENCABEZADOS_LIBRO_DIARIO`:
     - Índice 0: `fecha`
     - Índice 1: `tipo` (y compatibilidad `"movimiento"`)
     - Índice 2: `proveedor_cliente`
     - Índice 3: `nro_factura`
     - Índice 4: `neto`
     - Índice 5: `iva`
     - Índice 6: `total`
     - Índice 7: `categoria`
     - Índice 8: `comprobante`
   - En `src/api/blueprint.py` (`POST /movimientos`), construir la fila completa con las 16 columnas oficiales (o delegar directamente en `AgenteFinanciero` / `SheetsRepository.insert_movimiento_diario`), asegurando que `categoria` y `comprobante` ocupen sus columnas correctas y se calculen `neto` e `iva`.
   - En `src/api/blueprint.py` (`GET /obligaciones`), mapear explícitamente las claves del diccionario a `snake_case` (`id`, `nombre`, `monto_inicial`, `saldo_actual`, `estado`) antes de llamar a `jsonify()`.
   - En `src/api/blueprint.py` (`POST /ia/interpretar/aviso`), reemplazar el stub de `get_now_py()` integrando la extracción real con `AIService` o un parser de fechas contextual para extraer fecha y hora del texto libre, con fallback a fecha futura agendada (e.g. mañana 09:00), nunca la hora de ejecución del request.

2. **Alineación de Modelos y Robustez en Cliente Android (`GammaMobile`)**:
   - En `app/build.gradle.kts`, corregir `versionName = "0.0.2+${getGitCommitHash()}"` (o sincronizar desde `version.properties`).
   - En `com.gamma.mobile.domain.model.Models.kt`:
     - En `Movimiento`, agregar soporte para leer tanto `"tipo"` como `"movimiento"` si fuera necesario, o garantizar que el backend entregue `"tipo"`.
     - En `Obligacion`, agregar alias alternativos con `@SerializedName` o deserializador tolerante (o confirmar normalización en backend).
   - En `com.gamma.mobile.presentation.sistema.SistemaViewModel.kt`:
     - Actualizar `cargarDatosSistema()` para que, si viene de una acción de configuración o prueba, actualice `configMessage` con el resultado real: *"Conexión exitosa con el servidor"* o el mensaje de error correspondiente.
   - En `com.gamma.mobile.presentation.sistema.SistemaScreen.kt`:
     - Implementar animación de rotación en el ícono de recarga (`Modifier.rotate(rotationAngle)`) cuando `uiState.isLoading` sea verdadero y deshabilitar clics múltiples.
     - Diferenciar el color y estilo de `configMessage` (verde para éxito, rojo para error, primario para en progreso).

### Alternativas (Trade-offs)
- *Alternativa Solo-Android*: Adaptar únicamente el cliente móvil para absorber los defectos del backend (soportar claves PascalCase en `ObligacionEntity`, remapear columnas desfasadas). **Descartada**: Dejaría al backend corrompiendo la planilla de Google Sheets al insertar movimientos y mantendría la deuda técnica y el stub de avisos activo.

---

## Files Likely to Change

### Gamma_bot
- `src/api/blueprint.py` (corrección de lectura/escritura de Libro Diario, serialización de Obligaciones, implementación real de `/ia/interpretar/aviso`)
- `tests/test_api_blueprint.py` (nuevos tests de contrato y verificación de claves JSON)

### GammaMobile
- `app/build.gradle.kts` (`versionName = "0.0.2+..."`)
- `app/src/main/java/com/gamma/mobile/domain/model/Models.kt` (tolerancia de llaves y contratos)
- `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaViewModel.kt` (gestión de `configMessage` post-chequeo)
- `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt` (animación de refresh y estados de feedback)
- `app/src/test/java/com/gamma/mobile/presentation/sistema/SistemaViewModelTest.kt` (pruebas unitarias para validar transición de `configMessage`)

---

## Tests to Add or Update

1. **Backend Tests (`Gamma_bot`)**:
   - `test_get_movimientos_column_mapping`: Comprobar que una fila con 9+ columnas en `Libro_Diario` entregue `categoria` tomada de la columna 7 y `comprobante` de la columna 8, y que contenga la llave `"tipo"`.
   - `test_create_movimiento_row_alignment`: Verificar que al insertar un movimiento, `neto` y `total` contengan números y `categoria` se inserte en la columna correspondiente.
   - `test_get_obligaciones_contract_snake_case`: Validar que la respuesta JSON contenga llaves `id`, `nombre`, `monto_inicial`, `saldo_actual`, `estado`.
   - `test_ia_interpretar_aviso_scheduled_date`: Comprobar que un texto con fecha agendada extraiga la fecha solicitada y no devuelva idéntico timestamp a `now()`.

2. **Android Tests (`GammaMobile`)**:
   - `guardarConfiguracion_updates_configMessage_on_success_and_error`: Validar que `configMessage` cambie a estado completado tras `cargarDatosSistema()`.
   - `movimiento_json_deserialization_test`: Probar deserialización de JSON con `"tipo"` y `"movimiento"`.
   - `obligacion_json_deserialization_test`: Probar deserialización de JSON de obligaciones con claves normalizadas.
   - `build_gradle_version_name_is_v002`: Verificar que `BuildConfig.VERSION_NAME` empiece con `0.0.2`.

---

## Risks & Considerations

- **Integridad de datos históricos en Google Sheets**: Las filas insertadas durante las pruebas de v0.0.2 mediante `POST /movimientos` tienen texto en columnas `Neto` e `IVA`. Se requiere una pasada de auditoría o limpieza manual/script en la planilla de cálculo para corregir celdas desplazadas.
- **Retrocompatibilidad de clientes**: Si se corrigen las claves de `Obligaciones` en el backend, clientes antiguos (si existieran) podrían verse afectados. Dado que v0.0.2 es la primera versión con pantalla de obligaciones integrada, la normalización a `snake_case` es mandatoria e inocua.
- **Límites de cuota de Gemini en extracción de avisos**: La llamada a `AIService` para interpretar avisos debe incluir timeout y manejo de fallback determinista (e.g. regex para expresiones tipo "mañana", "el DD/MM", "a las HH:MM").

---

## Open Questions

- *Ninguna pregunta abierta bloqueante*: Las cinco anomalías fueron localizadas con exactitud en el código fuente y las causas raíz son deterministas y 100% reproducibles.
