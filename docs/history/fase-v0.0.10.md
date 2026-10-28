# HANDOFF — Gamma Mobile

**Fase cerrada:** v0.0.10 — Estabilización de CI/CD y Portabilidad de Compilación
**Fecha de cierre:** 2026-10-23
**Estado del build/tests:** ✅ pasando — Compilación local verificada (`./gradlew testDebugUnitTest` 50+ tests exitosos) y pipeline de CI en GitHub Actions (`Android CI` Run #40015171071) en verde con ejecución completa de tests y empaquetado de APK.

---

## 1. Estado actual

- **Pipeline de GitHub Actions en Verde (`Android CI`):** Workflow `.github/workflows/android.yml` ejecutando con éxito los pasos de checkout, setup JDK 17, `testDebugUnitTest` y `assembleDebug` sobre runners `ubuntu-latest`.
- **Portabilidad de Compilación Multiplataforma:** Eliminada la dependencia estricta de rutas absolutas de Windows en `gradle.properties`, permitiendo compilación agnóstica del SO (Windows/Linux) tanto en local como en CI/CD.
- **Release v0.0.10 Publicada e Inmutable:** Binario firmado `gamma-mobile-v0.0.10-release.apk` publicado en GitHub Releases y anclado al tag `v0.0.10` en el commit `b6268b4`.
- **Cronograma Mensual de Vencimientos (`GET /obligaciones/vencimientos`):** DTO `VencimientoCronogramaItem`, contrato Retrofit en `GammaApi`, fallback offline en `ObligacionRepositoryImpl` con `VencimientoUtils` y vista `CronogramaVencimientosSection`.
- **Estrategia de Desendeudamiento y Deuda Foco (`GET /obligaciones/foco`):** DTO `DeudaFocoResponse`, fallback offline (Bola de Nieve vs Avalancha), selector interactivo en `DeudaFocoCard` y acción de abono directo.
- **Sincronización Remota de Proyector de Capacidad (`POST /obligaciones/proyectar-capacidad`):** `CapacidadPagoViewModel.sincronizarProyeccionRemota()` combinando proyección remota y gastos ad-hoc locales.
- **Historial de Cierres Contables y Laborales:** `GET /cierres` consumido y persistido en Room v4, con visor nativo de PDF y descarga protegida.
- **Simulador de Capacidad con Gastos Ad-Hoc y Escenarios Persistentes (Room v5):** `CapacidadPagoScreen` con imprevistos, switches y escenarios persistidos.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Remover `org.gradle.java.home` con ruta Windows de `gradle.properties`.
  **Por qué:** La ruta absoluta de Windows sobreescribía la variable `JAVA_HOME` inyectada por el runner Linux de GitHub Actions, haciendo fallar la ejecución de Gradle de inmediato.
  **Alternativas descartadas:** Configurar condicionales en Gradle para detectar el SO (descartado porque Gradle ya resuelve `$JAVA_HOME` automáticamente sin necesidad de forzarlo en el repositorio).

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `gradle.properties` | Remoción de la propiedad `org.gradle.java.home=C:/Users/...` para habilitar compatibilidad con runners CI Linux. |
| `HANDOFF.md` | Actualización del estado del proyecto tras la estabilización de CI/CD. |

## 4. Pendientes explícitos para la próxima fase

- [ ] [QA / Reality Check en dispositivo físico: Validar visualmente la transición entre Bola de Nieve y Avalancha con datos reales del backend]
- [ ] [Notificaciones locales: Programar alarmas de vencimiento para deudas a vencer en los siguientes 3 días vía `AvisoAlarmScheduler`]
- [ ] [Exportación de Escenarios: Permitir compartir o exportar resumen de proyección financiera en formato texto/PDF]

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Alta concurrencia de consultas en Google Sheets si múltiples dispositivos consultan `GET /obligaciones/vencimientos` simultáneamente.
  **Impacto:** Error HTTP 429 (`RESOURCE_EXHAUSTED`).
  **Mitigación sugerida:** El backend cuenta con `InMemorySheetsCache` (TTL 180s/300s) y el cliente móvil aplica fallback automático a Room ante errores de red.

## 6. Cómo verificar que este handoff sigue vigente

```bash
./gradlew testDebugUnitTest && ./gradlew assembleDebug
```
(Debe compilar limpiamente en verde y superar 50+ pruebas unitarias sin errores).

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
