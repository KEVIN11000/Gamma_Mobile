# Plan de Arquitectura y Diseño Técnico: v004 - Refinamiento Operativo

## 1. Arquitectura General y Flujo de Datos

```
[Google Sheets / Calendar API]
         ▲
         │ (Credenciales Service Account / SheetsRepository)
         ▼
[Gamma_bot (Flask API WSGI)]
   ├── GET /dashboard/summary (Margen libre real + Posición IVA acumulada)
   ├── GET /movimientos (Ordenación cronológica y metadatos)
   ├── GET /avisos (Combinación avisos locales + Google Calendar events)
   ├── GET /asistencia/materias (Lista oficial desde Config_bot)
   └── POST /cierre/mensual (Soporte tipo 'horas' y tipo 'contable')
         ▲
         │ (HTTPS + Header X-API-KEY)
         ▼
[GammaMobile (Android / Compose / Room)]
   ├── Dashboard: Atajo marcación L-V + Margen Libre + Posición IVA
   ├── TransaccionesViewModel: Parseo Date/LocalDate en orden cronológico
   ├── ObligacionesScreen: FilterChips (Todas, Activas, Saldadas, Simuladas) + ExtendedFAB
   ├── AsistenciaViewModel: Carga dinámica de materias desde API
   ├── CierreScreen: Selector de tipo de cierre (Laboral / Contable)
   └── SistemaScreen: Candado de edición (Lock/Unlock) + Acordeón de tickets
```

---

## 2. Contratos de Datos y APIs

### 2.1. `GET /dashboard/summary` (Actualizado)
**Respuesta JSON (200 OK):**
```json
{
  "mes": "Octubre",
  "ingresos": 15000000,
  "gastos": 8500000,
  "flujo_neto": 6500000,
  "saldo_acumulado": 24500000,
  "margen_libre": 4200000,
  "posicion_iva": 750000,
  "es_debito_fiscal": false
}
```
* **Lógica Backend:**
  - `posicion_iva`: Suma de columna 14 (`Monto_IVA`) de filas con tipo `Gasto` (Crédito Fiscal) menos filas con tipo `Ingreso` (Débito Fiscal) del período no cerrado. Si el crédito fiscal supera al débito, `es_debito_fiscal = false` y representa IVA a favor del usuario.
  - `margen_libre`: Calculado tomando el ingreso base/salario del presupuesto del período anterior menos la cuota mensual de deudas activas (`get_obligaciones_activas()`) menos gastos esenciales proyectados.

### 2.2. `GET /asistencia/materias` (Nuevo Endpoint)
**Respuesta JSON (200 OK):**
```json
[
  "Sistemas de Información",
  "Investigación Operativa",
  "Gestión de Proyectos",
  "Seguridad Informática"
]
```
* **Lógica Backend:**
  - Lee la hoja `Config_bot`, extrae la columna 0 (materia), deduplica valores únicos no vacíos y los retorna ordenados alfabéticamente.

### 2.3. `GET /avisos` (Integración con Google Calendar)
**Respuesta JSON (200 OK):**
```json
[
  {
    "id": "cal-abc123",
    "titulo": "Reunión de Cierre Mensual",
    "fecha_evento": "20/10/2026 15:00",
    "dias_restantes": 6,
    "origen": "google_calendar"
  },
  {
    "id": "av-001",
    "titulo": "Vencimiento Tarjeta de Crédito",
    "fecha_evento": "25/10/2026 09:00",
    "dias_restantes": 11,
    "origen": "local"
  }
]
```
* **Lógica Backend:**
  - Invoca `AgenteAutonomoHoras.obtener_avisos_calendar()` si `CALENDAR_ID` está configurado.
  - Combina con los avisos persistidos en `avisos.json`.
  - Ordena la lista unificada por proximidad de fecha.

### 2.4. `GET /movimientos` y Ordenación en `TransaccionesViewModel`
- En el backend: Retornar los movimientos con su índice secuencial de fila o timestamp explícito.
- En la app Android (`TransaccionesViewModel.kt`):
  - Parsear `fecha` con formateador multi-patrón (`dd/MM/yyyy`, `dd/MM`, `yyyy-MM-dd`).
  - Ordenar mediante `Comparator` cronológico real (o por ID secuencial descendente si la fecha tiene el mismo día), evitando la distorsión alfabética donde `"31/08"` precedía a `"14/10"`.

---

## 3. Arquitectura Frontend (Jetpack Compose)

### 3.1. Dashboard (`DashboardScreen.kt`)
- **Tarjeta Atajo Laboral:**
  - Evaluación del día: `val esDiaLaboral = remember { val d = java.time.LocalDate.now().dayOfWeek; d != java.time.DayOfWeek.SATURDAY && d != java.time.DayOfWeek.SUNDAY }`.
  - Solo se renderiza de Lunes a Viernes.
  - Ofrece botón de acción rápida "Marcar Entrada / Salida" que conecta con `AsistenciaViewModel` o llama directamente al use case de marcación.
- **Tarjetas de Margen Libre y Posición IVA:**
  - Muestra "Margen Libre: X ₲" con etiqueta informativa "Disponible según presupuesto".
  - Muestra "IVA a Favor: X ₲" (Crédito Fiscal) o "IVA a Pagar: X ₲" (Débito Fiscal) con chip visual de estado.

### 3.2. Deudas / Obligaciones (`ObligacionesScreen.kt`)
- **Filtro por Estado:**
  - Enum `FiltroObligacion { TODAS, ACTIVAS, SALDADAS, SIMULADAS }`.
  - Barra de `FilterChip` idéntica a la ergonomía de `TransaccionesScreen`.
  - `ExtendedFloatingActionButton` con icono `Add` y texto `"Nueva Obligación"` para eliminar ambigüedad.

### 3.3. Materias (`AsistenciaViewModel.kt` & `AsistenciaScreen.kt`)
- Reemplazar la lista hardcodeada con un `StateFlow<List<String>>` que se carga en el `init` desde `AsistenciaRepository.getMaterias()`.
- Fallback con caché local si no hay conexión de red.

### 3.4. Sistema (`SistemaScreen.kt`)
- **Protección de Configuración:**
  - Variable `var isConfigLocked by remember { mutableStateOf(true) }`.
  - Cuando está bloqueado, los `OutlinedTextField` de URL y API Key están en `readOnly = true` con icono de candado.
  - Botón "Desbloquear para editar" para habilitar cambios explícitos.
- **Acordeón / Colapso de Tickets:**
  - `Card` de tickets con encabezado interactivo que expande/colapsa la lista mediante `AnimatedVisibility`.
  - Muestra un resumen con contador de tickets activos cuando está contraído.

---

## 4. Estrategia de Manejo de Errores y Resiliencia
- Si Google Calendar no tiene credenciales válidas en `Gamma_bot`, `/avisos` hace log de advertencia y devuelve los avisos locales sin romper el endpoint.
- Si `Config_bot` no tiene materias configuradas, `/asistencia/materias` devuelve un array vacío con status 200 y la app permite ingreso libre manual de materia.
- Si la fecha de un movimiento en `Libro_Diario` no cumple ningún formato estándar, se asigna fecha mínima para no descartar el movimiento y mantener la app estable.
