# HANDOFF — Gamma Mobile

**Fase cerrada:** Versionado de Distribuibles y Verificación Autónoma de Releases (v0.0.4)
**Fecha de cierre:** 2026-10-15
**Estado del build/tests:** ✅ pasando — Compilación y suites de tests unitarios verificados exitosamente (`./gradlew testDebugUnitTest assembleDebug assembleRelease` en GammaMobile y `pytest tests/test_mobile_api.py` en Gamma_bot).

---

## 1. Estado actual

- **Fuente de verdad única de versión (SSOT):** Archivo `VERSION` en la raíz con `0.0.4`, consumido dinámicamente por `app/build.gradle.kts` para calcular `versionName = "$appVersion+${getGitCommitHash()}"` y `versionCode = getGitCommitCount()`.
- **Nomenclatura versionada en distribuibles APK:** Android Gradle Plugin configurado en `applicationVariants.all` para generar automáticamente `gamma-mobile-v0.0.4-release.apk` (33.2 MB firmado con `gamma_release.jks`) y `gamma-mobile-v0.0.4-debug.apk` (39.8 MB), manteniendo copia `app-release.apk` y `app-debug.apk` para retrocompatibilidad.
- **Verificación autónoma in-app de releases de GitHub:** Interfaz `GitHubReleaseApi`, comparador semántico `SemverUtils` y caso de uso `CheckAppUpdateUseCase` operativos en `SistemaViewModel`, cubiertos por tests en `SemverUtilsTest` y `SistemaViewModelTest`.
- **UI reactiva de actualización en Sistema:** Tarjeta de versión en `SistemaScreen.kt` que muestra badge verde (`Versión al día con GitHub Releases`) si coincide la versión instalada o banner con botón de descarga directa (`Descargar actualización (vX.Y.Z)`) si hay un release superior en GitHub.
- **Release Oficial v0.0.4 publicado:** Tag `v0.0.4` publicado formalmente en GitHub Releases con los binarios `gamma-mobile-v0.0.4-release.apk` y `app-release.apk` adjuntos.
- **Pipeline de Tickets de Soporte:** Funcionalidad de reporte de tickets desde la app (`+ Nuevo`) conectada con el endpoint `/soporte/tickets` de `Gamma_bot`, con persistencia en `tickets.json`, notificación instantánea a Telegram (`CHAT_ID`) y reporte automático a Vigía/GitHub Issues.
- **Backend Gamma_bot sincronizado:** Endpoint `/health` actualizado a versión `0.0.4` y URL de descarga apuntando a GitHub Release `v0.0.4`, desplegado automáticamente en PythonAnywhere vía webhook de `Main-stable`.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Consulta autónoma directa a la API pública de GitHub Releases (`/repos/KEVIN11000/Gamma_Mobile/releases/latest`) para comprobar actualizaciones al refrescar la pantalla de Sistema.
  **Por qué:** Desacopla la detección de nuevas versiones de la app móvil del backend de PythonAnywhere, permitiendo que la app detecte inmediatamente cualquier nueva versión publicada en GitHub sin esperar a actualizar el worker WSGI.
  **Alternativas descartadas:** Depender exclusivamente del endpoint `/health` de `Gamma_bot` (requería editar y desplegar el backend en cada nuevo release).
- **Decisión:** Doble generación de artefactos APK (`gamma-mobile-v0.0.4-[variant].apk` y `app-[variant].apk`).
  **Por qué:** Garantiza nomenclatura clara para distribución y testers sin romper herramientas o pipelines previos que esperan `app-release.apk`.
  **Alternativas descartadas:** Renombrar únicamente el archivo sin copia de compatibilidad.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `VERSION` | Creación de la fuente única de verdad para el versionado semántico (`0.0.4`). |
| `app/build.gradle.kts` | Función `getAppVersion()`, cálculo dinámico de `versionName` y hook en `applicationVariants` para renombrar APKs distribuibles preservando compatibilidad. |
| `app/src/main/java/com/gamma/mobile/data/remote/GitHubReleaseApi.kt` | Interfaz Retrofit para consultar el último release público en GitHub. |
| `app/src/main/java/com/gamma/mobile/domain/model/UpdateModels.kt` | DTOs de GitHub Releases (`GitHubReleaseDto`, `GitHubAssetDto`) y estados sellados `UpdateStatus`. |
| `app/src/main/java/com/gamma/mobile/domain/util/SemverUtils.kt` | Comparador semántico de versiones (limpieza de prefijos `v` y sufijos de build). |
| `app/src/main/java/com/gamma/mobile/domain/usecase/CheckAppUpdateUseCase.kt` | Caso de uso que evalúa si el release de GitHub es superior a la versión local y extrae el activo APK. |
| `app/src/main/java/com/gamma/mobile/di/NetworkModule.kt` | Proveedor HTTP independiente para GitHub Releases sin interceptores de API key de Gamma. |
| `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaViewModel.kt` | Inyección de `CheckAppUpdateUseCase` y actualización de `updateStatus` durante `cargarDatosSistema()`. |
| `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt` | Tarjeta dinámica de versión con chip verde de confirmación o banner de descarga con un toque. |
| `Gamma_bot (src/api/blueprint.py)` | Endpoint `/health` actualizado a versión `0.0.4`, commit count `29` y URL de descarga hacia release `v0.0.4`. |

## 4. Pendientes explícitos para la próxima fase

- [ ] Atender y resolver los tickets de soporte reportados por el usuario desde la app móvil (errores detectados en funcionamiento durante la prueba en dispositivo).
- [ ] Implementar pipeline en `Gamma_bot` que al detectar un ingreso en `Libro_Diario` con categoría/motivo "Salario" (o "Sueldo"), actualice o inserte automáticamente la fila salarial en la hoja `Presupuesto_Base` (`Monto_Mensual_Gs`).
- [ ] Implementar fallback dinámico en `calcular_margen_de_ataque()` para leer el último salario percibido desde `Libro_Diario` si `Presupuesto_Base` no tiene ingreso registrado (`Margen = Salario - Gastos Fijos - Cuotas Activas`).
- [ ] Corregir comando de cierre contable en el core de `Gamma_bot` para permitir el corte mensual de ingresos/egresos del Libro Diario.
- [ ] Implementar sincronización bidireccional del estado de los tickets de soporte con GitHub Issues (Abierto / Cerrado).

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Límite de tasa de GitHub API pública (60 requests/hora por IP para llamadas no autenticadas).
  **Impacto:** Si un usuario refresca más de 60 veces en una hora la pantalla de Sistema, GitHub devuelve HTTP 403 (Rate Limit).
  **Mitigación sugerida:** La app ya implementa fallback silencioso: si GitHub falla por rate-limit o conectividad, no crashea y utiliza la URL de respaldo del endpoint `/health` de `Gamma_bot`.

## 6. Cómo verificar que este handoff sigue vigente

```bash
# En GammaMobile:
./gradlew testDebugUnitTest assembleDebug assembleRelease

# En Gamma_bot:
pytest tests/test_mobile_api.py
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
