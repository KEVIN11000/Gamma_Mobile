# Especificación de Requerimiento (Spec)
## Módulo: Receptor Webhook GitHub Issues para Sincronización Reactiva de Tickets

- **Fecha:** 15 de Octubre de 2026
- **Versión de Especificación:** 1.0.0
- **Repositorios involucrados:** `Gamma_bot` (API Backend Flask) y `GammaMobile` (Cliente de Telemetría)
- **Fase:** 0 (Especificación y Arquitectura)

---

## 1. Contexto y Justificación

En la iteración previa de soporte y telemetría, se habilitó el despacho unidireccional de tickets desde `Gamma_bot` hacia GitHub Issues (`GitHubIssueSyncService.create_github_issue` y `sync_ticket_status`) para que los incidentes reportados desde la aplicación móvil o el bot generen issues en el repositorio `KEVIN11000/Gamma_Mobile`.

Sin embargo, el ciclo de vida actualmente está incompleto:
1. **Asimetría operativa:** Si un desarrollador o administrador resuelve, cierra o reabre un issue directamente en GitHub (ya sea mediante la interfaz web de GitHub, la app GitHub Mobile o un commit con palabras clave como `closes #X`), el archivo local `tickets.json` no se entera y el ticket permanece con su estado desfasado.
2. **Dependencia de intervención manual:** Para reflejar el cierre en la app móvil, el administrador debe ejecutar un request `PATCH /api/v1/soporte/tickets/<id>` adicional en lugar de que el sistema se sincronice de forma reactiva.
3. **Ausencia de endpoint de recepción seguro:** No existe un webhook listener en el backend de `Gamma_bot` preparado para recibir y autenticar las notificaciones push de GitHub (`X-GitHub-Event: issues`).

Este requerimiento tiene como objetivo implementar el receptor de Webhooks de GitHub en `Gamma_bot`, permitiendo que eventos de cierre y reapertura de issues en GitHub actualicen automáticamente el estado de los tickets en `tickets.json` con validación criptográfica y alta tolerancia a fallos.

---

## 2. Alcance (In-Scope)

1. **Endpoint Receptor de Webhooks (`Gamma_bot`):**
   - Habilitar la ruta pública `POST /api/v1/webhooks/github` (desacoplada del middleware de API key de clientes móviles, autenticada mediante firma criptográfica de GitHub).
   - Manejar el evento `ping` de GitHub con código `200 OK` y confirmación de conectividad.
   - Manejar el evento `issues` con acciones:
     - `closed`: Cierre reactivo del ticket en `tickets.json`.
     - `reopened`: Reapertura reactiva del ticket en `tickets.json`.

2. **Seguridad y Validación Criptográfica HMAC-SHA256:**
   - Inspeccionar el encabezado `X-Hub-Signature-256` enviado por GitHub.
   - Si la variable de entorno `GITHUB_WEBHOOK_SECRET` está configurada:
     - Calcular el HMAC-SHA256 sobre el cuerpo crudo de la petición (`request.get_data()`).
     - Comparar la firma usando tiempo constante (`hmac.compare_digest`).
     - Si la firma no coincide o falta el encabezado, rechazar con `401 Unauthorized`.
   - Si `GITHUB_WEBHOOK_SECRET` no está configurado (entorno de desarrollo local sin secretos de webhook), registrar una advertencia en los logs y procesar la petición en modo permisivo.

3. **Lógica de Mapeo y Actualización en `tickets.json`:**
   - Extraer el identificador del ticket desde el título del issue mediante patrón regex estricto `r"\[(TCK-\d+)\]"`.
   - Localizar el ticket en `tickets.json`:
     - Al cerrar (`action == "closed"`):
       * Cambiar estado a `"Cerrado"`.
       * Registrar en `resolucion` el detalle automático: `"Cerrado vía GitHub Issue #<number> (motivo: <state_reason>)"` únicamente si el ticket no poseía una resolución previa.
     - Al reabrir (`action == "reopened"`):
       * Cambiar estado a `"Abierto"`.
     - Si el ticket no existe en `tickets.json`, responder con `200 OK` y estado `"ignored"` (para evitar que GitHub considere el webhook como fallido y reintente indefinidamente).

4. **Notificaciones y Auditoría Administrativa:**
   - Emitir log estructurado a nivel `INFO` registrando el evento recibido y el ticket afectado.
   - Si `CHAT_ID` y `bot_instance` están configurados, enviar notificación informativa al Telegram del administrador indicando el cambio de estado reactivo desde GitHub.

5. **Pruebas y Verificación Empírica:**
   - Suite unitaria dedicada `tests/test_github_webhook.py` que cubra:
     * Eventos de ping.
     * Cierre de issue válido y actualización de `tickets.json`.
     * Reapertura de issue y actualización de estado.
     * Validación de firmas HMAC válidas e inválidas.
     * Manejo de payloads sin formato TCK o eventos no relevantes con respuesta `200 OK` ignorada.
     * Aislamiento total de `tickets.json` durante las pruebas con `monkeypatch`/`tmp_path`.

---

## 3. Fuera de Alcance (Out-of-Scope)

- Creación automática de tickets locales cuando se abre un issue genérico en GitHub sin identificador `[TCK-XXX]`.
- Recepción de comentarios de GitHub para sincronización de mensajes de chat en tiempo real.
- Modificación en el código fuente de `GammaMobile` (este componente opera a nivel de backend Flask en `Gamma_bot`; la app móvil consume el estado actualizado a través de sus endpoints existentes `GET /api/v1/soporte/tickets`).

---

## 4. Criterios de Aceptación (Gherkin)

### Escenario 1: Recepción de ping de verificación de GitHub
- **Dado** que el servicio `Gamma_bot` está ejecutándose y recibe un POST en `/api/v1/webhooks/github` con `X-GitHub-Event: ping`,
- **Cuando** la petición es procesada,
- **Entonces** el servidor responde con HTTP 200 y JSON `{"status": "pong"}`.

### Escenario 2: Cierre reactivo de ticket desde GitHub Issue
- **Dado** un ticket existente `TCK-006` en `tickets.json` con estado `"Abierto"`,
- **Cuando** GitHub envía un webhook `issues` con `action: "closed"` y título `"[TCK-006] Despliegue y recarga de worker en PythonAnywhere"`,
- **Entonces** el ticket `TCK-006` en `tickets.json` pasa a estado `"Cerrado"` con su resolución registrada, y el endpoint retorna HTTP 200 con `{"status": "processed", "ticket_id": "TCK-006", "action": "closed"}`.

### Escenario 3: Reapertura reactiva de ticket desde GitHub Issue
- **Dado** un ticket existente `TCK-006` en `tickets.json` con estado `"Cerrado"`,
- **Cuando** GitHub envía un webhook `issues` con `action: "reopened"`,
- **Entonces** el ticket pasa a estado `"Abierto"` en `tickets.json` y el endpoint retorna HTTP 200.

### Escenario 4: Rechazo por firma HMAC inválida
- **Dado** que `GITHUB_WEBHOOK_SECRET` está configurado con valor `"super-secret"`,
- **Cuando** llega una petición con firma `X-Hub-Signature-256` incorrecta o manipulada,
- **Entonces** el endpoint rechaza la petición con HTTP 401 y código `"INVALID_SIGNATURE"`, sin modificar `tickets.json`.

### Escenario 5: Tolerancia ante issues no asociados o eventos ajenos
- **Dado** un issue en GitHub con título `"Fix typo in README"`,
- **Cuando** se recibe el webhook `issues.closed`,
- **Entonces** el endpoint identifica que no corresponde a ningún `TCK-XXX`, retorna HTTP 200 con `{"status": "ignored", "reason": "No TCK identifier found"}`, y `tickets.json` permanece inalterado.
