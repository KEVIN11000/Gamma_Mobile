# Especificación de Requerimiento (Spec) — Verificación In-App de Nuevas Versiones vía GitHub Releases

## 1. Contexto y Problema
Actualmente, la pantalla `SistemaScreen.kt` muestra siempre un botón estático para "Descargar actualización oficial (APK)" si el endpoint del backend (`/health`) devuelve una URL, sin importar si el usuario ya tiene instalada la última versión o una más reciente. Además:
- Depende de que el backend (`Gamma_bot`) actualice manualmente sus valores en cada release.
- Al presionar el botón de **Refresh 🔄** en la barra superior de `SistemaScreen`, no se le informa al usuario de manera clara si está al día o si existe un nuevo release disponible en GitHub.

---

## 2. Alcance (In-Scope)
- **Consulta Autónoma a GitHub Releases:**
  - Creación de un servicio o caso de uso (`CheckAppUpdateUseCase` o `GitHubReleaseApi`) para consultar `https://api.github.com/repos/KEVIN11000/Gamma_Mobile/releases/latest` sin requerir autenticación ni tokens.
  - Extracción de:
    - `tag_name` (ej. `"v0.0.4"`).
    - `browser_download_url` del archivo `*.apk`.
    - Tamaño del binario.
- **Comparación Semántica de Versiones:**
  - Comparador de versiones semánticas (ej. `0.0.4` vs `0.0.5`) contra `BuildConfig.VERSION_NAME` instalado.
- **Feedback Visual Dinámico en `SistemaScreen.kt`:**
  - Al presionar el botón de refresco 🔄 o entrar a Sistema:
    - **Si hay una versión nueva:** Mostrar una tarjeta destacada con banner: *"🚀 Nueva versión disponible: v0.0.5"* y botón directo de descarga del APK oficial.
    - **Si la versión está al día:** Mostrar un badge/tarjeta verde: *"✅ Estás en la última versión oficial (v0.0.4)"*.
- **Resiliencia Offline y Fallback:**
  - Si el dispositivo no tiene internet o GitHub no responde, mantener el estado previo o recurrir silenciosamente a la URL de `/health` sin bloquear la UI ni generar excepciones.

---

## 3. No-Scope (Exclusiones Explícitas)
- Descarga e instalación en segundo plano con auto-instalador silencioso (requiere permisos de Root o Device Admin no aplicables a esta app).
- Modificación del backend `Gamma_bot` (la Opción B es 100% autónoma en el cliente Android).

---

## 4. Criterios de Aceptación
1. Al refrescar la pantalla `Sistema`, la app consulta el último release publicado en GitHub.
2. Si la versión remota es igual o menor a la instalada (`v0.0.4`), la app indica visualmente que está actualizada.
3. Si la versión remota es mayor, se ofrece la acción de descarga del nuevo APK con su URL de GitHub Releases.
4. Si ocurre un fallo de red o timeout con GitHub, la app no crashea y mantiene la estabilidad de la pantalla.
5. Los tests unitarios cubren la lógica de comparación semántica y el mapeo del release.
