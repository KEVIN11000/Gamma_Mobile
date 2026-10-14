# 🚨 Registro de Incidentes (Incident Log) — Gamma Mobile

Este documento registra anomalías, causas raíz, soluciones técnicas y lecciones aprendidas para garantizar la resiliencia y gobernanza del ecosistema **Gamma**.

---

## Incidente 001: Desalineación de repositorio de distribución y omisión de UI de configuración de endpoints/credenciales en cliente móvil

- **Fecha:** 14 de Octubre de 2026
- **Severidad:** Media (Bloqueo de onboarding en despliegue inicial).
- **Afectados:** Distribución de release inicial v0.0.1 y conexión de usuarios a PythonAnywhere.

### 1. Síntomas Observados
1. El comando de Telegram `/app` y el endpoint `/api/v1/health` entregaban enlaces apuntando a `https://github.com/KEVIN11000/Gamma_bot/releases`, repositorio donde no existían releases de la app móvil (el binario se había publicado en el nuevo repositorio dedicado `KEVIN11000/Gamma_Mobile`).
2. Google Play Protect advirtió y bloqueó la instalación del APK descargado en el dispositivo físico (comportamiento estándar para APKs sideloaded firmados con certificados privados no indexados en Google Play).
3. Tras completar la instalación, el usuario ingresó directamente al Dashboard pero no encontró ningún campo visual para ingresar la API Key (`X-API-KEY`) ni la URL del servidor, imposibilitando la conexión con el backend de producción.

### 2. Causa Raíz
1. **Desacoplamiento tardío de repositorios:** En etapas previas de diseño se contempló publicar el binario en el repositorio central `Gamma_bot`. Al crearse el repositorio dedicado `Gamma_Mobile`, las URLs hardcodeadas en `src/com/handlers/base.py` y `src/api/blueprint.py` no fueron refactorizadas.
2. **Hardcodeo de endpoint local:** En `NetworkModule.kt`, la URL base de Retrofit estaba configurada como `http://127.0.0.1:5000/api/v1/` (mock server local usado en las suites de pruebas). En un teléfono físico, `127.0.0.1` apunta al propio dispositivo Android (donde no existe servidor alguno), causando fallos de red silenciosos.
3. **Falta de UI para SecureStorage:** La infraestructura de almacenamiento seguro (`SecureStorageRepository` respaldado por `EncryptedSharedPreferences`) existía en el dominio y datos, pero no se había integrado una tarjeta editable en `SistemaScreen` ni un modal inicial que permitiera al usuario guardar su clave secreta.

### 3. Soluciones Implementadas
1. **En Backend Core (`Gamma_bot`):**
   * Corrección de `src/com/handlers/base.py` para devolver los enlaces oficiales a `https://github.com/KEVIN11000/Gamma_Mobile/releases` y descarga directa del APK.
   * Corrección de `download_url` en `src/api/blueprint.py`.
   * Soporte en `config.py` y `security.py` para cargar `.env` tanto desde la raíz como desde `src/`, aceptando `X_API_KEY` y `GAMMA_MOBILE_API_KEY`.
2. **En Cliente Android (`GammaMobile`):**
   * Configuración de la URL de producción por defecto: `https://kevin11000.pythonanywhere.com/api/v1/` en `SecureStorageRepositoryImpl` y `NetworkModule`.
   * Creación de `DynamicBaseUrlInterceptor` en OkHttp para permitir cambio dinámico de servidor sin recompilar la app.
   * Interfaz en [SistemaScreen.kt](file:///e:/PG/vcp-s/GammaMobile/app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaScreen.kt): Tarjeta **"Conexión y API Key"** con visualización de estado (`CONFIGURADA` / `PENDIENTE`), campos para Servidor y Clave (con toggle de visibilidad) y botón **"Guardar y Probar Conexión"**.
   * Actualización de [SistemaViewModel.kt](file:///e:/PG/vcp-s/GammaMobile/app/src/main/java/com/gamma/mobile/presentation/sistema/SistemaViewModel.kt) y pruebas unitarias (100% pasando).

### 4. Lecciones Aprendidas y Reglas de Gobernanza
1. **Autonomía de configuración en clientes móviles:** Todo binario de release debe salir preconfigurado hacia el entorno de producción (`https`), pero **siempre** debe incluir una pantalla o sección de ajustes accesible para que el usuario pueda configurar/actualizar sus credenciales y host.
2. **Auditoría e2e de enlaces:** Antes de cerrar cualquier fase de despliegue, todo link generado en bots de mensajería debe ser verificado empíricamente contra el repositorio final.
3. **Manejo explícito de estado de credenciales:** Si la aplicación no tiene una API Key registrada, debe reflejar visualmente el estado "PENDIENTE" y facilitar la configuración en un solo toque.
