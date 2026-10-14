# Lista de Tareas Atómicas (Tasks) — Release Fixes

- [ ] **Tarea 1: Dinamización de versión en GammaMobile**
  - Crear archivo `VERSION` con `0.0.2` en la raíz de `GammaMobile`.
  - Actualizar `app/build.gradle.kts` para leer `versionName` desde dicho archivo.
  - *Criterio de aceptación:* `.\gradlew assembleDebug` compila y genera `BuildConfig.VERSION_NAME` con prefijo `0.0.2`.

- [ ] **Tarea 2: Feedback visual y resolución de bloqueo en SistemaScreen & ViewModel**
  - Modificar `SistemaViewModel.kt` para evaluar y actualizar `configMessage` con el resultado real de la prueba de conexión (éxito o error explícito) en `guardarConfiguracion`.
  - Añadir en `SistemaScreen.kt` un indicador de progreso (`LinearProgressIndicator`) cuando `uiState.isLoading` esté activo.
  - *Criterio de aceptación:* Tests de ViewModel pasan y la pantalla refleja el estado de carga y el resultado final sin quedar colgada en "Probando conexión...".

- [ ] **Tarea 3: Normalización de DTOs en GammaMobile (Obligaciones y Movimientos)**
  - En `com.gamma.mobile.domain.model.Models.kt`, asegurar que `Movimiento` soporte tanto `tipo` como `movimiento`, y que `Obligacion` tenga deserialización robusta.
  - *Criterio de aceptación:* Pruebas unitarias de parsing con Gson verifican la lectura correcta de payloads sin ceros espurios.

- [ ] **Tarea 4: Corrección de Mapeo de Columnas y Normalización en Backend (Gamma_bot)**
  - En `Gamma_bot/src/api/blueprint.py`, alinear índices de `Libro_Diario` (`row[1]` tipo, `row[4]` neto, `row[5]` iva, `row[6]` total, `row[7]` categoria, `row[8]` comprobante).
  - En `/api/v1/obligaciones`, normalizar las claves del diccionario a snake_case (`id`, `nombre`, `monto_inicial`, `saldo_actual`, `estado`).
  - *Criterio de aceptación:* `pytest tests/test_mobile_api.py` pasa exitosamente verificando la estructura exacta del JSON devuelto.

- [ ] **Tarea 5: Implementación de Extracción de Fecha real con IA en Avisos (Gamma_bot)**
  - En `Gamma_bot/src/api/blueprint.py` (`/api/v1/ia/interpretar/aviso`), conectar la interpretación con `AIService` o regex parser de fecha/hora para extraer la fecha real agendada por el usuario en lugar de fijar la hora del sistema.
  - *Criterio de aceptación:* Una solicitud con "Reunión el 25/12/2026 15:00" retorna `fecha_evento: "25/12/2026 15:00"`.

- [ ] **Tarea 6: Verificación End-to-End y Suite de Pruebas**
  - Ejecutar la suite completa de tests de Android (`./gradlew testDebugUnitTest`).
  - Ejecutar la suite de tests del backend (`pytest tests/test_mobile_api.py`).
  - *Criterio de aceptación:* 100% de tests verdes en ambos repositorios.
