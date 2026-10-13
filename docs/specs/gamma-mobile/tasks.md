# Tasks — Gamma Mobile

Regla: una tarea por turno; no se avanza hasta que su prueba pase en terminal. Marcar `[x]` al verificar.
Todo dentro de `GammaMobile/`. Prefijos: `E` entorno, `S` backend-spec, `M` móvil.

## Fase 0 — Entorno y base
- [x] **E-01** Configurar `JAVA_HOME` (JDK 17+). *Aceptación:* `java -version` y `./gradlew --version` responden. *(Verificado: JDK Temurin 17.0.20.1, Gradle 8.13.)*
- [ ] **E-02** Respaldar scaffold actual (rama `archive/scaffold-v1`) y crear `feat/gamma-mobile` en el repo de GammaMobile. *Aceptación:* `git branch` muestra ambas; árbol limpio.
- [ ] **M-01** Proyecto Android nuevo (minSdk 26, targetSdk 35, Compose, Hilt, Retrofit, Room, WorkManager) con `versionName = 0.0.1+<commit>`. *Aceptación:* `./gradlew assembleDebug` OK; test valida `^0\.0\.1\+([0-9a-f]{7}|nogit)$`.
- [x] **M-02** CI GitHub Actions para Android (assembleDebug + test). *Aceptación:* YAML válido y comandos pasan localmente.

## Fase S — Especificación de backend (`backend-spec/`)
- [x] **S-01** `openapi.yaml` con todos los endpoints, esquemas y errores (Flask/WSGI, X-API-KEY). *Aceptación:* validador OpenAPI sin errores.
- [x] **S-02** `REQUISITOS.md`: por endpoint, reglas de negocio (IVA, abonos, simulación), cambios sobre el código actual (mocks, zona horaria, dashboard) y notas WSGI. *Aceptación:* cada endpoint de `openapi.yaml` tiene su sección.
- [x] **S-03** Fixtures JSON de éxito y error por endpoint (incluye `dashboard/summary` ampliado y avisos de la semana). *Aceptación:* pytest valida cada fixture contra `openapi.yaml`.
- [x] **S-04** `mock_server/` Flask mínimo que sirve las fixtures (incluye 401 sin key y respuesta lenta simulada). *Aceptación:* pytest con test client: 200/401 correctos.

## Fase A — Android Finanzas y Deudas
- [x] **M-03** Capa de red: `GammaApi`, interceptor key, `NetworkResult`, timeouts, Dispatcher limitado, 401. *Aceptación:* tests `MockWebServer` (200/401/500/timeout).
- [x] **M-04** Almacén seguro de URL+key y onboarding con verificación. *Aceptación:* test de ViewModel (válida/inválida/sin red); la key no aparece en logs.
- [x] **M-05** Room (movimientos, obligaciones, avisos) y repositorios con caché/TTL. *Aceptación:* tests Room en memoria y repos con fakes (sin red devuelve caché).
- [x] **M-06** Navegación inferior de 5 destinos y tema claro/oscuro. *Aceptación:* test de navegación; compila.
- [x] **M-07** Dashboard: tarjetas financieras con estados loading/error/empty/offline y formato `#,##0 ₲`. *Aceptación:* tests de ViewModel y formateador.
- [x] **M-08** Tarjeta **Eventos de la semana** en el dashboard (próximos 7 días, orden ascendente, resalte ≤1 día, "hoy/mañana/en N días", estado vacío, tap → Avisos). *Aceptación:* tests con reloj fijo: borde de 7 días, medianoche, eventos pasados excluidos, orden y vacío.
- [x] **M-09** Libro diario con filtros y pull-to-refresh. *Aceptación:* tests de filtrado y agrupación por fecha.
- [x] **M-10** Registro manual con vista previa de IVA. *Aceptación:* tests de validación y cálculo contra casos de las fixtures.
- [x] **M-11** Cola offline de movimientos (`SyncMovimientosWorker`, backoff exponencial, `client_id`). *Aceptación:* test: reintento tras fallo y sin duplicados.
- [x] **M-12** Pestaña Deudas: lista con progreso, crear, aprobar, abonar. *Aceptación:* tests de ViewModel con fixtures.
- [x] **M-13** Simulador con indicador verde/amarillo/rojo. *Aceptación:* tests de mapeo de diagnóstico.
- [x] **QA-A** Emulador contra `mock_server`. *Aceptación:* checklist por pantalla con evidencia (Feedback aplicado).

## Fase B — Avisos, IA y OCR
- [x] **M-14** Pestaña Avisos (lista/crear/borrar, texto libre con IA). *Aceptación:* tests de ViewModel.
- [x] **M-15** `AvisosWorker` 08:00 Asunción, hitos 30/7/5/3/1, canal y permiso. *Aceptación:* tests de hitos con reloj fijo; notificación visible en emulador.
- [x] **M-16** Registro por IA (precarga campos, inferencia explícita de "Ingreso" vs "Gasto"; timeout 45 s con reintento). *Aceptación:* tests con respuesta simulada (éxito/timeout/JSON inválido).
- [x] **M-17** Foto de comprobante (cámara/galería) con confirmar/cancelar. *Aceptación:* test: cancelar no envía; confirmar crea movimiento.
- [ ] **QA-B** Emulador Fase B.

## Fase C — Asistencia, cierres, soporte
- [x] **M-18** Pestaña Asistencia: marcar normal/directo y materia. *Aceptación:* tests de ViewModel. *(Verificado: AsistenciaViewModelTest con 8 tests unitarios pasando, assembleDebug OK).*
- [x] **M-19** Cierre mensual con descuento y visor/descarga de PDF. *Aceptación:* tests de flujo y descarga. *(Verificado: CierreViewModelTest con 10 tests unitarios pasando, FileProvider configurado, assembleDebug OK).*
- [x] **M-20** Pestaña Sistema: tickets, salud del backend, versión `0.0.1+<commit>`, logs. *Aceptación:* tests; versión coincide con `BuildConfig`. *(Verificado: SistemaViewModelTest con 7 tests unitarios pasando, versión dinámica generada 0.0.1+90efaab en BuildConfig, assembleDebug OK).*
- [x] **QA-C** Emulador Fase C. *(Verificado: QA manual realizado por el usuario; feedback de menús desplegables para Materias, Períodos y Categorías implementado y probado).*

## Cierre
- [x] **F-01** Auditoría de seguridad: sin secretos en git, `network_security_config`, logs sin key. *Aceptación:* `git status`, `git diff --cached --stat` y búsqueda de secretos limpias. *(Verificado: network_security_config.xml implementado restringiendo cleartext a loopbacks locales/emulador, auditoría de logs y secretos limpia, assembleDebug y tests en verde).*
- [x] **F-02** Suites completas (`./gradlew test lint assembleDebug`, pytest de `backend-spec`). *Aceptación:* salida en verde registrada. *(Verificado: Gradle testDebugUnitTest + lintDebug + assembleDebug exitoso en 3m 41s con 59 tareas; pytest de backend-spec con 6/6 tests pasando en verde).*
- [ ] **F-03** APK release firmado (keystore fuera del repo), versión `0.0.1+<commit>`. *Aceptación:* `apksigner verify` OK.
- [ ] **F-04** Reporte para el gate de despliegue, con la lista de cambios que debe aplicar el backend real. *Aceptación:* reporte con evidencia y `git status` limpio; sin push hasta tu aprobación.


