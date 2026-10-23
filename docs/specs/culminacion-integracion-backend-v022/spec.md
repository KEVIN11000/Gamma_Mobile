# Especificación Funcional (Spec) — Versión v0.2.2
## Módulo: Culminación de Integración Backend Core (`Gamma_bot` v0.2.0) en GammaMobile

- **Fecha:** 22 de Octubre de 2026
- **Versión de Especificación:** 2.2.0
- **Fase SDD:** 0 (Especificación y Arquitectura - Modo Planificador)
- **Estado:** Pendiente de Aprobación por Usuario (Compuerta de Planificación)

---

## 1. Alcance y Objetivos

Una vez implementados y validados los requerimientos técnicos en el backend core (`Gamma_bot` / PythonAnywhere), `GammaMobile` debe culminar su ciclo de integración consumiendo directamente los nuevos endpoints expuestos para tesorería, timeline de vencimientos, estrategia de desendeudamiento y validación remota de capacidad de pago.

### Casos de Uso del Alcance (Scope)

#### UC-1: Integración de Cronograma de Vencimientos (`GET /obligaciones/vencimientos`)
- **Actor:** Usuario de GammaMobile.
- **Flujo:**
  1. Al ingresar a la pantalla de Obligaciones, la app solicita al backend el cronograma mensual de vencimientos ya ordenado y procesado.
  2. Si hay conexión de red, se muestra la agenda de vencimientos ordenada cronológicamente por día de vencimiento, marcando deudas vencidas, vencimientos de hoy y próximos vencimientos (siguientes 7 días).
  3. Si la app opera sin conexión o el backend no responde, la app ejecuta un fallback automático calculando la agenda a partir de las deudas en Room (`VencimientoUtils`).

#### UC-2: Estrategia de Desendeudamiento y Deuda Foco (`GET /obligaciones/foco`)
- **Actor:** Usuario de GammaMobile.
- **Flujo:**
  1. En `ObligacionesScreen`, se visualiza la tarjeta destacada de **"Deuda Foco del Mes"**.
  2. El usuario puede alternar la estrategia entre:
     - **Bola de Nieve** (`?criterio=saldo`): Enfocada en liquidar deudas de menor saldo para generar victorias psicológicas rápidas y liberar flujo de cuota.
     - **Avalancha / Prioridad** (`?criterio=prioridad`): Enfocada en liquidar las deudas de mayor costo o urgencia financiera.
  3. La app muestra la deuda recomendada por el algoritmo de `financiero.py` del backend, el ahorro estimado y un botón directo para registrar un abono a dicha deuda.
  4. Modo offline: Si no hay conexión, se determina la deuda foco localmente (mínimo saldo para Bola de Nieve, máxima cuota/saldo para Avalancha).

#### UC-3: Validación Remota del Motor de Proyección (`POST /obligaciones/proyectar-capacidad`)
- **Actor:** Usuario de GammaMobile.
- **Flujo:**
  1. En `CapacidadPagoScreen`, el cálculo interactivo inmediato (What-If) se mantiene 100% reactivo localmente para brindar 0ms de latencia al usuario.
  2. En segundo plano o al solicitar recarga, el ViewModel sincroniza con `POST /obligaciones/proyectar-capacidad` para cotejar la capacidad neta, semáforo y meses ahorrados calculados por el motor de `Gamma_bot`.

#### UC-4: Verificación Integral de Endpoints v0.2.0 ya Implementados
- **Actor:** Sistema / Usuario.
- **Flujo:**
  1. Confirmar el flujo bidireccional de actualización de deudas con estado `Pausado` (`PUT /obligaciones/{id}`).
  2. Confirmar que el abono (`POST /obligaciones/abonar`) amortiza el saldo y asienta el egreso contable en `Libro_Diario` invalidando la caché de balances en Sheets.
  3. Confirmar que el historial consolidado de cierres (`GET /cierres`) opera sin discrepancias de contrato con el backend.

---

## 2. Lo que queda Excluido (No-Scope)
- No se modificará el esquema de autenticación ni tokens.
- No se realizarán migraciones de base de datos Room adicionales a menos que sea estrictamente indispensable (las entidades `ObligacionEntity`, `CierreEntity` y `EscenarioCapacidadEntity` en Room v5 son suficientes; el cronograma y la deuda foco son proyecciones de vista).
- No se crearán archivos en repositorios externos durante esta fase de GammaMobile.
