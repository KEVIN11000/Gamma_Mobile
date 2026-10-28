# Especificación Funcional: Integración de Analíticas MTD y Descarga Dual (Excel / PDF) de Asistencia

- **ID de Característica:** `analiticas-reportes-asistencia`
- **Fecha de Creación:** 28 de Octubre de 2026
- **Estado:** Fase 0 (Especificación y Arquitectura)
- **Autor / Orquestador:** Antigravity (Modo Planificador)
- **Entorno Objetivo:** Android 8.0+ (API 26+) / Kotlin / Jetpack Compose M3 / Retrofit / Coroutines / Hilt

---

## 1. Contexto y Justificación del Negocio

El backend `Gamma_bot` (rama `Main-stable`) certificó nuevos endpoints REST bajo `X-API-KEY` tras la estandarización del cuadro resumen oficial de asistencia y liquidación laboral:
1. `GET /api/v1/asistencia/periodos`: Estado unificado de los períodos laboral y contable, indicando si se encuentran sincronizados.
2. `GET /api/v1/asistencia/analiticas?hoja={hoja}`: Analíticas MTD de horas trabajadas acumuladas, horas perdidas (< 8h en días laborales trabajados), horas extras (> 8h), balance neto y salario estimado devengado.
3. `GET /api/v1/asistencia/reporte/excel`: Generación y descarga binaria de la planilla oficial en formato nativo Excel (`.xlsx`).
4. `GET /api/v1/asistencia/reporte/pdf`: Descarga binaria del reporte en PDF respetando la paridad del cuadro resumen.

Hasta este momento, la aplicación móvil `GammaMobile` solo permitía ejecutar el cierre y descargar reportes PDF en el tab de Cierre, sin visibilidad previa del balance horario mensual (horas perdidas vs extras) ni opción de obtener la planilla en formato editable Excel (.xlsx).

---

## 2. Alcance y Casos de Uso (Scope)

### Caso de Uso 1: Visualización de Analíticas MTD en Tab Marcaciones / Asistencia
- **Actor:** Usuario autenticado en `GammaMobile`.
- **Flujo Principal:**
  1. Al acceder al tab *"Marcaciones"* o abrir la pantalla de Asistencia, se consulta automáticamente `GET /api/v1/asistencia/periodos` y `GET /api/v1/asistencia/analiticas`.
  2. Se renderiza una tarjeta de analíticas MTD con:
     - Indicador del período activo (ej. `"Octubre 2026"`) y badge de sincronización con el período contable (verde si `sincronizados == true`, ámbar si difieren).
     - Total de horas acumuladas con formato legible (ej. `"107h 18m"`).
     - Desglose de horas extras (ej. `"+2h 30m"` en verde) y horas perdidas (ej. `"-1h 25m"` en rojo/naranja).
     - Balance neto mensual y salario acumulado devengado en guaraníes (`Gs.`).
  3. Soporte para refresco manual (*Pull-to-refresh* o botón de actualización).

### Caso de Uso 2: Descarga y Visualización de Planilla Oficial en Excel (`.xlsx`)
- **Actor:** Usuario en el tab *"Ejecutar Cierre"* o desde la tarjeta de analíticas.
- **Flujo Principal:**
  1. El usuario visualiza la opción de descargar la planilla laboral del período seleccionado en formato Excel (`.xlsx`).
  2. Al pulsar *"Descargar Planilla Excel"*, se ejecuta una llamada `@Streaming` a `GET /api/v1/asistencia/reporte/excel?hoja={hoja}&incluir_iva=true`.
  3. El archivo se almacena en el directorio de caché de la app (`cacheDir/reportes`) y se abre mediante un `Intent(Intent.ACTION_VIEW)` usando `FileProvider` con MIME `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.
  4. Si no hay aplicación instalada para abrir hojas de cálculo, se muestra un `Toast` informativo informando que el archivo fue descargado.

### Caso de Uso 3: Descarga de Reporte PDF de Horas
- **Flujo:** Integración armónica con el flujo existente de PDF, garantizando que el usuario pueda optar libremente por PDF o Excel según su necesidad.

---

## 3. Límites Explícitos (No-Scope / Exclusiones)

- **Edición en línea de celdas de Excel desde la app:** La app solo descarga y visualiza el archivo generado por el backend, no implementa un visor/editor nativo embebido de celdas.
- **Mutación de períodos desde la app:** Los períodos se consultan y sincronizan en el backend; la app no forzará la creación manual de hojas sin confirmación de cierre.
- **Modificación de tarifas horarias:** La tarifa oficial ($14,634) es administrada por la configuración del backend; la app solo consume y formatea los importes devueltos.

---

## 4. Contratos de Interfaz Visual y Estados (UX / Compose M3)

1. **Estado Carga (`Loading`):** Shimmer o indicador circular discreto mientras se cargan las analíticas sin congelar los botones de marcación.
2. **Estado Éxito (`Success`):** Tarjeta estilizada con fondo `surfaceVariant`, contrastes accesibles y badges informativos.
3. **Estado Error / Degradação Silenciosa:** Si falla la carga de analíticas (ej. sin conexión), la marcación de entrada/salida continúa disponible con un botón de reintento para las métricas.
4. **Estado Descarga (`Downloading`):** Indicador de progreso durante la transferencia de bytes `.xlsx`, con notificación al completar.
