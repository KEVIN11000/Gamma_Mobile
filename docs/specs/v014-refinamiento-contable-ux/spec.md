# Especificación de Requerimiento — Versión 0.0.14
## Auto-Descubrimiento de Capacidades, Refinamiento Contable, Presupuesto Base y UX

**ID:** `v014-refinamiento-contable-ux`  
**Fecha:** 2026-11-12  
**Estado:** Fase 0 — Especificación y Arquitectura  
**Autor:** Agente Antigravity / Pair Programming  

---

### 1. Contexto y Justificación

Tras el despliegue de la versión `v0.0.13` en dispositivo físico, la auditoría empírica de las planillas de Google Sheets (`Obligaciones_Maestro`, `Cierres_Historicos` y `Presupuesto_Base`) y el análisis de la reciente **Guía de Integración Backend v2.1.4** (`Gamma_bot/docs/GUIA_INTEGRACION_GAMMAMOBILE_V2_1_4.md`), se identificaron siete requerimientos esenciales:

1. **Auto-Descubrimiento Dinámico de API (`GET /api/v1/capacidades`):**
   - **El cambio arquitectónico más relevante:** El backend expone en tiempo real qué endpoints y funcionalidades están activas (`capacidades_clave`). La app móvil ya no debe asumir de forma rígida qué botones mostrar ni arriesgar errores HTTP 404 si el backend se actualiza a distinto ritmo. Al consultar `/capacidades` en el arranque, la app activa o desactiva dinámicamente componentes visuales (*Feature Toggles* en tiempo de ejecución).
2. **Paridad de Contratos de Red:**
   - `GET /api/v1/bootstrap` en el backend devuelve el resumen bajo la clave `resumen_financiero`. La app esperaba `summary`, causando ceros por omisión en el balance del Dashboard.
   - La ruta del flujo de caja en backend es `GET /api/v1/proyecciones/flujo-caja`.
3. **Defensa Contable contra Desplazamiento y Estado Erróneo en Planilla:**
   - En `Obligaciones_Maestro`, deudas con saldo vivo (como el Préstamo GNB de 15.531.684 ₲) tienen colocado accidentalmente el texto `"Saldada"`. La app móvil forzaba `saldoReal = 0L` al leer dicha cadena, informando saldo cero y 100% abonado.
   - El backend confirmó oficialmente la **deprecación de los campos `Cuotas` (Col K) y `Event_ID` (Col L)**: la app no debe depender de ellos.
4. **Módulo Completo de Presupuesto Base y Gastos Fijos (`PresupuestoScreen`):**
   - La hoja `Presupuesto_Base` de Google Sheets y los endpoints del backend (`routes_presupuesto.py`) carecían de interfaz nativa en Android. Se requiere incorporar la pantalla de Presupuesto para gestionar salario base, gastos fijos por categoría y techos variables, gobernada por los feature flags de `/capacidades`.
5. **Capacidad de Edición de Obligaciones en Movilidad:**
   - La app debe contar con modal de edición directa (`PUT /api/v1/obligaciones/{id}`) para rectificar estados, saldos, cuotas y fechas sin ingresar a Google Sheets.
6. **Ergonomía Visual en Selectores y Logs Plegables:**
   - Estandarizar selectores en Dashboard y Transacciones para eliminar la ilusión óptica de selección invertida de `FilterChip`.
   - Convertir la tarjeta de registros y logs en `SistemaScreen` en un acordeón expandible/plegable.
7. **Inmutabilidad y Banco Oficial de Firmas (Gobernanza Crítica):**
   - **Mandato expreso:** La compilación y distribución oficial del release debe realizarse **estrictamente con el banco oficial de firmas del proyecto** (`gamma_release.jks` / `RELEASE_KEYSTORE_BASE64` en CI).
   - Queda terminantemente prohibido el uso de debug keystores o claves temporales en builds de release.
   - Auditoría criptográfica obligatoria previa a la publicación: verificación con `apksigner` constatando el DN `CN=Gamma Mobile, OU=Release, O=Gamma, L=Asuncion, ST=Central, C=PY` y la huella SHA-256 `2db8d51c88ad21c640a3bffb9fe04c50603e81182f8e1b7e54b3a568a580ffe7`.

---

### 2. Alcance Funcional (In-Scope)

1. **Auto-Descubrimiento de API (`GET /api/v1/capacidades`):**
   - Implementar `CapacidadesResponseDto` y declarar `@GET("capacidades")` en `GammaApi.kt`.
   - Crear `CapacidadesRepository` con persistencia en SharedPreferences/DataStore (cache local offline-first).
   - Hidratar las capacidades al iniciar la app junto a `/bootstrap`.
   - Enlazar los flags booleanos (`presupuesto_fijos_delete`, `presupuesto_techos_put`, `asistencia_excel_export`, `asistencia_pdf_a4_optimizado`, `obligaciones_deuda_foco`, `obligaciones_abonar_post`) con la visibilidad de acciones en la UI.
2. **Alineación de DTOs y Contratos Backend v2:**
   - Soporte simétrico en `BootstrapResponseDto` para `@SerializedName(value = "resumen_financiero", alternate = ["summary", "resumenFinanciero"])`.
   - Actualización de endpoint a `GET proyecciones/flujo-caja`.
   - Incorporación de contrato `PUT obligaciones/{id}` con `ObligacionUpdateDto`.
3. **Invariante de Verdad Contable en `Obligacion` (`Models.kt`):**
   - Regla estricta: Si `saldoActual > 0L`, el estado **nunca es saldada** (`esDefinitivamenteSaldada = false`), garantizando cálculo de cuotas pagadas real `(cuotasTotales - cuotasRestantes)` y saldo real positivo.
4. **Módulo de Presupuesto Base (`PresupuestoScreen`):**
   - Card superior de **Salario Base** editable (`PUT /api/v1/presupuesto/salario-base`).
   - Métricas de estructura: Total Gastos Fijos vs Margen Libre Proyectado.
   - Lista de **Gastos Fijos** agrupados por categoría con diálogo de edición (`PUT /presupuesto/fijos/{concepto}`) y eliminación con confirmación (`DELETE /presupuesto/fijos/{concepto}` condicionado por flag `presupuesto_fijos_delete`). FAB para "Nuevo Gasto Fijo" (`POST /presupuesto/fijos`).
   - Monitor de **Techos Presupuestarios Variables** con barras de progreso ($Gastado / Límite$), semáforos de advertencia (>80% Amarillo, $\ge 100\%$ Rojo) y ajuste de techo (`PUT /presupuesto/techos/{categoria}` condicionado por flag `presupuesto_techos_put`).
5. **Modal de Edición de Obligaciones en `ObligacionesScreen`:**
   - Edición accesible desde cada tarjeta de deuda (Nombre, Entidad, Saldo Actual, Cuota Mensual, Cuotas Restantes/Totales, Vencimiento, Estado).
6. **Estandarización de Selectores y Logs Plegables:**
   - Selectores con indicador visual positivo (borde enfocado, fondo activo e icono check).
   - Acordeón colapsable en la card de logs de `SistemaScreen`.
7. **Auditoría Criptográfica Inviolable:**
   - Verificación empírica con `apksigner` en la terminal local y compuerta en GitHub Actions asegurando la huella SHA-256 oficial.

---

### 3. Fuera de Alcance (No-Scope)

- Modificación destructiva de la estructura de 17 columnas en la hoja Google Sheets (el backend ya las maneja defensivamente).
- Implementación de alarmas autónomas complejas y guardias de asistencia (se abordará en `v0.0.15` como parte del `TICKET-MOB-10`).

---

### 4. Matriz de Impacto sobre Invariantes (`docs/SYSTEM_MAP.md`)

| Invariante | Descripción | Impacto de esta Versión | Verificación |
|---|---|---|---|
| `[#INV-01]` | Formato de Moneda Estricto (Guaraníes enteros `Long`) | Se preserva en saldos, cuotas, presupuestos y techos con `formatGuaranies()`. | Unit tests en `FormatUtilsTest` y `PresupuestoUiTest`. |
| `[#INV-02]` | Single Source of Truth para Estados de Asistencia | Sin alteración; se preservan los modos Normal y Directo. | `DashboardViewModelTest`. |
| `[#INV-04]` | No Destructive Loading en Dashboard | La lectura correcta de `resumen_financiero` previene ceros residuales y no borra vistas. | `DashboardViewModelTest`. |
| `[#INV-05]` | Integridad y Aislamiento de Red | Endpoints autenticados con `X-API-KEY` e idempotencia con `X-Client-Id`. Auto-descubrimiento en `/capacidades`. | `NetworkLayerTest` y `CapacidadesRepositoryTest`. |
| `[#INV-06]` | Resolución Local y Resiliencia Offline-First | Las capacidades, obligaciones y presupuestos se persisten localmente en caché reactiva. | `ObligacionRepositoryTest` y `PresupuestoRepositoryTest`. |
| `[#INV-07]` | Inmutabilidad Criptográfica y Banco Oficial de Firmas | **Absoluto:** Verificación de firma SHA-256 oficial obligatoria antes de dar por cerrado el release. | `apksigner verify --verbose --print-certs`. |
