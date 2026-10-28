# Implementation Plan: Flujo de Confirmación y Notificación de Marcación de Asistencia en Planilla

**Branch**: `main` (staging local) | **Date**: 2026-10-27 | **Spec**: [spec.md](file:///e:/PG/vcp-s/GammaMobile/docs/specs/asistencia-confirmacion-planilla/spec.md)

---

## 1. Resumen Ejecutivo

Implementar un flujo de retroalimentación en dos fases para las marcaciones de asistencia en Android:
1. Despacho inmediato informando la hora de envío al servidor (`"⏳ Marcación enviada al servidor a las HH:mm..."`).
2. Transición del banner y emisión de notificación local en la bandeja del sistema Android al confirmar que la marcación quedó registrada en la planilla de horas (`"✅ Marcación registrada en la planilla de horas a las HH:mm (Entrada/Salida)"`).
3. Enriquecimiento del modelo de datos `AsistenciaMarcaResponse` con metadatos de planilla (`planilla`, `fila`, `confirmado_en_planilla`).

---

## 2. Contratos y Arquitectura de Datos

### 2.1. Modelo `AsistenciaMarcaResponse` (Domain / Data)

```kotlin
data class AsistenciaMarcaResponse(
    @SerializedName("tipo") val tipo: String = "",
    @SerializedName("hora") val hora: String = "",
    @SerializedName("mensaje") val mensaje: String = "",
    @SerializedName("planilla") val planilla: String? = null,
    @SerializedName("fila") val fila: Int? = null,
    @SerializedName("confirmado_en_planilla") val confirmadoEnPlanilla: Boolean = false
)
```

### 2.2. Helper de Notificación Nativa (`MarcacionNotificationHelper`)

- **Ubicación:** `app/src/main/java/com/gamma/mobile/utils/MarcacionNotificationHelper.kt`
- **Canal:** `marcaciones_channel` (Nombre: "Marcaciones de Asistencia", Importancia: `IMPORTANCE_DEFAULT`).
- **Comportamiento:**
  - Crea el canal si no existe.
  - Verifica permiso `POST_NOTIFICATIONS` en Android 13+ (`Build.VERSION_CODES.TIRAMISU`).
  - Construye notificación con ícono `R.drawable.ic_check` o ícono de sistema `android.R.drawable.ic_dialog_info`.
  - Abre la aplicación (`MainActivity`) al pulsar sobre la notificación con flags inmutables.

### 2.3. Lógica de Evaluación (`MarcacionResultado.kt`)

Extender la evaluación de respuesta:
- Distinguir mensajes informativos (`ℹ️` o día libre) para mostrarlos con icono informativo neutral sin tildarlos de error ni de falsa marcación.
- Formatear el mensaje de éxito explícito: `"✅ Marcación registrada en la planilla de horas a las $hora ($tipoTexto)"`.

---

## 3. Modificaciones en Capa de Presentación

### 3.1. `DashboardViewModel.kt`
- Al iniciar `marcarAsistencia(modo)`:
  - Formatear hora local actual (`HH:mm`).
  - Asignar `_asistenciaMessage.value = "⏳ Marcación enviada al servidor a las $horaActual..."`.
  - Asignar `_asistenciaIsError.value = false`.
- Al completar la petición de red:
  - Si es éxito con hora confirmada:
    - Asignar mensaje de confirmación en planilla.
    - Notificar a través de `MarcacionNotificationHelper` (recibiendo `Context` opcional o inyectando `NotificationManager`).
  - Si es error / timeout:
    - Mostrar mensaje de contingencia claro: `"❌ No se pudo confirmar la marcación con el servidor. Reintente."`

### 3.2. `AsistenciaViewModel.kt`
- Exponer nuevo estado `AsistenciaUiState.Enviando(horaEnvio: String)` para reflejar visualmente la etapa de despacho en la pestaña de asistencia.
- Transicionar a `AsistenciaUiState.Success` con detalle enriquecido de planilla.

---

## 4. Requerimientos para Backend (`Gamma_bot`)

En el archivo complementario `requerimientos_backend_asistencia.md`:
1. Auditar en `AgenteAutonomoHoras` el archivo `periodo_actual.txt` para asegurar que las marcas se asienten en la pestaña del mes vigente (ej. `"Octubre 2026"`).
2. Devolver en `POST /api/v1/asistencia/marcar` los campos `planilla`, `fila` y `confirmado_en_planilla`.

---

## 5. Plan de Pruebas y Validación

1. **Pruebas Unitarias de ViewModel**:
   - `DashboardViewModelTest`: Verificar transición de estado (envío inmediato $\rightarrow$ confirmación en planilla).
   - `AsistenciaViewModelTest`: Validar transiciones a `Enviando` y `Success`.
2. **Pruebas de Helper**:
   - Validar construcción de payloads de notificación sin crashes.
3. **Validación Integral**:
   - `.\gradlew testDebugUnitTest` 100% pasando.
   - `.\gradlew assembleDebug` compila exitosamente.
