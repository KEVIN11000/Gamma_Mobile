# Data Model: Estado y Actualizaciones In-App

## Modelos Involucrados

### `UpdateStatus` (Domain Sealed Class)
Representa los cuatro estados mutuamente excluyentes del flujo de verificación:

```kotlin
sealed class UpdateStatus {
    object Idle : UpdateStatus()
    data class UpToDate(val version: String) : UpdateStatus()
    data class UpdateAvailable(
        val version: String,
        val downloadUrl: String,
        val assetName: String,
        val sizeMb: Double
    ) : UpdateStatus()
    data class CheckFailed(val reason: String?) : UpdateStatus()
}
```

### `GitHubReleaseDto` (Data Transfer Object)
Representa la respuesta serializada de la API pública de GitHub Releases:
- `tagName`: Tag del release (ej: `"v0.0.10"`).
- `name`: Nombre descriptivo.
- `body`: Notas de la versión en Markdown.
- `assets`: Lista de `GitHubAssetDto` con binarios disponibles (`.apk`).

### `SistemaUiState` (UI Presentation State)
Atributos consumidos por `SistemaScreen.kt`:
- `appVersion`: Versión local instalada (`BuildConfig.VERSION_NAME`).
- `updateStatus`: Instancia activa de `UpdateStatus`.
- `isLoading`: Indica si la petición de red está en progreso.
- `health?.downloadUrl`: Enlace de contingencia provisto por el backend.
