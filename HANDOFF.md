# HANDOFF — Gamma Mobile

**Fase cerrada:** Fase B — Avisos, IA y OCR
**Fecha de cierre:** 2026-10-08
**Estado del build/tests:** ✅ pasando — Todos los unit tests (incluyendo los mocks de OCR e IA) y el ensamblado de la app pasan sin problemas (`./gradlew testDebugUnitTest assembleDebug`).

---

## 1. Estado actual
- Los endpoints de IA y OCR han sido implementados usando Retrofit (`GammaApi`) y los repositorios, con un timeout protegido de 45 segundos y reintentos automáticos.
- La pantalla `AvisosScreen` ahora permite creación por texto libre con IA, borrado y visualización de detalles (vía un `AlertDialog` implementado en `AvisoItem`).
- El servicio en segundo plano `AlarmaAvisosWorker` notifica con éxito sobre avisos próximos en Asunción (hitos: 30, 7, 5, 3 y 1 días) y fue validado con tests unitarios bajo un reloj y zona horaria fijos usando Robolectric.
- La pantalla `NuevaTransaccionScreen` incorpora carga manual o IA (texto libre), además de un nuevo flujo "Foto" que habilita el uso de la Cámara o Galería usando `FileProvider` y `ActivityResultContracts`, enviando los bytes capturados al endpoint de OCR.

## 2. Decisiones tomadas en esta fase
- **Decisión:** Emulación del backend en Android Físico.
  **Por qué:** Para el Gate QA-B en dispositivo físico, se modificó el script python `mock_app.py` integrando latencia artificial simulada (2 seg) de IA/OCR. Se recomienda usar `adb reverse tcp:5000 tcp:5000` en lugar de alterar hardcoded URLs.
- **Decisión:** Postergar el rediseño de UI/UX (Quick Wins).
  **Por qué:** Tras recibir un detallado análisis de UX/UI mediante un subagente de IA, el reporte fue anexado formalmente a `docs/ui_ux_report.md`. Se decidió no alterar los flujos actuales para cerrar la Fase B limpia y evaluar estas mejoras en una etapa posterior de refinamiento.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `app/src/main/java/com/gamma/mobile/data/remote/GammaApi.kt` | Se agregaron los POST multipart para `movimientos/ocr` e `ia/interpretar`. |
| `app/src/main/java/com/gamma/mobile/worker/AlarmaAvisosWorker.kt` | Lógica central del job diario para notificaciones push de eventos futuros. |
| `app/src/main/java/com/gamma/mobile/presentation/transacciones/NuevaTransaccionScreen.kt` | Se agregaron pestañas ("IA", "Foto", "Manual") y los launcher nativos para interactuar con cámara y galería. |
| `app/src/main/java/com/gamma/mobile/presentation/avisos/AvisosScreen.kt` | Se arreglaron problemas de codificación de texto y se añadió el diálogo de detalle por tarjeta de aviso. |
| `docs/specs/gamma-mobile/tasks.md` | Marcadas todas las tareas M-14 a M-17 como completadas. |
| `docs/ui_ux_report.md` | Nuevo archivo conteniendo sugerencias de accesibilidad y "quick wins" de M3. |

## 4. Pendientes explícitos para la próxima fase (Fase C)
- [ ] **M-18** Pestaña Asistencia: marcar normal/directo y materia. *Aceptación:* tests de ViewModel.
- [ ] **M-19** Cierre mensual con descuento y visor/descarga de PDF. *Aceptación:* tests de flujo y descarga.
- [ ] **M-20** Pestaña Sistema: tickets, salud del backend, versión `0.0.1+<commit>`, logs. *Aceptación:* tests; versión coincide con `BuildConfig`.
- [ ] Opcional: Implementar los "Quick Wins" de UX descritos en `docs/ui_ux_report.md` (ej: `KeyboardType.Number`, ModalBottomSheet en Deudas).

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Limitación de API de Avisos.
  **Impacto:** El backend mock/real actual no soporta la edición (PUT/PATCH) de un Aviso, obligando a usar un workaround (borrar y crear) o solo visualización.
  **Mitigación sugerida:** Expandir el contrato OpenAPI en la Fase C si el usuario lo considera necesario.
- **Riesgo:** Antipatrones de Diseño y Accesibilidad.
  **Impacto:** Si no se aplican los arreglos propuestos en el reporte UX (teclado numérico, Dashboard scrolleable), la app será problemática en móviles de menor tamaño.

## 6. Cómo verificar que este handoff sigue vigente
Comando para confirmar que el estado descrito arriba sigue siendo cierto:

```bash
./gradlew testDebugUnitTest assembleDebug
```
