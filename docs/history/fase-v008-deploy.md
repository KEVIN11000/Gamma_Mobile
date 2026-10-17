# HANDOFF — Gamma Mobile & Core Bot

**Fase cerrada:** Release v0.0.8 y Despliegue en Producción (Remediación TCK-004)
**Fecha de cierre:** 2026-10-16
**Estado del build/tests:** ✅ pasando — Compilación y tests verificados al 100% (106 tests unitarios y APK de release firmado en GammaMobile; 187 tests unitarios en Gamma_bot).

---

## 1. Estado actual

- **Despliegue y Sincronización en Producción (`Gamma_bot`):**
  - Cambios consolidados en la rama `Main-stable` y publicados hacia `origin/Main-stable` en GitHub.
  - Endpoints en PythonAnywhere (`kevin11000.pythonanywhere.com`) operativos y auditados en vivo vía TLS.
  - Endpoint `POST /api/v1/asistencia/materia` validando horarios lectivos contra `Config_bot` (rebote HTTP 400 sin falsos positivos).
  - Endpoint `POST /api/v1/cierres/ejecutar` generando PDFs binarios conformes con ReportLab Platypus.
  - Endpoint `GET /api/v1/health` actualizado a `latest_app_version: "0.0.8"` con `latest_app_version_code: 45`.
- **Generación y Firma de APK Release v0.0.8 (`GammaMobile`):**
  - Archivo `VERSION` actualizado a `0.0.8`.
  - Binario `gamma-mobile-v0.0.8-release.apk` compilado exitosamente con `./gradlew assembleRelease` (33.28 MB).
  - Firma criptográfica verificada bajo APK Signature Scheme v2 con keystore de producción.
  - Capa de red (`SafeApiCall.kt`) con extracción descriptiva de mensajes de negocio JSON.
- **Auditoría de Tickets de Soporte:**
  - Ticket `TCK-004` cerrado en base de datos local y sincronizado con changelog técnico.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Desplegar directamente los commits de remediación y versionado en `Main-stable` activando el pipeline continuo de producción.
  **Por qué:** Consolida las correcciones críticas de cierre mensual y asistencia de materias de forma atómica en el servidor.
  **Alternativas descartadas:** Mantener versiones asimétricas entre el bot y la app móvil.
- **Decisión:** Publicar versión móvil `0.0.8` firmada con APK Signature Scheme v2 para empaquetar la propagación de mensajes de error de negocio.
  **Por qué:** Ofrece feedback inmediato al usuario en la interfaz sin requerir inspección de logs o códigos numéricos crudos.
  **Alternativas descartadas:** Postergar el bump de versión y compilar únicamente en modo debug.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `Gamma_bot (src/api/blueprint.py)` | Bump de `latest_app_version` a `0.0.8` y `latest_app_version_code` a `45` en `/health`. |
| `Gamma_bot (tests/test_mobile_api.py)` | Verificación de suites de API móvil. |
| `GammaMobile (VERSION)` | Bump de versión de `0.0.7` a `0.0.8`. |
| `GammaMobile (app/build/outputs/apk/release/...)` | Generación de APK firmado `gamma-mobile-v0.0.8-release.apk`. |
| `GammaMobile (docs/history/fase-tck-004.md)` | Archivo histórico de la fase previa de remediación. |
| `GammaMobile (HANDOFF.md)` | Nuevo handoff de estado del sistema post-despliegue. |

## 4. Pendientes explícitos para la próxima fase

- [ ] *(Opcional / Infraestructura)* Crear Release y subir el binario `gamma-mobile-v0.0.8-release.apk` a GitHub Releases bajo el tag `v0.0.8`.
- [ ] *(Opcional / Frontend)* Incorporar notificación push local en Android al completar la descarga de comprobantes en segundo plano.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Si la API de Google Sheets presenta latencia superior a los timeouts configurados en Retrofit (15s), las consultas de asistencia o cierres reportarán timeout.
  **Impacto:** El usuario deberá reintentar la acción desde la interfaz móvil.
  **Mitigación sugerida:** El mecanismo de `safeApiCall` y reintento en pantalla ya atrapa la excepción y permite pulsar nuevamente el botón sin bloquear la aplicación.

## 6. Cómo verificar que este handoff sigue vigente

```bash
# En Gamma_bot:
& "D:\Program Files\Anaconda\python.exe" -m pytest tests/test_mobile_api.py
& "D:\Program Files\Anaconda\python.exe" -m pytest

# En GammaMobile:
./gradlew testDebugUnitTest
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\34.0.0\apksigner.bat" verify --verbose "app/build/outputs/apk/release/gamma-mobile-v0.0.8-release.apk"
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
