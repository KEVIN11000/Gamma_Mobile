# Especificación de Requerimiento (Spec)
## Módulo: Gestión, Atención y Verificación de Estado de Tickets de Soporte

- **Fecha:** 15 de Octubre de 2026
- **Versión de Especificación:** 1.0.0
- **Repositorios involucrados:** `GammaMobile` (Android Compose) y `Gamma_bot` (API Backend)
- **Fase:** 0 (Especificación y Arquitectura)

---

## 1. Contexto y Justificación

En la versión actual de Gamma Mobile (v0.0.4), la pantalla de *Sistema y Soporte* (`SistemaScreen.kt`) incluye una sección de tickets en acordeón y un diálogo para reportar nuevos incidentes vía `POST /api/v1/soporte/tickets`.
Sin embargo:
1. **Falta de ciclo de vida del ticket:** No existe ningún mecanismo ni endpoint para transicionar el estado de un ticket (`Abierto`, `En proceso`, `Cerrado`). Los tickets quedan permanentemente en estado "Abierto".
2. **Ausencia de Origen y Tipología:** Los tickets no registran ni diferencian su canal de origen (`App Móvil`, `Telegram`, `GitHub CI`, `Servidor / Vigía`), mostrándose todos de forma homogénea e impidiendo priorizar incidentes según procedencia.
3. **Deficiencias visuales:** La interfaz no provee chips de filtrado ni badges cromáticos diferenciados (ej. verde para resueltos, naranja para abiertos, azul para en proceso, y distintivo de origen), dificultando saber rápidamente qué problemas siguen pendientes y de dónde surgieron.
4. **Contaminación de datos de prueba:** Las ejecuciones de `pytest tests/test_mobile_api.py` en el backend no aíslan el archivo `src/data/tickets.json`, generando 22 tickets sintéticos repetitivos ("Fallo sincronización") que saturan la vista de la app móvil en lugar de reflejar los incidentes reales del ecosistema.

Este requerimiento tiene como objetivo permitir la atención efectiva de tickets, auditar su origen y estado real en la app y consolidar una experiencia de soporte robusta.

---

## 2. Alcance (In-Scope)

1. **Saneamiento e Inoculación en Backend (`Gamma_bot`):**
   - Aislar la suite de pruebas unitarias (`test_mobile_api.py`) utilizando `tmp_path` o `monkeypatch` para evitar que `pytest` altere el archivo `tickets.json` de producción/desarrollo.
   - Reemplazar la lista sintética de tickets por los registros reales de incidentes del ecosistema (Incidentes 001, 002 y pendientes actuales de desarrollo) con sus estados (`Cerrado`, `En proceso`, `Abierto`) y orígenes correspondientes (`app`, `telegram`, `ci`, `servidor`).
2. **Contrato de API para Origen y Atención de Tickets (`Gamma_bot`):**
   - Incorporar el campo `origen` (`app`, `telegram`, `ci`, `servidor`) con valor por defecto `"app"` en el alta y lectura de tickets.
   - Implementar el endpoint `PATCH /api/v1/soporte/tickets/<ticket_id>` para permitir cambiar el `estado` (`Abierto`, `En proceso`, `Cerrado`) y registrar opcionalmente una nota de `resolucion`.
   - Validar entradas con esquemas Pydantic (`TicketUpdate`).
   - Emitir log y actualizar el archivo `tickets.json` de forma atómica y segura.
3. **Capa de Dominio y Datos en Cliente Móvil (`GammaMobile`):**
   - Extender `TicketItem` con el campo `origen: String = "app"` (y soporte `@SerializedName("origen")`).
   - Extender `GammaApi.kt` y `SistemaRepository` para soportar la actualización de tickets (`updateTicketStatus`).
   - Modelar `TicketUpdate` y soporte de campos en `TicketItem`.
4. **Lógica de Negocio y Presentación (`GammaMobile`):**
   - Incorporar en `SistemaViewModel` la función `actualizarEstadoTicket()` y el estado reactivo de filtro de tickets (`filtroEstadoTicket`: "Todos", "Abierto", "En proceso", "Cerrado").
   - Actualizar `SistemaScreen.kt`:
     * Barra de chips de filtro de tickets en el encabezado del acordeón.
     * Badges de colores semánticos por estado (`Abierto`: Ámbar/Warning, `En proceso`: Azul/Info, `Cerrado`: Verde/Success).
     * Distintivo visual del origen del ticket (`📱 App`, `✈️ Telegram`, `🤖 CI/CD`, `🚨 Servidor`).
     * Modal interactivo "Atender Ticket" al tocar una tarjeta de ticket, permitiendo cambiar su estado y consultar la resolución o detalle.
5. **Verificación Empírica:**
   - Pruebas unitarias de backend (`pytest tests/test_mobile_api.py`).
   - Pruebas unitarias en Android (`./gradlew testDebugUnitTest`).
   - Compilación completa de la app (`./gradlew assembleDebug`).

---

## 3. Fuera de Alcance (Out-of-Scope)

- Sincronización bidireccional automática mediante webhooks push de GitHub hacia PythonAnywhere (los webhooks requieren endpoints públicos con secret de firma y configuración en GitHub Settings, lo cual queda para una fase posterior de infraestructura).
- Eliminación física de tickets en la base de datos (por trazabilidad y gobernanza de incidentes, los tickets cerrados se preservan).
- Soporte para adjuntar archivos binarios (imágenes/capturas) dentro del ticket en esta iteración.

---

## 4. Criterios de Aceptación (Gherkin)

```gherkin
Escenario: Filtrado de tickets por estado en la app
  Dado que el usuario abre la pantalla de Sistema y Soporte
  Y expande el acordeón de Tickets de Soporte
  Cuando presiona el chip "Abiertos"
  Entonces solo se visualizan los tickets con estado "Abierto"
  Y el contador de tickets refleja la cantidad correspondiente.

Escenario: Atención y cambio de estado de un ticket desde la app
  Dado que el usuario visualiza un ticket con estado "Abierto"
  Cuando pulsa sobre la tarjeta del ticket y selecciona el nuevo estado "Cerrado"
  Y confirma la acción
  Entonces la app envía una petición PATCH al backend
  Y el ticket se actualiza inmediatamente a estado "Cerrado" con badge verde.

Escenario: Aislamiento de tests en el backend
  Dado que se ejecuta "pytest tests/test_mobile_api.py" en Gamma_bot
  Cuando finalizan los tests
  Entonces el archivo de datos "src/data/tickets.json" no contiene nuevos registros sintéticos generados por la prueba.
```
