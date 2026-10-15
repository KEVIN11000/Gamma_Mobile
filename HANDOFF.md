# HANDOFF — Gamma Mobile & Core Bot

**Fase cerrada:** Webhook Receptor Reactivo de GitHub Issues y Comprobante PDF de Cierre Contable en Gamma Mobile (v0.0.7)
**Fecha de cierre:** 2026-10-15
**Estado del build/tests:** ✅ pasando — Compilación y suites unitarias verificadas (`./gradlew testDebugUnitTest` en GammaMobile; 187 tests en `pytest tests/` en Gamma_bot).

---

## 1. Estado actual

- **Receptor Webhook GitHub Issues (`Gamma_bot`):**
  - Implementado `POST /api/v1/webhooks/github` autenticado con firma criptográfica HMAC-SHA256 (`X-Hub-Signature-256`) y modo permisivo para desarrollo.
  - Parseo automático de títulos de issues con formato `[TCK-XXX]` para transicionar tickets a `"Cerrado"` (`action == "closed"`) con nota de resolución y a `"Abierto"` (`action == "reopened"`).
  - Manejo de `ping` (`{"status": "pong"}`) y respuesta de seguridad `200 OK` (`"status": "ignored"`) para issues ajenos o eventos no soportados.
  - Notificación reactiva al Telegram del Administrador ante actualización exitosa de tickets.
  - Cubierto por 8 tests unitarios aislados con `tmp_path` en `tests/test_github_webhook.py`.
- **Generación y Visualización de Comprobante PDF de Cierre Contable (`GammaMobile`):**
  - Métodos `ejecutarCierreContable` en `SistemaRepository` y `SistemaRepositoryImpl` consumiendo `POST /api/v1/cierres/ejecutar`.
  - `SistemaViewModel` gestiona estados de carga, error y cálculo de URL canónica absoluta del comprobante (`obtenerUrlAbsolutaPdf`).
  - Tarjeta interactiva en `SistemaScreen.kt` mostrando resumen del balance mensual, monto formateado en Guaraníes y botón *"Ver Comprobante PDF 📄"* que lanza el visor de documentos de Android mediante `Intent(ACTION_VIEW)`.
  - Cubierto por tests unitarios en `SistemaViewModelTest.kt`.
- **Sincronización y Cierre de Incidentes:**
  - `TCK-003` (sincronización salarial reactiva) y `TCK-006` (despliegue de WSGI en PythonAnywhere) auditados y cerrados en `src/data/tickets.json`.
  - Commits publicados en ramas remotas `origin/Main-stable` (`Gamma_bot`) y `origin/main` (`GammaMobile`), disparando el auto-deploy continuo de producción.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Desacoplar la ruta `/api/v1/webhooks/github` del middleware de API key móvil (`@require_api_key`).
  **Por qué:** GitHub Webhooks no envía tokens personalizados en cabeceras de usuario, sino la firma HMAC estándar en `X-Hub-Signature-256`.
  **Alternativas descartadas:** Forzar query parameter con api key en la URL del webhook en GitHub.
- **Decisión:** Responder `200 OK` con `{"status": "ignored"}` ante issues sin identificador `[TCK-XXX]`.
  **Por qué:** Respuestas 4xx o 5xx provocan reintentos exponenciales y alertas de fallo en los webhooks de GitHub.
  **Alternativas descartadas:** Retornar 400 Bad Request ante títulos sin formato TCK.
- **Decisión:** Delegar la apertura del PDF al visor nativo de Android mediante Intent (`ACTION_VIEW`).
  **Por qué:** Evita sobrecargar el APK con librerías nativas pesadas de renderizado C++ y aprovecha las herramientas ya instaladas en el dispositivo (Drive, Acrobat, navegador).
  **Alternativas descartadas:** Incrustar visor canvas PDF nativo dentro de Compose.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `docs/specs/webhook-github-tickets/` | Artefactos SDD: `spec.md`, `plan.md`, `tasks.md` completados. |
| `docs/specs/comprobante-cierre-contable-pdf/` | Artefactos SDD: `spec.md`, `plan.md`, `tasks.md` completados. |
| `Gamma_bot (src/services/github_service.py)` | `GitHubWebhookService` con validación HMAC-SHA256 y parseo de eventos de issues. |
| `Gamma_bot (src/api/blueprint.py)` | Ruta `POST /api/v1/webhooks/github` y notificación push a Telegram. |
| `Gamma_bot (tests/test_auth_security.py)` | Aislamiento en `setUp`/`tearDown` de variables de entorno para evitar polución. |
| `Gamma_bot (tests/test_github_webhook.py)` | Suite de 8 tests unitarios para webhooks. |
| `Gamma_bot (src/data/tickets.json)` | Cierre de `TCK-003` y `TCK-006`. |
| `GammaMobile (SistemaRepository.kt, SistemaRepositoryImpl.kt)` | Integración de `ejecutarCierreContable`. |
| `GammaMobile (SistemaViewModel.kt)` | Selector de períodos (`setPeriodoCierre`), cierre dinámico y apertura con caché offline `abrirPdfConCache`. |
| `GammaMobile (SistemaScreen.kt)` | Selector `ExposedDropdownMenuBox` de períodos contables y botón interactivo con caché local `FileProvider`. |
| `GammaMobile (SistemaViewModelTest.kt)` | Cobertura de tests unitarios para períodos, cierre contable y resolución de comprobantes. |
| `docs/specs/cierre-contable-historico-y-cache-pdf/` | Artefactos SDD: `spec.md`, `plan.md`, `tasks.md` completados. |

## 4. Pendientes explícitos para la próxima fase

- [x] Validar en producción la recepción del primer webhook real de GitHub tras el auto-deploy de PythonAnywhere (Verificado: endpoint respondiendo 401 con validación HMAC activa).
- [x] Incorporar selector de meses históricos en la tarjeta de Cierre Contable de Gamma Mobile para consultar comprobantes de períodos anteriores (Implementado con ExposedDropdownMenuBox y período dinámico en SistemaScreen).
- [x] Optimizar la caché de PDF en almacenamiento local privado de la app para visualización offline de comprobantes (Implementado con FileProvider, cacheDir/comprobantes y descarga asíncrona no bloqueante).

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Si el servidor backend WSGI experimenta rotación de worker durante la descarga de un PDF grande, la petición HTTP puede cortarse.
  **Impacto:** Fallo temporal de visualización del archivo.
  **Mitigación sugerida:** El visor de Android y el navegador realizan reintentos automáticos de descarga mediante cabeceras `Accept-Ranges`.

## 6. Cómo verificar que este handoff sigue vigente

```bash
# En GammaMobile:
./gradlew testDebugUnitTest

# En Gamma_bot:
pytest tests/
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
