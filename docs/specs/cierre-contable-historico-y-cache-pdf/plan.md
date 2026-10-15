# Plan de Arquitectura y Contratos Técnicos (Plan)
## Módulo: Selector de Meses Históricos de Cierre Contable y Caché Local Offline de Comprobantes PDF en Gamma Mobile

- **Fecha:** 15 de Octubre de 2026
- **Versión:** 1.0.0
- **Fase:** 0 (Especificación y Arquitectura)
- **Estado:** En Revisión (Gate Obligatorio de Planificación)

---

### 1. Arquitectura Modular

```
Gamma Mobile (Presentation)
  └── SistemaScreen.kt
        ├── DropdownMenu / FilterChips (Período: "Período Actual", "Octubre 2026", "Septiembre 2026", ...)
        └── Card Cierre Contable (Visualización de balance, botón Ver PDF con soporte local/offline)
              │
              ▼
Gamma Mobile (ViewModel / State)
  └── SistemaViewModel.kt
        ├── selectedPeriodoCierre: StateFlow<String>
        ├── periodosDisponibles: List<String>
        ├── ejecutarCierreContable(hoja: String)
        └── abrirPdfConCache(context: Context, pdfUrl: String, periodo: String)
              │
              ▼
Gamma Mobile (Storage / Cache Helper)
  └── PdfCacheManager.kt (o método interno en ViewModel/Storage)
        ├── File(context.cacheDir, "comprobantes/cierre_contable_${sanitizedPeriod}.pdf")
        ├── Descarga con OkHttpClient (o HttpURLConnection) en Dispatchers.IO
        └── FileProvider.getUriForFile("com.gamma.mobile.fileprovider", ...)
              │
              ▼
Gamma Backend API (`Gamma_bot`)
  └── POST /api/v1/cierres/ejecutar
        └── Payload: {"tipo": "contable", "hoja": "<periodo>"}
        └── Response: {"resumen": "...", "pdf_url": "...", "total_monto": 4500000, "tipo": "contable"}
```

---

### 2. Contratos de Datos y Estado

#### 2.1 Extensiones a `SistemaUiState`
```kotlin
data class SistemaUiState(
    // ... campos existentes ...
    val selectedPeriodoCierre: String = "Período Actual",
    val periodosDisponibles: List<String> = listOf(
        "Período Actual",
        "Octubre 2026",
        "Septiembre 2026",
        "Agosto 2026",
        "Julio 2026",
        "Junio 2026"
    ),
    val isDescargandoPdf: Boolean = false,
    val isPdfEnCache: Boolean = false
)
```

#### 2.2 Gestión de Archivos y Caché con FileProvider
- Directorio de destino: `File(context.cacheDir, "comprobantes")`
- Nombres de archivo normalizados: `cierre_contable_${periodo.replace(' ', '_').lowercase(Locale.ROOT)}.pdf`
- Autoridad en Manifest: `com.gamma.mobile.fileprovider`
- XML de rutas: ya configurado en `@xml/file_paths` como `<cache-path name="cache" path="." />`.
- Lanzamiento de Intent seguro:
```kotlin
val uri = FileProvider.getUriForFile(context, "com.gamma.mobile.fileprovider", localFile)
val intent = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(uri, "application/pdf")
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
context.startActivity(intent)
```

---

### 3. Estrategia de Pruebas Unitarias
- `SistemaViewModelTest.kt`:
  - `ejecutarCierreContable_actualizaPeriodoSeleccionadoYObtieneResultado()`
  - `seleccionarPeriodo_actualizaEstadoCorrectamente()`
  - `obtenerUrlAbsolutaPdf_resuelveRutasRelativasYAbsolutas()`
  - Cobertura de manejo de errores de red en la descarga del PDF y fallback de notificaciones al usuario.
