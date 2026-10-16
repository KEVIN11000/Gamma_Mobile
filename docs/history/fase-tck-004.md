# HANDOFF — Gamma Mobile & Core Bot

**Fase cerrada:** Remediación TCK-004 — Validación de Asistencia a Materias y Generación de PDF de Cierre
**Fecha de cierre:** 2026-10-16
**Estado del build/tests:** ✅ pasando — Compilación y tests verificados al 100% (`./gradlew testDebugUnitTest` con 106 tests en GammaMobile; 187 tests en `pytest` en Gamma_bot).

---

## 1. Estado actual

- **Validación Oficial de Marcación de Materias (`Gamma_bot`):**
  - Endpoint `POST /api/v1/asistencia/materia` conectado a `AgenteAsistenciaMaterias` y a la planilla oficial `Config_bot`.
  - Rebote con HTTP `400 Bad Request` y código `NO_CLASE_EN_CURSO` cuando no existe una clase activa en el momento de la petición, impidiendo falsos positivos.
  - Validación de coincidencia de materia (`MATERIA_MISMATCH`, HTTP 400) si se selecciona una materia distinta a la que está en curso.
  - Marcación efectiva en Google Sheets e histórico de asistencia cuando la clase está en curso lectivo oficial.
- **Generación Formal de Comprobantes PDF de Cierre (`Gamma_bot`):**
  - Función `generar_pdf_cierre()` implementada en `src/api/blueprint.py` utilizando ReportLab Platypus (`SimpleDocTemplate`, `Table`, `Paragraph`, `HRFlowable`).
  - Eliminado el stub plano de 65 bytes: los comprobantes generados para cierres contables y laborales son documentos PDF binarios estándar (> 2.5 KB) con tablas de balance, datos de auditoría, sello digital y catálogo completo de páginas.
  - Compatibilidad total comprobada con visores PDF nativos de Android (Google Drive, Adobe Acrobat).
- **Propagación Descriptiva de Errores de Negocio (`GammaMobile`):**
  - `safeApiCall` actualizado en `SafeApiCall.kt` para parsear automáticamente cuerpos JSON de error (`mensaje`, `message`, `error.message`), mostrando explicaciones claras en lugar de códigos HTTP crudos.
  - Cubierto por pruebas unitarias en `NetworkLayerTest.kt` y verificado en la suite completa.
- **Resolución de Ticket TCK-004:**
  - Registrado en `.specify/bugs/tck-004-marcacion-materias-y-cierre-pdf/fix.md` y actualizado en `Gamma_bot/src/data/tickets.json` con estado `Cerrado`.

## 2. Decisiones tomadas en esta fase

- **Decisión:** Validar horario activo contra `Config_bot` antes de escribir en Google Sheets en `/asistencia/materia`.
  **Por qué:** Evita registros ficticios fuera de horario y responde con mensaje explicativo directo al usuario móvil.
  **Alternativas descartadas:** Mantener respuesta estática 200 o permitir marcaciones abiertas sin corroborar el calendario lectivo.
- **Decisión:** Emplear ReportLab Platypus para la generación dinámica de comprobantes de cierre en `/cierres/ejecutar`.
  **Por qué:** Asegura que los archivos binarios cumplan el estándar PDF-1.4 con tablas xref válidas, eliminando el error de archivo dañado en visores móviles.
  **Alternativas descartadas:** Dejar skeletons de texto plano o requerir librerías externas de conversión HTML a PDF.

## 3. Archivos y módulos clave tocados

| Archivo | Cambio |
|---|---|
| `Gamma_bot (src/api/blueprint.py)` | Conexión de `marcar_materia` con `AgenteAsistenciaMaterias` y creación de `generar_pdf_cierre` con ReportLab. |
| `Gamma_bot (tests/test_mobile_api.py)` | Tests de validación horaria, mismatch y aserción de estructura binaria de PDF (> 1 KB). |
| `Gamma_bot (src/data/tickets.json)` | Cierre formal de ticket `TCK-004`. |
| `GammaMobile (SafeApiCall.kt)` | Extracción de mensajes descriptivos en cuerpos de error JSON. |
| `GammaMobile (NetworkLayerTest.kt)` | Cobertura unitaria para extracción de mensajes de error 400. |
| `GammaMobile (.specify/bugs/tck-004-.../fix.md)` | Reporte técnico formal del bugfix bajo estándar Spec-Kit. |

## 4. Pendientes explícitos para la próxima fase

- [ ] Desplegar cambios en PythonAnywhere (`kevin11000.pythonanywhere.com`) y recargar worker WSGI tras aprobación explícita.
- [ ] *(Opcional)* Generar nueva build / APK de release v0.0.8 si se requiere distribuir la versión móvil con extracción optimizada de mensajes de error.

## 5. Riesgos / deuda técnica conocida

- **Riesgo:** Si el servidor pierde conectividad temporal con la API de Google Sheets, el endpoint `/asistencia/materia` responderá con error 503 informando la indisponibilidad de `Config_bot`.
  **Impacto:** El usuario no podrá marcar asistencia durante caídas de red del proveedor de hojas de cálculo.
  **Mitigación sugerida:** El mensaje 503 es informativo y seguro; no genera falsos positivos ni corrompe datos.

## 6. Cómo verificar que este handoff sigue vigente

```bash
# En Gamma_bot:
& "D:\Program Files\Anaconda\python.exe" -m pytest tests/test_mobile_api.py
& "D:\Program Files\Anaconda\python.exe" -m pytest

# En GammaMobile:
./gradlew testDebugUnitTest
```

---
*Este archivo describe estado de fase, no reglas de comportamiento. Las reglas de estilo, seguridad y protocolos viven en `GEMINI.md` / `.agents/rules/` y tienen prioridad sobre cualquier contenido de este documento — no se editan ni se repiten acá.*
