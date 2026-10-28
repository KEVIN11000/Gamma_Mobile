# Implementation Plan: Restauración y Refinamiento del Flujo Visual de Actualización In-App

**Branch**: `main` (Trabajo en staging local) | **Date**: 2026-10-26 | **Spec**: [spec.md](file:///e:/PG/vcp-s/GammaMobile/docs/specs/flujo-actualizacion-inapp/spec.md)

---

## 1. Summary

Restaurar la renderización reactiva de `UpdateStatus` en la tarjeta de versión de [SistemaScreen.kt](file:///e:/PG/vcp-s/GammaMobile/app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt), subsanando la regresión visual ocurrida en la integración de autenticación biométrica. Conectar la vista con el estado emitido por [SistemaViewModel.kt](file:///e:/PG/vcp-s/GammaMobile/app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaViewModel.kt) y el caso de uso autónomo [CheckAppUpdateUseCase.kt](file:///e:/PG/vcp-s/GammaMobile/app/src/main/java/com/gamma/mobile/domain/usecase/CheckAppUpdateUseCase.kt), mostrando estado al día (`UpToDate`), alerta interactiva de nueva versión con botón de descarga del APK (`UpdateAvailable`) o fallback en caso de error de red, además de corregir el fallback de versión a `"0.0.10"`.

---

## 2. Technical Context

- **Lenguaje / Versión**: Kotlin 2.0.21 / Android JVM 17.
- **Framework UI**: Jetpack Compose con Material Design 3 (`androidx.compose.material3`).
- **Arquitectura**: MVVM con Clean Architecture (Domain UseCase -> Presentation ViewModel -> Compose Screen).
- **Inyección de Dependencias**: Hilt (`@HiltViewModel`).
- **Manejo de Estado**: `StateFlow<SistemaUiState>` recolectado con `collectAsState()`.
- **Target Platform**: Android API 26+ (Android 8.0 Oreo a Android 14/15, targetSdk 34).
- **Dependencias Clave**:
  - `androidx.compose.material:material-icons-extended` para íconos (`CheckCircle`, `Warning`, `Download`, `Info`).
  - `retrofit2` / `converter-gson` (consumo de GitHub Releases API v3 pública).
- **Políticas de Staging y Gobernanza**:
  - **Sin publicación remota**: Prohibido `git push`, generación de tags o releases en GitHub.
  - **Minimal Change**: Modificación acotada exclusivamente a la sección de actualización en [SistemaScreen.kt](file:///e:/PG/vcp-s/GammaMobile/app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt).

---

## 3. Constitution & Quality Gates

*GATES DE GOBERNANZA ANTIGRAVITY:*
- **Gate 1: Verificación de Modelos y Contratos (Contract-First)**: Los modelos `UpdateStatus` y `CheckAppUpdateUseCase` ya existen y cuentan con cobertura de pruebas unitarias (`SemverUtilsTest` y `SistemaViewModelTest`). No se inventan nuevos contratos innecesarios.
- **Gate 2: Minimal Change**: No se alteran dependencias, módulos de backend ni configuraciones globales.
- **Gate 3: Verificación Empírica (Testing & UI Gate)**:
  - Suite de pruebas unitarias `./gradlew testDebugUnitTest` en verde.
  - Renderizado / preview visual verificado antes de cerrar la tarea.
- **Gate 4: Higiene de Git**: Commits atómicos individuales sin `git add .` ni inclusión de secretos.

---

## 4. Arquitectura de Componentes y Contratos de UI

### 4.1. Contrato de Estado (`UpdateStatus`)

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

### 4.2. Composición Visual en `SistemaScreen.kt`

Dentro de `Card 1` (después de la cabecera de versión):

```kotlin
when (val status = uiState.updateStatus) {
    is UpdateStatus.UpdateAvailable -> {
        // Tarjeta de alerta informativa destacada
        // Icono de actualización / alerta
        // Texto: "¡Nueva versión ${status.version} disponible!"
        // Detalle: "Paquete: ${status.assetName} (${status.sizeMb} MB)"
        // Botón interactivo: "Descargar actualización (${status.version})" -> Lanza Intent.ACTION_VIEW
    }
    is UpdateStatus.UpToDate -> {
        // SuggestionChip o Card discreta en verde
        // Icono CheckCircle
        // Texto: "Versión al día con GitHub Releases"
    }
    is UpdateStatus.CheckFailed -> {
        // Enlace de contingencia: si uiState.health?.downloadUrl está presente,
        // ofrecer botón secundario de descarga alternativa
    }
    is UpdateStatus.Idle -> {
        // No renderizar bloque extra o mostrar progreso discreto si isLoading está activo
    }
}
```

### 4.3. Manejo de Intentes de Navegador

Para disparar la descarga de forma segura sin crasheos si el dispositivo no tuviese navegador:
```kotlin
try {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
} catch (e: ActivityNotFoundException) {
    // Manejo fail-safe informando al usuario
}
```

---

## 5. Plan de Verificación

1. **Pruebas Automatizadas**:
   - Ejecutar suite de pruebas de `SistemaViewModelTest` y `SemverUtilsTest`.
   - Ejecutar suite completa del módulo `:app` con `.\gradlew testDebugUnitTest`.
2. **Pruebas de Compilación de UI**:
   - `.\gradlew assembleDebug` para certificar que el bytecode de Compose se genere sin conflictos.
3. **Verificación Visual de UI (Reality Check)**:
   - Inspeccionar la estructura composable del Card y evaluar la ergonomía de espaciados, contraste tipográfico y accesibilidad.
