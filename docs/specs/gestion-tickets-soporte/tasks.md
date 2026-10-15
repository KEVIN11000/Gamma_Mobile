# Tareas de Implementación (Tasks)
## Módulo: Gestión, Atención y Verificación de Estado de Tickets de Soporte

- **Fecha:** 15 de Octubre de 2026
- **Versión:** 1.0.0
- **Documentos Asociados:** [spec.md](file:///e:/PG/vcp-s/GammaMobile/docs/specs/gestion-tickets-soporte/spec.md) | [plan.md](file:///e:/PG/vcp-s/GammaMobile/docs/specs/gestion-tickets-soporte/plan.md)

---

## Checklist de Tareas Atómicas y Secuenciales

- [x] **Tarea 1: Saneamiento y Aislamiento de Tests en Backend (`Gamma_bot`)**
  - **Archivos:** `tests/test_mobile_api.py`, `src/data/tickets.json`
  - **Acción:** Aislar la prueba de tickets en `test_mobile_api.py` mediante monkeypatch de `TICKETS_FILE` a un directorio temporal (`tmp_path`) para que no altere los datos del sistema. Limpiar `src/data/tickets.json` cargando los tickets reales del proyecto (TCK-001 a TCK-006) con sus estados (`Cerrado`, `En proceso`, `Abierto`) y orígenes (`app`, `telegram`, `ci`, `servidor`) reales correspondientes.
  - **Criterio de Verificación:** `pytest tests/test_mobile_api.py` corre exitosamente y `src/data/tickets.json` no sufre modificaciones residuales.

- [x] **Tarea 2: Soporte de `origen` y Endpoint `PATCH /api/v1/soporte/tickets/<ticket_id>` en Backend (`Gamma_bot`)**
  - **Archivos:** `src/api/blueprint.py`, `tests/test_mobile_api.py`
  - **Acción:** Incluir el campo `origen` ("app", "telegram", "ci", "servidor") con default `"app"` en el alta y consulta de tickets. Crear el endpoint `PATCH /soporte/tickets/<ticket_id>` con validación de estado (`Abierto`, `En proceso`, `Cerrado`), persistencia atómica en `TICKETS_FILE` y manejo de 404/400. Agregar pruebas unitarias en `test_mobile_api.py`.
  - **Criterio de Verificación:** `pytest tests/test_mobile_api.py` pasa al 100% cubriendo el nuevo endpoint de actualización de estado y la presencia de `origen`.

- [x] **Tarea 3: Contratos y Capa de Datos en Android (`GammaMobile`)**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/domain/model/SistemaModels.kt`, `app/src/main/java/com/gamma/mobile/data/remote/GammaApi.kt`, `app/src/main/java/com/gamma/mobile/domain/repository/SistemaRepository.kt`, `app/src/main/java/com/gamma/mobile/data/repository/SistemaRepositoryImpl.kt`
  - **Acción:** Añadir campo `origen: String = "app"` en `TicketItem`. Añadir `TicketUpdate` y campo `resolucion` en `TicketItem`. Declarar `PATCH("soporte/tickets/{id}")` en `GammaApi` e implementarlo en `SistemaRepositoryImpl`.
  - **Criterio de Verificación:** `./gradlew compileDebugKotlin` compila sin errores de tipos o contratos.

- [x] **Tarea 4: Lógica de Negocio y Filtrado en `SistemaViewModel` (`GammaMobile`)**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaViewModel.kt`, `app/src/test/java/com/gamma/mobile/presentation/sistema/SistemaViewModelTest.kt`
  - **Acción:** Implementar `actualizarEstadoTicket(ticketId, nuevoEstado, resolucion)` y `setFiltroEstadoTicket(filtro)` en el ViewModel, con estado reactivo para filtrar la lista visible de tickets. Actualizar tests unitarios.
  - **Criterio de Verificación:** `./gradlew testDebugUnitTest --tests "*SistemaViewModelTest*"` pasa 100% en verde.

- [x] **Tarea 5: Interfaz de Usuario, Origen y Modal de Atención en `SistemaScreen` (`GammaMobile`)**
  - **Archivos:** `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt`
  - **Acción:** Integrar barra de chips de filtro (`Todos`, `Abiertos`, `En proceso`, `Cerrados`), badges visuales con colores semánticos (`Cerrado` en verde, `En proceso` en azul, `Abierto` en ámbar), icono/badge del origen del ticket (`📱 App`, `✈️ Telegram`, `🤖 CI/CD`, `🚨 Servidor`), y diálogo interactivo "Atender Ticket" para cambiar su estado y registrar resolución opcional.
  - **Criterio de Verificación:** La pantalla renderiza correctamente los orígenes y estados, permite abrir el modal, seleccionar nuevo estado y actualizar la lista en tiempo real.

- [x] **Tarea 6: Verificación Global End-to-End y Regla de Calidad**
  - **Archivos:** Todos los involucrados
  - **Acción:** Ejecutar la suite completa de pruebas unitarias y generación de APK (`./gradlew testDebugUnitTest assembleDebug` en GammaMobile y `pytest tests/test_mobile_api.py` en Gamma_bot). Auditar `git status` para asegurar cero archivos residuales.
  - **Criterio de Verificación:** Builds exitosos, 0 regresiones y árbol git limpio.
