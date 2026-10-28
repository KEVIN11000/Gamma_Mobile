# Especificación de Requerimiento (Spec) — Flujo de Confirmación y Notificación de Marcación de Asistencia en Planilla

## 1. Contexto y Problema

Actualmente, al presionar "Marcar Asistencia" desde la aplicación móvil (`DashboardScreen` o `AsistenciaScreen`), el usuario experimenta incertidumbre operativa:
1. **Falta de confirmación de despacho intermedio:** La app pasa a un estado de carga genérico sin indicar la hora exacta en que la petición fue enviada al backend.
2. **Falsos positivos de asentamiento:** El servidor puede responder `HTTP 200 OK` con un mensaje genérico, pero si la hoja activa en el backend (`AgenteAutonomoHoras`) está desfasada (ej. `periodo_actual.txt` apuntando a otro mes) o hay inconsistencias en Google Sheets, la marcación no se refleja en la planilla que el usuario inspecciona.
3. **Ausencia de notificación nativa fuera de pantalla:** Si el usuario pulsa marcar y sale de la app o bloquea el teléfono, no recibe una confirmación fehaciente en la barra de estado de Android indicando que la marcación quedó formalmente registrada en la planilla de horas.

---

## 2. Alcance (In-Scope)

1. **Flujo de Marcación en Dos Etapas (Two-Stage Feedback)**:
   - **Etapa 1 (Envío Inmediato):** Al pulsar "Marcar Asistencia", la app muestra inmediatamente en el banner / estado:
     `"⏳ Marcación enviada al servidor a las [HH:mm]..."`
     garantizando trazabilidad temporal del intento de marcación.
   - **Etapa 2 (Confirmación en Planilla de Horas):**
     - Al recibir respuesta confirmada del backend:
       Cambiar el banner a estado de éxito:
       `"✅ Marcación registrada en la planilla de horas a las [HH:mm] ([Entrada/Salida])"`
     - En caso de respuestas informativas del bot (ej. día libre o registros ya completados):
       Reflejarlo con claridad:
       `"ℹ️ [Mensaje del servidor]"` (sin marcarlo como error ni como falso éxito de marcación).
     - En caso de error de red o timeout:
       `"❌ No se pudo conectar con el servidor para asentar la marcación."`
2. **Notificación Local en Barra de Estado de Android (`MarcacionNotificationHelper`)**:
   - Disparar una notificación local del sistema al completarse exitosamente la marcación:
     - **Canal:** `marcaciones_channel` ("Marcaciones de Asistencia") con prioridad por defecto.
     - **Título:** `"Marcación Registrada"`
     - **Texto:** `"Registrada en la planilla de horas a las [HH:mm] ([Entrada/Salida])"`
   - Manejo seguro de permisos en Android 13+ (`POST_NOTIFICATIONS`).
3. **Contratos Enriquecidos de Asistencia (`AsistenciaMarcaResponse`)**:
   - Incorporar campos opcionales en el DTO móvil para recibir telemetría de asentamiento si el backend la provee:
     - `planilla: String?` (nombre de la hoja, ej. `"Octubre 2026"`).
     - `fila: Int?` (número de fila en la planilla).
     - `confirmado_en_planilla: Boolean` (confirmación explícita de escritura en Google Sheets).
4. **Handoff y Especificación para `Gamma_bot`**:
   - Documentar la corrección requerida en `Gamma_bot` (`AgenteAutonomoHoras`) para auditar `periodo_actual.txt` y retornar metadatos de la hoja y fila modificadas.

---

## 3. Exclusiones Explícitas (No-Scope)

- Modificación de credenciales de Google Cloud o APIs privadas de Google Workspace.
- Marcación biométrica por geolocalización o geocercas GPS (fuera del alcance funcional actual).
- Modificación directa de código en el repositorio de `Gamma_bot` desde este workspace móvil (se entrega documento formal de requerimientos para la sesión backend dedicada).

---

## 4. Entradas y Salidas

- **Entradas**:
  - Acción del usuario: Clic en botón "Marcar Asistencia" en `DashboardScreen` o `AsistenciaScreen`.
  - Modo: `"normal"` o `"directo"`.
  - Hora del sistema local en momento de despacho (`java.time.LocalTime`).
  - Respuesta HTTP del backend: `AsistenciaMarcaResponse`.
- **Salidas**:
  - Banner dinámico en `DashboardScreen`: Transición de `⏳ Enviando...` a `✅ Registrada...` o `❌ Error...`.
  - Estado en `AsistenciaScreen`: `AsistenciaUiState.Enviando` $\rightarrow$ `AsistenciaUiState.Success`.
  - Notificación Android en la bandeja del sistema.

---

## 5. Criterios de Aceptación Verificables

1. Al presionar "Marcar Asistencia", el banner muestra de inmediato la hora de envío (`"⏳ Marcación enviada al servidor a las HH:mm..."`).
2. Al recibir el retorno de la API con hora válida, el banner cambia a `"✅ Marcación registrada en la planilla de horas a las HH:mm"`.
3. El sistema emite una notificación local de Android visible en la barra de estado con el detalle de la marcación.
4. Si la respuesta contiene rechazos o mensajes informativos (`ℹ️`, `⚠️`, `❌`), se presentan con su icono y texto adecuado sin generar falsas notificaciones de éxito.
5. Todas las pruebas unitarias pasan en verde (`.\gradlew testDebugUnitTest`).
