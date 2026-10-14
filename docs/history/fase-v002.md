# HANDOFF — Gamma Mobile

**Fase cerrada:** Despliegue de App, Fusión Backend Core y Configuración de Conexión (v0.0.2)
**Fecha de cierre:** 2026-10-14
**Estado del build/tests:** ✅ pasando — Compilación y suites de tests unitarios verificados exitosamente (`./gradlew testDebugUnitTest assembleDebug`).

---

## 1. Estado actual

- **Repositorio Dedicado Gamma_Mobile:** Vinculado y sincronizado en `origin/main` (`https://github.com/KEVIN11000/Gamma_Mobile.git`) con `README.md`, `ARCHITECTURE.md`, `CONTRIBUTING.md`, `.gitignore` y pipeline de CI en `.github/workflows/android.yml`.
- **Backend Integrado y Desplegado:** Rama `Main-stable` en `Gamma_bot` fusionada y desplegada automáticamente en PythonAnywhere (`https://kevin11000.pythonanywhere.com/api/v1/health`), protegida por autenticación con `X-API-KEY`.
- **Configuración Dinámica y UI de Conexión:** Pantalla `SistemaScreen` equipada con tarjeta "Conexión y API Key" que permite editar la URL del servidor (default `https://kevin11000.pythonanywhere.com/api/v1/`), ingresar la `X-API-KEY` de forma segura, y probar conectividad en vivo.
- **Intercepción de Red Dinámica:** `DynamicBaseUrlInterceptor` operativo en OkHttp para reescribir scheme, host y puerto en tiempo de ejecución sin recompilar el APK.
- **Persistencia Segura:** Credenciales almacenadas mediante `EncryptedSharedPreferences` y `MasterKey` AES-256 en el dispositivo.
- **Releases Oficiales Publicados:** Versión `v0.0.1` y `v0.0.2` publicadas en GitHub Releases con binarios firmados (`app-release.apk`) bajo esquema V2 (`versionCode` 23).
- **Incident Log Documentado:** Registrado el Incidente 001 con análisis de causa raíz y reglas de gobernanza en `docs/incident_log.md`.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Repositorio independiente en GitHub (`Gamma_Mobile`) para el cliente Android.
  **Por qué:** Desacopla el ciclo de vida, versionado y pipelines del frontend móvil del backend de Telegram/Flask (`Gamma_bot`).
  **Alternativas descartadas:** Mantener estructura de monorepo mezclando Gradle y Python.
- **Decisión:** Interceptor dinámico en OkHttp para host y scheme.
  **Por qué:** Permite a los usuarios o desarrolladores cambiar de servidor de desarrollo a producción desde la propia app sin necesidad de generar múltiples variantes de compilación.
- **Decisión:** Preconfiguración de URL oficial de producción por defecto.
  **Por qué:** Evita errores silenciosos por consultas a `localhost` en dispositivos físicos donde no corre servidor local.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `app/src/main/java/com/gamma/mobile/data/remote/DynamicBaseUrlInterceptor.kt` | Interceptor OkHttp para reescritura dinámica de la URL base según configuración del usuario. |
| `app/src/main/java/com/gamma/mobile/data/repository/SecureStorageRepositoryImpl.kt` | Métodos `saveBaseUrl` y `getBaseUrl` con fallback por defecto al host de producción. |
| `app/src/main/java/com/gamma/mobile/domain/repository/SecureStorageRepository.kt` | Contrato de interfaz ampliado para gestión de URL base. |
| `app/src/main/java/com/gamma/mobile/di/NetworkModule.kt` | Configuración de baseUrl oficial y registro de `DynamicBaseUrlInterceptor`. |
| `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt` | Tarjeta visual interactiva de configuración de conexión, servidor y API Key con toggle de visualización y botón de prueba. |
| `app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaViewModel.kt` | Manejo de estado de credenciales y lógica de guardado y recarga de diagnóstico. |
| `docs/incident_log.md` | Documentación exhaustiva del Incidente 001. |
| `Gamma_bot (src/com/handlers/base.py)` | Corrección del comando `/app` con enlaces oficiales hacia `Gamma_Mobile`. |
| `Gamma_bot (src/api/blueprint.py)` | Actualización de `download_url` y versión en endpoint `/health`. |
| `Gamma_bot (src/config.py & security.py)` | Soporte para `.env` en raíz/src y alias `GAMMA_MOBILE_API_KEY`. |

## 4. Pendientes explícitos para la próxima fase

- [ ] Verificar en el dispositivo la carga y renderizado de datos reales del Dashboard (flujo de caja, balance, IVA y margen libre).
- [ ] Verificar el libro diario de Transacciones con datos reales del Google Sheets del usuario.
- [ ] Validar la creación de una transacción real con cálculo de IVA y su sincronización hacia el backend.
- [ ] Evaluar la ergonomía UX/UI y rendimiento de navegación en dispositivo físico Android en condiciones reales de uso.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Limitación de concurrencia en worker WSGI único de PythonAnywhere.
  **Impacto:** Si se ejecutan múltiples peticiones pesadas simultáneas (ej. bot de Telegram + app móvil), puede haber latencia o encolamiento.
  **Mitigación sugerida:** El cliente móvil ya implementa `Dispatchers.IO.limitedParallelism(1)` y timeout de 15 segundos con política offline-first en Room.

## 6. Cómo verificar que este handoff sigue vigente

```bash
./gradlew testDebugUnitTest assembleDebug
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
