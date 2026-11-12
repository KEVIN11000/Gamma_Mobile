# SYSTEM_MAP — Gamma Mobile

**Versión:** 1.1.0  
**Última Actualización:** 2026-11-12  
**Estado:** Activo / Producción v0.0.14  
**Repositorio:** `Gamma_Mobile` (Android Nativo / Jetpack Compose)

---

## 1. Dominios y Módulos Arquitecturales

Gamma Mobile implementa **Clean Architecture** estructurada en capas desacopladas:

```
app/src/main/java/com/gamma/mobile/
├── data/
│   ├── local/            # Room Database (GammaDatabase v6), DAOs y Entidades
│   ├── remote/           # Retrofit (GammaApi), Interceptors, SafeApiCall, DTOs
│   └── repository/       # Implementaciones de repositorio (SSOT, Room + Remote)
├── di/                   # Inyección de dependencias (Hilt Modules)
├── domain/
│   ├── model/            # Modelos puros de dominio y contratos de negocio
│   └── repository/       # Interfaces de repositorio desacopladas
├── presentation/         # Jetpack Compose UI, ViewModels, States, Navigation
├── worker/               # Background Jobs con WorkManager (Avisos, Sync)
└── utils/ / logging/     # AppLogBuffer, formateadores de moneda, extensiones
```

---

## 2. Catálogo de Capacidades Activas por Módulo

| Módulo | Pantalla / Componente | Capacidad Funcional Activa | Archivos Ancla |
|---|---|---|---|
| **Capacidades & Toggles** | `CapacidadesRepository` / `GammaApi.getCapacidades()` | Auto-descubrimiento dinámico de endpoints y módulos (`GET /api/v1/capacidades`), caché local tolerante a caídas en SharedPreferences (`gamma_cache`) y Feature Toggles reactivos StateFlow. | `CapacidadesRepository.kt`, `CapacidadesRepositoryImpl.kt`, `CapacidadesDtos.kt` |
| **Presupuesto Base** | `PresupuestoScreen` / `PresupuestoViewModel` | Gestión de salario base presupuestado, gastos fijos desglosados por categoría con modales de alta/edición/baja, y monitor en vivo de techos para gastos variables con semáforos (Verde $\le 80\%$, Amarillo $80-100\%$, Rojo $> 100\%$). | `PresupuestoScreen.kt`, `PresupuestoViewModel.kt`, `PresupuestoRepositoryImpl.kt` |
| **Dashboard** | `DashboardScreen` / `DashboardViewModel` | Resumen financiero mensual desacoplado (`resumen_financiero` / `summary`), Margen Libre, Saldo Acumulado, Posición IVA, carrusel de avisos, Flujo de Caja 30D, Clima laboral y selectores ergonómicos con foco visual. | `DashboardScreen.kt`, `DashboardViewModel.kt`, `BootstrapDto.kt` |
| **Transacciones** | `TransaccionesScreen` / `TransaccionesViewModel` | Libro diario de movimientos continuo: agrupación por fecha, chips estandarizados (Todos, Gastos, Ingresos con borde primario e icono Check), selector de rango de fechas y exportación PDF local. | `TransaccionesScreen.kt`, `TransaccionesViewModel.kt`, `MovimientoDao.kt` |
| **Carga Inteligente** | `NuevaTransaccionScreen` | Registro asistido por doble motor IA (`POST /api/v1/movimientos/parse-nlp` + Gemini On-Device) y escaneo OCR KuDE SIFEN con resolución automática de RUC. | `NuevaTransaccionScreen.kt`, `OcrScanner.kt` |
| **Obligaciones** | `ObligacionesScreen` / `ObligacionesViewModel` | Gestión de pasivos con blindaje contable de deudas vivas (`saldoActual > 0L`), cálculo auditado de cuotas abonadas reales, modal de edición integral (`PUT /api/v1/obligaciones/{id}`) y amortización en `Libro_Diario`. | `ObligacionesScreen.kt`, `ObligacionesViewModel.kt`, `ObligacionRepositoryImpl.kt`, `Models.kt` |
| **Capacidad de Pago** | `CapacidadPagoScreen` / `CapacidadPagoViewModel` | Proyector de capacidad de pago con semáforo de viabilidad (Superávit, Ajustado, Déficit), sliders de abono mensual y soporte de imprevistos/gastos ad-hoc. | `CapacidadPagoScreen.kt`, `CapacidadPagoViewModel.kt` |
| **Avisos Contables** | `AvisosScreen` / `AvisosViewModel` | Registro y consulta de hitos y recordatorios contables en `avisos.json` con días restantes y alarmas locales periódicas. | `AvisosScreen.kt`, `AlarmaAvisosWorker.kt`, `AvisoDao.kt` |
| **Asistencia Laboral** | `AsistenciaScreen` / `AsistenciaViewModel` | Reloj biométrico de control laboral: marcación de entrada/salida (Normal/Directo con chips ergonómicos), periodos mensuales, analíticas y reportes PDF/Excel. | `AsistenciaScreen.kt`, `AsistenciaViewModel.kt` |
| **Cierres Históricos** | `HistorialCierresScreen` / `CierresViewModel` | Visualización del historial de balances consolidados mensuales con desglose fidedigno (`Total Facturado / Ingresos:`) y descarga/apertura de comprobantes PDF desde caché local. | `HistorialCierresScreen.kt`, `CierresViewModel.kt`, `CierreDao.kt` |
| **Soporte & Feedback** | `TicketsScreen` / `TicketsViewModel` | Registro y seguimiento de tickets de incidencia (`TCK-xxx`) con prioridades, estados y trazabilidad. | `TicketsScreen.kt`, `TicketsViewModel.kt` |
| **Sistema & Logs** | `SistemaScreen` / `SistemaViewModel` | Configuración dinámica de API con `EncryptedSharedPreferences`, diagnóstico de salud, actualización in-app y Card de logs plegable/expandible interactivo. | `SistemaScreen.kt`, `SistemaViewModel.kt`, `AppLogBuffer.kt` |
| **Auto-Update** | `GitHubReleaseApi` / `InAppUpdate` | Comprobación semver directa contra GitHub Releases y descarga de APK oficial en segundo plano con notificación. | `GitHubReleaseApi.kt`, `UpdateReceiver.kt` |

---

## 3. Invariantes Críticos No Negociables (*Invariants*)

Los siguientes principios operativos y contratos técnicos tienen **prohibido ser alterados o vulnerados** en cualquier refactorización o nueva característica:

* **[#INV-01] Localización Monetaria Guaraní (PYG / ₲):**  
  Todos los montos en moneda son enteros sin decimales (`Long` en Kotlin, `int` en JSON). Los formatos visuales deben usar separador de miles con punto (`#.##0 ₲`), renderizado monolínea (`maxLines = 1, softWrap = false`) y alineación simétrica para prevenir desbordamientos o wrap de texto.

* **[#INV-02] Blindaje Contable de Deuda Viva:**  
  Una deuda con `saldoActual > 0L` jamás puede considerarse saldada (`esDefinitivamenteSaldada = false`), independientemente de desajustes, errores de digitación o textos espurios (`"Saldada"`) en Google Sheets. Las cuotas pagadas y montos amortizados se computan fidedignamente mediante `cuotasPagadasReal` y `montoAbonadoReal`.

* **[#INV-03] Resolución Global de Deuda Foco:**  
  El lookup de la obligación al accionar *"Abonar Cuota"* o *"Pausar este mes"* en la tarjeta de Deuda Foco debe realizarse sobre la colección completa (`todasLasObligaciones`), sin depender de la pestaña de filtro que se encuentre activa en ese momento.

* **[#INV-04] Zona Horaria Oficial `America/Asuncion`:**  
  Toda fecha, marca temporal, cálculo de días restantes o alerta debe procesarse bajo la zona horaria paraguaya oficial (UTC-3 / UTC-4).

* **[#INV-05] Resiliencia Offline-First (SSOT en Room / Cache):**  
  Toda pantalla debe leer primariamente desde Room Database o SharedPreferences locales (`gamma_cache`). Los fallos o caídas de red nunca deben vaciar ni resetear los datos locales cacheados (prohibido el *destructive loading*).

* **[#INV-06] Inmutabilidad Criptográfica de Distribución:**  
  Los workflows de release en CI/CD (`release.yml`) y builds de producción exigen verificación criptográfica obligatoria con `apksigner verify --verbose --print-certs` contra la huella SHA-256 oficial (`2db8d51c88ad21c640a3bffb9fe04c50603e81182f8e1b7e54b3a568a580ffe7`) y DN `CN=Gamma Mobile, OU=Release, O=Gamma, L=Asuncion, ST=Central, C=PY`. Queda estrictamente prohibido usar debug keystores o firmas efímeras como fallback.

* **[#INV-07] Seguridad de Credenciales y Secretos:**  
  La `X-API-KEY` y las configuraciones de red dinámicas se almacenan exclusivamente en `EncryptedSharedPreferences` mediante `MasterKey` AES-256. La URL base por defecto (`DEFAULT_BASE_URL`) se inyecta en tiempo de compilación vía `BuildConfig.DEFAULT_BASE_URL` desde `local.properties` / secretos de CI (`GAMMA_API_BASE_URL`) con fallback neutro (`https://api.gamma.example.com/api/v1/`), y `android:allowBackup="false"` protege la base de datos local contra extracciones ADB. Ninguna clave, contraseña ni URL privada de producción debe versionarse en el código fuente.

* **[#INV-08] Auto-descubrimiento Dinámico de Capacidades:**  
  El cliente móvil negocia dinámicamente sus capacidades con el backend vía `GET /api/v1/capacidades`. Si un módulo o endpoint no está soportado en una versión de servidor antigua, la app oculta o deshabilita la opción de forma segura sin arrojar excepciones ni degradar las pantallas centrales.

---

## 4. Archivos Ancla y Suites de Prueba

Las suites de pruebas que custodian estos invariantes y deben ejecutarse obligatoriamente con `./gradlew testDebugUnitTest` son:

| Invariante Custodiado | Archivos Ancla de Código | Suite de Prueba Asociada |
|---|---|---|
| [#INV-01] Localización Monetaria | `FormatUtils.kt`, `TransaccionesScreen.kt` | `FormatUtilsTest.kt` |
| [#INV-02] Blindaje de Deuda Viva | `Models.kt`, `ObligacionRepositoryImpl.kt` | `ObligacionModelTest.kt`, `ObligacionesViewModelTest.kt`, `ObligacionRepositoryTest.kt` |
| [#INV-03] Deuda Foco Global | `ObligacionesScreen.kt`, `ObligacionesViewModel.kt` | `ObligacionesViewModelTest.kt` |
| [#INV-04] Zona Horaria Asunción | `DateUtils.kt`, `AvisoUiMapper.kt` | `DateUtilsTest.kt`, `AvisosViewModelTest.kt` |
| [#INV-05] Resiliencia Room/Cache | `GammaDatabase.kt`, `*RepositoryImpl.kt` | `RoomDatabaseTest.kt`, `CapacidadesRepositoryTest.kt`, `PresupuestoRepositoryTest.kt` |
| [#INV-06] CI/CD Criptográfico | `.github/workflows/release.yml`, `app/build.gradle.kts` | `release.yml`, `apksigner.bat` audit |
| [#INV-07] Seguridad Credenciales | `SecureStorageRepositoryImpl.kt`, `ApiKeyInterceptor.kt` | `SecureStorageTest.kt`, `NetworkModuleTest.kt` |
| [#INV-08] Auto-descubrimiento | `CapacidadesRepositoryImpl.kt`, `PresupuestoViewModel.kt` | `CapacidadesRepositoryTest.kt`, `PresupuestoViewModelTest.kt` |

---
*Fin del Mapeo del Sistema Gamma Mobile.*
