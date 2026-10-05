# Spec — Gamma Mobile (app Android + contrato de backend)

Estado: BORRADOR — pendiente de aprobación (Gate Fase 0)

## 0. Restricción de carpeta
Todo el trabajo se hace **dentro de `GammaMobile/`**. No se modifica `Gamma_bot` desde este proyecto. El backend se entrega como **especificación ejecutable** en `GammaMobile/backend-spec/` (OpenAPI, requisitos, fixtures JSON y tests de contrato). La implementación real en `Gamma_bot` la aplica el usuario u otra sesión usando esa especificación.

## 1. Objetivo
Reconstruir desde cero la app Android **Gamma Mobile** para consumir la API del servidor, cubriendo las 5 divisiones del bot de Telegram. Los problemas del backend actual (obligaciones mock, dashboard incompleto, IVA sin calcular, zona horaria errónea, sin endpoints de asistencia/cierres/soporte) son requisitos obligatorios en `backend-spec/`; la app se desarrolla contra ese contrato.

## 2. Usuario, distribución y versión
- **Un solo usuario (tú) en v0.0.1.** Multiusuario (cuentas y hojas por usuario) queda para una versión futura: el diseño no lo impide, pero no se implementa ni se configura ahora.
- Distribución: **APK directo**; firma release con keystore local fuera del repo.
- Versión: `versionName = 0.0.1+<hash-corto-commit>` (git en Gradle); `versionCode` = `git rev-list --count HEAD`. Sin git: `0.0.1+nogit`.
- minSdk 26, targetSdk 35.

## 3. Autenticación
- Una única API key (`X-API-KEY`) guardada en variable de entorno del servidor.
- La app la guarda cifrada (EncryptedSharedPreferences). Primera pantalla: URL del servidor + API key, validadas con `POST /api/v1/auth/verify`.
- Como hay un solo usuario, no hay roles: logs/debug quedan disponibles porque el usuario es el admin.

## 4. Restricción de hosting (PythonAnywhere, solo WSGI)
El servidor solo ejecuta un worker WSGI, sin ASGI, sin hilos/procesos persistentes y con timeouts por request. Consecuencias de diseño:
- La API se implementa como **Flask Blueprint** dentro del `flask_app.py` existente (validación con Pydantic); no FastAPI.
- Todos los endpoints son síncronos, rápidos y sin trabajo en segundo plano. Las operaciones largas (Gemini, OCR, PDF) tienen timeout explícito y la app muestra estado de progreso y error/reintento.
- Un solo worker procesa de a una request: la app no dispara llamadas en paralelo masivas (ráfagas serializadas, caché local, refresco bajo demanda).
- Sin push ni websockets: los avisos se calculan en el dispositivo.
- Tareas periódicas en el servidor, si hicieran falta, usan Scheduled Tasks de PythonAnywhere.

## 5. Alcance funcional (5 divisiones = 5 pestañas)
Navegación: barra inferior de 5 pestañas, tema claro/oscuro, español, Guaraníes (`#,##0 ₲`), zona `America/Asuncion`.

### 5.1 Finanzas y Presupuestos (Fase A)
- **Dashboard:** margen libre, saldo acumulado, posición IVA (débito/crédito), ingresos, gastos y flujo neto del mes.
- **Eventos próximos en el dashboard:** tarjeta "Esta semana" con los avisos de los próximos 7 días, ordenados por fecha ascendente (más urgente primero). Cada ítem muestra título, fecha/hora y "faltan N días"/"hoy"/"mañana". Los de ≤1 día se resaltan. Si no hay: estado vacío "Sin eventos esta semana". Tocar un ítem abre la pestaña Avisos. Hasta que exista la pestaña Avisos (Fase B), la tarjeta lee el caché local de avisos.
- Libro diario: agrupado por fecha, filtros (Todos/Gastos/Ingresos/Deudas), mes/categoría, pull-to-refresh.
- Registrar movimiento: formulario manual (IVA 10%/5%/Exento), texto libre con IA y foto de comprobante con OCR/IA con confirmar/cancelar.
- Presupuesto base: solo lectura.

### 5.2 Deudas y Proyecciones (Fase A)
- Listar obligaciones (Activo/Simulado/Saldado) con progreso; crear; aprobar; abonar (descuenta saldo, inserta gasto, pasa a Saldado en 0).
- Simulador de viabilidad contra margen libre (Viable/Ajustado/Riesgo con verde/amarillo/rojo).

### 5.3 Avisos y Agenda (Fase B)
- Listar, crear (texto libre vía IA o manual) y borrar avisos.
- Notificaciones locales (WorkManager, 08:00 America/Asuncion; hitos 30/7/5/3/1 días).

### 5.4 Asistencia y RRHH (Fase C)
- Marcar entrada/salida (normal/directo), marcar materia, cierre mensual con descuento opcional y reporte PDF.

### 5.5 Sistema y Soporte (Fase C)
- Crear/ver tickets (`TCK-xxx`), estado de salud del backend, versión de la app y logs/debug.

## 6. Requisitos de backend (entregados como `backend-spec/`)
1. Obligaciones (`GET/POST`, `aprobar`, `abonar`, `simular`) reales sobre `Obligaciones_Maestro` y `Libro_Diario` (hoy son mocks).
2. `GET /dashboard/summary` con `margen_libre`, `saldo_acumulado`, `posicion_iva`, `es_debito_fiscal` además de ingresos/gastos/flujo.
3. `GET /movimientos` con `monto_gravado`, `monto_iva`, `clasificacion_iva`, `tasa_iva` reales.
4. Zona horaria `America/Asuncion`; meses en español sin depender del locale del servidor.
5. `POST /movimientos` idempotente con `client_id`.
6. `GET /dashboard/eventos?dias=7`: avisos de los próximos N días ordenados (alternativa: filtrar en la app desde `GET /avisos`; se decide en el plan).
7. Endpoints nuevos: asistencia, cierres/PDF, tickets, health, logs, OCR.
8. Implementación como Flask Blueprint sincrónico (sección 4); `SPREADSHEET_ID` leído de entorno, sin valor hardcodeado.

## 7. Offline
- Lecturas: caché en Room; se muestra caché y se refresca con red.
- Escrituras: solo **movimientos** se encolan sin red (PENDING/SYNCED, WorkManager con backoff exponencial, `client_id` para evitar duplicados). El resto exige conexión.

## 8. Estados de UI
Loading, Error (con reintentar), Empty, Offline ("datos guardados"), 401 (volver a configurar API key).

## 9. No-Scope
- Cualquier modificación a `Gamma_bot` desde este proyecto.
- Multiusuario, roles, login usuario/contraseña, OAuth.
- Push (FCM), Play Store, FastAPI/ASGI en el servidor.
- Offline total para obligaciones, avisos, asistencia o tickets.
- Edición de presupuesto base, dictado por voz, ajustes avanzados (cambiar URL, exportar, borrar caché).
- Migrar lógica contable a Kotlin.

## 10. Criterios globales de aceptación
- `./gradlew assembleDebug` y tests unitarios pasan en terminal con `JAVA_HOME` configurado.
- La app pasa tests de contrato contra las fixtures de `backend-spec/` (MockWebServer); nunca se usan credenciales ni hojas de producción.
- QA en emulador contra un servidor mock local con las fixtures.
- La versión `0.0.1+<commit>` se ve en Sistema.
- Ningún secreto (API key, `credentials.json`, keystore) en git.
- La integración real contra PythonAnywhere queda como paso posterior, tras aplicar el backend y con tu aprobación.
