# HANDOFF — Gamma Mobile & Core Bot

**Fase cerrada:** v0.0.7 — Selector de Períodos Históricos, Caché Offline de Comprobantes PDF y Webhook GitHub Reactivo
**Fecha de cierre:** 2026-10-15
**Estado del build/tests:** ✅ pasando — Compilación y tests unitarios verificados (`./gradlew testDebugUnitTest` en GammaMobile con 105 tests; 187 tests en `pytest tests/` en Gamma_bot).

---

## 1. Estado actual

- **Auditoría y Validación en Producción de Webhook GitHub:**
  - Endpoint `POST /api/v1/webhooks/github` verificado en vivo en PythonAnywhere respondiendo 401 bajo firma HMAC-SHA256 (`INVALID_SIGNATURE`), confirmando auto-deploy WSGI activo y protección criptográfica operativa.
- **Selector de Períodos Históricos de Cierre Contable:**
  - `ExposedDropdownMenuBox` implementado en `SistemaScreen.kt` permitiendo alternar entre "Período Actual", mes corriente ("Octubre 2026") y meses históricos anteriores.
  - `SistemaViewModel.kt` expone `selectedPeriodoCierre`, `periodosDisponibles` y `setPeriodoCierre()`, propagando el período a `POST /api/v1/cierres/ejecutar`.
- **Caché Local Privada y Visualización Offline de Comprobantes PDF:**
  - `abrirPdfConCache` en `SistemaViewModel.kt` implementa descarga atómica no bloqueante hacia `context.cacheDir/comprobantes/cierre_contable_<periodo>.pdf` usando archivo temporal `.tmp` y cleanup ante fallos.
  - Apertura instantánea sin peticiones de red adicionales cuando el comprobante ya reside en caché local.
  - Exposición segura vía `androidx.core.content.FileProvider` (`com.gamma.mobile.fileprovider`) con `FLAG_GRANT_READ_URI_PERMISSION` y control de `ActivityNotFoundException`.
- **Higiene de Versionado y Repositorio:**
  - `VERSION` actualizado a `0.0.7` para generación de APKs versionados.
  - `.gitignore` actualizado para ignorar `.agents/`, `.specify/` e `img/`.
  - Tickets de soporte `TCK-007` al `TCK-017` cerrados en `src/data/tickets.json`.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Implementar descarga atómica con archivo intermedio `.tmp` antes de consolidar el archivo `.pdf` en caché.
  **Por qué:** Previene la corrupción de la caché local si una conexión móvil se corta a mitad de la descarga.
  **Alternativas descartadas:** Escribir directamente en el archivo destino `.pdf` sin verificación de completitud.
- **Decisión:** Delegar visualización de PDF al visor nativo del dispositivo mediante `FileProvider` e `Intent(ACTION_VIEW)`.
  **Por qué:** Evita incrementar el tamaño del APK con librerías nativas de renderizado y respeta el ecosistema Android del usuario.
  **Alternativas descartadas:** Incrustar una librería de visor PDF dentro de Jetpack Compose.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `VERSION` | Bump de versión de 0.0.6 a 0.0.7. |
| `.gitignore` | Inclusión de `.agents/`, `.specify/` e `img/`. |
| `docs/specs/cierre-contable-historico-y-cache-pdf/` | Artefactos SDD: `spec.md`, `plan.md`, `tasks.md` completados. |
| `docs/history/fase-v007.md` | Archivo histórico de la fase v0.0.7. |
| `GammaMobile (SistemaViewModel.kt)` | `selectedPeriodoCierre`, `setPeriodoCierre` y descarga atómica con caché offline en `abrirPdfConCache`. |
| `GammaMobile (SistemaScreen.kt)` | `ExposedDropdownMenuBox` de períodos y botón con indicador `isDescargandoPdf`. |
| `GammaMobile (SistemaViewModelTest.kt)` | Cobertura de tests unitarios para períodos, cierre contable y resolución de URLs. |
| `Gamma_bot (src/data/tickets.json)` | Cierre de tickets `TCK-007` a `TCK-017`. |

## 4. Pendientes explícitos para la próxima fase

- [ ] Generar e inspeccionar el APK firmado de release v0.0.7 (`gamma-mobile-v0.0.7-release.apk`) para distribución.
- [ ] Incorporar notificación push local en Android al completar la descarga de comprobantes en segundo plano.
- [ ] Explorar visor de PDF en miniatura (thumbnail) para previsualización directa en Compose si se requiere en el futuro.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Si el usuario borra los datos de caché desde Ajustes de Android, los comprobantes deberán descargarse nuevamente la próxima vez que se consulten.
  **Impacto:** Consumo de datos menor en la primera reapertura tras la limpieza.
  **Mitigación sugerida:** El comportamiento es estándar en Android y la app descarga automáticamente el archivo si no existe.

## 6. Cómo verificar que este handoff sigue vigente

```bash
# En GammaMobile:
./gradlew testDebugUnitTest

# En Gamma_bot:
pytest tests/
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
