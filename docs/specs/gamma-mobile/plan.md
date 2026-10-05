# Plan — Gamma Mobile

Estado: BORRADOR — pendiente de aprobación (Gate Fase 0)

## 1. Arquitectura
```
App Android (Kotlin, Compose M3, Hilt, Retrofit, Room, WorkManager)
   │ HTTPS + X-API-KEY (única)
Flask (WSGI, 1 worker, PythonAnywhere) — Blueprint /api/v1   ← se implementa en Gamma_bot FUERA de este proyecto
   └─ logic/* existentes → Google Sheets + avisos.json
```
Dentro de este proyecto:
```
GammaMobile/
  app/                 (Android)
  backend-spec/
    openapi.yaml       (contrato; fuente única)
    REQUISITOS.md      (por endpoint: reglas, errores, ejemplos, cambios sobre el código actual)
    fixtures/          (JSON de ejemplo por endpoint, éxito y error)
    mock_server/       (servidor Flask mínimo de solo lectura de fixtures, para QA en emulador)
    tests/             (pytest: valida fixtures contra openapi.yaml)
  docs/specs/gamma-mobile/
```
El backend nunca devuelve strings formateados para Telegram; devuelve JSON tipado.

## 2. Contrato (resumen; detalle en `openapi.yaml`)
Se mantienen los esquemas actuales de `api/schemas.py` y se agregan/ajustan:

```
DashboardSummary: mes, ingresos, gastos, flujo_neto, margen_libre, saldo_acumulado, posicion_iva, es_debito_fiscal
EventoProximo:   indice, titulo, fecha_evento (DD/MM/AAAA HH:MM), dias_restantes
MovimientoCreate (+ client_id: UUID opcional)
Obligacion*:     id_obligacion, tipo, nombre, monto_inicial, saldo_actual, estado, fecha_inicio, cuota_referencia_gs
Asistencia:      AsistenciaMarcaRequest{modo: normal|directo} -> {tipo: entrada|salida, hora, mensaje}
                 MateriaMarcaResponse{materia?, hora, mensaje}
Cierre:          CierreRequest{hoja, aplicar_descuento} -> {resumen, pdf_url?}
Ticket:          TicketCreate{asunto, descripcion} -> TicketItem{id TCK-xxx, estado, fecha}
Health:          {status ok|degraded, sheets, avisos, version_api}
Ocr:             {propuesta: MovimientoCreate, confianza?}   (no persiste)
Error:           {error:{code, message, detail?}}
```
Endpoints (`/api/v1`): `auth/verify`, `health`, `dashboard/summary`, `dashboard/eventos?dias=7`, `movimientos` (GET/POST), `movimientos/ocr`, `presupuesto`, `obligaciones` (GET/POST), `obligaciones/{id}/aprobar`, `obligaciones/abonar`, `obligaciones/simular`, `avisos` (GET/POST/DELETE), `ia/interpretar`, `asistencia/marcar`, `asistencia/materia`, `cierres/ejecutar`, `cierres/{id}/pdf`, `soporte/tickets` (GET/POST), `sistema/logs`.

Decisión eventos: el dashboard usa `GET /avisos` (ya cacheado en Room) y filtra localmente los próximos 7 días; `dashboard/eventos` queda como endpoint opcional para ahorrar una llamada al servidor de un solo worker. Se implementa primero el filtrado local (menos acoplamiento con el backend).

Errores HTTP: 401 key, 404, 409 duplicado, 422 validación, 502 fallo Sheets/Gemini, 503 degradado. Sin trazas internas al cliente.

## 3. Restricciones WSGI → diseño de la app
- Un request a la vez en el servidor: cliente con un único `Dispatcher` limitado (`maxRequestsPerHost = 2`), sin refrescos paralelos de todas las pestañas.
- Refresco bajo demanda y TTL de caché por pantalla (p. ej. 60 s); pull-to-refresh fuerza actualización.
- Timeouts: conexión 10 s, lectura 30 s en general; 45 s en IA/OCR/PDF, con mensaje de progreso y reintentar.
- Sin polling ni websockets. Los avisos se evalúan localmente (WorkManager).
- Tolerar respuestas lentas/frías (primera request tras inactividad): indicadores de carga, nunca bloquear la UI.

## 4. Android — módulos
```
com.gamma.mobile/
  core/ (NetworkResult, Money, Clock America/Asuncion, Version)
  di/ · data/{remote,local,repository} · domain/{model,usecase}
  presentation/{navigation,onboarding,finanzas,deudas,avisos,asistencia,sistema,theme}
  worker/ (SyncMovimientosWorker, AvisosWorker)
```
- Estado: `StateFlow<UiState>` (`Loading|Success|Empty|Error|Offline`).
- Interceptor `X-API-KEY`; 401 → onboarding.
- Persistencia: Room (cache + cola PENDING); `EncryptedSharedPreferences` para URL/key.
- `network_security_config`: solo HTTPS; cleartext únicamente en debug (`10.0.2.2`) para el mock server.
- DTOs de Kotlin validados con tests de contrato contra `backend-spec/fixtures`.
- Dashboard: `EventosSemanaCard` = avisos con 0 ≤ días ≤ 7, orden ascendente por fecha; resalte ≤1 día.
- Notificaciones: canal "Avisos", permiso `POST_NOTIFICATIONS`.

## 5. Fases
| Fase | Contenido |
|---|---|
| 0 | Entorno (JAVA_HOME), proyecto nuevo, versionado, CI |
| S | `backend-spec/`: OpenAPI, requisitos, fixtures, mock server |
| A | App: onboarding, Finanzas (con eventos de semana), Deudas |
| B | Avisos + notificaciones, IA texto, OCR |
| C | Asistencia/RRHH, cierres/PDF, soporte |
| Final | QA en emulador con mock server, APK release firmado, reporte para el gate |

Integración real con PythonAnywhere: posterior, cuando apliques el backend y lo apruebes.

## 6. Pruebas
- `backend-spec/tests`: pytest valida cada fixture contra `openapi.yaml`.
- Android: unit tests (ViewModels, repos con fakes, moneda, versión, lógica de eventos), `MockWebServer` con fixtures, Room en memoria.
- QA visual manual en emulador contra `mock_server` (checklist por pantalla: loading/error/empty/offline).

## 7. Riesgos
| Riesgo | Mitigación |
|---|---|
| Backend real diverge del contrato | OpenAPI único + fixtures + paso de integración posterior |
| Un solo worker WSGI se satura | Dispatcher limitado, TTL de caché, sin polling |
| Timeout en IA/OCR/PDF | Timeouts explícitos, reintento, modo manual |
| Duplicados al sincronizar | `client_id` idempotente |
| Fuga de API key | cifrado en cliente, nunca en git/logs |
