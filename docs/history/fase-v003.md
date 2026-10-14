# Reporte de Cierre de Fase: Evolución v0.0.3 - SIFEN QR, Tipado PYG y Consolidación Ergonómica

- **Feature**: `v003-evolution-sifen`
- **Fecha**: 2026-10-14
- **Versión**: `v0.0.3`
- **Estado**: Finalizada con Éxito (100% Tests Verificados)
- **APK Generado**: `app/build/outputs/apk/debug/app-debug.apk` (39.8 MB)

---

## 1. Resumen Ejecutivo de Capacidades Entregadas

En esta fase de evolución, basada en las auditorías de los especialistas de Producto (Alex - PM) y Arquitectura Móvil (Mobile App Builder), se incorporaron avances críticos para la madurez del producto:

1. **Prevención de Desbordamiento Aritmético en Guaraníes (PYG Long)**:
   - Migración completa de montos de `Int` a `Long` en modelos de dominio (`Movimiento`, `Obligacion`), entidades de persistencia (`MovimientoEntity`, `ObligacionEntity`), DAOs, repositorios y ViewModels.
   - Elevación de base de datos Room a versión `2` con fallback destructivo y re-sincronización transparente.
   - Soporte verificado para compras y ventas de más de `₲ 2.500.000.000` sin errores de desbordamiento de 32 bits.

2. **Resiliencia Offline y Persistencia Room**:
   - Corrección en `MovimientoRepositoryImpl.kt`: eliminación de la purga indiscriminada tras sincronización remota (`deleteSynced` selectivo).
   - El Libro Diario preserva localmente el histórico completo y transacciones pendientes aun en Modo Avión o sin conexión.

3. **Gestión de Permisos POST_NOTIFICATIONS (Android 13+ / API 33+)**:
   - Permiso `android.permission.POST_NOTIFICATIONS` declarado y controlado en Compose con banner educativo y `rememberLauncherForActivityResult`.
   - Canales oficiales de notificación configurados en `GammaApplication.kt`.

4. **Consolidación Ergonómica de Navegación (6 ➔ 4 Pestañas)**:
   - Reducción de la barra inferior a 4 destinos nucleares: **Dashboard**, **Diario**, **Compromisos** y **Sistema**.
   - Creación de `CompromisosScreen.kt` con pestañas superiores (*TabRow*) para alojar **Deudas**, **Avisos** y **Asistencia**.
   - Preservación de atajos de navegación directa desde el Dashboard.

5. **Módulo de Escaneo de Facturas Electrónicas SIFEN (DNIT Paraguay)**:
   - Integración nativa de **CameraX** y **Google ML Kit Barcode Scanning**.
   - Implementación del caso de uso `ParseSifenQrUseCase.kt` para extraer CDC (44 dígitos), RUC emisor, número de factura, total e IVA a partir de códigos QR oficiales de la DNIT / e-Kuatia.
   - Creación de `SifenScannerScreen.kt` con marco guía animado y `SifenConfirmBottomSheet.kt` para validación y registro en Libro Diario con un toque.
   - Acceso rápido a escaneo desde `Libro Diario` (botón superior y FAB) y desde `Nueva Transacción` (pestaña dedicada SIFEN QR).

---

## 2. Archivos Modificados y Creados

### Nuevos Componentes
- `app/src/main/java/com/gamma/mobile/domain/model/SifenInvoiceData.kt`
- `app/src/main/java/com/gamma/mobile/domain/usecase/ParseSifenQrUseCase.kt`
- `app/src/main/java/com/gamma/mobile/presentation/compromisos/CompromisosScreen.kt`
- `app/src/main/java/com/gamma/mobile/presentation/sifen/SifenScannerViewModel.kt`
- `app/src/main/java/com/gamma/mobile/presentation/sifen/SifenScannerScreen.kt`
- `app/src/main/java/com/gamma/mobile/presentation/sifen/SifenConfirmBottomSheet.kt`
- `app/src/test/java/com/gamma/mobile/domain/usecase/ParseSifenQrUseCaseTest.kt`

### Modificados
- `app/build.gradle.kts`: Dependencias CameraX, ML Kit Barcode, `versionName = "0.0.3"`.
- `app/src/main/AndroidManifest.xml`: Permisos `CAMERA` y feature de cámara.
- `app/src/main/java/com/gamma/mobile/MainActivity.kt`: Navegación consolidada a 4 tabs y ruteo a escáner SIFEN.
- `app/src/main/java/com/gamma/mobile/presentation/transacciones/TransaccionesScreen.kt`: Acciones de escáner QR.
- `app/src/main/java/com/gamma/mobile/presentation/transacciones/NuevaTransaccionScreen.kt`: Pestaña dedicada SIFEN QR.
- `app/src/main/java/com/gamma/mobile/presentation/obligaciones/ObligacionesScreen.kt`: Parámetro `showTopBar`.
- `app/src/main/java/com/gamma/mobile/presentation/asistencia/AsistenciaScreen.kt`: Parámetro `showTopBar`.
- `app/src/main/java/com/gamma/mobile/domain/model/Models.kt`, `CierreModels.kt`: Migración a `Long`.
- `app/src/main/java/com/gamma/mobile/data/local/`: Entidades Room, DAOs y migración a versión 2.
- `app/src/main/java/com/gamma/mobile/data/repository/MovimientoRepositoryImpl.kt`: Política sin purga.

---

## 3. Evidencia Empírica de Pruebas

- **Unit Tests**: `./gradlew testDebugUnitTest` ➔ **BUILD SUCCESSFUL** (87/87 tests pasando al 100%).
- **Compilación de Artefacto**: `./gradlew assembleDebug` ➔ **BUILD SUCCESSFUL** en 1m 4s.
- **APK Resultante**: `app/build/outputs/apk/debug/app-debug.apk` (39.8 MB).
