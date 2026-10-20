package com.gamma.mobile.worker

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.repository.AvisoRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class FakeWorkerAvisoRepository : AvisoRepository {
    var result: NetworkResult<List<Aviso>> = NetworkResult.Success(emptyList())
    override suspend fun getAvisos(forceRefresh: Boolean): NetworkResult<List<Aviso>> = result
    override suspend fun createAviso(aviso: Aviso): NetworkResult<Unit> = NetworkResult.Success(Unit)
    override suspend fun deleteAviso(id: String): NetworkResult<Unit> = NetworkResult.Success(Unit)
    override suspend fun updateAviso(aviso: Aviso): NetworkResult<Unit> = NetworkResult.Success(Unit)
    override suspend fun interpretarAvisoIa(texto: String): NetworkResult<Aviso> = NetworkResult.Success(Aviso())
}

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33])
class AlarmaAvisosWorkerTest {

    private lateinit var context: Context
    private lateinit var fakeRepo: FakeWorkerAvisoRepository
    private lateinit var fixedClock: Clock

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeRepo = FakeWorkerAvisoRepository()
        org.robolectric.shadows.ShadowApplication.getInstance().grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        fixedClock = Clock.fixed(Instant.parse("2026-10-08T12:00:00Z"), ZoneId.of("America/Asuncion"))
    }

    @Test
    fun `doWork notifies for 30, 7, 5, 3, 1 days`() = runBlocking {
        fakeRepo.result = NetworkResult.Success(listOf(
            Aviso(id = "1", titulo = "En 30 dias", fechaEvento = "07/11/2026 00:00"), 
            Aviso(id = "2", titulo = "En 7 dias", fechaEvento = "15/10/2026 00:00"), 
            Aviso(id = "3", titulo = "En 1 dia", fechaEvento = "09/10/2026 00:00"), 
            Aviso(id = "4", titulo = "En 2 dias", fechaEvento = "10/10/2026 00:00") 
        ))

        val worker = TestListenableWorkerBuilder<AlarmaAvisosWorker>(context)
            .setWorkerFactory(object : androidx.work.WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: androidx.work.WorkerParameters
                ): ListenableWorker {
                    return AlarmaAvisosWorker(appContext, workerParameters, fakeRepo, fixedClock)
                }
            })
            .build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = shadowOf(notificationManager)
        
        assertEquals(3, shadowNm.allNotifications.size)
    }
}


