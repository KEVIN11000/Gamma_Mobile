package com.gamma.mobile.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.repository.AvisoRepository
import com.gamma.mobile.presentation.dashboard.toUiModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock

@HiltWorker
class AlarmaAvisosWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val avisoRepository: AvisoRepository,
    private val clock: Clock
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val avisosList = when (val result = avisoRepository.getAvisos(forceRefresh = true)) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error, is NetworkResult.Exception -> {
                // Fetch from cache explicitly if getAvisos fails without returning cache
                // But AvisosRepositoryImpl already returns cache on error if available.
                // Wait, AvisoRepositoryImpl returns Success(cache) if cache is available and network fails.
                // So if it's Error/Exception here, it means cache is empty too.
                return Result.retry()
            }
        }

        val hitos = listOf(30, 7, 5, 3, 1)

        avisosList.forEach { aviso ->
            val uiModel = aviso.toUiModel(clock)
            if (uiModel != null && uiModel.diasRestantes in hitos) {
                showNotification(
                    id = aviso.id.hashCode(),
                    title = "Aviso: ${aviso.titulo}",
                    message = "Vence en ${uiModel.diasRestantes} días."
                )
            }
        }

        return Result.success()
    }

    private fun showNotification(id: Int, title: String, message: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val channelId = "avisos_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "Avisos", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(id, notification)
    }
}
