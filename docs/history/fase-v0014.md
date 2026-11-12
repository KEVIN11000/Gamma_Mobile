# HANDOFF — Gamma Mobile

**Fase cerrada:** Refinamiento Contable, Auto-descubrimiento y Ergonomía UX (v0.0.14)  
**Fecha de cierre:** 2026-11-12  
**Estado del build/tests:** ✅ pasando — Suite completa de pruebas unitarias (`.\gradlew.bat testDebugUnitTest`), compilación `assembleRelease` y auditoría criptográfica oficial con `apksigner` completadas con éxito.

---

## 1. Estado actual

- **Auto-descubrimiento de Capacidades y Feature Toggles:** Integración de `GET /api/v1/capacidades` en `CapacidadesRepositoryImpl`, permitiendo a la aplicación consultar dinámicamente qué módulos, endpoints y mutaciones soporta el servidor backend, con persistencia offline segura en `gamma_cache` y StateFlow reactivo.
- **Desacoplamiento Contable de Bootstrap:** Compatibilidad total con la respuesta de `GET /api/v1/bootstrap` soportando alternativamente tanto `resumen_financiero` como `summary` en `BootstrapDto`.
- **Blindaje Contable de Deuda Viva:** Invariante `[#INV-02]` fortalecida: si una obligación presenta `saldoActual > 0L`, se garantiza irrevocablemente que `esDefinitivamenteSaldada = false` y permanece en la pestaña `Activas`, previniendo que lecturas erróneas de Google Sheets oculten deudas impagas.
- **Cálculo Auditado de Cuotas y Amortización Real:** Implementación en dominio de `cuotasPagadasReal` y `montoAbonadoReal`, calculados defensivamente a partir del `montoInicial`, `saldoActual` y `cuotasTotales`, mitigando desfasajes de columnas en hojas de cálculo.
- **Edición y Sincronización de Obligaciones:** Incorporación de `PUT /api/v1/obligaciones/{id}` en `GammaApi` y `ObligacionRepositoryImpl`, junto con el diálogo ergonómico `EditarObligacionDialog` en `ObligacionesScreen`.
- **Módulo Integral de Presupuesto Base:** Nueva pantalla `PresupuestoScreen` accesible desde `CompromisosScreen` y navegación central:
  - Edición del salario base presupuestado mensual (`PUT /api/v1/presupuesto/salario-base`).
  - Acordeón interactivo de gastos fijos agrupados por categoría con modales para agregar, editar y eliminar conceptos (`/gastos-fijos`).
  - Monitor en vivo de techos presupuestarios para gastos variables con semáforos de advertencia (Verde $\le 80\%$, Amarillo $80-100\%$, Rojo $> 100\%$) y barras de progreso.
- **Historial de Cierres y Logs del Sistema:**
  - Corrección de etiqueta en `HistorialCierresScreen` a `Total Facturado / Ingresos:` para fidelidad con la lógica contable del backend.
  - Card de "Registros y Logs" en `SistemaScreen` convertida en acordeón interactivo expandible/plegable con conteo de registros y chevron animado.
- **Estandarización Ergonómica de Selectores:**
  - `DashboardScreen`: Chips de Marcación ("Normal" / "Directo") con fondo temático, borde primario de 1.5 dp e icono de confirmación `Check`.
  - `TransaccionesScreen`: Chips de filtrado ("Todos", "Ingresos", "Gastos") con borde enfocado e icono `Check` activo.
- **Inmutabilidad y Auditoría Criptográfica Oficial:**
  - Binario `gamma-mobile-v0.0.14-release.apk` firmado exclusivamente con el banco oficial de claves (`gamma_release.jks`).
  - Auditoría superada con `apksigner verify --verbose --print-certs`: DN `CN=Gamma Mobile, OU=Release, O=Gamma, L=Asuncion, ST=Central, C=PY` y huella SHA-256 `2db8d51c88ad21c640a3bffb9fe04c50603e81182f8e1b7e54b3a568a580ffe7`.

---

## 2. Decisiones tomadas en esta fase

- **Decisión:** Priorizar `saldoActual > 0L` como criterio de autoridad contable sobre el campo de texto `estado` de la planilla.
  **Por qué:** Errores humanos o desajustes de columnas en Google Sheets catalogaban deudas vivas con saldo millonario como "Saldada", lo que en la versión v0.0.13 las ocultaba de la vista del usuario (`#INV-02`).
- **Decisión:** Almacenar las capacidades en SharedPreferences en formato JSON y exponerlas mediante `StateFlow<CapacidadesResponseDto?>`.
  **Por qué:** Evita bloqueos de interfaz durante arranques sin red o en modo avión (`#INV-05`), permitiendo que los Feature Toggles reaccionen inmediatamente al restaurar conexión.
- **Decisión:** BaseFakeGammaApi para desacoplar tests unitarios de mutaciones de endpoints.
  **Por qué:** Al añadir endpoints en `GammaApi`, múltiples tests que implementaban la interfaz directamente fallaban por métodos faltantes. `BaseFakeGammaApi` implementa stubs seguros por defecto.
- **Decisión:** Prohibición absoluta de debug keystore o silent fallback en release (`#INV-06`).
  **Por qué:** Garantiza la inmutabilidad de la cadena de confianza en distribución hacia el usuario final, evitando errores `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

---

## 3. Archivos y módulos clave tocados

| Módulo / Archivo | Cambio |
|---|---|
| `VERSION` | Bump de versión oficial a `0.0.14`. |
| `data/remote/dto/CapacidadesDtos.kt` | DTOs de auto-descubrimiento y Feature Toggles. |
| `data/remote/dto/PresupuestoDtos.kt` | DTOs de salario base, gastos fijos y techos de gastos variables. |
| `data/remote/dto/ObligacionUpdateDto.kt` | DTO para actualización y sincronización de pasivos. |
| `data/remote/dto/BootstrapDto.kt` | Soporte de alias contable (`resumen_financiero` / `summary`). |
| `data/remote/GammaApi.kt` | Endpoints de capacidades, presupuesto y mutaciones de obligaciones. |
| `domain/model/Models.kt` | Blindaje contable de deudas (`esDefinitivamenteSaldada`, `cuotasPagadasReal`, `montoAbonadoReal`). |
| `domain/repository/*` e `impl/*` | Repositorios `CapacidadesRepository` y `PresupuestoRepository`. |
| `presentation/presupuesto/*` | Módulo completo UI, ViewModel y componentes de Presupuesto Base. |
| `presentation/obligaciones/*` | Diálogo interactivo de edición de obligaciones y validación de estado activo. |
| `presentation/dashboard/*` | Estandarización de selector de marcación e hidratación de capacidades. |
| `presentation/transacciones/*` | Estandarización de chips de filtro con borde primario e icono Check. |
| `presentation/cierre/HistorialCierresScreen.kt` | Corrección de etiqueta a `Total Facturado / Ingresos:`. |
| `presentation/sistema/SistemaScreen.kt` | Card de logs plegable con indicador chevron interactivo. |
| `docs/SYSTEM_MAP.md` | Actualización con nuevos módulos, invariantes `[#INV-02]` y `[#INV-08]`. |
| `docs/specs/v014-refinamiento-contable-ux/*` | Especificación, plan arquitectónico y tareas completadas al 100%. |

---

## 4. Pendientes explícitos para la próxima fase

- [ ] Instalar el APK `gamma-mobile-v0.0.14-release.apk` en el dispositivo físico de producción (HONOR Magic6 Lite).
- [ ] Validar en vivo la interacción con `kevin11000.pythonanywhere.com`: respuesta del endpoint `/capacidades` y edición de deudas contra Google Sheets.
- [ ] Probar la configuración de gastos fijos y techos presupuestarios en la nueva pantalla de Presupuesto Base.

---

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Latencia de mutación en Google Sheets al actualizar obligaciones o presupuestos.
  **Mitigación:** Los repositorios manejan llamadas seguras asíncronas con feedback inmediato al usuario (indicador circular de progreso en botones y snackbars informativos).

---

## 6. Cómo verificar que este handoff sigue vigente

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleRelease
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\34.0.0\apksigner.bat" verify --verbose --print-certs app/build/outputs/apk/release/gamma-mobile-v0.0.14-release.apk
git status
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de gobernanza viven en los protocolos globales del proyecto.*
