package com.gamma.mobile.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.gamma.mobile.MainActivity
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.utils.AvisoAlarmScheduler

class AvisoAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val avisoId = intent.getStringExtra(EXTRA_AVISO_ID) ?: return
        val titulo = intent.getStringExtra(EXTRA_TITULO) ?: "Aviso programado"
        val fechaEvento = intent.getStringExtra(EXTRA_FECHA_EVENTO) ?: ""
        val repeticion = intent.getStringExtra(EXTRA_REPETICION) ?: Aviso.REPETICION_NINGUNA
        val hitoMinutos = intent.getIntExtra(EXTRA_HITO_MINUTOS, 0)
        val hitosList = intent.getIntegerArrayListExtra(EXTRA_HITOS_LIST) ?: arrayListOf()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // Crear canal de notificación si es necesario
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios de eventos y avisos de Gamma"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            avisoId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val textoNotificacion = if (hitoMinutos > 0) {
            val textoHito = if (hitoMinutos >= 1440) {
                "${hitoMinutos / 1440} día(s)"
            } else if (hitoMinutos >= 60) {
                "${hitoMinutos / 60} hora(s)"
            } else {
                "$hitoMinutos min"
            }
            "Recordatorio: Faltan $textoHito para el evento ($fechaEvento)"
        } else {
            "Es el momento del evento programado ($fechaEvento)"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(textoNotificacion)
            .setStyle(NotificationCompat.BigTextStyle().bigText(textoNotificacion))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Descartable al tocar
            .setOngoing(false)   // NO anclada (requisito estricto del usuario)
            .build()

        val notificationId = avisoId.hashCode() + hitoMinutos
        notificationManager.notify(notificationId, notification)

        // Si fue el evento principal (hitoMinutos == 0) y tiene repetición, programar el siguiente ciclo
        if (hitoMinutos == 0 && repeticion != Aviso.REPETICION_NINGUNA) {
            val siguienteFecha = AvisoAlarmScheduler.calcularSiguienteRepeticion(fechaEvento, repeticion)
            if (siguienteFecha != null) {
                val siguienteAviso = Aviso(
                    id = avisoId,
                    titulo = titulo,
                    fechaEvento = siguienteFecha,
                    repeticion = repeticion,
                    hitosMinutos = hitosList
                )
                AvisoAlarmScheduler.programarAviso(context, siguienteAviso)
            }
        }
    }

    companion object {
        const val CHANNEL_ID = "avisos_eventos_channel"
        const val CHANNEL_NAME = "Eventos y Avisos"

        const val EXTRA_AVISO_ID = "extra_aviso_id"
        const val EXTRA_TITULO = "extra_titulo"
        const val EXTRA_FECHA_EVENTO = "extra_fecha_evento"
        const val EXTRA_REPETICION = "extra_repeticion"
        const val EXTRA_HITO_MINUTOS = "extra_hito_minutos"
        const val EXTRA_HITOS_LIST = "extra_hitos_list"
    }
}
