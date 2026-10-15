# Especificación de Requerimiento (Spec)
## Módulo: Selector de Meses Históricos de Cierre Contable y Caché Local Offline de Comprobantes PDF en Gamma Mobile

- **Fecha:** 15 de Octubre de 2026
- **Versión:** 1.0.0
- **Fase:** 0 (Especificación y Arquitectura)
- **Estado:** En Revisión (Gate Obligatorio de Planificación)

---

### 1. Contexto y Justificación
Durante la fase v0.0.7 se integró exitosamente la ejecución de Cierres Contables mensuales y la emisión del comprobante PDF asociado mediante la ruta `POST /api/v1/cierres/ejecutar`.
No obstante, quedaron identificadas las siguientes oportunidades de mejora para culminar la fase:
1. **Consulta Histórica:** El usuario únicamente podía solicitar el balance del "Período Actual", careciendo de una interfaz en `SistemaScreen` para seleccionar períodos contables anteriores (ej. meses previos del año fiscal).
2. **Resiliencia Offline y Caché:** La visualización del comprobante delegaba una URL remota directamente al Intent `ACTION_VIEW`, requiriendo conectividad continua para visualizar comprobantes previamente emitidos y exponiendo la operación a interrupciones por reconexión o timeout.

Adicionalmente, se consolida la verificación empírica de producción del webhook reactivo de GitHub (`POST /api/v1/webhooks/github`) en PythonAnywhere.

---

### 2. Alcance (In-Scope)
- **Selector de Períodos Históricos:**
  - Componente visual interactivo (Dropdown o selector de chips/filtro) en la tarjeta de Cierre Contable de `SistemaScreen.kt`.
  - Opciones disponibles dinámicas o predeterminadas: "Período Actual", mes actual ("Octubre 2026") y meses históricos previos (ej. "Septiembre 2026", "Agosto 2026", "Julio 2026", etc.).
  - Persistencia del período seleccionado en `SistemaUiState.selectedPeriodoCierre`.
  - Invocación a `repository.ejecutarCierreContable(hoja = selectedPeriodo)`.

- **Caché Local Privada y Visualización Offline de Comprobantes PDF:**
  - Descarga y persistencia del archivo PDF en el almacenamiento local de caché privado de la aplicación (`context.cacheDir/comprobantes/`).
  - Verificación previa de existencia local: si el comprobante ya se descargó previamente para dicho período, se abre directamente desde el archivo local sin requerir tráfico de red adicional.
  - Exposición segura al visor nativo de Android mediante `androidx.core.content.FileProvider` (`com.gamma.mobile.fileprovider`) y `content://` URI con permisos temporales de lectura (`FLAG_GRANT_READ_URI_PERMISSION`).
  - Indicador visual / estado en `SistemaUiState` que refleja la descarga o disponibilidad local del archivo.

- **Verificación de Producción de Webhooks GitHub:**
  - Registro auditable de la validación del endpoint `https://kevin11000.pythonanywhere.com/api/v1/webhooks/github` confirmando respuesta activa con validación HMAC-SHA256:
    * Comando ejecutado: `curl.exe -s -X POST "https://kevin11000.pythonanywhere.com/api/v1/webhooks/github" -H "X-GitHub-Event: ping" -H "Content-Type: application/json" -d "{}"`
    * Respuesta obtenida: `{"error":{"code":"INVALID_SIGNATURE","message":"Firma HMAC de GitHub no coincide con el secret configurado."}}` (HTTP 401).
    * Diagnóstico: Servidor WSGI en PythonAnywhere respondiendo reactivamente con protección criptográfica activa.

---

### 3. Casos de Uso

#### CU-01: Selección y Ejecución de Cierre Contable Histórico
- **Actor:** Usuario administrativo desde Gamma Mobile.
- **Flujo Principal:**
  1. El usuario navega a la pestaña "Sistema" y visualiza la tarjeta "Cierre Contable Mensual".
  2. El usuario selecciona un período de la lista (ej. "Septiembre 2026").
  3. El usuario presiona "Consultar / Generar Cierre".
  4. La aplicación ejecuta `POST /api/v1/cierres/ejecutar` enviando `{"tipo": "contable", "hoja": "Septiembre 2026"}`.
  5. La interfaz presenta el resumen del balance y monto en Guaraníes correspondiente a dicho período.

#### CU-02: Descarga y Visualización Offline de Comprobante PDF
- **Actor:** Usuario administrativo desde Gamma Mobile.
- **Flujo Principal:**
  1. Una vez generado/consultado el cierre, el botón "Ver Comprobante PDF 📄" queda habilitado.
  2. Al pulsar el botón, la app verifica si el archivo ya reside en `cacheDir/comprobantes/cierre_contable_<periodo>.pdf`.
  3. Si no existe en caché, se descarga el archivo desde la URL absoluta del backend y se persiste localmente.
  4. La app obtiene la URI segura vía `FileProvider.getUriForFile` y lanza el Intent `ACTION_VIEW` con `type = "application/pdf"`.
  5. En subsecuentes accesos (incluso sin conexión de red), el PDF se abre instantáneamente desde el caché local.

---

### 4. Lo que queda Excluido (No-Scope)
- Creación de visor PDF nativo incrustado en Jetpack Compose (se mantiene la delegación vía Intent al visor del sistema operativo).
- Modificación en esquemas de base de datos o endpoints adicionales en backend (el endpoint `/api/v1/cierres/ejecutar` ya soporta el campo `hoja`).
- Eliminación manual de archivos PDF cacheados desde la UI (el sistema operativo Android gestiona la limpieza de `cacheDir` si el espacio es requerido).
