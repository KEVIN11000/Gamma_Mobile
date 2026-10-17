# Plan de Arquitectura y Contratos Técnicos (Plan)
## Módulo: Notificaciones Push Locales de Descarga de Comprobantes PDF y Publicación de Release v0.0.8

- **Fecha:** 17 de Octubre de 2026
- **Versión:** 1.0.0
- **Fase:** 0 (Especificación y Arquitectura)
- **Estado:** En Revisión (Gate Obligatorio de Planificación)

---

### 1. Arquitectura Modular

```
Gamma Mobile (Presentation / UI)
  ├── MainActivity.kt
  │     └── Solicitud de permiso runtime POST_NOTIFICATIONS en Android 13+ (API 33+)
  ├── SistemaScreen.kt -> SistemaViewModel.kt
  │     └── abrirPdfConCache(...)
  │           ├── Descarga atómica en Dispatchers.IO -> cacheDir/comprobantes/cierre_contable_<periodo>.pdf
  │           └── ComprobanteNotificationHelper.mostrarNotificacionDescarga(context, file, titulo, descripcion)
  └── AsistenciaScreen.kt (CierreTabContent) -> CierreViewModel.kt
        └── LaunchedEffect(CierreUiState.PdfDownloaded)
              ├── Escritura en cacheDir/comprobantes/<fileName>
              └── ComprobanteNotificationHelper.mostrarNotificacionDescarga(context, file, titulo, descripcion)
                    │
                    ▼
Gamma Mobile (Utils / Notification Layer)
  └── ComprobanteNotificationHelper.kt
        ├── Canal: "comprobantes_channel" ("Comprobantes y Reportes PDF")
        ├── Verificación de ContextCompat.checkSelfPermission(POST_NOTIFICATIONS) en API >= 33
        ├── Construcción de URI segura vía FileProvider ("${context.packageName}.fileprovider")
        ├── PendingIntent.getActivity con Intent.ACTION_VIEW ("application/pdf") + FLAG_IMMUTABLE
        └── NotificationManager.notify(notificationId, notification)
```

---

### 2. Contratos de Datos y Componentes

#### 2.1 `ComprobanteNotificationHelper` (`com.gamma.mobile.utils.ComprobanteNotificationHelper`)
Objeto/clase utilitaria con métodos puros de construcción de datos de notificación y método de despacho en Android:

```kotlin
package com.gamma.mobile.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

data class ComprobanteNotificationPayload(
    val id: Int,
    val channelId: String,
    val title: String,
    val message: String,
    val fileName: String
)

object ComprobanteNotificationHelper {
    const val CHANNEL_ID = "comprobantes_channel"
    const val CHANNEL_NAME = "Comprobantes y Reportes PDF"

    fun buildPayload(periodoOrFileName: String): ComprobanteNotificationPayload {
        val cleanLabel = periodoOrFileName.trim().ifEmpty { "Reporte PDF" }
        return ComprobanteNotificationPayload(
            id = cleanLabel.hashCode(),
            channelId = CHANNEL_ID,
            title = "Comprobante PDF descargado",
            message = "El comprobante ($cleanLabel) está listo para visualizarse.",
            fileName = cleanLabel
        )
    }

    fun mostrarNotificacionDescarga(
        context: Context,
        file: File,
        etiqueta: String
    ): Boolean {
        if (!file.exists() || file.length() == 0L) return false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        val payload = buildPayload(etiqueta)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return false

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            payload.id,
            viewIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(payload.title)
            .setContentText(payload.message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(payload.id, notification)
        return true
    }
}
```

#### 2.2 Manejo de Errores y Permisos
- **Sin permiso `POST_NOTIFICATIONS` (Android 13+):** `mostrarNotificacionDescarga` retorna `false` de forma silenciosa y segura sin interrumpir el flujo de visualización del PDF en primer plano.
- **Archivo inexistente o vacío (`length == 0L`):** No se emite notificación si la descarga falló o el archivo está vacío.
- **Excepciones de `FileProvider` o `NotificationManager`:** Capturadas en bloque `runCatching` para garantizar que un fallo del subsistema de notificaciones jamás bloquee la apertura del documento PDF.

---

### 3. Estrategia de Pruebas Unitarias
- `ComprobanteNotificationHelperTest.kt`:
  - `buildPayload genera titulo, mensaje e id determinista para un periodo dado()`
  - `buildPayload usa etiqueta por defecto cuando el texto viene vacio()`
- `SistemaViewModelTest.kt` y `CierreViewModelTest.kt`:
  - Verificar que el flujo de cierre y descarga de PDF mantiene el 100% de compatibilidad con las pruebas unitarias existentes.
