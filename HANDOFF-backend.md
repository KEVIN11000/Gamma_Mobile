# Traspaso Backend (Gamma Mobile -> Gamma_bot)
## Saneamiento de Tickets, Período Dinámico de Horas y Metadatos de Planilla

- **Fecha:** 28 de Octubre de 2026
- **Origen:** Gamma Mobile (Flujo de Actualización In-App y Confirmación de Asistencia en Planilla)
- **Destino:** Repositorio `Gamma_bot` (`E:\PG\vcp-s\Gamma_bot`) para desarrollo en sesión dedicada.
- **Especificaciones Detalladas:**
  - [requerimientos_gamma_bot_saneamiento_tickets.md](file:///e:/PG/vcp-s/GammaMobile/docs/specs/flujo-actualizacion-inapp/requerimientos_gamma_bot_saneamiento_tickets.md)
  - [requerimientos_backend_asistencia.md](file:///e:/PG/vcp-s/GammaMobile/docs/specs/asistencia-confirmacion-planilla/requerimientos_backend_asistencia.md)

---

### Resumen Ejecutivo para la Sesión Dedicada de `Gamma_bot`

Al iniciar la sesión en `Gamma_bot`, debes ejecutar las siguientes tareas atómicas:

#### Módulo 1: Saneamiento y Blindaje de Tickets
1. **Saneamiento de Almacén de Tickets (`src/data/tickets.json`)**:
   - Eliminar los 70 registros de prueba acumulados (`TCK-007` a `TCK-076`) generados por `pytest`.
   - Conservar estrictamente los 6 tickets reales del historial (`TCK-001` a `TCK-006`), todos en estado `Cerrado`.

2. **Blindaje de Aislamiento en Pruebas Unitarias (`tests/test_mobile_api.py`)**:
   - En la prueba `test_github_sync_resilience_and_circuit_breaker`, inyectar `monkeypatch` y `tmp_path` para apuntar `api.blueprint.TICKETS_FILE` a un archivo temporal `tmp_path / "tickets.json"`, evitando que `pytest` vuelva a escribir o mutar el archivo productivo `src/data/tickets.json`.

3. **Verificación de Telemetría Vigía y `/health` (`src/api/blueprint.py`)**:
   - Confirmar que `reportar_incidente_vigia()` maneje fallos de red hacia GitHub sin romper el flujo de creación de tickets locales ni emitir errores HTTP 500.
   - Confirmar que `download_url` en `/health` apunte al repositorio oficial `https://github.com/KEVIN11000/Gamma_Mobile/releases/latest` con versión móvil `0.0.10`.

#### Módulo 2: Corrección de Hoja Activa y Confirmación en Planilla (`AgenteAutonomoHoras`)
1. **Sincronización Dinámica de `periodo_actual.txt` (`src/logic/logic.py`)**:
   - Causa raíz de falta de escritura en planilla visible: Si `periodo_actual.txt` tenía un período desactualizado (ej. `"Mayo 2026"`), el bot escribía en esa hoja antigua dejando el mes actual (`"Octubre 2026"`) vacío a pesar de responder 200 OK.
   - En la propiedad `ws` de `AgenteAutonomoHoras`, calcular el período calendario corriente (`f"{mes_espanol} {ano}"` con `get_now_py()`) y sincronizar automáticamente `periodo_actual.txt` si difiere, asegurando que `self.wb.worksheet(...)` apunte siempre a la hoja del mes en curso.

2. **Retorno Enriquecido de Metadatos (`src/api/blueprint.py` y `src/logic/logic.py`)**:
   - En `POST /api/v1/asistencia/marcar`, retornar en el JSON:
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
   - Permitir que Gamma Mobile confirme al usuario con certeza si la marcación fue impactada en la hoja de cálculo y dispare la notificación nativa.

---

### Comandos de Validación en la Sesión de `Gamma_bot`

```bash
# 1. Ejecutar tests con aislamiento de tickets y marcación
pytest tests/test_mobile_api.py -v

# 2. Confirmar que tickets.json se mantiene intacto
git status src/data/tickets.json

# 3. Linter
flake8 src/api/blueprint.py tests/test_mobile_api.py
```
