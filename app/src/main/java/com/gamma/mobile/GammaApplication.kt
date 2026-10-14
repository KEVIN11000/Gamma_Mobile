package com.gamma.mobile

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.gamma.mobile.worker.AlarmaAvisosWorker
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class GammaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val logFile = File(filesDir, "crash.txt")
                logFile.writeText("Crash in thread ${thread.name}:\n" + throwable.stackTraceToString())
                Log.e("GammaApplication", "CRASH DETECTED", throwable)
            } catch (e: Exception) {
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        createNotificationChannels()
        scheduleAvisosWorker()
    }

    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(android.app.NotificationManager::class.java) ?: return
            val channelAvisos = android.app.NotificationChannel(
                "avisos_channel",
                "Avisos y Vencimientos",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de vencimientos, compromisos y avisos programados"
            }
            val channelSync = android.app.NotificationChannel(
                "sync_channel",
                "Sincronización en Segundo Plano",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Estado de sincronización de movimientos con el servidor"
            }
            notificationManager.createNotificationChannel(channelAvisos)
            notificationManager.createNotificationChannel(channelSync)
        }
    }

    private fun scheduleAvisosWorker() {
        val now = ZonedDateTime.now(ZoneId.of("America/Asuncion"))
        var nextRun = now.withHour(8).withMinute(0).withSecond(0).withNano(0)
        if (now.isAfter(nextRun) || now.isEqual(nextRun)) {
            nextRun = nextRun.plusDays(1)
        }
        val initialDelay = Duration.between(now, nextRun).toMillis()

        val workRequest = PeriodicWorkRequestBuilder<AlarmaAvisosWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AlarmaAvisosWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
