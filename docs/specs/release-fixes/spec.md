# Especificación de Requerimiento (Spec) — Correcciones Release v0.0.2 (TCK-001 y TCK-002)

## 1. Contexto y Problema
En el release `v0.0.2`, el usuario levantó dos tickets de soporte a través de la aplicación móvil (`TCK-001` y `TCK-002`) reportando fallas críticas de UI/UX, deserialización de datos contables y procesamiento de avisos con IA.

### Hallazgos de Causa Raíz (Root Cause Analysis):
1. **TCK-001 — Bloqueo de UI en Configuración y Botón de Recarga:**
   - En `SistemaViewModel.kt`, el método `guardarConfiguracion` asigna `configMessage = "Configuración guardada. Probando conexión..."`, pero al culminar la llamada `cargarDatosSistema()`, `configMessage` jamás se actualiza con el resultado de éxito o fallo ni se limpia, quedando visible permanentemente.
   - En `SistemaScreen.kt`, el estado `uiState.isLoading` no está vinculado a ningún indicador visual (`CircularProgressIndicator` o similar), por lo que al presionar el botón de recarga superior parece no haber respuesta más que la animación nativa del botón.
2. **TCK-001 — Datos en 0 o mal leídos en Dashboard, Deudas y Libro Diario:**
   - **Deudas (Obligaciones):** El backend `Gamma_bot` en `/api/v1/obligaciones` retorna las columnas de Google Sheets tal cual en PascalCase (`ID_Obligacion`, `Nombre`, `Monto_Inicial`, `Saldo_Actual`, `Estado`), mientras que el cliente Android espera `@SerializedName("id")`, `@SerializedName("monto_inicial")`, `@SerializedName("saldo_actual")`, etc. Por tanto, Gson deserializa todos los valores numéricos como `0` y cadenas vacías.
   - **Libro Diario (Movimientos):** En `Gamma_bot/src/api/blueprint.py` (`/api/v1/movimientos`), la clave devuelta es `"movimiento"` pero la app espera `"tipo"`. Además, los índices de columna de `Libro_Diario` estaban desalineados: `row[4]` (`Neto`) se asignó a `"categoria"` y `row[5]` (`IVA`) a `"comprobante"`, cuando `Categoría` es `row[7]` y `Comprobante` es `row[8]`.
   - **Dashboard:** La posición IVA está fijada en 0 y el balance depende de la correcta lectura de `Libro_Diario`.
3. **TCK-002 — Avisos guardados con fecha y hora actual en lugar de la del evento:**
   - En `Gamma_bot/src/api/blueprint.py` (`/api/v1/ia/interpretar/aviso`), el endpoint tiene una implementación mock/hardcoded que asigna `"fecha_evento": get_now_py().strftime("%d/%m/%Y %H:%M")`, ignorando por completo la extracción de fecha/hora por IA del texto libre.
4. **TCK-002 — Versión atascada en 0.0.1:**
   - En `app/build.gradle.kts`, `versionName` estaba hardcodeado como `"0.0.1+${getGitCommitHash()}"` en lugar de sincronizarse dinámicamente con la versión del proyecto (ej. `0.0.2` o leída de archivo `VERSION`), y en `SistemaScreen.kt` había fallback a `"0.0.1+debug"`.

---

## 2. Alcance (In-Scope)
- **Frontend (`GammaMobile`):**
  - **SistemaScreen:** Incorporar feedback visual explícito durante la carga (`isLoading`), actualizar y limpiar `configMessage` tras verificar la conexión (éxito o mensaje de error claro).
  - **Soporte de Compatibilidad Gson:** Ajustar las clases de datos (`Movimiento`, `Obligacion`) con `@SerializedName` alternativos o normalizados para que acepten tanto nombres canónicos de la API como nombres del Sheet.
  - **Versionado:** Dinamizar `versionName` en `app/build.gradle.kts` a partir de un archivo `VERSION` centralizado o propiedad de Gradle (`0.0.2`).
- **Backend (`Gamma_bot`):**
  - **Alineación de Columnas Libro Diario:** Corregir mapeo de índices y nombres de claves en `/api/v1/movimientos` para retornar `tipo`, `categoria`, `comprobante`, `neto`, `iva` y `total` reales.
  - **Contrato Normalizado de Obligaciones:** Retornar claves en snake_case estandarizadas en `/api/v1/obligaciones` (`id`, `nombre`, `monto_inicial`, `saldo_actual`, `estado`).
  - **Extracción Real de Fecha en Avisos:** Conectar `/api/v1/ia/interpretar/aviso` con el servicio de IA (`AIService` / Gemini) o parser de fecha para extraer la fecha/hora real del evento descrita por el usuario.

---

## 3. No-Scope (Exclusiones Explícitas)
- Rediseño general de la arquitectura de la app o cambio de stack.
- Modificación de credenciales en producción o esquemas de base de datos que no correspondan a las columnas documentadas en `logic/constants.py`.
- Funcionalidades no reportadas en los tickets (ej. nuevos módulos de pagos o cierres fiscales).

---

## 4. Criterios de Aceptación
1. Al pulsar "Guardar y Probar Conexión" en Sistema, la app notifica explícitamente "Conexión exitosa con el servidor" o el error exacto (ej. "Error 401: API Key inválida").
2. Al pulsar el botón de recarga superior en Sistema, se muestra un indicador visual de progreso durante el refreshing.
3. La versión instalada mostrada en la app refleja fielmente la versión actual (`0.0.2`).
4. Al ingresar un aviso por texto libre con una fecha futura (ej. "Reunión el 20 de octubre a las 15:00"), la fecha del evento agendada corresponde a la fecha futura y no a la fecha y hora de creación.
5. Las deudas (obligaciones) cargadas desde el Sheet muestran sus montos iniciales y saldos actuales reales en la app (sin ceros indebidos).
6. El Libro Diario muestra las transacciones con su tipo ("Ingreso" / "Gasto"), categoría y comprobante correctos.
