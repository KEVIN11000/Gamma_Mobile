# HANDOFF — Gamma Mobile

**Fase cerrada:** Fase C — Asistencia, Cierres, Soporte y Quick Wins de UX
**Fecha de cierre:** 2026-10-13
**Estado del build/tests:** ✅ pasando — Todos los unit tests y el ensamblado de la app pasan sin problemas (`./gradlew testDebugUnitTest assembleDebug`).

---

## 1. Estado actual
- **M-18 (Asistencia):** Pestaña Asistencia operativa con marcación laboral normal/directo y selección guiada por menú desplegable de Materia (`AsistenciaScreen`, `AsistenciaViewModel`, `AsistenciaRepositoryImpl`). Cubierto por unit tests en `AsistenciaViewModelTest.kt`.
- **M-19 (Cierres):** Pestaña Cierre operativa para ejecución de cierre mensual con selector desplegable de Período, switch de descuento y visor/descarga nativo de PDF vía `FileProvider` (`CierreViewModel`, `CierreRepositoryImpl`). Cubierto por unit tests en `CierreViewModelTest.kt`.
- **M-20 (Sistema):** Pestaña Sistema operativa mostrando diagnóstico de salud del backend (`HealthStatus`), creación y listado de tickets de soporte, visor de logs y versión semántica `0.0.1+<commit>` generada dinámicamente vía `BuildConfig` (`SistemaViewModel`, `SistemaScreen`). Cubierto por unit tests en `SistemaViewModelTest.kt`.
- **Quick Wins de UX:** Desplegables (`ExposedDropdownMenuBox`) implementados para Materias, Períodos de Cierre y Categorías de transacciones; teclados numéricos (`KeyboardType.Number`) en formularios de obligaciones; contenedor scrolleable (`verticalScroll`) en Dashboard; confirmación de borrado seguro en Avisos; y filtros con `FilterChip` M3 en Libro Diario.
- **QA-C:** Validación manual de flujos completada y verificada contra el servidor mock local.

## 2. Decisiones tomadas en esta fase
- **Decisión:** Reducción de fricción mediante menús desplegables (`ExposedDropdownMenuBox`).
  **Por qué:** Evita errores de tipeo en campos con valores preconfigurados en el sistema (materias académicas, períodos/hojas mensuales y categorías estándar).
- **Decisión:** Apertura nativa de reportes PDF.
  **Por qué:** En lugar de incrustar un visor PDF pesado de terceros en Compose, los reportes se almacenan en caché y se delegan al visor PDF del sistema mediante `FileProvider` con `Intent.ACTION_VIEW`.
- **Decisión:** Separación estricta entre app Android y desarrollo de Backend real.
  **Por qué:** El repositorio `GammaMobile` se mantiene como cliente Android puro contra especificaciones (`backend-spec/`); la implementación real en `Gamma_bot` (PythonAnywhere) se abordará en su respectiva rama/repositorio usando la especificación técnica generada.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `app/src/main/java/com/gamma/mobile/data/remote/GammaApi.kt` | Se agregaron endpoints de asistencia, materia, cierres, descarga PDF, health, tickets y logs. |
| `app/src/main/java/com/gamma/mobile/domain/model/*` | Se definieron modelos para Asistencia, Cierre y Sistema (`AsistenciaModels`, `CierreModels`, `SistemaModels`). |
| `app/src/main/java/com/gamma/mobile/domain/repository/*` | Interfaces `AsistenciaRepository`, `CierreRepository` y `SistemaRepository`. |
| `app/src/main/java/com/gamma/mobile/data/repository/*` | Implementaciones de repositorios con dispatchers protegidos y safeApiCall. |
| `app/src/main/java/com/gamma/mobile/presentation/asistencia/*` | UI y ViewModel de asistencia y cierre con menús desplegables integrados. |
| `app/src/main/java/com/gamma/mobile/presentation/cierre/*` | ViewModel para gestión de cierres y descarga de PDF. |
| `app/src/main/java/com/gamma/mobile/presentation/sistema/*` | UI y ViewModel para diagnóstico, versión dinámica, tickets y logs. |
| `app/src/main/java/com/gamma/mobile/presentation/transacciones/*` | Menú desplegable para categorías y filtros de chip M3 en lista de movimientos. |
| `app/src/main/java/com/gamma/mobile/MainActivity.kt` | Integración de navegación inferior con los 6 destinos principales. |
| `app/build.gradle.kts` | Versionado dinámico semántico `0.0.1+<commit>` y activación de `buildConfig`. |
| `docs/specs/gamma-mobile/tasks.md` | Marcadas M-18, M-19, M-20 y QA-C como completadas. |
| `docs/specs/gamma-mobile/backend-spec/mock_server/mock_app.py` | Rutas mock para asistencia, cierres, PDF dummy, tickets y logs. |

## 4. Pendientes explícitos para la próxima fase
Tareas de Cierre Final del Proyecto (Fase de Cierre):

- [ ] **F-01** Auditoría de seguridad: sin secretos en git, `network_security_config`, logs sin key.
- [ ] **F-02** Suites completas (`./gradlew test lint assembleDebug`, pytest de `backend-spec`).
- [ ] **F-03** APK release firmado (keystore fuera del repo), versión `0.0.1+<commit>`.
- [ ] **F-04** Reporte para el gate de despliegue, con la lista de cambios que debe aplicar el backend real en su repositorio/rama correspondiente (`Gamma_bot` en PythonAnywhere).

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Paridad de endpoints con el backend real en producción.
  **Impacto:** Los endpoints de Fase C (`/asistencia/*`, `/cierres/*`, `/soporte/*`, `/sistema/*`) fueron validados contra el mock server local; deben implementarse en el backend real en PythonAnywhere para operar en producción.
  **Mitigación sugerida:** El reporte de la tarea F-04 servirá como especificación exacta y detallada para implementar la API en `Gamma_bot`.

## 6. Cómo verificar que este handoff sigue vigente
Comando para confirmar que el estado descrito arriba sigue siendo cierto:

```bash
./gradlew testDebugUnitTest assembleDebug
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
