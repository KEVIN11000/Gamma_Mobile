# Especificación de Requerimiento (Spec)
## Módulo: Proyector de Capacidad de Pago con Gastos Ad-Hoc y Escenarios Persistentes (v0.2.1)

- **Fecha:** 22 de Octubre de 2026
- **Versión de Spec:** 1.0.0
- **Estado:** En Revisión (Fase 0 SDD)

---

### 1. Contexto y Justificación
En la versión v0.2.0 se implementó el Proyector de Capacidad de Pago (`CapacidadPagoScreen`), permitiendo simular cuotas de obligaciones bancarias y préstamos existentes, pausarlas temporalmente, aportar ingresos extraordinarios y asignar abonos a capital con cálculo automático de meses ahorrados.

Sin embargo, los usuarios de pequeñas empresas y profesionales independientes a menudo enfrentan egresos imprevistos o gastos discrecionales futuros (por ejemplo: mantenimiento vehicular, reparaciones de taller, cursos de capacitación, compras de insumos excepcionales) que no constituyen "deudas formales" con cuotas registradas en el sistema.

Para alcanzar la visión completa de la herramienta, el Proyector debe permitir:
1. Incorporar libremente gastos de cualquier concepto y monto dentro de la sesión de proyección.
2. Evaluar de forma interactiva el impacto de cada gasto sobre el Semáforo de Viabilidad M3, margen remanente y ratio DTI proyectado.
3. Activar y pausar temporalmente cada gasto ad-hoc sin necesidad de eliminarlo.
4. Guardar, nombrar, listar, cargar y eliminar "Escenarios de Proyección" persistidos en la base de datos local (Room) para planificar diferentes hipótesis financieras (ej. "Mes Austero", "Vacaciones", "Inversión en Equipos").

---

### 2. Alcance (In-Scope)

#### 2.1 Gastos Ad-Hoc en Simulación
- **Modelado en sesión:** Cada gasto ad-hoc cuenta con:
  - `id`: Identificador único UUID.
  - `concepto`: Texto descriptivo ingresado por el usuario (ej: *"Mantenimiento Camioneta"*).
  - `monto`: Valor numérico positivo en Guaraníes (Gs.).
  - `incluido`: Booleano para alternar entre "Incluido en cálculo" y "Pausado temporalmente".
- **Interacción en UI:**
  - Botón *"Agregar Gasto"* en una sección dedicada de la pantalla.
  - Diálogo modal accesible y ergonómico con validación de concepto no vacío y monto mayor a cero.
  - Tarjeta individual para cada gasto proyectado, mostrando su concepto, monto formateado en Guaraníes, switch de inclusión y botón de eliminar (ícono basurero).
  - Recálculo en tiempo real del Semáforo M3, Capacidad Neta y DTI al agregar, modificar estado o eliminar un gasto.

#### 2.2 Persistencia de Escenarios en Room
- **Entidad `EscenarioCapacidadEntity`:**
  - `id`: String (UUID).
  - `nombre`: Nombre asignado por el usuario (ej: *"Noviembre - Vacaciones"*).
  - `fechaCreacion`: Timestamp en milisegundos.
  - `fechaActualizacion`: Timestamp en milisegundos.
  - `ingresoExtra`: Long.
  - `deudasPausadasJson`: String (JSON de IDs de deudas pausadas).
  - `abonosExtraJson`: String (JSON de mapa `{deudaId: monto}`).
  - `gastosAdHocJson`: String (JSON de lista de `GastoSimulado`).
- **Gestión de Escenarios en UI:**
  - Botón en `TopAppBar` (ícono carpeta/escenarios) que despliega un `ModalBottomSheet`.
  - Lista de escenarios guardados ordenados por fecha de actualización descendente.
  - Opciones por escenario: *"Cargar"* y *"Eliminar"*.
  - Botón superior en el BottomSheet: *"Guardar estado actual"* con diálogo para ingresar el nombre.
  - Carga fluida: Al seleccionar un escenario, la pantalla del proyector restaura instantáneamente todos los ingresos extra, deudas pausadas, abonos y gastos ad-hoc, recalculando el semáforo y las métricas.

#### 2.3 Resumen Numérico y Semáforo M3
- Tarjeta de métricas actualizada con una fila explícita:
  - `Fondo Disponible Mes: Gs. X`
  - `Compromisos Cuotas: Gs. Y`
  - `Gastos Proyectados: Gs. Z` *(Nueva fila)*
  - `Margen Remanente: Gs. (X - Y - Abonos - Z)`
  - `DTI Proyectado: % (Total Egresos Simulados / Fondo Disponible)`
- Semáforo de viabilidad recalibrado:
  - **Superávit:** Capacidad neta $> 0$ y remanente $\ge 15\%$ del fondo disponible.
  - **Ajustado:** Capacidad neta $\ge 0$ pero remanente $< 15\%$ del fondo disponible.
  - **Déficit:** Capacidad neta $< 0$ o fondo disponible $\le 0$.

---

### 3. Fuera de Alcance (No-Scope)
- **Conversión directa a asientos del Libro Diario:** Descartado explícitamente en el refinamiento para mantener el simulador libre de acoplamiento contable y evitar registros accidentales en producción.
- **Sincronización de escenarios en servidor:** Los escenarios se almacenan exclusivamente en el dispositivo local vía Room; no requieren endpoints en el backend `Gamma_bot` ni cuotas de red.
- **Categorizaciones o taxonomías fijas obligatorias:** El concepto de gasto es texto libre para máxima velocidad de ingreso sin fricción de selección.

---

### 4. Casos de Uso Principales

#### UC-1: Simular Gasto Extraordinario Ad-Hoc
1. El usuario abre el Proyector de Capacidad de Pago.
2. En la sección *"Gastos Adicionales / Imprevistos"*, presiona `+ Agregar Gasto`.
3. Ingresa Concepto *"Pintura del Local"* y Monto `750000`. Presiona *"Agregar"*.
4. Aparece la tarjeta del gasto; el resumen métrico suma Gs. 750.000 a *"Gastos Proyectados"*, disminuye el *"Margen Remanente"* y actualiza el color del Semáforo M3 si el colchón cae bajo el 15%.

#### UC-2: Probar Alternancia de Inclusión (What-If sin Borrar)
1. Con 3 gastos ad-hoc agregados, el usuario apaga el Switch del gasto más pesado.
2. La tarjeta adopta un estilo translúcido indicando *"Pausado en simulación"*.
3. El Semáforo y el Margen Remanente se recalculan instantáneamente como si ese gasto no se fuera a realizar este mes.

#### UC-3: Guardar Escenario Actual
1. El usuario configuró un ingreso extra de Gs. 1.000.000, pausó una deuda y agregó dos gastos ad-hoc.
2. Toca el ícono de *"Escenarios"* en la barra superior.
3. Se despliega el BottomSheet y presiona *"Guardar estado actual"*.
4. Escribe el nombre *"Escenario Fin de Año"* y confirma.
5. El escenario queda registrado localmente en Room.

#### UC-4: Cargar Escenario Previo
1. Días después, el usuario abre el Proyector (que carga con los datos base de cuotas del mes).
2. Toca *"Escenarios"*, selecciona *"Escenario Fin de Año"* y pulsa *"Cargar"*.
3. La pantalla se actualiza de inmediato con todas las configuraciones guardadas.
