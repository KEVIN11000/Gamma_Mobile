package com.gamma.mobile.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.receiver.AvisoAlarmReceiver
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class AvisoAlarmTrigger(
    val avisoId: String,
    val titulo: String,
    val triggerMillis: Long,
    val hitoMinutosPrevios: Int,
    val requestCode: Int
)

object AvisoAlarmScheduler {

    private val FORMATTERS = listOf(
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault()),
        DateTimeFormatter.ofPattern("d/M/yyyy HH:mm", Locale.getDefault()),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault()),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
    )

    fun parseFechaEventoToMillis(fechaStr: String, zoneId: ZoneId = ZoneId.systemDefault()): Long? {
        val clean = fechaStr.trim()
        if (clean.isEmpty()) return null

        for (formatter in FORMATTERS) {
            try {
                val ldt = LocalDateTime.parse(clean, formatter)
                return ldt.atZone(zoneId).toInstant().toEpochMilli()
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Calcula los disparos válidos para un aviso (evento principal + hasta 5 hitos previos).
     * Solo incluye aquellos cuyo triggerMillis sea mayor a currentTimeMillis.
     */
    fun calcularDisparos(
        aviso: Aviso,
        currentTimeMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<AvisoAlarmTrigger> {
        val eventoMillis = parseFechaEventoToMillis(aviso.fechaEvento, zoneId) ?: return emptyList()
        val disparos = mutableListOf<AvisoAlarmTrigger>()
        val baseIdHash = aviso.id.hashCode()

        // 1. Hitos previos (ordenados de mayor anticipación a menor)
        val hitosNormalizados = Aviso.normalizarHitos(aviso.hitosMinutos)
        hitosNormalizados.forEachIndexed { index, minutosPrevios ->
            if (minutosPrevios > 0) {
                val triggerMillis = eventoMillis - (minutosPrevios * 60 * 1000L)
                if (triggerMillis > currentTimeMillis) {
                    disparos.add(
                        AvisoAlarmTrigger(
                            avisoId = aviso.id,
                            titulo = aviso.titulo,
                            triggerMillis = triggerMillis,
                            hitoMinutosPrevios = minutosPrevios,
                            requestCode = baseIdHash + (index + 1) * 1000
                        )
                    )
                }
            }
        }

        // 2. Disparo en la hora exacta del evento
        if (eventoMillis > currentTimeMillis) {
            disparos.add(
                AvisoAlarmTrigger(
                    avisoId = aviso.id,
                    titulo = aviso.titulo,
                    triggerMillis = eventoMillis,
                    hitoMinutosPrevios = 0,
                    requestCode = baseIdHash
                )
            )
        }

        return disparos
    }

    /**
     * Calcula la siguiente fecha formateada ("dd/MM/yyyy HH:mm") según la regla de repetición.
     */
    fun calcularSiguienteRepeticion(
        fechaStr: String,
        repeticion: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String? {
        val clean = fechaStr.trim()
        val modo = Aviso.normalizarRepeticion(repeticion)
        if (modo == Aviso.REPETICION_NINGUNA) return null

        for (formatter in FORMATTERS) {
            try {
                val ldt = LocalDateTime.parse(clean, formatter)
                val nuevaFecha = when (modo) {
                    Aviso.REPETICION_DIARIA -> ldt.plusDays(1)
                    Aviso.REPETICION_SEMANAL -> ldt.plusWeeks(1)
                    Aviso.REPETICION_MENSUAL -> ldt.plusMonths(1)
                    Aviso.REPETICION_ANUAL -> ldt.plusYears(1)
                    else -> ldt
                }
                val outFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
                return nuevaFecha.format(outFormatter)
            } catch (_: Exception) {}
        }
        return null
    }

    fun programarAviso(context: Context, aviso: Aviso) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val disparos = calcularDisparos(aviso)

        for (disparo in disparos) {
            val intent = Intent(context, AvisoAlarmReceiver::class.java).apply {
                putExtra(AvisoAlarmReceiver.EXTRA_AVISO_ID, aviso.id)
                putExtra(AvisoAlarmReceiver.EXTRA_TITULO, aviso.titulo)
                putExtra(AvisoAlarmReceiver.EXTRA_FECHA_EVENTO, aviso.fechaEvento)
                putExtra(AvisoAlarmReceiver.EXTRA_REPETICION, aviso.repeticion)
                putExtra(AvisoAlarmReceiver.EXTRA_HITO_MINUTOS, disparo.hitoMinutosPrevios)
                putIntegerArrayListExtra(AvisoAlarmReceiver.EXTRA_HITOS_LIST, ArrayList(aviso.hitosMinutos))
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                disparo.requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        disparo.triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        disparo.triggerMillis,
                        pendingIntent
                    )
                }
            } catch (_: SecurityException) {
                // Fallback si no tiene permiso de alarma exacta
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    disparo.triggerMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelarAviso(context: Context, avisoId: String, maxHitos: Int = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val baseIdHash = avisoId.hashCode()

        // Cancelar evento principal
        cancelarPendingIntent(context, alarmManager, baseIdHash)

        // Cancelar posibles hitos
        for (i in 1..maxHitos) {
            cancelarPendingIntent(context, alarmManager, baseIdHash + (i * 1000))
        }
    }

    private fun cancelarPendingIntent(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, AvisoAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
