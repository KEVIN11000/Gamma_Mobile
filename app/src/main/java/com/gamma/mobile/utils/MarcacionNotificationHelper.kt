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
import com.gamma.mobile.MainActivity

data class MarcacionNotificationPayload(
    val id: Int,
    val channelId: String,
    val title: String,
    val message: String
)

object MarcacionNotificationHelper {
    const val CHANNEL_ID = "marcaciones_channel"
    const val CHANNEL_NAME = "Marcaciones de Asistencia"

    fun buildPayload(hora: String, tipo: String = "asistencia", planilla: String? = null): MarcacionNotificationPayload {
        val tipoFormateado = if (tipo.isNotBlank()) tipo.replaceFirstChar { it.uppercase() } else "Marcación"
        val horaTexto = if (hora.isNotBlank()) " a las $hora" else ""
        val planillaTexto = if (!planilla.isNullOrBlank()) " en planilla '$planilla'" else " en la planilla de horas"

        val title = "Marcación Registrada"
        val message = "$tipoFormateado registrado$horaTexto$planillaTexto."

        val hash = (hora + tipo + (planilla ?: "")).hashCode()
        return MarcacionNotificationPayload(
            id = if (hash != 0) hash else 1001,
            channelId = CHANNEL_ID,
            title = title,
            message = message
        )
    }

    fun mostrarNotificacionMarcacion(
        context: Context,
        hora: String,
        tipo: String = "asistencia",
        planilla: String? = null
    ): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return false
            }

            val payload = buildPayload(hora, tipo, planilla)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return false

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                payload.id,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
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
