# Especificación Funcional: Integración Global de Backend v2.0.0 y Optimizaciones de Red (v0.0.13)

- **ID de Característica:** `v013-backend-v2-integracion-global`
- **Fase SDD:** 0 (Especificación y Arquitectura)
- **Fecha:** 2026-11-09
- **Cliente Objetivo:** Android (`GammaMobile` — Kotlin + Jetpack Compose + Material 3)
- **Servidor Objetivo:** Python / Flask (`Gamma_bot` — Blueprints v2.0.0)

---

## 1. Contexto y Justificación

Gamma Mobile ha alcanzado la versión `v0.0.12` con estabilidad transaccional, clasificación robusta de pasivos (Deuda Foco), reloj de asistencia laboral y pipeline de releases auditado criptográficamente.

Sin embargo, subsisten capacidades no integradas correspondientes a la **Sección 8 de `especificacion_gamma_mobile_android.md`** (Backend v2.0.0) y a la **Sección 4 de `ESPECIFICACIONES_API_MOBILE.md`** (Optimizaciones de API):
1. El arranque de la app ejecuta múltiples peticiones HTTP concurrentes (`/health`, `/dashboard/summary`, `/avisos`, `/obligaciones/vencimientos`), incrementando la latencia en redes móviles.
2. El Backend v2.0.0 expone un motor algorítmico de **Flujo de Caja Proyectado a 30 Días** (`/proyecciones/flujo-caja`), un **Parser Determinista NLP** (`/movimientos/parse`), **Servicio de Clima y Alerta de Paraguas** (`/clima`) y **Registro de Tokens Push** (`/dispositivos/registrar`) que actualmente no son consumidos por el cliente móvil.
3. El escaneo de facturas QR KuDE requiere autocompletar la Razón Social mediante `POST /sifen/consultar-ruc`.
4. El cliente móvil carece de políticas automáticas para `HTTP 429 Retry-After`, límites de carga preventiva e idempotencia con `X-Client-Id`.

Esta especificación unifica **todos los pendientes de los specs** en una arquitectura coherente, modular y comprobable empíricamente para la versión `v0.0.13`.

---

## 2. Alcance Detallado (In-Scope)

### Módulo 1: Hidratación Atómica de Arranque (`GET /api/v1/bootstrap`)
* Consumo del endpoint consolidado `GET bootstrap` en el inicio de la app.
* Desempaquetado atómico de: estado de salud (`health`), balance financiero (`summary`), próximos avisos (`avisos`), cronograma de vencimientos (`vencimientos`) y periodo activo de asistencia (`asistencia`).
* Persistencia en Room en una única transacción de base de datos (`@Transaction`).
* Reducción de llamadas concurrentes de arranque de 4 a 1 sola.

### Módulo 2: Flujo de Caja Proyectado a 30 Días (`GET /api/v1/proyecciones/flujo-caja`)
* Integración del endpoint de proyecciones con parámetros opcionales (`saldo_inicial`, `gasto_diario`, `umbral_reserva`).
* Modelado de datos en Kotlin: `FlujoCajaResponse`, `FlujoCajaItemDiarioDto` y `DiaCriticoDto`.
* Componente visual en Compose: Tarjeta interactiva de proyección a 30 días en el Dashboard, con alerta visual para días críticos (`saldo_final < 0` o por debajo de la reserva mínima).
* Almacenamiento local en Room para visualización sin conexión.

### Módulo 3: Parser Financiero NLP Determinista (`POST /api/v1/movimientos/parse`)
* Consumo del endpoint determinista de baja latencia ($< 5\text{ ms}$) `POST movimientos/parse`.
* Payload de entrada: `{"texto": "Gasto 50.000 combustible en Shell"}`.
* Mapeo de respuesta: `monto`, `moneda`, `tipo_movimiento`, `categoria`, `proveedor_cliente`, `confianza`.
* Integración en `NuevaTransaccionScreen` con selector rápido entre:
  * **Modo Ultrarrápido (NLP Determinista):** Respuesta inmediata para gastos cotidianos.
  * **Modo Completo (Gemini IA):** Para descripciones complejas o dictados extensos.

### Módulo 4: Widget de Clima y Alerta de Paraguas (`GET /api/v1/clima`)
* Consumo de `GET clima` al actualizar el Dashboard.
* Datos: `temperatura_actual`, `temperatura_min`, `temperatura_max`, `condicion`, `alerta_paraguas` (`activar`, `probabilidad_lluvia`, `motivo`).
* Componente visual en Compose: Banner compacto o chip en la cabecera del Dashboard indicando condición y recordatorio de paraguas si la probabilidad de lluvia es $\ge 35\%$.

### Módulo 5: Registro de Dispositivos Push (`POST /api/v1/dispositivos/registrar`)
* Contrato y DTOs para vinculación de token: `device_token`, `platform = "android"`, `alias_usuario`.
* Servicio en repositorio para sincronizar el token local con el backend al obtener o renovar credenciales.

### Módulo 6: Resiliencia de Red, Idempotencia y Cuotas (Sección 8.4)
* **Manejo de Rate Limiting (`HTTP 429`):** Interceptor OkHttp que capture la cabecera `Retry-After` (en segundos) y programe un reintento o notifique al ViewModel sin colapsar.
* **Control de Carga Preventivo (`HTTP 413`):** Validación previa al envío para adjuntos u OCR, limitando el tamaño a $< 64\text{ KB}$ o redimensionando la imagen antes de transmitir.
* **Cabecera de Idempotencia:** Inyección de `X-Client-Id: <UUID>` en mutaciones críticas (`POST /movimientos`, `POST /asistencia/marcar`, `POST /obligaciones/abonar`) para evitar duplicaciones por reintentos de conexión móvil.

### Módulo 7: Resolución SIFEN RUC KuDE y Sincronización Diferencial
* Endpoint `POST /api/v1/sifen/consultar-ruc` con body `{"ruc": "..."}` para autocompletar la **Razón Social / Proveedor** en la captura QR.
* Soporte en `GET /api/v1/movimientos` para parámetro `since={timestamp}` permitiendo sincronización incremental eficiente.

### Módulo 8: Higiene de Versión
* Actualización de [`VERSION`](file:///e:/PG/vcp-s/GammaMobile/VERSION) a `0.0.13`.

---

## 3. Fuera de Alcance (No-Scope)

* Modificaciones al código de backend Python (`Gamma_bot` en PythonAnywhere). Esta especificación implementa los clientes, adaptadores y componentes UI de Android que consumen dichos endpoints.
* Reemplazo del motor local de avisos de `WorkManager`; el registro de tokens push se prepara como canal complementario, manteniendo activas las alarmas locales existentes.

---

## 4. Casos de Uso y Criterios de Aceptación

### Caso de Uso 1: Arranque Rápido con Hidratación Atómica
* **Actor:** Usuario que abre la aplicación Gamma Mobile.
* **Flujo:**
  1. La aplicación realiza una única llamada `GET /api/v1/bootstrap`.
  2. El backend responde con el payload consolidado.
  3. Gamma Mobile actualiza simultáneamente el estado de salud, balance del dashboard, lista de avisos inminentes y cronograma de vencimientos.
* **Criterio de Aceptación:** El tiempo de carga percibido se reduce a menos de 1 segundo; ninguna de las tarjetas del Dashboard muestra estado destructivo (*destructive loading*). Si la llamada falla, la app muestra los datos cacheados en Room.

### Caso de Uso 2: Consulta de Flujo de Caja Proyectado a 30 Días
* **Actor:** Usuario en `DashboardScreen`.
* **Flujo:**
  1. En el Dashboard se muestra la tarjeta "Proyección a 30 Días" con saldo actual y saldo proyectado a 30 días.
  2. Si existen días con saldo negativo o por debajo del umbral de reserva, la tarjeta resalta en color ámbar/rojo con la fecha del día crítico y el compromiso causante.
* **Criterio de Aceptación:** El usuario puede pulsar la tarjeta para expandir la serie temporal diaria proyectada.

### Caso de Uso 3: Carga Rápida con Parser NLP Determinista
* **Actor:** Usuario que registra un gasto en lenguaje natural ("Gasto 45000 nafta shell").
* **Flujo:**
  1. El usuario tipea la frase y pulsa "Interpretar".
  2. La app envía la frase a `POST /movimientos/parse`.
  3. En $< 50\text{ ms}$, los campos Monto (`45.000 ₲`), Categoría (`Combustible`), Proveedor (`Shell`) y Tipo (`Gasto`) quedan rellenados automáticamente.
* **Criterio de Aceptación:** El formulario se precarga sin demoras perceptibles, permitiendo guardar en un solo toque.

### Caso de Uso 4: Visualización de Alerta de Paraguas y Clima
* **Actor:** Usuario que abre el Dashboard por la mañana.
* **Flujo:**
  1. La cabecera muestra la temperatura actual y condición meteorológica de Asunción.
  2. Si `alerta_paraguas.activar == true`, aparece un chip visible: *"Llevar paraguas: 60% prob. de lluvia"*.
* **Criterio de Aceptación:** El chip solo se renderiza si la alerta está activa.

---

## 5. Matriz de Impacto sobre Invariantes Existentes (`docs/SYSTEM_MAP.md`)

| Invariante en `SYSTEM_MAP.md` | Impacto de la Característica | Verificación de No-Regresión |
|---|---|---|
| **[#INV-01] Localización Monetaria Guaraní (₲)** | Los montos de flujo de caja y proyecciones deben representarse como `Long` sin decimales y con formato monolínea `#.##0 ₲`. | Verificado en DTOs y pruebas unitarias con `FormatUtils`. |
| **[#INV-02] Autoridad de Estado de Obligaciones** | El cronograma de vencimientos y flujo de caja no altera `estadoReal` ni las amortizaciones de pasivos. | Tests existentes de `ObligacionesViewModelTest` deben mantenerse 100% en verde. |
| **[#INV-03] Resolución Global de Deuda Foco** | Intacto; no se alteran los callbacks de abono global. | Cobertura en `ObligacionesViewModelTest`. |
| **[#INV-04] Zona Horaria `America/Asuncion`** | La serie temporal del flujo de caja (30 días) y las alertas meteorológicas utilizan UTC-3 Asunción. | Verificado con `DateUtils`. |
| **[#INV-05] Resiliencia Offline-First (Room)** | Toda entidad nueva (Bootstrap, FlujoCaja, Clima) debe almacenarse en Room con fallback ante ausencia de conectividad. | Pruebas de repositorio con mock sin conexión. |
| **[#INV-06] Inmutabilidad Criptográfica** | No altera el flujo de firma ni el pipeline `release.yml`. | Verificado con `./gradlew assembleRelease` y `apksigner`. |
| **[#INV-07] Seguridad de Credenciales** | La cabecera `X-API-KEY` se mantiene inalterada y las nuevas cabeceras (`X-Client-Id`) no exponen secretos. | Pruebas de interceptores de red. |

---

## 6. Contrato de Interfaz Visual y Estados de UX

* **Estado de Carga (Loading):** Se utiliza `PullRefresh` o shimmer sutil superpuesto; queda terminantemente prohibido vaciar las tarjetas existentes durante la carga.
* **Estado de Error (Error):** Snackbar informativo con el mensaje del servidor o código `HTTP 429 Retry-After`. Los datos locales en pantalla permanecen visibles.
* **Estado Vacío (Empty):** Si no hay proyecciones o alertas, la tarjeta muestra un estado sobrio *"Sin alertas críticas en los próximos 30 días"*.
* **Estado Offline:** Badge discreto *"Modo sin conexión — datos cacheados"* cuando la petición de red no responde.
