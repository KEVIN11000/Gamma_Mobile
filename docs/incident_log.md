# 🚨 Registro de Incidentes (Incident Log) — Gamma Mobile

Este documento registra anomalías, causas raíz, soluciones técnicas y lecciones aprendidas para garantizar la resiliencia y gobernanza del ecosistema **Gamma**.

---

## Incidente 001: Desalineación de repositorio de distribución y omisión de UI de configuración de endpoints/credenciales en cliente móvil

- **Fecha:** 14 de Octubre de 2026
- **Severidad:** Media (Bloqueo de onboarding en despliegue inicial).
- **Afectados:** Distribución de release inicial v0.0.1 y conexión de usuarios a PythonAnywhere.

### 1. Síntomas Observados
1. El comando de Telegram `/app` y el endpoint `/api/v1/health` entregaban enlaces apuntando a `https://github.com/KEVIN11000/Gamma_bot/releases`, repositorio donde no existían releases de la app móvil (el binario se había publicado en el nuevo repositorio dedicado `KEVIN11000/Gamma_Mobile`).
2. Google Play Protect advirtió y bloqueó la instalación del APK descargado en el dispositivo físico (comportamiento estándar para APKs sideloaded firmados con certificados privados no indexados en Google Play).
3. Tras completar la instalación, el usuario ingresó directamente al Dashboard pero no encontró ningún campo visual para ingresar la API Key (`X-API-KEY`) ni la URL del servidor, imposibilitando la conexión con el backend de producción.

### 2. Causa Raíz
1. **Desacoplamiento tardío de repositorios:** En etapas previas de diseño se contempló publicar el binario en el repositorio central `Gamma_bot`. Al crearse el repositorio dedicado `Gamma_Mobile`, las URLs hardcodeadas en `src/com/handlers/base.py` y `src/api/blueprint.py` no fueron refactorizadas.
2. **Hardcodeo de endpoint local:** En `NetworkModule.kt`, la URL base de Retrofit estaba configurada como `http://127.0.0.1:5000/api/v1/` (mock server local usado en las suites de pruebas). En un teléfono físico, `127.0.0.1` apunta al propio dispositivo Android (donde no existe servidor alguno), causando fallos de red silenciosos.
3. **Falta de UI para SecureStorage:** La infraestructura de almacenamiento seguro (`SecureStorageRepository` respaldado por `EncryptedSharedPreferences`) existía en el dominio y datos, pero no se había integrado una tarjeta editable en `SistemaScreen` ni un modal inicial que permitiera al usuario guardar su clave secreta.

### 3. Soluciones Implementadas
1. **En Backend Core (`Gamma_bot`):**
   * Corrección de `src/com/handlers/base.py` para devolver los enlaces oficiales a `https://github.com/KEVIN11000/Gamma_Mobile/releases` y descarga directa del APK.
   * Corrección de `download_url` en `src/api/blueprint.py`.
   * Soporte en `config.py` y `security.py` para cargar `.env` tanto desde la raíz como desde `src/`, aceptando `X_API_KEY` y `GAMMA_MOBILE_API_KEY`.
2. **En Cliente Android (`GammaMobile`):**
   * Configuración de la URL de producción por defecto: `https://kevin11000.pythonanywhere.com/api/v1/` en `SecureStorageRepositoryImpl` y `NetworkModule`.
   * Creación de `DynamicBaseUrlInterceptor` en OkHttp para permitir cambio dinámico de servidor sin recompilar la app.
   * Interfaz en [SistemaScreen.kt](file:///e:/PG/vcp-s/GammaMobile/app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt): Tarjeta **"Conexión y API Key"** con visualización de estado (`CONFIGURADA` / `PENDIENTE`), campos para Servidor y Clave (con toggle de visibilidad) y botón **"Guardar y Probar Conexión"**.
   * Actualización de [SistemaViewModel.kt](file:///e:/PG/vcp-s/GammaMobile/app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaViewModel.kt) y pruebas unitarias (100% pasando).

### 4. Lecciones Aprendidas y Reglas de Gobernanza
1. **Autonomía de configuración en clientes móviles:** Todo binario de release debe salir preconfigurado hacia el entorno de producción (`https`), pero **siempre** debe incluir una pantalla o sección de ajustes accesible para que el usuario pueda configurar/actualizar sus credenciales y host.
2. **Auditoría e2e de enlaces:** Antes de cerrar cualquier fase de despliegue, todo link generado en bots de mensajería debe ser verificado empíricamente contra el repositorio final.
3. **Manejo explícito de estado de credenciales:** Si la aplicación no tiene una API Key registrada, debe reflejar visualmente el estado "PENDIENTE" y facilitar la configuración en un solo toque.


---

## Incidente 002: Desalineación de esquemas en Libro Diario, serialización defectuosa en Obligaciones, stub en avisos IA y deficiencias de reactividad en UI de Sistema (Release v0.0.2)

- **Fecha:** 14 de Octubre de 2026
- **Severidad:** Alta (Afectación funcional de pantallas centrales: Dashboard, Diario, Deudas, Avisos y Sistema).
- **Afectados:** Release v0.0.2 (Tickets TCK-001 y TCK-002).

### 1. Síntomas Observados
1. **Configuración colgada en UI:** Al pulsar *"Guardar y Probar Conexión"* en la pantalla de Sistema, el mensaje inferior se congela indefinidamente en *"Configuración guardada. Probando conexión..."* sin confirmar conexión exitosa ni reportar errores de red.
2. **Falta de feedback de carga:** El botón de recarga superior en Sistema permanece completamente estático al pulsarse, sin animación de giro ni indicador visual de progreso mientras `isLoading` está activo.
3. **Pérdida y desalineación de datos en Libro Diario:**
   - La pantalla de Libro Diario muestra importes monetarios correspondientes al Neto y al IVA en las etiquetas destinadas a Categoría y Comprobante.
   - El filtrado por tipo ("Ingresos" o "Gastos") oculta todas las transacciones porque el campo de tipo llega vacío, y todas las filas se muestran erróneamente con estilo de egreso.
4. **Ceros en Obligaciones (Deudas) y Dashboard vacío:**
   - En la pantalla de Obligaciones, todas las deudas muestran saldos en `0 ₲`, montos iniciales en `0 ₲` y títulos vacíos.
   - En el Dashboard, los indicadores de Margen Libre y Saldo Acumulado reportan `0 ₲` o se presenta el estado vacío ("No hay datos registrados aún").
5. **Avisos IA agendados en tiempo presente:** Al dictar un aviso a la IA con una fecha futura (ej. *"25 de octubre a las 10:00"*), el aviso se almacena con la fecha y hora exacta del sistema en el instante de la solicitud (`now`), provocando notificaciones inmediatas o erróneas.
6. **Versión instalada congelada en 0.0.1:** A pesar de haber instalado el artefacto APK correspondiente al release v0.0.2, la pantalla de Sistema reporta `0.0.1+<commit>`, provocando desconfianza en el usuario sobre la actualización del software.

### 2. Causa Raíz Técnica
1. **Omisión de actualización de `configMessage` en ViewModel:** En `SistemaViewModel.kt`, `guardarConfiguracion()` asigna `configMessage = "Configuración guardada. Probando conexión..."`, pero la función asíncrona subsecuente `cargarDatosSistema()` no actualiza este campo al recibir el `NetworkResult`, dejándolo perpetuamente en estado transitorio.
2. **Ausencia de modificadores reactivos en TopAppBar:** En `SistemaScreen.kt`, el `IconButton` de refresco no está conectado con un modificador de rotación dependiente de `uiState.isLoading` ni deshabilita eventos concurrentes mientras dura la sincronización.
3. **Disparidad de contrato en `Libro_Diario` (`blueprint.py` vs `constants.py`):**
   - El esquema canónico `ENCABEZADOS_LIBRO_DIARIO` define 16 columnas: `[Fecha, Movimiento, Proveedor/Cliente, Nro Factura, Neto, IVA, Total, Categoría, Comprobante, ...]`.
   - El endpoint `GET /movimientos` en `src/api/blueprint.py` asumió un esquema de 7 columnas, leyendo `row[4]` (Neto) como categoría y `row[5]` (IVA) como comprobante, ignorando las columnas 7 y 8.
   - El endpoint `POST /movimientos` insertaba sólo 7 campos, inyectando texto (categoría y comprobante) en columnas numéricas contables (`Neto` e `IVA`).
   - La API emite la llave `"movimiento"`, mientras que el modelo Gson `Movimiento` en Android espera `@SerializedName("tipo")`. Al fallar el mapeo, `tipo` se asigna como cadena vacía `""`.
4. **Discrepancia de Naming Convention en Obligaciones (PascalCase vs snake_case):**
   - `GET /obligaciones` en `blueprint.py` devuelve directamente los registros crudos de Google Sheets cuyas llaves son los nombres de encabezado en PascalCase (`ID_Obligacion`, `Nombre`, `Monto_Inicial`, `Saldo_Actual`, `Estado`).
   - El modelo `Obligacion` en Android utiliza anotaciones `@SerializedName` estrictamente en `snake_case` (`id`, `nombre`, `monto_inicial`, `saldo_actual`, `estado`). Gson, al ser sensible a mayúsculas y minúsculas, no mapeó ningún campo, asignando ceros y cadenas vacías por defecto.
5. **Stub / Mock hardcodeado en interpretación de avisos:** El endpoint `POST /ia/interpretar/aviso` en `blueprint.py` es un stub de desarrollo no conectado a `AIService`, el cual genera estáticamente `"fecha_evento": get_now_py().strftime("%d/%m/%Y %H:%M")` sin procesar el texto agendado.
6. **Hardcodeo de `versionName` en `app/build.gradle.kts`:** La configuración de Gradle conservó la cadena literal `"0.0.1+${getGitCommitHash()}"`, de modo que `BuildConfig.VERSION_NAME` nunca reflejó la versión semántica v0.0.2.

### 3. Plan de Solución
1. **En Backend Core (`Gamma_bot`):**
   - Corregir `GET /movimientos` en `src/api/blueprint.py` para mapear los índices canónicos de `ENCABEZADOS_LIBRO_DIARIO` (col 7: Categoría, col 8: Comprobante) y proveer la clave `"tipo"` compatible con `"movimiento"`.
   - Ajustar `POST /movimientos` para poblar el esquema completo de 16 columnas con cálculos fiscales Paraguay correctos (`Neto`, `IVA`, `Monto_Gravado`, `Monto_IVA`).
   - Normalizar la respuesta de `GET /obligaciones` a formato JSON estándar con claves `snake_case`.
   - Implementar la extracción real de fechas agendadas en `POST /ia/interpretar/aviso` mediante `AIService` o parser regex de fechas contextuales en Paraguay.
2. **En Cliente Android (`GammaMobile`):**
   - En `SistemaViewModel.kt`, actualizar `cargarDatosSistema()` para reportar explícitamente *"Conexión verificada con éxito"* o detallar el error de conexión en `configMessage`.
   - En `SistemaScreen.kt`, implementar animación de rotación continua (`rememberInfiniteTransition`) en el ícono de refresco mientras `isLoading` sea `true`.
   - Actualizar `app/build.gradle.kts` a `versionName = "0.0.2+${getGitCommitHash()}"`.
   - Agregar tolerancia de nombres en los modelos de datos de Android (`Models.kt`) para blindar la deserialización ante variaciones en el casing.

### 4. Lecciones Aprendidas y Reglas de Gobernanza
1. **Prohibición de Stubs silenciosos en APIs de Producción:** Ningún endpoint expuesto en la API pública de `Gamma_bot` debe retornar marcas de tiempo simuladas mediante `get_now_py()` o datos fijos haciéndose pasar por servicios de IA operativos. Si un servicio dependiente no está listo, debe responder con código HTTP 501 / 503 explícito.
2. **Gobernanza de Contratos de Serialización (Contract-First):** Todo modelo de datos expuesto por la API debe contar con esquemas Pydantic que garanticen respuestas normalizadas en `snake_case`. Queda prohibido devolver diccionarios crudos indexados por encabezados de Google Sheets.
3. **Respeto Estricto de Esquemas Canónicos de Planillas:** Cualquier lectura o escritura en Google Sheets debe utilizar `ENCABEZADOS_*` de `constants.py` y resolver índices mediante nombres de columna (`index(header)`), nunca mediante números mágicos hardcodeados.
4. **Validación de Versión Semántica en Pipelines CI/CD:** El número de versión semántica en `build.gradle.kts` debe derivarse de una única fuente de verdad (ej. archivo `VERSION` en la raíz o tag de Git) para evitar versiones desfasadas entre el release y el binario instalado.
5. **Feedback Visual Incondicional en Acciones de Usuario:** Toda acción asíncrona disparada por el usuario (probar conexión, refrescar pantalla) debe proveer retroalimentación visual inmediata durante su ejecución y un estado final inequívoco (éxito o error).
