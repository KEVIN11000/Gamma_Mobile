# Requisitos del Backend (Implementación Blueprint Flask)

## 1. Generalidades
- Cambiar la zona horaria a `America/Asuncion` en todas las fechas generadas por el sistema.
- Validar `X-API-KEY` de forma segura.
- Asegurar que la persistencia en Google Sheets no exceda la cuota, procesando todo dentro de los límites de timeout WSGI.
- Todo endpoint devuelve un JSON estándar o un error estandarizado `{"error": ...}`.

## 2. Dashboard y Movimientos
- `GET /dashboard/summary`: Debe calcular el Margen Libre. Para esto, necesita leer la nueva pestaña `Presupuesto_Base` (que lista gastos fijos/obligatorios). `Margen Libre = Flujo Neto - Gastos Presupuestados - Cuotas Activas de Obligaciones`.
- `GET /movimientos`: Se deben mapear y calcular correctamente las columnas de IVA de la hoja (`neto`, `iva`, etc.) para devolver `monto_gravado`, `monto_iva`, y `tasa_iva`.
- `POST /movimientos`: Debe recibir `tasa_iva` ("10%", "5%", "Exento"), hacer la matemática, y escribir el neto e IVA correctos en la hoja. Usar `client_id` para evitar inyecciones dobles por problemas de red.

## 3. Obligaciones
- Reemplazar los endpoints mock (`[]`).
- Crear pestaña `Obligaciones_Maestro` con las columnas necesarias.
- Al hacer `POST /obligaciones/abonar`, descontar del saldo en la hoja maestra y registrar un Gasto en `Libro_Diario`.

## 4. Avisos
- Usar `obtener_lista_avisos_calendar` y `guardar_aviso_calendar` (Google Calendar) como fuente real.
- Guardar una copia en `avisos.json` que actúa como caché. El endpoint `GET /avisos` siempre lee del caché, mientras que `POST` y `DELETE` escriben en Calendar y actualizan el caché.

## 5. Asistencia y Reportes (Nuevos endpoints)
- `POST /asistencia/marcar`: Reutiliza `AgenteAutonomoHoras.ejecutar_marcado_api`.
- `POST /asistencia/materia`: Reutiliza `AgenteAsistenciaMaterias.marcar_asistencia`.
- `POST /cierres/ejecutar`: Ejecuta `ejecutar_cierre_periodo_manual` y luego llama a `generar_reporte_pdf`. Devuelve el link al PDF.
- `GET /cierres/reportes/<file>`: Flask sirve el directorio estático `reportes/`.

## 6. Tickets
- Crear hoja `Soporte_Tickets`.
- Endpoints `GET` y `POST` para que el usuario deje reportes de fallos en la app.
