# Especificación de Requerimientos: v004 - Refinamiento Operativo de Dashboard, Diario y Sistema

## 1. Contexto y Diagnóstico Empírico de Hallazgos

A partir del feedback del usuario y la investigación del código fuente:

1. **Causa del corte de movimientos en Agosto en Libro Diario:**
   - **Diagnóstico:** En `TransaccionesViewModel.kt`, la lista de movimientos se ordenaba con `filtered.sortedByDescending { it.fecha }`. Dado que `it.fecha` es un `String` en formato latinoamericano `"DD/MM/YYYY"` o `"DD/MM"` (ej. `"31/08"`, `"14/10"`), la comparación lexicográfica de texto posicionaba fechas como `"31/08"`, `"28/08"`, `"25/08"` *antes* de `"14/10"` o `"05/10"` (porque `'3'` > `'1'` > `'0'`). Además, el backend leía `reversed(registros)` invirtiendo filas secuenciales.
   - **Solución:** Parseo canónico de fechas (`LocalDate` / timestamp o preservación del orden cronológico nativo de filas) para que los movimientos de octubre y los más recientes siempre encabecen la lista.

2. **Margen Libre en 0 y Posición IVA no calculada:**
   - **Diagnóstico:** En `Gamma_bot` (`src/api/blueprint.py`), `posicion_iva` estaba fijada en `0` (`"posicion_iva": 0, "es_debito_fiscal": False`) y el margen libre utilizaba un cálculo aproximado `max(0, flujo_neto - int(gastos * 0.2))`.
   - **Solución:**
     - **Margen Libre:** Cálculo real = (Ingreso o Salario base presupuestado/período anterior) menos (Compromisos fijos/cuotas de obligaciones activas + gastos fijos del período).
     - **Posición IVA:** Suma acumulada de Crédito Fiscal (IVA 10% y 5% de gastos/compras del período actual no cerrado) menos Débito Fiscal (IVA de ingresos facturados), reflejando el crédito fiscal real a favor del usuario.

3. **Atajo de Marcación Laboral en el Dashboard:**
   - Se requiere una tarjeta/botón visible de lunes a viernes en el inicio que permita registrar la marcación laboral de entrada/salida directamente sin navegar hasta Compromisos/Asistencia.

4. **Filtros en Deudas/Obligaciones y Claridad de Creación:**
   - En `ObligacionesScreen.kt`, los estados `"Activo"`, `"Saldado"` y `"Simulado"` estaban mezclados en una sola lista sin discriminar.
   - El botón flotante `+` no indicaba qué acción ejecutaba. Se requiere filtro por chips (`Todas`, `Activas`, `Saldadas`, `Simuladas`) y botón extendido `"Nueva Obligación"`.

5. **Materias Reales de Estudio:**
   - `AsistenciaViewModel.kt` usaba un listado estático mock de 10 materias de informática.
   - En el backend, las materias oficiales están en la hoja `Config_bot` (`materia`, `dia`, `inicio_teo`, etc.). Se debe exponer `GET /asistencia/materias` y consumirlo dinámicamente en la app móvil.

6. **Cierre Mensual Bifuncional:**
   - Actualmente solo ejecuta cierre laboral (`horas`). Debe incorporar la opción de Cierre Contable (`ingresos_egresos`) para balancear el Libro Diario del período.

7. **Avisos sincronizados con Google Calendar:**
   - El backend `GET /avisos` leía exclusivamente un archivo local `avisos.json` ignorando `AgenteAutonomoHoras.obtener_avisos_calendar()`. Debe combinar eventos de Google Calendar con avisos locales.

8. **Optimización de Pantalla Sistema (Tickets, Logs y Seguridad):**
   - Lista de tickets colapsable / acordeón para no obstaculizar el scroll.
   - Salud del backend reactiva y diagnóstico de servicios.
   - Bloqueo de seguridad (Read-Only) en los campos de URL y API Key para evitar alteraciones accidentales, con opción de desbloqueo intencional.

---

## 2. Casos de Uso y Alcance

### CU-01: Visualización Real de Margen Libre y Posición IVA en Dashboard
- **Actor:** Usuario.
- **Flujo Principal:**
  1. El usuario abre el Dashboard o pulsa recargar.
  2. La app consulta `GET /dashboard/summary`.
  3. Muestra:
     - Saldo Acumulado (Balance actual en ₲).
     - Margen Libre disponible (según salario base / presupuesto menos compromisos del período).
     - Posición IVA: Monto en ₲ a favor (Crédito Fiscal) del período no cerrado.

### CU-02: Atajo Rápido de Marcación Laboral en Dashboard (Lunes a Viernes)
- **Actor:** Usuario.
- **Flujo Principal:**
  1. Si el día actual es entre Lunes y Viernes, el Dashboard muestra una tarjeta destacada "Marcación Laboral de Hoy".
  2. Con un solo toque, el usuario registra su entrada/salida según el modo configurado (`normal` o `directo`).
  3. El Dashboard notifica éxito y actualiza el estado.

### CU-03: Cronología Correcta y Paginación en Libro Diario
- **Actor:** Usuario.
- **Flujo Principal:**
  1. El usuario abre "Diario".
  2. La app lista las transacciones ordenadas cronológicamente por fecha real (octubre, setiembre, agosto...).
  3. Los movimientos del mes corriente no quedan ocultos o desplazados por ordenaciones de texto erróneas.

### CU-04: Gestión Filtrada de Deudas / Obligaciones
- **Actor:** Usuario.
- **Flujo Principal:**
  1. En Compromisos ➔ Deudas, el usuario selecciona chips: "Activas" (por defecto), "Saldadas", "Simuladas", "Todas".
  2. El botón de acción rápida muestra texto explícito: "Nueva Obligación" / "Simular Cuota".

### CU-05: Carga Oficial de Materias desde `Config_bot`
- **Actor:** Usuario.
- **Flujo Principal:**
  1. Al entrar a Asistencia ➔ Materias, la app consulta `GET /asistencia/materias`.
  2. Se listan las materias oficiales configuradas en Google Sheets en lugar de datos simulados.

### CU-06: Cierre Mensual Dual (Laboral y Contable)
- **Actor:** Usuario.
- **Flujo Principal:**
  1. En Cierre Mensual, el usuario elige el tipo: "Cierre Laboral (Horas y Nómina)" o "Cierre Contable (Libro Diario)".
  2. Ejecuta el cierre para el período y visualiza el comprobante/reporte correspondiente.

### CU-07: Protección de Configuración y Ergonomía en Sistema
- **Actor:** Usuario.
- **Flujo Principal:**
  1. En la pestaña Sistema, los campos de URL y API Key aparecen bloqueados/protegidos por defecto.
  2. Para editar, el usuario activa el interruptor o botón "Desbloquear configuración".
  3. La sección de Tickets de Soporte se presenta colapsable para mejorar la navegación vertical.
  4. Los logs se actualizan bajo demanda o al refrescar.

---

## 3. Criterios de Aceptación (Acceptance Criteria)

1. **AC-01 (Fechas Diario):** En la pestaña "Diario", los registros de octubre aparecen en la parte superior. Las fechas `"31/08"` no superan a `"14/10"`.
2. **AC-02 (Margen e IVA):** El Dashboard muestra el Margen Libre con la fórmula acordada y la Posición de IVA real (Crédito Fiscal acumulado no cerrado).
3. **AC-03 (Atajo Laboral):** El Dashboard renderiza el atajo de marcación de lunes a viernes y dispara la llamada a `/asistencia/marcar`.
4. **AC-04 (Filtros Deudas):** Existen 4 filtros funcionales (`Todas`, `Activas`, `Saldadas`, `Simuladas`). El botón flotante es descriptivo.
5. **AC-05 (Materias Oficiales):** La lista desplegable de materias se obtiene desde el endpoint `/asistencia/materias` vinculado a `Config_bot`.
6. **AC-06 (Avisos Calendar):** `/avisos` devuelve tanto los avisos locales como los eventos programados de Google Calendar.
7. **AC-07 (Sistema Seguro y Compacto):** La configuración de conexión cuenta con candado de edición y los tickets de soporte tienen modo colapsable.

---

## 4. Fuera de Alcance (No-Scope)
- Modificación del esquema de base de datos de PostgreSQL en producción.
- Creación de un sistema de nómina externa fuera de las hojas de Google Sheets existentes.
- Integración de biométricos por hardware directo sin fallback a PIN/toggle en Android.
