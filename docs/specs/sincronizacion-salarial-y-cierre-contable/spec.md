# Especificación de Requerimiento (Spec)
## Módulo: Sincronización Salarial, Cierre Contable y Sincronización de Tickets GitHub

- **Fecha:** 15 de Octubre de 2026
- **Versión de Especificación:** 1.0.0
- **Repositorios involucrados:** `Gamma_bot` (Backend Core & API) y `GammaMobile` (Android Compose)
- **Fase:** 0 (Especificación y Arquitectura)

---

## 1. Contexto y Justificación

Tras la estabilización del ciclo de tickets en Android y la normalización de estados de deudas en v0.0.5, quedaron tres pendientes heredados críticos en el core financiero y operativo del ecosistema:
1. **Desconexión entre Libro Diario y Presupuesto Base:** Cuando un usuario registra el cobro de su salario o sueldo en `Libro_Diario`, la hoja `Presupuesto_Base` (`Monto_Mensual_Gs`) no se actualiza automáticamente. Aunque el ticket TCK-004 mitigó el cálculo del margen libre mediante un fallback dinámico, la arquitectura requiere que la base presupuestaria se sincronice de manera reactiva e idempotente.
2. **Fragilidad en el Cierre Contable Mensual (TCK-005):** El comando de cierre contable (`execute_monthly_closing`) transfiere saldos y archiva períodos, pero carece de validación previa de consistencia contable (balance cuadrado entre ingresos, egresos, impuestos y saldo transferido), lo que puede generar discrepancias al resetear `Libro_Diario` para el siguiente mes.
3. **Desincronización de Soporte con GitHub Issues:** Los tickets generados o atendidos en la app y el bot no sincronizan su estado de apertura/cierre con el repositorio de GitHub Issues (`KEVIN11000/Gamma_Mobile`), obligando a la duplicación manual del seguimiento administrativo.

---

## 2. Alcance (In-Scope)

1. **Pipeline de Auto-Sincronización Salarial (`Libro_Diario` -> `Presupuesto_Base`):**
   - Detección reactiva en `registrar_movimiento` / `insert_movimiento_diario`: Si el movimiento es de tipo `Ingreso` y su categoría o concepto corresponde a `"Salario"` o `"Sueldo"`.
   - Lógica de actualización o inserción idempotente en `SheetsRepository`:
     * Si ya existe una fila de salario en `Presupuesto_Base`, actualizar su `Monto_Mensual_Gs`.
     * Si no existe, insertar una nueva fila con `Tipo_Flujo="Ingreso Fijo"`, `Categoria="Salario"`, `Concepto="Sueldo Mensual"`, `Tipo_Ingreso_Gasto="Fijo"`.
2. **Validación y Robustecimiento de Cierre Contable Mensual (`execute_monthly_closing`):**
   - Validación de balance previo al corte: Verificar que la sumatoria de débitos/créditos y flujo neto coincida con los movimientos del mes antes de ejecutar el reset.
   - Manejo de excepciones y reversibilidad: Si el backup a Google Drive o la escritura en `Cierres_Historicos` falla, el contenido de `Libro_Diario` no debe ser borrado ni alterado.
   - Generación de resumen de balance verificado y retorno estructurado de métricas.
   - Cierre del ticket de soporte `TCK-005`.
3. **Sincronización Bidireccional de Tickets con GitHub Issues:**
   - Módulo `GitHubIssueSyncService` en `Gamma_bot` utilizando `GITHUB_TOKEN` y PyGithub o requests autenticados hacia `KEVIN11000/Gamma_Mobile`.
   - Al crear un ticket en la app (`POST /api/v1/soporte/tickets`), abrir automáticamente un issue en GitHub y enlazar su número.
   - Al cambiar el estado a `"Cerrado"` (`PATCH /api/v1/soporte/tickets/<id>`), cerrar el issue correspondiente en GitHub con un comentario de resolución.
   - Endpoint o job de reconciliación para sincronizar tickets cerrados en GitHub hacia `tickets.json`.

---

## 3. Fuera de Alcance (Out-of-Scope)

- Migración a motor de base de datos SQL relacional externo; se preserva la infraestructura basada en Google Sheets y persistencia JSON.
- Recepción de Webhooks sin autenticación o en túneles locales no certificados.
- Cierre contable de períodos fiscales multianuales consolidados (solo cortes mensuales).

---

## 4. Criterios de Aceptación (Gherkin)

### Escenario 1: Sincronización salarial automática tras ingreso
- **Dado** que `Presupuesto_Base` tiene configurado un salario previo de 4.000.000 Gs o está vacío,
- **Cuando** se registra un ingreso en `Libro_Diario` con categoría `"Salario"` por monto de 5.500.000 Gs,
- **Entonces** `Presupuesto_Base` actualiza la fila salarial a 5.500.000 Gs de forma atómica y sin duplicar filas.

### Escenario 2: Validación de balance en cierre mensual
- **Dado** un mes con movimientos en `Libro_Diario` con ingresos de 6.000.000 Gs y egresos de 2.000.000 Gs,
- **Cuando** se solicita el cierre contable del período,
- **Entonces** el sistema valida que el margen libre y saldo transferido coincidan exactamente, realiza el backup en CSV/Drive, registra la fila en `Cierres_Historicos` y genera la fila de saldo inicial en `Libro_Diario` sin pérdida de información.

### Escenario 3: Sincronización de tickets con GitHub Issues
- **Dado** un ticket de soporte con ID `TCK-005` y número de issue asociado en GitHub,
- **Cuando** el administrador atiende el ticket marcándolo como `"Cerrado"` vía `PATCH /api/v1/soporte/tickets/TCK-005`,
- **Entonces** el issue asociado en GitHub se cierra automáticamente con la resolución provista y `tickets.json` queda auditado.
