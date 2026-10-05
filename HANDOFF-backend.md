# Instrucciones de Implementación Backend (Gamma Mobile)

Este documento es el traspaso (handoff) para el agente que implementará la API del cliente móvil dentro de `Gamma_bot`.

## 1. Contexto del Proyecto
Se está desarrollando **Gamma Mobile**, una app Android que consumirá una API REST desde `Gamma_bot`. El servidor corre en **PythonAnywhere**, que **solo soporta WSGI (1 worker, síncrono)** y tiene un límite de tiempo por petición. 

Por lo tanto, la API no se implementará en FastAPI. Se debe implementar como un **Blueprint de Flask** dentro de la aplicación existente (`flask_app.py`), usando `pydantic` para la validación de contratos (DTOs).

## 2. Requisitos Generales de Arquitectura
*   **Blueprint:** Crear el blueprint `/api/v1` dentro de `flask_app.py` o importarlo desde una carpeta `api/`.
*   **Autenticación:** Proteger todos los endpoints con el header `X-API-KEY`. El valor se lee de la variable de entorno `X_API_KEY`. Comparar usando `hmac.compare_digest` y `hashlib.sha256` para evitar timing attacks. Si es inválido, retornar HTTP 401.
*   **Sincronía Estricta:** Todo el código debe ser compatible con un entorno WSGI sin hilos en background. Los cálculos pesados (OCR, IA, PDF) deben tener timeouts configurados (ej: en las llamadas a Gemini).
*   **Zona Horaria:** Se debe cambiar la zona horaria en todo el código de `America/Buenos_Aires` a `America/Asuncion` usando `zoneinfo` o `pytz`.
*   **Idempotencia:** En `POST /api/v1/movimientos`, soportar un `client_id` (UUID) opcional para evitar duplicados si la app reintenta la petición.
*   **Salida de Errores:** Todos los errores HTTP y excepciones deben manejarse devolviendo un JSON estándar: `{"error": {"code": "HTTP_STATUS", "message": "Descripción amigable", "detail": "Opcional"}}`.
*   **Multiusuario (Futuro):** Diseñar asumiendo un solo usuario (leer `SPREADSHEET_ID` del entorno, no hardcodeado), pero mantener las funciones desacopladas para que luego se pueda pasar el ID de la hoja según el usuario.

## 3. Endpoints a Implementar y Corregir

A continuación, los cambios específicos sobre la lógica existente (`logic/financiero.py` y `logic/logic.py`):

### 3.1. Finanzas (`/api/v1/movimientos`, `/api/v1/dashboard/summary`)
*   **Dashboard:** Modificar `obtener_balance()` (o crear uno nuevo) para que retorne no solo ingresos/gastos, sino también `margen_libre`, `saldo_acumulado` y `posicion_iva`. (Margen libre = ingresos fijos - gastos obligatorios del presupuesto - cuotas activas).
*   **Movimientos (GET):** Retornar los campos de IVA reales (`monto_gravado`, `monto_iva`, `clasificacion_iva`, `tasa_iva`), que actualmente están hardcodeados o ignorados al leer de la hoja.
*   **Movimientos (POST):** Recibir la `tasa_iva` ("10%", "5%", "Exento") y hacer el cálculo real del neto e IVA para guardarlo en la hoja. Asegurar que el `mes` guardado esté en español, independientemente del locale del servidor.
*   **OCR (`POST /api/v1/movimientos/ocr`):** Endpoint que recibe un archivo de imagen, llama a `analizar_ticket_con_ia()` y devuelve la propuesta en JSON (sin persistir en Sheets).

### 3.2. Obligaciones y Presupuesto
Actualmente, los endpoints de obligaciones en el código viejo devuelven mocks (`[]`).
*   **Hojas Nuevas:** Se debe crear (si no existen) las pestañas `Obligaciones_Maestro` y `Presupuesto_Base` en la hoja de cálculo.
*   **GET/POST /obligaciones:** Leer y escribir proyectos y deudas en `Obligaciones_Maestro`.
*   **POST /obligaciones/abonar:** Descuenta el `saldo_actual` de la obligación y además inserta un Gasto en el `Libro_Diario` por el monto abonado. Si el saldo llega a 0, cambiar el estado a `Saldado`.
*   **POST /obligaciones/simular:** Proyectar la cuota mensual contra el margen libre e indicar si es Viable, Ajustado o Riesgo.

### 3.3. Avisos
*   La app seguirá consumiendo `GET/POST/DELETE /api/v1/avisos`.
*   El backend debe usar **Google Calendar** como fuente de verdad (lo que ya hace el bot con `obtener_lista_avisos_calendar`), pero usar `avisos.json` como caché en disco para responder rápido a la app. Al hacer POST o DELETE, actualizar Calendar y refrescar el caché json.

### 3.4. Asistencia y Cierres (Módulos Nuevos en API)
Exponer la funcionalidad existente de `logic.py` en la API:
*   `POST /api/v1/asistencia/marcar`: Llama a `ejecutar_marcado_api(modo)`.
*   `POST /api/v1/asistencia/materia`: Llama a `marcar_asistencia()`.
*   `POST /api/v1/cierres/ejecutar`: Ejecuta `ejecutar_cierre_periodo_manual()` y luego `generar_reporte_pdf(descuento)`. Devuelve el resumen y la URL del PDF.
*   `GET /api/v1/cierres/reportes/<filename>`: Servir estáticamente el archivo PDF generado en la carpeta `reportes/`.

### 3.5. Sistema y Soporte
*   `GET/POST /api/v1/soporte/tickets`: Crear y listar incidencias. Guardarlas en una nueva pestaña `Soporte_Tickets`.
*   `GET /api/v1/health`: Devolver estado ok, verificar si hay conexión a Sheets y Calendar, y retornar la versión.

## 4. Pruebas y Validación
*   Escribir tests usando `pytest` probando los endpoints del blueprint.
*   **Crucial:** Utilizar fixtures y mockear `gspread` (MagicMock) y `google.genai` para que los tests corran en CI sin tocar la hoja de producción y sin requerir la API de Gemini.
*   No usar `git add .`. Añadir los archivos uno por uno y hacer commits atómicos.
