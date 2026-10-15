# Checklist de Tareas Atómicas de Implementación (Tasks)
## Módulo: Receptor Webhook GitHub Issues para Sincronización Reactiva de Tickets

- **Fecha:** 15 de Octubre de 2026
- **Versión de Tareas:** 1.0.0
- **Fase:** 0 (Especificación y Arquitectura)

---

### Tareas Atómicas de Construcción

- [x] **Tarea 1: Implementar `GitHubWebhookService` y validador HMAC en `Gamma_bot`**
  - **Archivos:** `Gamma_bot/src/services/github_service.py`
  - **Acción:** 
    - Implementar métodos de clase `verify_signature(payload_bytes, signature_header, secret)` usando `hmac` y `hashlib.sha256`.
    - Implementar `extract_ticket_id(title: str) -> str | None` con regex `r"\[(TCK-\d+)\]"`.
    - Implementar `process_issue_event(action: str, issue_data: dict, tickets_file_path: Path) -> dict[str, Any]` para actualizar `tickets.json` con la transición de estado (`closed` -> `"Cerrado"`, `reopened` -> `"Abierto"`) y nota de resolución.
  - **Criterio de Aceptación:** Métodos unitariamente testeables con retorno predecible ante tickets existentes, inexistentes o títulos sin formato TCK.

- [x] **Tarea 2: Implementar endpoint `POST /api/v1/webhooks/github` en `Gamma_bot`**
  - **Archivos:** `Gamma_bot/src/api/blueprint.py`
  - **Acción:**
    - Declarar la ruta `@mobile_api.route("/webhooks/github", methods=["POST"])` sin el decorador `require_api_key`.
    - Validar encabezado `X-Hub-Signature-256` si `GITHUB_WEBHOOK_SECRET` está presente en el entorno; si es inválido retornar `401 Unauthorized`.
    - Parsear el encabezado `X-GitHub-Event`:
      * Si es `ping`: retornar `200 OK` con `{"status": "pong"}`.
      * Si es `issues`: delegar en `GitHubWebhookService.process_issue_event` y retornar `200 OK` con el resultado del procesamiento.
      * Si es otro evento: retornar `200 OK` con `{"status": "ignored"}`.
    - Notificar opcionalmente al Telegram del administrador en caso de actualización exitosa.
  - **Criterio de Aceptación:** Endpoint responde a peticiones simuladas en Flask test client devolviendo los códigos de estado y esquemas JSON especificados en `plan.md`.

- [x] **Tarea 3: Crear suite de pruebas exhaustiva `tests/test_github_webhook.py`**
  - **Archivos:** `Gamma_bot/tests/test_github_webhook.py`
  - **Acción:**
    - Crear suite con `pytest` y fixtures aislados con `tmp_path` para `tickets.json`.
    - Testear:
      1. Ping de GitHub (`X-GitHub-Event: ping`).
      2. Cierre de issue válido (`action: closed`) actualizando ticket a `"Cerrado"`.
      3. Reapertura de issue válido (`action: reopened`) actualizando ticket a `"Abierto"`.
      4. Validación de firma HMAC correcta e incorrecta (código 401).
      5. Issue sin formato TCK (código 200 con status `"ignored"`).
      6. Evento desconocido o no implementado (código 200 con status `"ignored"`).
  - **Criterio de Aceptación:** 100% de los nuevos tests pasando en `pytest tests/test_github_webhook.py` sin alterar el archivo real `src/data/tickets.json`.

- [x] **Tarea 4: Revalidación integral end-to-end y auditoría de higiene**
  - **Archivos:** Todo el proyecto (`Gamma_bot` y `GammaMobile`)
  - **Acción:**
    - Ejecutar suite completa `pytest tests/` en `Gamma_bot`.
    - Ejecutar `./gradlew testDebugUnitTest` en `GammaMobile`.
    - Ejecutar `git status` para confirmar ausencia de archivos residuales o efímeros.
  - **Criterio de Aceptación:** Todas las suites de pruebas en verde y árbol de trabajo limpio.
