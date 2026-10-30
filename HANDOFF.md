# HANDOFF — Gamma Mobile

**Fase / Parche cerrado:** v0.0.12-pre — Normalización Defensiva de Deudas, Operatividad de Deuda Foco, Ajuste Ergonómico de Proyector y Formato Monolínea
**Fecha de cierre:** 2026-10-29
**Estado del build/tests:** ✅ PASANDO — 220 tests unitarios completados con éxito (`./gradlew testDebugUnitTest` BUILD SUCCESSFUL). Compilación local del APK (`assembleDebug`) completada en 33s e instalada/verificada en el dispositivo físico vía Wi-Fi ADB.
**Estado de Git:** ✅ Árbol limpio en rama `main` (Commit `79bf46a`: *fix(deudas): resolver clasificacion de deudas, operatividad de deuda foco, boton de proyector y formateo de importes*).

---

## 1. Estado actual y mejoras verificadas en dispositivo físico

- **Clasificación y recuperación de deudas en pestañas:**
  - Se implementó normalización defensiva (`estadoReal`, `cuotaReal`, `saldoReal`, `cuotaDesdeEstado`, `cuotaDesdeEntidad`).
  - Verificado en pantalla: la pestaña **`Activas`** ahora muestra las deudas con estado activo (ej. *"Un par de cubiertas Michelin"*), cuotas reales sin inflación y montos limpios.
- **Estrategia y Deuda Foco funcional:**
  - Se eliminó el texto corrupto (*mojibake* `â „ï¸ Bola de Nieve` y `Avalancha` truncada), reemplazándolos con iconos nativos Compose (`AcUnit` y `Bolt`).
  - Chips organizados en fila horizontal simétrica con `Modifier.weight(1f)`.
  - La cuota muestra el valor real (`400.000 ₲` en lugar del valor inflado de 16M).
  - Los botones de acción (**"Abonar Cuota"** y **"Pausar este mes"**) ahora resuelven la obligación de forma global sobre `todasLasObligaciones`, abriendo el modal de abono sin importar qué pestaña de filtro esté activa.
- **Proyector de Capacidad de Pago:**
  - Se corrigió el botón `+ Gasto` en *Gastos Adicionales / Imprevistos* agregando `weight(1f)` a la columna de texto. Se verificó visualmente que el botón se renderiza completo, horizontal y ergonómico.
- **Diario de Transacciones:**
  - El importe (ej. `1.431.300 ₲`) ya no se quiebra en dos líneas gracias a `softWrap = false, maxLines = 1` y `weight(1f)`.
- **Diagnóstico de Balance Actual Inflado (Backend):**
  - Se ubicó la duplicación del 200% del `saldo_inicial` entre `financiero.py:407` y `blueprint.py:105` del backend Python (`Gamma_bot`). Todo el detalle de corrección quedó registrado en `HANDOFF-backend.md` (Módulo 6).

---

## 2. Decisiones tomadas en esta fase

- **Decisión:** Autoridad primaria al estado (`estadoReal`) y tolerancia a desfasaje de columnas en el cliente móvil.
  **Por qué:** Mientras las planillas de Google Sheets o el backend contengan columnas desfasadas, la app móvil no debe colapsar ni clasificar erróneamente todas las deudas como saldadas.
- **Decisión:** Iconos vectoriales de Compose (`Icons.Default.AcUnit` e `Icons.Default.Bolt`) para estrategias de pago.
  **Por qué:** Elimina cualquier riesgo de corrupción de caracteres o codificación UTF-8 entre distintas plataformas o versiones de Android.
- **Decisión:** Resolución desacoplada en `onAbonarClick` buscando en `todasLasObligaciones` (con fallback de ID y nombre).
  **Por qué:** La Deuda Foco puede ser activa mientras la UI está en la pestaña `Saldadas` o `Pausadas`; el modal de pago debe funcionar en todo momento.

---

## 3. Archivos modificados y commiteados

| Archivo | Cambio |
|---|---|
| `app/src/main/java/com/gamma/mobile/domain/model/Models.kt` | Lógica de normalización defensiva (`cuotaReal`, `saldoReal`, `estadoReal`, `montoInicialReal`, etc.) y claves flexibles en `DeudaFocoResponse`. |
| `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesScreen.kt` | Rediseño de `DeudaFocoCard` con iconos vectoriales y layout simétrico; lookup robusto para modal de abono. |
| `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesViewModel.kt` | Exposición de `todasLasObligaciones` para lookup global. |
| `app/src/main/java/com/gamma/mobile/presentation/capacidad/CapacidadPagoScreen.kt` | Corrección del ancho intrínseco del botón `+ Gasto`. |
| `app/src/main/java/com/gamma/mobile/presentation/transacciones/TransaccionesScreen.kt` | Formato monolínea para importes en historial de transacciones. |
| `app/src/test/java/com/gamma/mobile/presentation/obligaciones/ObligacionesViewModelTest.kt` | Cobertura de pruebas unitarias para deudas con columnas desplazadas. |
| `.specify/bugs/deudas-mapeo-planilla/assessment.md` | Diagnóstico y análisis de impacto empírico. |
| `.specify/bugs/deudas-mapeo-planilla/fix.md` | Reporte final de remediación verificado en pantalla. |
| `HANDOFF-backend.md` | Módulos 5 y 6 documentando correcciones pendientes en Google Sheets y Python backend. |

---

## 4. Próximos pasos para la siguiente sesión

- [ ] **Git Push:** Ejecutar `git push origin main` previa autorización del usuario.
- [ ] **Backend `Gamma_bot`:** Intervenir el backend y la hoja de cálculo con los lineamientos de `HANDOFF-backend.md`:
  - Módulo 5: Script de realineación de columnas en `Obligaciones_Maestro` de Google Sheets.
  - Módulo 6: Eliminar la doble adición de `saldo_inicial` en `/dashboard/summary`.
- [ ] **QA Continuo:** Probar flujo de registro de un nuevo abono real contra el backend una vez sincronizada la planilla.
