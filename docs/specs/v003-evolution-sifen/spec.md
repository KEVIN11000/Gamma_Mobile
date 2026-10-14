# Feature Specification: Evolución v0.0.3 - SIFEN QR, Tipado PYG y Consolidación Ergonómica

- **Feature**: `v003-evolution-sifen`
- **Fecha**: 2026-10-14
- **Estado**: Propuesta de Planificación (Fase 0 SDD)
- **Origen**: Informes de Asesoría Estratégica Multi-Agente ([Alex - PM](file:///E:/PG/vcp-s/GammaMobile/docs/advisory/product_perspective.md) & [Mobile App Builder](file:///E:/PG/vcp-s/GammaMobile/docs/advisory/mobile_ux_perspective.md))

---

## 1. Visión y Propósito del Producto

Consolidar a **Gamma Mobile** como la herramienta financiera definitiva para personas físicas y microempresas (PyMEs) en Paraguay, combinando:
1. **Cero fricción en captura contable**: Escaneo instantáneo de facturas electrónicas de la DNIT mediante códigos QR SIFEN (50ms, offline, sin consumo de tokens de IA).
2. **Integridad financiera enterprise**: Prevención absoluta de desbordamiento aritmético en la moneda local guaraní (PYG ₲) migrando todos los campos monetarios de `Int` a `Long`.
3. **Resiliencia offline**: Preservación permanente del Libro Diario en Room (SQLite local), eliminando el borrado erróneo tras sincronización y asegurando disponibilidad 100% sin conexión.
4. **Ergonomía y claridad mental**: Reducción de la sobrecarga cognitiva en la interfaz agrupando la navegación inferior de 6 a 4 pestañas nucleares.
5. **Avisos garantizados**: Gestión de permisos `POST_NOTIFICATIONS` para Android 13+ (API 33+).

---

## 2. Alcance (In-Scope)

### Epic 1: Migración de Tipos Monetarios Seguros (PYG Long)
- Cambiar todos los montos de `Int` a `Long` en:
  - `Movimiento` y `MovimientoEntity` (`total`, `montoNeto`, `montoIva`).
  - `Obligacion` y `ObligacionEntity` (`montoInicial`, `saldoActual`).
  - DAOs, Repositorios y ViewModels (`TransaccionesViewModel`, `DashboardViewModel`, `DeudasViewModel`).
  - Utilidades de formateo de moneda (`formatGuarani(Long)` con separador de miles por punto `1.500.000 ₲`).

### Epic 2: Corrección de Persistencia Offline en Room
- Eliminar el llamado `movimientoDao.deleteBySyncStatus(SyncStatus.SYNCED)` en `MovimientoRepositoryImpl.kt`.
- El repositorio debe actualizar el `syncStatus` a `SYNCED` sin purgar los registros de la base de datos local.
- Permitir consulta y filtrado de todo el histórico de transacciones aun cuando el dispositivo esté en Modo Avión o sin conexión a internet.

### Epic 3: Notificaciones y Permisos Android 13+
- Declarar el permiso `android.permission.POST_NOTIFICATIONS` en `AndroidManifest.xml`.
- Implementar diálogo educativo / modal de solicitud de permiso en Compose antes de agendar el primer aviso en `AvisosScreen`.
- Asegurar que `AlarmManager` y `NotificationManager` despierten el dispositivo en la fecha agendada sin ser bloqueados por el SO.

### Epic 4: Módulo de Escaneo QR SIFEN (DNIT Paraguay)
- Integrar biblioteca de escaneo de cámara rápida (ML Kit Barcode Scanning + CameraX o ZXing Android Embedded).
- Parser de URLs oficiales de consulta SIFEN (`https://sifen.set.gov.py/de/consulta?cdc=...` o `https://ekuatia.set.gov.py/...`):
  - Extracción de CDC (Código de Control de 44 dígitos).
  - RUC emisor / Proveedor.
  - Fecha de emisión.
  - Importe total, IVA 10% e IVA 5%.
- Pantalla / Hoja de confirmación (BottomSheet) para validar y guardar en Libro Diario con un solo toque.

### Epic 5: Rediseño Ergonómico de Navegación (4 Pestañas)
- Reorganizar el `NavigationBar` de 6 destinos a 4 núcleos:
  1. **Dashboard**: Balance acumulado, Margen Libre, Posición IVA, tarjeta de estado de sincronización.
  2. **Movimientos**: Libro Diario unificado con histórico, filtros por tipo (Ingresos/Gastos), búsqueda y botón flotante (+) con opciones de Carga Manual / Escaneo QR / Audio IA.
  3. **Compromisos**: Pestaña unificada con selector superior de pestañas (Tabs):
     - Subpestaña A: *Obligaciones / Deudas* (cuotas pendientes, saldos).
     - Subpestaña B: *Avisos y Vencimientos* (calendario y recordatorios).
  4. **Ajustes**: Sistema, URL/Key del servidor, estado de conexión, versión compilada y acceso a tickets de soporte con feedback en tiempo real.

---

## 3. Fuera de Alcance (No-Scope)

- Conexión directa SOAP con los web services de firma digital de SIFEN (requiere certificado digital tributario en dispositivo, fuera del alcance de la app cliente).
- Pasarelas de pago o débitos bancarios automáticos (SIPAP/BANCARD).
- Versión para iOS (enfoque 100% nativo Android Kotlin / Jetpack Compose).

---

## 4. Requerimientos Funcionales y Criterios de Aceptación

### RF-01: Registro de Montos Superiores a ₲ 2.147.483.647
- **Dado** que un usuario registra una compra de inmueble o maquinaria por `₲ 3.500.000.000`.
- **Cuando** el movimiento es guardado y visualizado en Diario o Dashboard.
- **Entonces** el sistema almacena el valor sin excepciones de desbordamiento (`ArithmeticException` o valores negativos) y se muestra correctamente formateado como `3.500.000.000 ₲`.

### RF-02: Disponibilidad Offline Total
- **Dado** que el dispositivo no tiene acceso a internet.
- **Cuando** el usuario ingresa a la pestaña Movimientos o Dashboard.
- **Entonces** el usuario puede visualizar todas las transacciones históricas sincronizadas previamente en Room sin mensajes de error bloqueantes ni listas vacías.

### RF-03: Escaneo de Factura DNIT SIFEN
- **Dado** que el usuario apunta la cámara al QR de una factura legal paraguaya.
- **Cuando** el código QR es detectado.
- **Entonces** en menos de 200 ms se extraen el RUC, fecha, total e IVA, abriendo un BottomSheet pre-llenado listo para confirmar con 1 tap.

### RF-04: Navegación Simplificada
- **Dado** que el usuario abre la aplicación.
- **Cuando** observa la barra de navegación inferior.
- **Entonces** visualiza exactamente 4 iconos claramente rotulados: *Dashboard*, *Movimientos*, *Compromisos* y *Ajustes*.

---

## 5. Requerimientos No Funcionales

- **Rendimiento**: Tiempo de escaneo y parseo de QR < 100ms.
- **Compatibilidad**: Mínimo Android 8.0 (API 26), target Android 14 (API 34).
- **Seguridad**: Cámara solo se activa bajo demanda del usuario en la pantalla de escaneo.
