# HANDOFF — Gamma Mobile

**Fase cerrada:** v0.0.10 (Mobile) — Cronograma de Vencimientos, Deuda Foco y Sincronización Remota de Proyección
**Fecha de cierre:** 2026-10-23
**Estado del build/tests:** ✅ pasando — Compilación y tests verificados al 100% (`./gradlew testDebugUnitTest` 50+ tests exitosos en 28s, `./gradlew assembleDebug` limpio en 37s).

---

## 1. Estado actual

- **Cronograma Mensual de Vencimientos (`GET /obligaciones/vencimientos`):** DTO `VencimientoCronogramaItem`, contrato Retrofit en `GammaApi`, implementación en `ObligacionRepositoryImpl` con cálculo offline fallback sobre Room vía `VencimientoUtils`, y carrusel horizontal `CronogramaVencimientosSection` con badges visuales de urgencia (*Venció*, *Hoy*, *En Xd*).
- **Estrategia de Desendeudamiento y Deuda Foco (`GET /obligaciones/foco`):** DTO `DeudaFocoResponse`, métodos en `GammaApi` y repositorio con fallback offline (menor saldo para Bola de Nieve, mayor cuota para Avalancha), componente `DeudaFocoCard` con alternancia interactiva entre *❄️ Bola de Nieve* y *⚡ Avalancha* y botón directo para amortizar.
- **Sincronización Remota del Motor de Proyección (`POST /obligaciones/proyectar-capacidad`):** Función `sincronizarProyeccionRemota()` en `CapacidadPagoViewModel` que fusiona la proyección del backend con los gastos ad-hoc simulados localmente, manteniendo 0ms de respuesta reactiva local.
- **Soporte de Estado Pausado y Asiento Contable de Abonos:** Integración bidireccional con el backend que excluye deudas pausadas (`PUT /obligaciones/{id}`) y registra egresos contables en `Libro_Diario` tras abonos (`POST /obligaciones/abonar`).
- **Historial de Cierres Contables y Laborales:** `GET /cierres` consumido y persistido en Room v4, con visor nativo de comprobantes PDF y descarga protegida.
- **Simulador de Capacidad con Gastos Ad-Hoc y Escenarios Persistentes (Room v5):** `CapacidadPagoScreen` con gestión de imprevistos, switches de exclusión y guardado/restauración de escenarios en Room.
- **Publicación Oficial de Release `v0.0.10` en GitHub Releases:** Tag `v0.0.10` y commit `b6268b4` publicados en `origin/main`. Binario firmado `gamma-mobile-v0.0.10-release.apk` (33,476,278 bytes, verificado bajo APK Signature Scheme v2) subido y publicado en GitHub Releases (`https://github.com/KEVIN11000/Gamma_Mobile/releases/tag/v0.0.10`).

## 2. Decisiones tomadas en esta fase

- **Decisión:** Fallback offline robusto en repositorio para cronograma de vencimientos y deuda foco.
  **Por qué:** Si el backend está temporalmente no disponible o hay latencia de red, la app calcula localmente el cronograma (`VencimientoUtils`) y la deuda foco desde Room sin bloquear la experiencia de usuario.
- **Decisión:** Mantener el cálculo reactivo local inmediato (0ms de latencia) al proyectar capacidad en `CapacidadPagoScreen`.
  **Por qué:** Evita retrasos en sliders o switches táctiles, ejecutando en segundo plano la sincronización con `POST /obligaciones/proyectar-capacidad` del backend.
- **Decisión:** Fusión de gastos ad-hoc locales con la respuesta remota de proyección.
  **Por qué:** Preserva el aislamiento contable (los gastos ad-hoc no existen en el backend) restándolos de la capacidad neta calculada por el motor de servidor.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `app/src/main/java/com/gamma/mobile/domain/model/Models.kt` | DTOs `VencimientoCronogramaItem` y `DeudaFocoResponse` con serialización Gson. |
| `app/src/main/java/com/gamma/mobile/data/remote/GammaApi.kt` | Endpoints Retrofit para vencimientos, foco y proyector de capacidad. |
| `app/src/main/java/com/gamma/mobile/domain/repository/ObligacionRepository.kt` | Declaración de métodos para cronograma, foco y proyección remota. |
| `app/src/main/java/com/gamma/mobile/data/repository/ObligacionRepositoryImpl.kt` | Implementación con llamadas remotas y fallback offline con `VencimientoUtils` y `formatGuaranies`. |
| `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesViewModel.kt` | StateFlows de cronograma, foco y lógica para cambiar estrategia de desendeudamiento. |
| `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesScreen.kt` | Componentes M3 `CronogramaVencimientosSection`, `VencimientoItemChip` y `DeudaFocoCard`. |
| `app/src/main/java/com/gamma/mobile/presentation/capacidad/CapacidadPagoViewModel.kt` | Método `sincronizarProyeccionRemota()` combinando proyección remota y gastos ad-hoc. |
| `app/src/test/java/com/gamma/mobile/data/repository/ObligacionRepositoryTest.kt` | Suite de tests para llamadas remotas y fallbacks offline. |
| `app/src/test/java/com/gamma/mobile/presentation/obligaciones/ObligacionesViewModelTest.kt` | Tests de ViewModel para cronograma, foco y cambio de criterio. |
| `app/src/test/java/com/gamma/mobile/presentation/capacidad/CapacidadPagoViewModelTest.kt` | Tests de sincronización remota y fusión con gastos simulados. |
| `docs/specs/culminacion-integracion-backend-v022/` | Documentación SDD completa (`spec.md`, `plan.md`, `tasks.md`). |
| `VERSION` | Bump a versión 0.0.10. |
| `app/build/outputs/apk/release/gamma-mobile-v0.0.10-release.apk` | Binario de producción compilado y firmado bajo APK Signature Scheme v2 (33,476,278 bytes). |

## 4. Pendientes explícitos para la próxima fase

- [ ] [QA / Reality Check en dispositivo físico: Validar visualmente la transición entre Bola de Nieve y Avalancha con datos reales del backend]
- [ ] [Notificaciones locales: Programar alarmas de vencimiento para deudas a vencer en los siguientes 3 días vía `AvisoAlarmScheduler`]
- [ ] [Exportación de Escenarios: Permitir compartir o exportar resumen de proyección financiera en formato texto/PDF]

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Alta concurrencia de consultas en Google Sheets si múltiples dispositivos consultan `GET /obligaciones/vencimientos` simultáneamente.
  **Impacto:** Error HTTP 429 (`RESOURCE_EXHAUSTED`).
  **Mitigación sugerida:** El backend ya cuenta con `InMemorySheetsCache` (TTL 180s/300s) y el cliente móvil aplica fallback automático a Room ante errores de red.

## 6. Cómo verificar que este handoff sigue vigente

```bash
./gradlew testDebugUnitTest && ./gradlew assembleDebug
```
(Debe compilar limpiamente en verde y superar 50+ pruebas unitarias sin errores).
