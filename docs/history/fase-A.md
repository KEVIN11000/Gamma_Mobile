# HANDOFF — Gamma Mobile

**Fase cerrada:** Fase A — Android Finanzas y Deudas
**Fecha de cierre:** 2026-10-07
**Estado del build/tests:** ✅ pasando — Todos los unit tests de los módulos de Finanzas y Deudas pasan con éxito (`./gradlew testDebugUnitTest assembleDebug`).

---

## 1. Estado actual
- [Capa de Red (M-03) completada, manejando 401 y offline fallbacks].
- [Capa de Datos Local (M-04, M-05, M-06) integrada con Room y Hilt].
- [Dashboard (M-07, M-08) implementado, incluyendo formateo de guaraníes y filtrado estricto de eventos próximos (7 días)].
- [Libro Diario (M-09, M-10) funcional, con pull-to-refresh y cálculo dinámico de IVA (10%, 5%, Exento)].
- [WorkManager Offline Queue (M-11) operativa con backoff exponencial para registrar movimientos sin red].
- [Obligaciones y Simulador (M-12, M-13) finalizados, permitiendo abonos, cambio de estado y cálculo de semáforo de riesgo contra el margen libre].
- [QA-A manual pendiente de aprobación formal en terminal/emulador por parte del usuario].

## 2. Decisiones tomadas en esta fase
- **Decisión:** Usar `RobolectricTestRunner` para testear `WorkManager` e inyectar dependencias (como `Context` de la app).
  **Por qué:** Para evitar refactorizaciones pesadas y poder testear integraciones híbridas de Coroutines/WorkManager.
- **Decisión:** Integrar `SyncMovimientosWorker` en la capa de persistencia mediante inyección optimista.
  **Por qué:** Mejora notablemente la experiencia de usuario: los movimientos se insertan con estado "PENDING" localmente primero, se actualiza la UI inmediatamente y `WorkManager` retenta en background sin bloquear al usuario.
- **Decisión:** Cálculos de fecha basados en inyección de `Clock` estático para tests.
  **Por qué:** Evita flaky tests al cambiar de día, garantizando que el "Hoy" y "Mañana" sean deterministas.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `docs/specs/gamma-mobile/tasks.md` | Actualizado y marcadas todas las tareas M-07 a M-13 como finalizadas. |
| `app/build.gradle.kts` | Agregadas dependencias de WorkManager, Robolectric y test-core. |
| `.../presentation/dashboard/AvisoUiMapper.kt` | Lógica pura para clasificar Avisos en (Hoy, Mañana, En X días) usando inyección de reloj. |
| `.../data/worker/SyncMovimientosWorker.kt` | Orquestador de la cola offline, consumiendo Room y llamando a Retrofit con backoff. |
| `.../presentation/obligaciones/ObligacionesViewModel.kt` | Controla los abonos optimistas, actualización de estado (Simulado->Activo) y el Simulador de Cuotas. |

## 4. Pendientes explícitos para la próxima fase
- [ ] Ejecutar el check manual (QA-A) en el emulador conectándose al `mock_server` si no se ha validado empíricamente.
- [ ] Realizar un commit formal en Git para congelar los cambios (fuga cero / higiene pre-commit).
- [ ] Iniciar la **Fase B — Avisos, IA y OCR**.
- [ ] Implementar M-14 (Servicio de polling de Avisos o WorkManager).

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Múltiples WorkManagers encolados.
  **Impacto:** Si la API devuelve un error semántico (no transitorio), el worker podría quedar reintentando eternamente.
  **Mitigación sugerida:** Implementar límite de reintentos en `SyncMovimientosWorker` en futuras fases y notificar a la UI si la deuda es irrecuperable.

## 6. Cómo verificar que este handoff sigue vigente
```bash
# Verifica que el build y los tests pasan
./gradlew testDebugUnitTest assembleDebug
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
