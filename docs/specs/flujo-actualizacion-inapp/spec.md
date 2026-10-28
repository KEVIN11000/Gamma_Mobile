# Especificación de Requerimiento (Spec) — Restauración y Refinamiento del Flujo Visual de Actualización In-App

## 1. Contexto y Problema

En la pantalla de [SistemaScreen.kt](file:///e:/PG/vcp-s/GammaMobile/app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt), la tarjeta superior (`Card 1: Versión de la Aplicación y Estado de Actualizaciones`) sufrió una regresión visual durante la consolidación de autenticación del dispositivo (commit `7e295687`). En dicha refactorización se eliminó el bloque reactivo que presentaba el estado de actualización.

Actualmente:
- El caso de uso [`CheckAppUpdateUseCase.kt`](file:///e:/PG/vcp-s/GammaMobile/app/src/main/java/com/gamma/mobile/domain/usecase/CheckAppUpdateUseCase.kt) y [`SistemaViewModel.kt`](file:///e:/PG/vcp-s/GammaMobile/app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaViewModel.kt) consultan con éxito los releases publicados en GitHub (`https://api.github.com/repos/KEVIN11000/Gamma_Mobile/releases/latest`) y calculan el estado [`UpdateStatus`](file:///e:/PG/vcp-s/GammaMobile/app/src/main/java/com/gamma/mobile/domain/model/UpdateModels.kt).
- Sin embargo, [SistemaScreen.kt](file:///e:/PG/vcp-s/GammaMobile/app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt) **no renderiza `uiState.updateStatus`**, dejando la pantalla sin retroalimentación visual sobre si la app está al día o si hay una nueva versión disponible.
- Adicionalmente, el texto de versión instalada conserva un fallback hardcodeado desactualizado (`"0.0.9"`), a pesar de que el release instalado y base del proyecto es `0.0.10`.

---

## 2. Alcance (In-Scope)

1. **Restauración y Enriquecimiento de UI en `SistemaScreen.kt`**:
   - Integrar dentro de `Card 1` un bloque reactivo que evalúe `uiState.updateStatus`:
     - **`UpdateStatus.UpToDate`**: Mostrar un chip o tarjeta verde distintiva indicando que la aplicación está en la última versión oficial (`"Versión al día con GitHub Releases"`).
     - **`UpdateStatus.UpdateAvailable`**: Mostrar una tarjeta destacada con alerta informativa, especificando la nueva versión remota (`status.version`), el nombre del artefacto APK (`status.assetName`), el peso en megabytes (`${status.sizeMb} MB`), y un botón interactivo para iniciar la descarga directa del APK oficial mediante un `Intent` del sistema hacia el navegador (`status.downloadUrl`).
     - **`UpdateStatus.CheckFailed`**: Si la consulta a GitHub falla (por ejemplo, sin conexión o rate limit), mostrar un estado no bloqueante con opción de fallback a la URL provista por el endpoint `/health` del backend si estuviese disponible.
     - **`UpdateStatus.Idle` / Loading**: Estado neutral sin parpadeos mientras `isLoading` está activo.
2. **Actualización de Fallback de Versión**:
   - Ajustar el fallback de texto de versión instalada a `"0.0.10"` en correspondencia con el archivo [VERSION](file:///e:/PG/vcp-s/GammaMobile/VERSION) y `build.gradle.kts`.
3. **Ergonomía Táctil y Accesibilidad (Compose Material 3)**:
   - Respetar espaciados de seguridad táctil (mínimo 48dp en áreas cliqueables).
   - Tipografía contrastada según la paleta de Material 3 (`MaterialTheme.colorScheme`).

---

## 3. Exclusiones Explícitas (No-Scope)

- **Sin publicación ni push**: No se realizarán commits de tag, `git push` a repositorios remotos ni publicación de nuevos releases en GitHub durante esta fase (se mantiene estrictamente en entorno local / staging).
- **Sin auto-instalador silencioso**: No se requiere empaquetar un servicio de instalación de fondo desatendido (Android restringe la instalación no supervisada de APKs externos sin permisos de administración o root; la descarga vía navegador con posterior apertura del gestor de paquetes es el estándar de seguridad).
- **Backend `Gamma_bot` diferido**: La limpieza y saneamiento de tickets espurios en `Gamma_bot/src/data/tickets.json` y el mockeo del test en Python se incorporarán en la fase inmediatamente posterior según la directiva del usuario.

---

## 4. Entradas y Salidas

- **Entrada**: Estado reactivo emitido por `SistemaViewModel.uiState`:
  - `uiState.appVersion`: String (ej. `"0.0.10+f6aefe9"`).
  - `uiState.updateStatus`: Sealed class `UpdateStatus` (`UpToDate`, `UpdateAvailable`, `CheckFailed`, `Idle`).
  - `uiState.health?.downloadUrl`: String? (fallback de descarga del backend).
- **Salida Visual**: Componentes Compose interactivos en `SistemaScreen.kt`:
  - `UpdateAvailableCard`: Botón para abrir URL de descarga en navegador (`Intent.ACTION_VIEW`).
  - `UpToDateChip`: Confirmación visual de versión al día.
  - `FallbackDownloadButton`: Botón secundario si la consulta a GitHub falló pero `/health` reporta URL válida.

---

## 5. Criterios de Aceptación Verificables

1. **Estado UpToDate**: Cuando `installedVersion == remoteVersion` (ambos en `v0.0.10`), se visualiza claramente el indicador de que la aplicación está al día.
2. **Estado UpdateAvailable**: Si una versión remota mayor es detectada (ej. en pruebas simuladas con `v0.0.11`), se presenta la tarjeta destacada con nombre de archivo, peso y botón de descarga.
3. **Pulsación de Descarga**: Al presionar *"Descargar actualización"*, se dispara un `Intent` con `ACTION_VIEW` apuntando a la URL del release en GitHub.
4. **Resiliencia ante Fallos**: Si la API de GitHub no responde o no hay red, la pantalla no sufre crasheos ni bloqueos de scroll.
5. **Calidad y Tests**: La suite de tests unitarios pasa al 100% (`.\gradlew testDebugUnitTest`) y `assembleDebug` compila limpiamente sin advertencias de UI.
