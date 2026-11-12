# Project Decision & Trace Ledger (L2)

## Architecture & Invariants

- Hidratacion global atomica via GET /api/v1/bootstrap en BootstrapRepositoryImpl sin borrar datos previos en DashboardViewModel. [^1], [^2], [^3] <!--m_01M70XNMG1KRSMAH0BHXX901ZR-->
- Room v6 con MIGRATION_5_6 crea tablas flujo_caja y clima_cache para consulta reactiva offline-first con TTL en backend. [^1], [^4], [^5] <!--m_01M70XNV0NRDCB2H5XJ88V59PJ-->
- Parsing dual en NuevaTransaccionScreen: NLP backend v2 primario con fallback a Gemini on-device para preservar capacidad offline. [^1], [^6], [^5] <!--m_01M70XNV7C6AWAHEM8KJCJXJEE-->
- Autocompletado de Razon Social de proveedor por RUC en KuDE SIFEN manteniendo el campo de edicion manual libre para el usuario. [^1], [^6] <!--m_01M70XNVEFSCX1ZGSJZQBMH53Q-->
- Interceptores OkHttp: IdempotencyInterceptor (X-Client-Id UUID unico), RateLimitInterceptor (429) y PayloadSizeGuard (>64KB 413). [^1], [^7] <!--m_01M70XNVNV80ZHJANTRMXJ7EGP-->
- DEFAULT_BASE_URL inyectado via BuildConfig desde local.properties o CI (GAMMA_API_BASE_URL) y allowBackup=false en Manifest. [^8], [^9], [^10] <!--m_01M78FNFAZSTFKEQ4KFWRJCGNG-->
- Deuda viva blindada (saldoActual > 0L fuerza esDefinitivamenteSaldada=false) y auto-descubrimiento en /api/v1/capacidades. [^11], [^12], [^13], [^14] <!--m_01M78FNFH8CF7AWDMB6M670YW5-->

---

[^1]: spec:v013
[^2]: test:BootstrapRepositoryTest
[^3]: invariant:INV-04
[^4]: test:FlujoCajaAndClimaDaoTest
[^5]: invariant:INV-06
[^6]: test:NlpAndSifenUseCaseTest
[^7]: test:RateLimitAndPayloadGuardTest
[^8]: bug:seguridad-gamma-mobile
[^9]: test:SecureStorageTest
[^10]: invariant:INV-07
[^11]: spec:v014-refinamiento-contable-ux
[^12]: test:ObligacionModelTest
[^13]: invariant:INV-02
[^14]: invariant:INV-08
