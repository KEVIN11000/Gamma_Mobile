# HANDOFF — Gamma Mobile

**Fase cerrada:** v0.0.11 — Asistencia en Dos Etapas, Analíticas Laborales MTD y Exportación Dual de Planillas (Excel/PDF)
**Fecha de cierre:** 2026-10-29
**Estado del build/tests:** ✅ pasando — Compilación local y suite de pruebas unitarias completadas con éxito (`./gradlew testDebugUnitTest`, `./gradlew assembleDebug` y `./gradlew assembleRelease` en BUILD SUCCESSFUL).

---

## 1. Estado actual

- **Marcación laboral en dos etapas operativa:** Feedback inmediato de envío local/servidor (`⏳ Marcación enviada al servidor a las HH:mm`) y confirmación asíncrona verificada en planilla de Google Sheets (`✅ Marcación registrada en la planilla...`) con manejo de mensajes informativos y advertencias de estado.
- **Notificaciones nativas del sistema:** `MarcacionNotificationHelper` con canal dedicado `asistencia_confirmaciones` para confirmación visual e inmediata de registros laborales en barra de estado de Android.
- **Analíticas laborales MTD integradas en `AsistenciaScreen`:** Card visual con selector interactivo de períodos (`GET /asistencia/periodos`), métricas de horas trabajadas MTD, días activos, promedio diario, saldo proyectado e insignia de sincronización en tiempo real (`GET /asistencia/analiticas`).
- **Exportación dual de planillas de asistencia:** Descarga y visualización protegida de planilla oficial en formato Excel `.xlsx` (`GET /asistencia/reporte-excel`) vía `FileProvider` con MIME `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, y comprobante PDF (`GET /asistencia/reporte-pdf`).
- **Verificador de versiones in-app en `SistemaScreen`:** Consulta a releases de GitHub (`GET /repos/KEVIN11000/Gamma_Mobile/releases/latest`) y endpoint `/health` del backend; alerta visual de versión disponible y botón de descarga directa de APK con selector seguro de intenciones.
- **Saneamiento y aislamiento de tickets de backend para soporte:** Definido protocolo de aislamiento para `Gamma_bot` documentado en `HANDOFF-backend.md`.
- **Publicación Oficial de Release `v0.0.11` en GitHub Releases:** Tag `v0.0.11` publicado en `origin/main`. Binario firmado `gamma-mobile-v0.0.11-release.apk` (33,509,046 bytes) publicado en GitHub Releases (`https://github.com/KEVIN11000/Gamma_Mobile/releases/tag/v0.0.11`).

## 2. Decisiones tomadas en esta fase

- **Decisión:** Implementar confirmación en dos etapas para la marcación laboral usando campos `confirmado_en_planilla` y `mensaje_adicional` devueltos por la API.
  **Por qué:** Evita incertidumbre operativa en el colaborador cuando la API HTTP responde 200 OK pero la escritura en Google Sheets tarda segundos adicionales o presenta bloqueos de cuota.
  **Alternativas descartadas:** Polling continuo a la planilla de cálculo desde la app móvil (descartado por consumo excesivo de cuota de Google Sheets API y gasto de batería).

- **Decisión:** Apertura de archivos `.xlsx` y `.pdf` mediante `FileProvider` y bandera `Intent.FLAG_GRANT_READ_URI_PERMISSION`.
  **Por qué:** Garantiza seguridad en Android 10+ (API 29+) cumpliendo con Scoped Storage sin requerir permisos invasivos de almacenamiento global (`MANAGE_EXTERNAL_STORAGE`).
  **Alternativas descartadas:** Guardar en carpetas públicas compartidas sin FileProvider.

- **Decisión:** Implementar stubs y mocks desacoplados en suites de tests (`CierreRepositoryTest`, `MovimientoRepositoryTest`, `ObligacionRepositoryTest`, `SyncMovimientosWorkerTest`, `OnboardingViewModelTest`).
  **Por qué:** Mantiene `testDebugUnitTest` 100% aislado de la red, garantizando reproducibilidad y evitando fallos intermitentes en CI/CD.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `VERSION` | Incremento de versión base a `0.0.11`. |
| `app/src/main/java/com/gamma/mobile/data/remote/GammaApi.kt` | Contratos para `getPeriodos()`, `getAnaliticas()`, `descargarReporteExcel()` y `descargarReportePdfAsistencia()`. |
| `app/src/main/java/com/gamma/mobile/data/remote/dto/AsistenciaModels.kt` | DTOs `PeriodosResponse`, `AnaliticasHorasResponse`, y campos de confirmación en planilla en `AsistenciaMarcaResponse`. |
| `app/src/main/java/com/gamma/mobile/domain/model/MarcacionResultado.kt` | Modelo de dominio para dos etapas con badges de estado (éxito, advertencia, informativo, error). |
| `app/src/main/java/com/gamma/mobile/domain/repository/AsistenciaRepository.kt` | Contratos de repositorio para períodos, analíticas y streaming de archivos temporales Excel y PDF. |
| `app/src/main/java/com/gamma/mobile/data/repository/AsistenciaRepositoryImpl.kt` | Implementación con descargas protegidas y almacenamiento en cache privado. |
| `app/src/main/java/com/gamma/mobile/presentation/asistencia/AsistenciaViewModel.kt` | Gestión de `AnaliticasUiState`, `ExcelDownloadState`, dos etapas y auto-refresh tras marcaciones. |
| `app/src/main/java/com/gamma/mobile/presentation/asistencia/AsistenciaScreen.kt` | Componentes `AnaliticasCard`, selector de período, botones de descarga dual Excel/PDF y previews Compose. |
| `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt` | Verificador de actualizaciones in-app y fallback visual a `0.0.11`. |
| `app/src/main/java/com/gamma/mobile/presentation/cierre/CierreViewModel.kt` | Manejo de intent seguro para apertura de Excel y PDF con fallback a navegador y manejo de errores. |
| `app/src/main/java/com/gamma/mobile/util/MarcacionNotificationHelper.kt` | Notificaciones nativas para estado de marcaciones laborales en barra de estado. |
| `app/build/outputs/apk/release/gamma-mobile-v0.0.11-release.apk` | Binario de producción compilado y firmado (33,509,046 bytes). |
| `HANDOFF-backend.md` | Especificación técnica y directivas de saneamiento para backend `Gamma_bot`. |

## 4. Pendientes explícitos para la próxima fase

- [ ] [QA / Reality Check en dispositivo físico: Validar visualmente la descarga y apertura de archivos .xlsx con aplicaciones ofimáticas externas (ej. Microsoft Excel / Google Sheets)]
- [ ] [Biometría y Autenticación: Implementar re-solicitud biométrica o PIN para operaciones financieras críticas y exportación de planillas]
- [ ] [Notificaciones locales: Programar alarmas de vencimiento para deudas a vencer en los siguientes 3 días vía AvisoAlarmScheduler]

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Inexistencia de visor ofimático en el dispositivo para abrir archivos `.xlsx`.
  **Impacto:** El Intent de visualización puede fallar si el dispositivo no posee una aplicación capaz de leer hojas de cálculo.
  **Mitigación sugerida:** Se capturó `ActivityNotFoundException` mostrando toast informativo al usuario recomendando instalar un visor de hojas de cálculo.
- **Riesgo:** Cuotas de lectura en Google Sheets para analíticas MTD ante accesos frecuentes.
  **Impacto:** Latencia o errores HTTP 429 en backend.
  **Mitigación sugerida:** Cache MTD en backend (`InMemorySheetsCache`) y selector manual de refresco en la app.

## 6. Cómo verificar que este handoff sigue vigente

```bash
./gradlew testDebugUnitTest && ./gradlew assembleDebug
```
(Debe compilar limpiamente en verde y superar todas las pruebas unitarias sin errores).

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
