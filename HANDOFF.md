# HANDOFF — Gamma Mobile

**Fase cerrada:** v0.0.9 — Mejoras Integrales (Reactividad, Libro Diario PDF, Avisos, Obligaciones y Seguridad)
**Fecha de cierre:** 2026-10-20
**Estado del build/tests:** ✅ pasando — Compilación y tests verificados al 100% (`./gradlew testDebugUnitTest` 32/32 tareas exitosas, 0 errores).

---

## 1. Estado actual

- **Marcación Laboral en Dashboard:** Selector de modo de marcación (`normal` / `directo`), evaluación semántica de respuesta del servidor (`evaluarMarcacion`) para descartar falsos positivos y banner de estado con confirmación y horas registradas (cubierto en `DashboardViewModelTest.kt`).
- **Balances Reactivos:** Recálculo local instantáneo de Balance Actual, Margen Libre y Posición IVA mediante `DashboardSummaryCalculator` ante transacciones locales/remotas y refresco al regresar a la pantalla con `DashboardViewModel.onScreenVisible()` (cubierto en `DashboardViewModelTest.kt`).
- **Navegación Móvil y Solapamientos:** `ScreenBackStack` con gestión de retroceso y `BackHandler` en `SifenScanner` y `NuevaTransaccion` para volver a la pantalla anterior sin cerrar la app; espaciado `Spacer(96.dp)` al pie de `DashboardScreen` que elimina solapamientos táctiles con el FAB.
- **Libro Diario y Reporte PDF:** Generador PDF 100% local (`LocalDiarioPdfGenerator`) con modelo puro `DiarioPdfReportModel`, disparo de notificación push local (`ComprobanteNotificationHelper`), selector de rango de fechas (`setRangoFechas`/`limpiarRangoFechas`), alternancia de vista Tarjetas <-> Planilla y retiro del botón QR redundante (cubierto en `TransaccionesViewModelTest.kt`).
- **Avisos y Alarmas:** Formulario unificado de creación y edición (`AvisoFormDialog`), selector de repetición y hasta 5 hitos previos; reprogramación con alarmas descartables no ancladas (`ongoing = false`) mediante `AvisoAlarmScheduler` y `AvisoAlarmReceiver` (cubierto en `AvisoAlarmSchedulerTest.kt`, `AvisosViewModelTest.kt`).
- **Obligaciones y Deudas:** Simulador de cuotas ampliado con cálculo amortizado dinámico, cuota editable y traspaso fluido a Nueva Obligación; formulario completo con acreedor, cuotas, categoría y notas; eliminación de deudas (`deleteById`) y cancelación de deudas simuladas con sincronización remota y persistencia local Room v3 (cubierto en `ObligacionesViewModelTest.kt`, `EntityMappingTest.kt`).
- **Seguridad en Sistema:** Verificación de bloqueo de pantalla del dispositivo (`DeviceAuthHelper` con `KeyguardManager`) para visualizar o editar la API Key.
- **Mantenimiento y Soporte:** Retiro de tarjeta "Cierre Contable Mensual"; tickets de soporte con visualización de estado en solo lectura, modal de detalle y alta de nuevos tickets; buffer dinámico de logs en tiempo real (`AppLogBuffer`) con recarga manual y fallback local (cubierto en `SistemaViewModelTest.kt`).
- **Publicación de Release `v0.0.9` en GitHub Releases:** Tag `v0.0.9` y commit `f50a87b` publicados en `origin/main`. Binario firmado `gamma-mobile-v0.0.9-release.apk` (33,345,210 bytes, verificado bajo APK Signature Scheme v2) subido y disponible en GitHub Releases (`https://github.com/KEVIN11000/Gamma_Mobile/releases/tag/v0.0.9`).
- **Sincronización con Backend Core (`Gamma_bot`):** Endpoints del core pusheados y sincronizados.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Desacoplar `DiarioPdfReportModel` y `AvisoAlarmScheduler` de APIs nativas de Android en capas puras de lógica.
  **Por qué:** Permite ejecución instantánea de los tests en JUnit 4 sobre la JVM sin requerir emuladores ni dependencias pesadas de Robolectric.
  **Alternativas descartadas:** Utilizar tests instrumentados en dispositivo físico para verificar generación de reportes y cálculos temporales.
- **Decisión:** Validación semántica en `evaluarMarcacion` inspeccionando texto y campos de hora en la respuesta.
  **Por qué:** El endpoint de marcación puede responder HTTP 200 con un mensaje de fallo textual; el parseo semántico previene reportar falsos éxitos al usuario.
  **Alternativas descartadas:** Confiar únicamente en el código de estado HTTP 200.
- **Decisión:** Restricción estricta de cambios al cliente Android (`GammaMobile`), sin tocar código en `Gamma_bot`.
  **Por qué:** Gobernanza multi-repositorio y aislamiento de responsabilidades; los requerimientos de backend se aislaron en `requerimientos_gamma_bot.md`.
  **Alternativas descartadas:** Modificar directamente los scripts de PythonAnywhere durante la sesión móvil.
- **Decisión:** Autenticación de seguridad para edición de API Key delegada en `KeyguardManager`.
  **Por qué:** Emplea el método estándar del sistema operativo (PIN, patrón, huella o rostro) de forma nativa sin forzar contraseñas propietarias adicionales en la app.
  **Alternativas descartadas:** Solicitar confirmación de texto plano o contraseña separada dentro de la app.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `app/src/main/AndroidManifest.xml` | Registro de `AvisoAlarmReceiver` con `exported="false"`. |
| `app/src/main/java/com/gamma/mobile/MainActivity.kt` | Integración de `ScreenBackStack`, `BackHandler` global y paso de `onScreenVisible` al dashboard. |
| `app/src/main/java/com/gamma/mobile/ScreenBackStack.kt` | Pila de navegación interna para retroceso consistente. |
| `app/src/main/java/com/gamma/mobile/data/local/GammaDatabase.kt` | Bump de versión de Room a v3. |
| `app/src/main/java/com/gamma/mobile/data/local/dao/ObligacionDao.kt` | Método `deleteById` para eliminación de deudas. |
| `app/src/main/java/com/gamma/mobile/data/local/entity/AvisoEntity.kt` | Campos `repeticion` e `hitosPreviosMinutosCsv`. |
| `app/src/main/java/com/gamma/mobile/data/local/entity/ObligacionEntity.kt` | Campos ampliados (`notas`, `categoria`, `estado`, `numeroCuotas`, `tasaInteresAnual`). |
| `app/src/main/java/com/gamma/mobile/data/remote/GammaApi.kt` | Endpoints de actualización de avisos y eliminación de obligaciones. |
| `app/src/main/java/com/gamma/mobile/data/repository/AvisoRepositoryImpl.kt` | Implementación de `actualizarAviso` con fallback Room. |
| `app/src/main/java/com/gamma/mobile/data/repository/DashboardRepositoryImpl.kt` | Manejo de modo de marcación y recalculo dinámico. |
| `app/src/main/java/com/gamma/mobile/data/repository/DashboardSummaryCalculator.kt` | Calculador puro de balance, margen libre y posición IVA. |
| `app/src/main/java/com/gamma/mobile/data/repository/ObligacionRepositoryImpl.kt` | Implementación de `eliminarObligacion` con borrado local Room. |
| `app/src/main/java/com/gamma/mobile/domain/model/Models.kt` | Extensiones de modelos de dominio para `Aviso`, `Obligacion` y marcación. |
| `app/src/main/java/com/gamma/mobile/presentation/avisos/AvisosScreen.kt` | Diálogo unificado de alta/edición de avisos con repetición e hitos. |
| `app/src/main/java/com/gamma/mobile/presentation/avisos/AvisosViewModel.kt` | Flujo de edición y reprogramación de alarmas. |
| `app/src/main/java/com/gamma/mobile/presentation/dashboard/DashboardScreen.kt` | Selector modo normal/directo, feedback semántico y spacer anti-solapamiento. |
| `app/src/main/java/com/gamma/mobile/presentation/dashboard/DashboardViewModel.kt` | Refresco dinámico con `onScreenVisible` y evaluación semántica. |
| `app/src/main/java/com/gamma/mobile/presentation/dashboard/MarcacionResultado.kt` | Parser semántico de respuestas de marcación. |
| `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesScreen.kt` | Simulador ampliado, formulario completo y botones de eliminación/cancelación. |
| `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesViewModel.kt` | Gestión de simulación interactiva y eliminación de deudas. |
| `app/src/main/java/com/gamma/mobile/presentation/sifen/SifenScannerScreen.kt` | `BackHandler` para retorno controlado. |
| `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt` | Retiro de cierre contable, tickets solo lectura + alta, desbloqueo seguro de credencial. |
| `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaViewModel.kt` | Buffer dinámico de logs y soporte de tickets. |
| `app/src/main/java/com/gamma/mobile/presentation/transacciones/NuevaTransaccionScreen.kt` | `BackHandler` para retorno controlado. |
| `app/src/main/java/com/gamma/mobile/presentation/transacciones/TransaccionesScreen.kt` | Retiro de botón QR, filtro por fechas, alternancia tabla/tarjetas y botón PDF. |
| `app/src/main/java/com/gamma/mobile/presentation/transacciones/TransaccionesViewModel.kt` | Generación y exportación de PDF con notificación y filtrado. |
| `app/src/main/java/com/gamma/mobile/receiver/AvisoAlarmReceiver.kt` | Receptor de alarmas locales con notificación push descartable (`ongoing = false`). |
| `app/src/main/java/com/gamma/mobile/utils/AppLogBuffer.kt` | Buffer circular de logs en memoria para diagnósticos en tiempo real. |
| `app/src/main/java/com/gamma/mobile/utils/AvisoAlarmScheduler.kt` | Scheduler puro de alarmas e hitos previos. |
| `app/src/main/java/com/gamma/mobile/utils/DeviceAuthHelper.kt` | Helper de autenticación nativa con `KeyguardManager`. |
| `app/src/main/java/com/gamma/mobile/utils/LocalDiarioPdfGenerator.kt` | Generador de reportes PDF locales en Android Canvas. |
| `docs/specs/mejoras-integrales-v009/` | Artefactos SDD (`spec.md`, `plan.md`, `tasks.md`). |
| `VERSION` | Bump de versión de `0.0.8` a `0.0.9`. |
| `app/build/outputs/apk/release/gamma-mobile-v0.0.9-release.apk` | Binario de producción compilado y firmado bajo APK Signature Scheme v2. |

## 4. Pendientes explícitos para la próxima fase

- [x] Atender en sesión de `Gamma_bot` los requerimientos de backend decantados en `requerimientos_gamma_bot.md` (completado y pusheado por el usuario).
- [x] Realizar bump de versión a `v0.0.9` en `VERSION`, compilar release APK firmado y publicar tag y release en GitHub.
- [ ] Ejecutar prueba manual en dispositivo físico para verificar la estética del PDF generado y la respuesta biométrica en distintas versiones de Android.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Desincronización con Google Calendar si los endpoints en `Gamma_bot` tardan en desplegarse.
  **Impacto:** Los avisos operarán de manera estrictamente local en el dispositivo sin reflejarse en la cuenta remota de Google Calendar.
  **Mitigación sugerida:** El diseño desacoplado con Room v3 y `AvisoAlarmScheduler` garantiza funcionamiento autónomo sin depender de red.

## 6. Cómo verificar que este handoff sigue vigente

```bash
./gradlew testDebugUnitTest
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\34.0.0\apksigner.bat" verify --verbose "app/build/outputs/apk/release/gamma-mobile-v0.0.9-release.apk"
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
