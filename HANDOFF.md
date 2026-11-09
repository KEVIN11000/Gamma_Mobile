# HANDOFF — Gamma Mobile

**Fase cerrada:** Backend v2 Integración Global (v0.0.13-backend-v2-integracion-global)
**Fecha de cierre:** 2026-11-09
**Estado del build/tests:** ✅ pasando — Suite completa de pruebas unitarias (`.\gradlew.bat testDebugUnitTest`) y `assembleRelease` completados con éxito.

---

## 1. Estado actual

- **Hidratación Global Atómica (Bootstrap):** Consumo del endpoint `GET /api/v1/bootstrap` implementado en `BootstrapRepositoryImpl`, hidratando de forma atómica y no destructiva el resumen financiero, avisos y periodo contable en `DashboardViewModel`.
- **Flujo de Caja 30D y Clima Offline-First:** Persistencia Room v6 (`MIGRATION_5_6`). Componentes `ClimaHeaderWidget` (alerta lluvia $\ge 35\%$) y `FlujoCajaCard` (semáforo de liquidez y proyección 30 días) en `DashboardScreen`.
- **Inteligencia Financiera Híbrida (NLP Backend v2 + Gemini On-Device):** Botón dual en `NuevaTransaccionScreen` ("⚡ Rápido (NLP)" vía `POST /api/v1/movimientos/parse-nlp` y "✨ Gemini IA" vía on-device).
- **Integración SIFEN KuDE:** Resolución y autocompletado de Razón Social de proveedores a partir de RUC (`POST /api/v1/sifen/consultar-ruc`) en `NuevaTransaccionViewModel`, `SifenScannerViewModel` y `SifenConfirmBottomSheet`.
- **Resiliencia e Idempotencia de Red:** Interceptores OkHttp `IdempotencyInterceptor` (`X-Client-Id` único por sesión), `RateLimitInterceptor` (gestión de 429 con `Retry-After`) y `PayloadSizeGuardInterceptor` (prevención de desbordamiento >64KB).
- **Invariantes Arquitecturales:** Preservación estricta de invariantes `[#INV-01]` a `[#INV-07]` de `docs/SYSTEM_MAP.md` (montos enteros `Long` en Guaraníes `formatGuaranies()`, no destructive loading, resolución local offline-first).

## 2. Decisiones tomadas en esta fase

- **Decisión:** Mantener arquitectura de parsing dual en `NuevaTransaccionScreen` (NLP Backend v2 + Gemini on-device).
  **Por qué:** Asegura máxima velocidad de respuesta y economía de batería en el camino primario (NLP Backend), preservando capacidad funcional offline mediante Gemini on-device (`#INV-06`).
- **Decisión:** Implementar migración incremental de Room `MIGRATION_5_6` (creación de tablas `flujo_caja` y `clima_cache`).
  **Por qué:** Evitar pérdida de datos locales preexistentes en dispositivos de producción al elevar a Room DB v6.
- **Decisión:** Filtrar actualizaciones nulas en `DashboardViewModel.observeAuxiliaryData()`.
  **Por qué:** Respetar el principio de carga no destructiva (`#INV-04`): el Dashboard nunca borra el contenido visible ante flows iniciales vacíos o retrasos de red.
- **Decisión:** Autocompletar razón social vía `ConsultarRucSifenUseCase` preservando edición manual.
  **Por qué:** Agilizar la carga contable KuDE sin bloquear al usuario si el RUC no se encuentra en la base del backend o no hay conexión inmediata.

## 3. Archivos y módulos clave tocados

| Módulo / Archivo | Cambio |
|---|---|
| `VERSION` | Bump de versión oficial a `0.0.13`. |
| `data/remote/dto/BackendV2Dtos.kt` | DTOs de Backend v2 (`BootstrapDto`, `FlujoCajaDto`, `ClimaDto`, `ParsedMovementDto`, `SifenDto`, etc.). |
| `data/remote/GammaApi.kt` | Endpoints backend v2 (`/bootstrap`, `/flujo-caja/30d`, `/clima/actual`, `/movimientos/parse-nlp`, `/sifen/consultar-ruc`). |
| `data/remote/interceptor/*` | Interceptores `IdempotencyInterceptor`, `RateLimitInterceptor` y `PayloadSizeGuardInterceptor`. |
| `data/local/entity/*` y `dao/*` | Entidades y DAOs Room `FlujoCajaEntity`, `ClimaEntity`, `FlujoCajaDao`, `ClimaDao`. |
| `data/local/GammaDatabase.kt` | Elevación a DB v6 con migración `MIGRATION_5_6`. |
| `domain/repository/*` e `impl/*` | `BootstrapRepository`, `FlujoCajaRepository`, `ClimaRepository`. |
| `domain/usecase/*` | `ParseMovimientoNlpUseCase` y `ConsultarRucSifenUseCase`. |
| `presentation/dashboard/*` | Hidratación bootstrap y widgets `ClimaHeaderWidget` y `FlujoCajaCard`. |
| `presentation/transacciones/*` y `sifen/*` | Integración de botón NLP backend y autocompletado KuDE SIFEN por RUC. |
| `docs/SYSTEM_MAP.md` | Mapeo documental vivo actualizado con dominios, módulos e invariantes `[#INV-01]` - `[#INV-07]`. |
| `docs/specs/v013-backend-v2-integracion-global/*` | Especificación, plan arquitectónico y checklist de tareas de la versión 0.0.13. |

## 4. Pendientes explícitos para la próxima fase

- [ ] Descargar e instalar el APK Release `v0.0.13` en el dispositivo físico de producción (HONOR Magic6 Lite).
- [ ] Validar en vivo la sincronización con el backend v2 (`kevin11000.pythonanywhere.com`): hidratación bootstrap, clima y proyección de flujo de caja 30D.
- [ ] Probar en dispositivo el botón "⚡ Rápido (NLP)" en `NuevaTransaccionScreen` con transacciones reales en guaraníes.
- [ ] Verificar el autocompletado de Razón Social por RUC en el escáner KuDE SIFEN contra la base unificada.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Cuotas de API externa de clima en el backend.
  **Mitigación:** Room actúa como cache offline-first con política de TTL en backend; la app móvil nunca falla si el clima no está disponible.
- **Riesgo:** RUC KuDE no registrado en la base unificada de SIFEN.
  **Mitigación:** La UI mantiene el campo de Razón Social siempre editable para que el usuario complete o ajuste el nombre manualmente.

## 6. Cómo verificar que este handoff sigue vigente

```bash
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleRelease
git status
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
