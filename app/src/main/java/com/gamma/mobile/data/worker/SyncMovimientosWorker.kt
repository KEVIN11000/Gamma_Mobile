package com.gamma.mobile.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gamma.mobile.data.local.dao.MovimientoDao
import com.gamma.mobile.data.local.entity.toDomain
import com.gamma.mobile.data.remote.GammaApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncMovimientosWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val dao: MovimientoDao,
    private val api: GammaApi
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pendingMovimientos = dao.getPendingSync()
        if (pendingMovimientos.isEmpty()) {
            return@withContext Result.success()
        }

        var hasFailures = false

        for (entity in pendingMovimientos) {
            try {
                // El ID de entidad generado por UUID (en Movimiento) sirve como client_id para idempotencia.
                val response = api.createMovimiento(entity.toDomain())
                if (response.isSuccessful) {
                    dao.updateSyncStatus(entity.id)
                } else if (response.code() in 400..499) {
                    // Errores de cliente / validación: marcar para no reintentar infinitamente en segundo plano
                    dao.updateSyncStatus(entity.id, "FAILED_CLIENT_VALIDATION")
                } else {
                    hasFailures = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                hasFailures = true
            }
        }

        if (hasFailures) {
            Result.retry() // Esto gatilla el backoff exponencial de WorkManager
        } else {
            Result.success()
        }
    }
}
