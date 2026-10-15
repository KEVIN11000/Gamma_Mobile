# Especificación de Requerimiento (Spec)
## Módulo: Visualización de Comprobante PDF de Cierre Contable Mensual en Gamma Mobile

- **Fecha:** 15 de Octubre de 2026
- **Versión de Especificación:** 1.0.0
- **Repositorios involucrados:** `GammaMobile` (Android Jetpack Compose) y `Gamma_bot` (API Backend)
- **Fase:** 0 (Especificación y Arquitectura)

---

## 1. Contexto y Justificación

El backend de `Gamma_bot` cuenta con el endpoint `POST /api/v1/cierres/ejecutar` (`tipo = "contable"`), el cual genera el balance del Libro Diario y crea un comprobante PDF oficial en el directorio de reportes, retornando la URL relativa de descarga (`/api/v1/cierres/reportes/<filename>`). Asimismo, `GammaApi.kt` en el cliente Android ya posee las firmas de `ejecutarCierre` y `descargarArchivoPdf`.

Sin embargo, en la pantalla de *Sistema y Soporte* (`SistemaScreen.kt`) de `GammaMobile`:
1. **Ausencia de control de cierre contable:** No existe ninguna tarjeta ni interfaz que permita al usuario solicitar la generación o consulta del cierre contable mensual.
2. **Imposibilidad de auditar comprobantes:** El usuario no puede visualizar ni descargar el comprobante en formato PDF desde la app móvil tras realizar las operaciones contables del mes, obligándolo a recurrir a la consola del bot en Telegram.

Este requerimiento tiene como objetivo incorporar en la pantalla de Sistema una tarjeta interactiva de **Cierre Contable Mensual**, permitiendo ejecutar el corte contable del período y abrir directamente el comprobante PDF generado en el visor del dispositivo.

---

## 2. Alcance (In-Scope)

1. **Capa de Dominio y Repositorio (`GammaMobile`):**
   - Extender `SistemaRepository` y `SistemaRepositoryImpl` con el método:
     `suspend fun ejecutarCierreContable(hoja: String = "Período Actual"): NetworkResult<CierreResponse>`
   - Reutilizar el modelo existente `CierreResponse` (`resumen`, `pdfUrl`, `totalMonto`, `tipo`).

2. **Capa de Presentación y ViewModel (`GammaMobile`):**
   - Extender `SistemaUiState` con:
     * `cierreContable: CierreResponse? = null`
     * `isEjecutandoCierre: Boolean = false`
     * `cierreMensaje: String? = null`
   - Incorporar en `SistemaViewModel`:
     * `fun ejecutarCierreContable(hoja: String = "Período Actual")`
     * Helper para construir la URL canónica absoluta del PDF (`baseUrl` + `pdfUrl`).

3. **Interfaz de Usuario Jetpack Compose (`SistemaScreen.kt`):**
   - Agregar una nueva tarjeta ("Card: Cierre Contable Mensual"):
     * Encabezado con icono semántico y título de sección.
     * Botón de acción para "Ejecutar / Generar Cierre Contable".
     * Panel de resultado informativo al completarse el cierre:
       - Resumen del período.
       - Monto total liquidado formateado en Guaraníes (₲).
       - Botón destacado "Ver Comprobante PDF 📄".
     * Al presionar "Ver Comprobante PDF", despachar un `Intent(Intent.ACTION_VIEW, Uri.parse(fullPdfUrl))` para abrir el documento con el visor nativo del dispositivo (Drive Viewer, Acrobat, navegador, etc.).

4. **Verificación Empírica:**
   - Pruebas unitarias de ViewModel / Repositorio con Gradle.
   - Compilación completa sin advertencias: `./gradlew testDebugUnitTest`.

---

## 3. Fuera de Alcance (Out-of-Scope)

- Renderizado de PDF mediante motor canvas binario incrustado dentro de la vista Compose (se prioriza el Intent estándar del sistema para máxima ligereza y compatibilidad universal en Android).
- Eliminación o edición de cierres contables históricos desde la app móvil.

---

## 4. Criterios de Aceptación (Gherkin)

### Escenario 1: Generación exitosa de cierre contable
- **Dado** que el usuario se encuentra en la pantalla de Sistema con conexión activa al backend,
- **Cuando** presiona el botón "Ejecutar Cierre Contable",
- **Entonces** el sistema realiza `POST /api/v1/cierres/ejecutar`, muestra el indicador de carga y actualiza el panel con el resumen y monto total del período.

### Escenario 2: Visualización del comprobante PDF
- **Dado** que un cierre contable fue generado y posee una URL de PDF válida,
- **Cuando** el usuario presiona "Ver Comprobante PDF",
- **Entonces** la app lanza el visor de documentos de Android con la URL canónica absoluta del comprobante.
