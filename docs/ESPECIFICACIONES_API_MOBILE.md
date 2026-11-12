# Especificación de Contratos y Requerimientos de API: Gamma Mobile ↔ Gamma Backend

**Versión:** 1.0.0 (Corte acumulado `v0.0.12-pre`)  
**Fecha:** Noviembre 2026  
**Cliente:** Android (`GammaMobile` — Jetpack Compose, Retrofit, Kotlinx Coroutines, Room, WorkManager)  
**Servidor:** Python / Flask (`Gamma_bot` — Blueprints, Pydantic, GSpread / Google Sheets, SQLite)  
**Host de Producción:** `<GAMMA_API_BASE_URL>` (inyectado vía `BuildConfig.DEFAULT_BASE_URL` desde `local.properties`)

---

## 1. Arquitectura General de Comunicación

### 1.1 Protocolo y Convenciones
- **Protocolo:** HTTPS / REST sobre JSON.
- **Base URL:** `<GAMMA_API_BASE_URL>` (por defecto `https://api.gamma.example.com/api/v1/` en código público, inyectado desde `local.properties` en tiempo de compilación y configurable dinámicamente en tiempo de ejecución vía `DynamicBaseUrlInterceptor`).
- **Autenticación:** Header estándar `X-API-KEY: <token>` inyectado por `ApiKeyInterceptor`.
- **Moneda:** Guaraní Paraguayo (₲ / PYG). Todos los importes monetarios se manejan como **enteros sin decimales** (`Long` en Kotlin, `int` en Python).
- **Zonas Horarias:** Todas las marcas temporales de eventos y fechas deben expresarse en la zona horaria oficial de Paraguay (`America/Asuncion`, UTC-3 o UTC-4 según horario de verano/invierno).

---

## 2. Catálogo Detallado de Endpoints por Módulo

---

### Módulo A: Monitoreo, Salud y Actualizaciones

#### A.1 Chequeo de Salud del Servidor
- **Método / Ruta:** `GET health`
- **Uso en Mobile:** Diagnóstico en `SistemaViewModel` y verificación rápida de conectividad.
- **Headers:** `X-API-KEY`
- **Respuesta Exitosa (`HTTP 200 OK`):**
```json
{
  "status": "ok",
  "sheets": true,
  "avisos": true,
  "version_api": "1.0.0",
  "latest_app_version": "0.0.12",
  "latest_app_version_code": 12,
  "download_url": "https://github.com/KEVIN11000/Gamma_Mobile/releases/download/v0.0.12/app-release.apk"
}
```
- **Campos:**
  - `status`: `"ok"` o `"degraded"`.
  - `sheets`: Conexión activa con la API de Google Drive / Google Sheets.
  - `avisos`: Disponibilidad del subsistema de avisos.
  - `latest_app_version`: Versión semver disponible del APK para auto-update.

#### A.2 Logs del Sistema
- **Método / Ruta:** `GET sistema/logs`
- **Respuesta Exitosa (`HTTP 200 OK`):**
```json
[
  {
    "timestamp": "2026-11-04 15:30:12",
    "nivel": "INFO",
    "mensaje": "Sincronización de hoja Obligaciones completada con éxito."
  }
]
```

---

### Módulo B: Dashboard y Resumen Financiero

#### B.1 Resumen Financiero para Dashboard
- **Método / Ruta:** `GET dashboard/summary`
- **Uso en Mobile:** Alimenta las tarjetas superiores de `DashboardScreen` (Margen Libre, Saldo Disponible y Posición IVA).
- **Headers:** `X-API-KEY`
- **Respuesta Exitosa (`HTTP 200 OK`):**
```json
{
  "margen_libre": 2500000,
  "saldo_acumulado": 1431300,
  "posicion_iva": 125000,
  "es_debito_fiscal": false
}
```
- **Semántica de Campos:**
  - `margen_libre`: Saldo proyectado libre de compromisos de deuda del mes corriente.
  - `saldo_acumulado`: Saldo real disponible en caja/bancos (`saldo_inicial + total_ingresos - total_gastos`).
  - `posicion_iva`: Monto neto de IVA acumulado del mes.
  - `es_debito_fiscal`: `true` si se debe pagar IVA (débito fiscal > crédito fiscal); `false` si es saldo a favor del contribuyente (crédito fiscal).

> [!CAUTION]
> **Defecto Crítico en Backend (`Módulo 6` de `HANDOFF-backend.md`):**
> En `src/logic/financiero.py:407`, `flujo_neto` ya suma `saldo_inicial`. Luego `src/api/blueprint.py:105` vuelve a sumar `saldo_inicial`. Esto provoca que `saldo_acumulado` devuelva **el doble del saldo inicial**.
> **Corrección:** Definir `flujo_neto = total_ingresos - total_gastos` y `saldo_acumulado = saldo_inicial + flujo_neto`.

---

### Módulo C: Movimientos y Libro Diario

#### C.1 Listado de Movimientos
- **Método / Ruta:** `GET movimientos`
- **Uso en Mobile:** Alimenta `TransaccionesScreen`, cacheado en Room (`MovimientoDao`).
- **Respuesta Exitosa (`HTTP 200 OK`):**
```json
[
  {
    "id": "mov-2026-11-04-001",
    "fecha": "04/11/2026",
    "tipo": "Gasto",
    "categoria": "Combustible",
    "total": 250000,
    "comprobante": "Virtual Legal",
    "proveedor_cliente": "Petrobras",
    "nro_factura": "001-001-0004521",
    "tasa_iva": "10%",
    "client_id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
  }
]
```

#### C.2 Registrar Nuevo Movimiento
- **Método / Ruta:** `POST movimientos`
- **Uso en Mobile:** Creación manual desde `NuevaTransaccionScreen` o sincrónica en segundo plano con `SyncMovimientosWorker`.
- **Payload (`JSON`):**
```json
{
  "id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "fecha": "04/11/2026",
  "tipo": "Gasto",
  "categoria": "Supermercado",
  "total": 185000,
  "comprobante": "Virtual Legal",
  "proveedor_cliente": "Superseis",
  "nro_factura": "001-002-1234567",
  "tasa_iva": "10%",
  "client_id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
}
```
- **Requisitos de Backend:**
  - Debe aceptar indistintamente `"tipo"` o `"movimiento"`.
  - Debe aceptar `"total"` o `"monto"`.
  - Debe aceptar `"proveedor_cliente"` o `"concepto"`.
  - **Idempotencia:** Si se recibe `client_id` y ya fue registrado en las últimas 24 horas, debe devolver `HTTP 200 OK` sin duplicar la fila en Google Sheets.
  - **Invalidación de Caché:** Inmediatamente después de insertar en la hoja, llamar a `_sheets_cache.clear()`.

#### C.3 Interpretación de Movimiento con Inteligencia Artificial
- **Método / Ruta:** `POST ia/interpretar`
- **Payload (`JSON`):**
```json
{
  "texto": "Almuerzo en Biggie por 35000 factura legal"
}
```
- **Respuesta Exitosa (`HTTP 200 OK`):**
```json
{
  "id": "",
  "fecha": "04/11/2026",
  "tipo": "Gasto",
  "categoria": "Alimentación",
  "total": 35000,
  "comprobante": "Física Legal",
  "proveedor_cliente": "Biggie Express",
  "nro_factura": "S/N",
  "tasa_iva": "10%"
}
```
> [!NOTE]
> El backend debe retornar tanto `"tipo"` como `"tipo_movimiento"` para garantizar paridad con el deserializador móvil.

#### C.4 Escaneo OCR de Comprobantes Físicos
- **Método / Ruta:** `POST movimientos/ocr`
- **Content-Type:** `multipart/form-data`
- **Parámetro:** `file` (archivo de imagen JPG/PNG).
- **Respuesta:** Objeto `Movimiento` con los campos extraídos del ticket por visión artificial.

---

### Módulo D: Obligaciones, Deudas y Capacidad de Pago

#### D.1 Listar Obligaciones
- **Método / Ruta:** `GET obligaciones`
- **Uso en Mobile:** Alimenta `ObligacionesScreen` (pestañas Activas, Saldadas, Pausadas).
- **Respuesta Exitosa (`HTTP 200 OK`):**
```json
[
  {
    "id": "DEUDA-001",
    "nombre": "Préstamo Banco Familiar",
    "entidad": "Banco Familiar",
    "tipo": "Préstamo",
    "monto_inicial": 15000000,
    "saldo_actual": 8200000,
    "cuota_referencia_gs": 650000,
    "cuotas_totales": 24,
    "cuotas_restantes": 13,
    "dia_vencimiento": 10,
    "fecha_inicio": "2025-10-10",
    "estado": "Activo",
    "observaciones": "Débito automático cuenta salario"
  }
]
```

> [!WARNING]
> **Mapeo Defensivo en Android por Desfasaje de Columnas en Google Sheets:**
> Si la hoja `Obligaciones_Maestro` tiene columnas desplazadas (por ejemplo, encabezado `Entidad` sobre montos numéricos o `Saldo_Actual` sobre textos de estado), la app móvil ejecuta lógica de saneamiento:
> - `estadoReal`: Si el estado viene numérico, infiere que es la cuota y clasifica como `"Activo"`.
> - `saldoReal`: Si `saldo_actual` es 0 pero `cuota_referencia_gs` contiene el saldo, lo rescata.
> - `cuotaReal`: Extrae el valor real de la cuota desde `cuotaDesdeEstado` o `cuotaDesdeEntidad`.
> **Acción Requerida en Backend:** Ejecutar migración de columnas en Sheets (Módulo 5).

#### D.2 Creación, Edición y Eliminación de Obligaciones
- **Crear:** `POST obligaciones` (Body: `ObligacionCreate`)
- **Editar:** `PUT obligaciones/{id}` (Body: `ObligacionUpdate`)
- **Eliminar:** `DELETE obligaciones/{id}`

#### D.3 Abonar Cuota o Pago Extraordinario
- **Método / Ruta:** `POST obligaciones/abonar`
- **Uso en Mobile:** Modal "Abonar Cuota" en `ObligacionesScreen`.
- **Payload (`JSON`):**
```json
{
  "id_obligacion": "DEUDA-001",
  "monto": 650000,
  "nro_factura": "001-001-0098765",
  "tasa_iva": "10%"
}
```
- **Respuesta Exitosa (`HTTP 200 OK`):**
```json
{
  "status": "ok",
  "id_obligacion": "DEUDA-001",
  "saldo_actual": 7550000,
  "estado": "Activo"
}
```

#### D.4 Cronograma de Vencimientos
- **Método / Ruta:** `GET obligaciones/vencimientos`
- **Respuesta Exitosa (`HTTP 200 OK`):**
```json
[
  {
    "id": "DEUDA-001",
    "nombre": "Préstamo Banco Familiar",
    "entidad": "Banco Familiar",
    "dia_vencimiento": 10,
    "cuota_gs": 650000,
    "saldo_actual": 7550000,
    "vencido": false,
    "dias_restantes": 6,
    "fecha_vencimiento_exacta": "10/11/2026"
  }
]
```

#### D.5 Deuda Foco (Estrategias Avalancha / Bola de Nieve)
- **Método / Ruta:** `GET obligaciones/foco?criterio={saldo|prioridad}`
- **Parámetros:**
  - `criterio`: `"saldo"` (Bola de Nieve: menor saldo primero) o `"prioridad"` (Avalancha: mayor cuota o tasa de interés primero).
- **Respuesta Esperada por Mobile (Plana en raíz):**
```json
{
  "id_deuda": "DEUDA-002",
  "nombre": "Cubiertas Michelin",
  "entidad": "Chacomer",
  "saldo_actual": 1200000,
  "cuota_gs": 400000,
  "criterio": "saldo",
  "ahorro_estimado_meses": 3,
  "explicacion": "Al liquidar esta deuda de menor saldo liberas 400.000 ₲ mensuales para acelerar las demás."
}
```

#### D.6 Simulador y Proyector de Capacidad de Pago
- **Método / Ruta:** `POST obligaciones/proyectar-capacidad`
- **Uso en Mobile:** `CapacidadPagoScreen`.
- **Payload (`JSON`):**
```json
{
  "deudas_pausadas_simuladas": ["DEUDA-003"],
  "abonos_extraordinarios": {
    "DEUDA-001": 1000000
  },
  "ajuste_ingreso_extra": 500000
}
```
- **Respuesta Exitosa (`HTTP 200 OK`):**
```json
{
  "compromiso_total_mes": 1050000,
  "fondo_disponible_mes": 3500000,
  "total_gastos_proyectados": 1950000,
  "capacidad_neta": 1550000,
  "semaforo": "SUPERAVIT",
  "dti_proyectado": 0.30,
  "meses_ahorrados_por_abonos": {
    "DEUDA-001": 2
  }
}
```
- **Semáforo:** `"SUPERAVIT"`, `"AJUSTADO"`, `"DEFICIT"`.

---

### Módulo E: Avisos, Notificaciones y Recordatorios

#### E.1 Listar y Administrar Avisos
- `GET avisos` -> Retorna lista de avisos.
- `POST avisos` -> Crea un aviso.
- `PUT avisos/{id}` -> Modifica un aviso.
- `DELETE avisos/{id}` -> Elimina un aviso.

- **Esquema de Datos (`Aviso`):**
```json
{
  "id": "aviso-101",
  "titulo": "Vencimiento Cuota Banco Familiar",
  "fecha_evento": "10/11/2026 09:00",
  "repeticion": "MENSUAL",
  "hitos_minutos": [1440, 120, 15]
}
```
- **Repeticiones admitidas:** `"NINGUNA"`, `"DIARIA"`, `"SEMANAL"`, `"MENSUAL"`, `"ANUAL"`.
- **Hitos (`hitos_minutos`):** Lista de minutos previos al evento en los que `AvisoAlarmScheduler` de Android debe disparar una notificación local (`1440` = 24 horas antes, `120` = 2 horas antes, `15` = 15 minutos antes).

#### E.2 Parseo con Inteligencia Artificial
- **Método / Ruta:** `POST ia/interpretar/aviso`
- **Payload:** `{"texto": "Recordarme pagar el seguro el 15 a las 10am todos los meses"}`
- **Respuesta:** Objeto `Aviso` estructurado.

---

### Módulo F: Asistencia y Control Laboral

#### F.1 Marcar Entrada / Salida
- **Método / Ruta:** `POST asistencia/marcar`
- **Payload:** `{"modo": "normal"}` o `{"modo": "directo"}`
- **Respuesta Exitosa (`HTTP 200 OK`):**
```json
{
  "tipo": "entrada",
  "hora": "08:02",
  "mensaje": "✅ Entrada registrada a las 08:02",
  "planilla": "Horas_Noviembre_2026",
  "fila": 14,
  "confirmado_en_planilla": true
}
```

> [!IMPORTANT]
> **Respuesta ante Marcación Previa (Evitar HTTP 400):**
> Cuando el usuario ya registró su entrada/salida hoy, el backend **NO debe retornar `HTTP 400 Bad Request`**. Debe retornar `HTTP 200 OK` con `"tipo": "info"` para que la app muestre una confirmación amigable sin fallar el worker ni alertar de error de red:
> ```json
> {
>   "tipo": "info",
>   "hora": "08:02",
>   "mensaje": "ℹ️ Ya registraste tu entrada hoy a las 08:02",
>   "confirmado_en_planilla": true
> }
> ```

#### F.2 Analíticas y Devengado de Salario
- **Método / Ruta:** `GET asistencia/analiticas?hoja={nombre_hoja}`
- **Respuesta Exitosa (`HTTP 200 OK`):**
```json
{
  "periodo": "Noviembre 2026",
  "monto_por_hora": 35000,
  "dias_trabajados": 3,
  "total_horas_decimal": 24.5,
  "total_horas_formato": "24h 30m",
  "salario_acumulado_gs": 857500,
  "horas_perdidas_decimal": 0.0,
  "horas_extras_decimal": 0.5,
  "balance_neto_decimal": 0.5,
  "horas_perdidas_formato": "0h 00m",
  "horas_extras_formato": "0h 30m",
  "balance_neto_formato": "+0h 30m",
  "promedio_horas_por_dia": 8.16
}
```

#### F.3 Descarga de Reportes
- `GET asistencia/reporte/excel?hoja=...&descuento=...&saldo_anterior=...&incluir_iva=true`
- `GET asistencia/reporte/pdf?hoja=...&descuento=...&saldo_anterior=...&incluir_iva=true`
- Retornan el flujo binario del archivo para guardado en almacenamiento local o apertura vía `FileProvider`.

---

### Módulo G: Cierres Contables y Laborales

#### G.1 Ejecutar Cierre
- **Método / Ruta:** `POST cierres/ejecutar`
- **Payload:**
```json
{
  "hoja": "Horas_Octubre_2026",
  "tipo": "horas",
  "aplicar_descuento": false
}
```
- **Respuesta Exitosa (`HTTP 200 OK`):**
```json
{
  "resumen": "Cierre laboral de Octubre completado. Total: 168.0 horas trabajadas.",
  "pdf_url": "/api/v1/cierres/reportes/cierre_octubre_2026.pdf",
  "total_horas": 168.0,
  "total_monto": 5880000,
  "tipo": "horas"
}
```

#### G.2 Historial de Cierres
- **Método / Ruta:** `GET cierres`
- **Respuesta Exitosa:**
```json
[
  {
    "id": "cierre-2026-10",
    "tipo": "horas",
    "periodo": "Octubre 2026",
    "fecha": "31/10/2026",
    "total_monto": 5880000,
    "total_horas": 168.0,
    "resumen": "Período cerrado exitosamente",
    "pdf_url": "/api/v1/cierres/reportes/cierre_octubre_2026.pdf",
    "pdf_disponible": true
  }
]
```

---

### Módulo H: Soporte y Tickets de Incidencias

#### H.1 Listar, Crear y Actualizar Tickets
- `GET soporte/tickets` -> Lista de tickets.
- `POST soporte/tickets` ->
```json
{
  "asunto": "Error en sincronización de abono",
  "descripcion": "El abono a Deuda Chacomer no actualizó el saldo en la planilla",
  "origen": "app"
}
```
- `PATCH soporte/tickets/{id}` ->
```json
{
  "estado": "En proceso",
  "resolucion": null
}
```

---

## 3. Matriz de Brechas Actuales (Mobile vs Backend)

| Requerimiento | Implementado en Mobile | Estado en Backend | Acción Prioritaria para Backend |
|---|---|---|---|
| **Soporte Alias `"tipo"` en Movimientos** | ✅ `tipo` ("Ingreso"/"Gasto") | ⚠️ Esperaba `"movimiento"`, descartaba `tipo` | Configurar `alias="tipo"` en `MovimientoCreate` de Pydantic. |
| **Deduplicación de `saldo_inicial`** | ✅ Defensivo / Cache Room | ❌ Duplicación 200% en `/dashboard/summary` | Corregir cálculo en `financiero.py:407` y `blueprint.py:105`. |
| **Estructura Plana en Deuda Foco** | ✅ Desempaqueta `deuda_foco` si existe | ⚠️ Retorna anidado `{criterio, deuda_foco: {}}` | Retornar campos planos o mantener compatibilidad con ambos. |
| **Marcación Repetida HTTP 200** | ✅ Maneja `"tipo": "info"` | ❌ Retornaba `HTTP 400 Bad Request` | Enviar `HTTP 200` con `tipo="info"` en vez de excepción 400. |
| **Claves Simétricas en IA OCR** | ✅ Espera `"tipo"` | ⚠️ Retorna `"tipo_movimiento"` | Enviar ambas claves en el JSON de respuesta. |
| **Invalidación Reactiva de Caché** | ✅ Refresca UI tras mutación | ❌ TTL de 180s en `_sheets_cache` | Disparar `_sheets_cache.clear()` tras `POST /movimientos` y `POST /obligaciones/abonar`. |
| **Realineación `Obligaciones_Maestro`** | ✅ Normalización defensiva con `estadoReal` | ❌ Columnas desplazadas en Google Sheets | Script de migración en Python para reinsertar columna `Tipo`. |

---

## 4. Oportunidades de Mejora de la API para Mayor Interactividad

Para que la app móvil ofrezca una experiencia más reactiva, rápida y offline-first, se recomienda incorporar las siguientes capacidades en la API:

### 4.1 Endpoint de Hidratación Global (`GET /api/v1/bootstrap`)
- **Problema:** Al abrir la app, Android debe disparar 4 llamadas concurrentes (`/health`, `/dashboard/summary`, `/avisos`, `/obligaciones/vencimientos`).
- **Mejora:** Un endpoint unificado `GET /api/v1/bootstrap` que devuelva en un solo round-trip:
  ```json
  {
    "health": { ... },
    "summary": { ... },
    "proximos_avisos": [ ... ],
    "proximos_vencimientos": [ ... ],
    "periodo_actual": { ... }
  }
  ```
  Esto reduce el tiempo de arranque de la app en conexiones 4G/LTE de 2.5s a menos de 500ms.

### 4.2 Endpoint de Validación y Resolución SIFEN (`POST /api/v1/sifen/consultar-ruc`)
- **Problema:** El escáner QR KuDE solo extrae el RUC del emisor (ej. `80012345-6`), pero no el nombre de fantasía de la empresa (ej. "Supermercados Stock").
- **Mejora:** Exponer un endpoint en el backend que reciba el RUC y consulte la base local o el padrón de la DNIT/SET para devolver la **Razón Social / Proveedor**, autocompletando el campo en la app móvil.

### 4.3 Sincronización Diferencial / Delta (`GET /api/v1/movimientos?since={timestamp}`)
- **Problema:** A medida que crezca el `Libro_Diario`, descargar todas las transacciones de Google Sheets será cada vez más costoso.
- **Mejora:** Permitir consultar transacciones por rango de fechas o desde el último `timestamp` sincronizado, reduciendo la cuota de la API de Google Sheets y el consumo de datos móviles.

### 4.4 Esquema Unificado de Errores
- Adoptar una estructura estándar para errores (`HTTP 4xx` / `HTTP 5xx`):
  ```json
  {
    "error": true,
    "codigo": "SHEETS_RATE_LIMIT",
    "mensaje": "Se excedió la cuota de lectura de Google Sheets. Intente en 30 segundos.",
    "detalles": null
  }
  ```
  Esto permite a la app móvil mostrar Snackbars con mensajes de ayuda específicos en lugar de errores genéricos.
