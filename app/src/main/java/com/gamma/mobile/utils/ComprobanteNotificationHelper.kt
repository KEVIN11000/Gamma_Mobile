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
    private const val DEFAULT_LABEL = "Reporte PDF"

    fun buildPayload(periodoOrFileName: String?): ComprobanteNotificationPayload {
        val cleanLabel = periodoOrFileName?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_LABEL
        return ComprobanteNotificationPayload(
            id = cleanLabel.hashCode(),
            channelId = CHANNEL_ID,
            title = "Comprobante PDF descargado",
            message = "El comprobante ($cleanLabel) está listo para visualizarse.",
            fileName = cleanLabel
        )
    }

    fun isValidPdfFile(file: File?): Boolean {
        return file != null && file.exists() && file.isFile && file.length() > 0L
    }

    fun mostrarNotificacionDescarga(
        context: Context,
        file: File,
        etiqueta: String?
    ): Boolean {
        if (!isValidPdfFile(file)) return false

        return runCatching {
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
            true
        }.getOrDefault(false)
    }
}
