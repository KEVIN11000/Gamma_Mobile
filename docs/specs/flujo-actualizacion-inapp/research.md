# Research: Restauración del Flujo de Actualizaciones In-App

## 1. Decisiones Técnicas

- **Decisión**: Reutilizar de forma directa el modelo [`UpdateStatus`](file:///e:/PG/vcp-s/GammaMobile/app/src/main/java/com/gamma/mobile/domain/model/UpdateModels.kt) y el caso de uso [`CheckAppUpdateUseCase`](file:///e:/PG/vcp-s/GammaMobile/app/src/main/java/com/gamma/mobile/domain/usecase/CheckAppUpdateUseCase.kt) ya existentes en el dominio.
  - **Razón**: La lógica de negocio y consumo de la API pública de GitHub Releases (`https://api.github.com/repos/KEVIN11000/Gamma_Mobile/releases/latest`) ya se encuentra plenamente implementada, testeada en `SistemaViewModelTest` y acoplada a `SistemaUiState`. La deficiencia era estrictamente una omisión en la capa de presentación Compose.
  - **Alternativas descartadas**: Crear un nuevo diálogo modal emergente intrusivo al abrir la app (descartado porque interrumpe la navegación principal del usuario en el Dashboard; el patrón acordado en la arquitectura es la consulta visible en Sistema y disparada al refrescar).

- **Decisión**: Invocar la descarga mediante un `Intent(Intent.ACTION_VIEW, Uri.parse(status.downloadUrl))` seguro envuelto en `try-catch` con captura de `ActivityNotFoundException`.
  - **Razón**: Es el estándar idiomático de Android para delegar la descarga de archivos `.apk` en el navegador del dispositivo, delegando en el gestor de descargas del sistema operativo la verificación de integridad y seguridad.
  - **Alternativas descartadas**: Descargador HTTP en background mediante `DownloadManager` o servicio propio (innecesariamente complejo para una actualización manual y propenso a problemas de permisos en Android 13+).

- **Decisión**: Mantener el scope acotado exclusivamente a `SistemaScreen.kt` sin publicar ni commitear hacia remotos.
  - **Razón**: Instrucción explícita del usuario: "sin publicar, solo staging y luego le incorporamos el saneamiento".
