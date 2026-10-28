package com.gamma.mobile.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.gamma.mobile.data.local.dao.MovimientoDao
import com.gamma.mobile.data.local.entity.MovimientoEntity
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.domain.model.Movimiento
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
class SyncMovimientosWorkerTest {

    private lateinit var context: Context
    private lateinit var fakeDao: FakeWorkerDao
    private lateinit var fakeApi: FakeWorkerApi

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeDao = FakeWorkerDao()
        fakeApi = FakeWorkerApi()
    }

    @Test
    fun `doWork returns success when no pending movimientos`() = runBlocking {
        val worker = TestListenableWorkerBuilder<SyncMovimientosWorker>(context)
            .setWorkerFactory(SyncWorkerFactory(fakeDao, fakeApi))
            .build()
            
        val result = worker.doWork()
        
        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun `doWork returns success and updates status when API succeeds`() = runBlocking {
        val pendingEntity = MovimientoEntity("1", "2026-10-06", "Gasto", "Test", 100L, "Legal", "PENDING")
        fakeDao.insert(pendingEntity)
        fakeApi.shouldSucceed = true

        val worker = TestListenableWorkerBuilder<SyncMovimientosWorker>(context)
            .setWorkerFactory(SyncWorkerFactory(fakeDao, fakeApi))
            .build()

        val result = worker.doWork()
        
        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals("SYNCED", fakeDao.data[0].syncStatus)
        assertEquals(1, fakeApi.callCount)
    }

    @Test
    fun `doWork returns retry when API fails`() = runBlocking {
        val pendingEntity = MovimientoEntity("1", "2026-10-06", "Gasto", "Test", 100L, "Legal", "PENDING")
        fakeDao.insert(pendingEntity)
        fakeApi.shouldSucceed = false

        val worker = TestListenableWorkerBuilder<SyncMovimientosWorker>(context)
            .setWorkerFactory(SyncWorkerFactory(fakeDao, fakeApi))
            .build()

        val result = worker.doWork()
        
        assertTrue(result is ListenableWorker.Result.Retry)
        assertEquals("PENDING", fakeDao.data[0].syncStatus)
        assertEquals(1, fakeApi.callCount)
    }

    @Test
    fun `doWork handles exception and returns retry`() = runBlocking {
        val pendingEntity = MovimientoEntity("1", "2026-10-06", "Gasto", "Test", 100L, "Legal", "PENDING")
        fakeDao.insert(pendingEntity)
        
        val fakeApiThrowing = object : FakeWorkerApi() {
            override suspend fun createMovimiento(movimiento: Movimiento): Response<Unit> {
                throw RuntimeException("Network breakdown")
            }
        }

        val worker = TestListenableWorkerBuilder<SyncMovimientosWorker>(context)
            .setWorkerFactory(SyncWorkerFactory(fakeDao, fakeApiThrowing))
            .build()

        val result = worker.doWork()
        
        assertTrue(result is ListenableWorker.Result.Retry)
        assertEquals("PENDING", fakeDao.data[0].syncStatus)
    }
}

class SyncWorkerFactory(
    private val dao: MovimientoDao,
    private val api: GammaApi
) : androidx.work.WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: androidx.work.WorkerParameters
    ): ListenableWorker {
        return SyncMovimientosWorker(appContext, workerParameters, dao, api)
    }
}

class FakeWorkerDao : MovimientoDao {
    val data = mutableListOf<MovimientoEntity>()

    override suspend fun getAll(): List<MovimientoEntity> = data

    override suspend fun insertAll(movimientos: List<MovimientoEntity>) {
        data.addAll(movimientos)
    }

    override suspend fun insert(movimiento: MovimientoEntity) {
        data.add(movimiento)
    }

    override suspend fun getPendingSync(): List<MovimientoEntity> {
        return data.filter { it.syncStatus == "PENDING" }
    }

    override suspend fun updateSyncStatus(id: String, status: String) {
        val idx = data.indexOfFirst { it.id == id }
        if (idx != -1) {
            val updated = data[idx].copy(syncStatus = status)
            data[idx] = updated
        }
    }

    override suspend fun deleteSynced() {
        data.removeAll { it.syncStatus == "SYNCED" }
    }

    override suspend fun deleteAll() {
        data.clear()
    }
}

open class FakeWorkerApi : GammaApi {
    var shouldSucceed = true
    var callCount = 0

    override suspend fun checkHealth(): Response<Unit> = throw NotImplementedError()
    override suspend fun getDashboardSummary() = throw NotImplementedError()
    override suspend fun getMovimientos() = throw NotImplementedError()
    override suspend fun getAvisos() = throw NotImplementedError()
    override suspend fun createAviso(aviso: com.gamma.mobile.domain.model.Aviso): retrofit2.Response<Unit> = retrofit2.Response.success(Unit)
    override suspend fun deleteAviso(id: String): retrofit2.Response<Unit> = retrofit2.Response.success(Unit)
    override suspend fun interpretarAvisoIa(request: com.gamma.mobile.data.remote.IaTextRequest): retrofit2.Response<com.gamma.mobile.domain.model.Aviso> = retrofit2.Response.success(com.gamma.mobile.domain.model.Aviso())
    override suspend fun getObligaciones() = throw NotImplementedError()
    override suspend fun createObligacion(obligacion: com.gamma.mobile.domain.model.Obligacion) = throw NotImplementedError()
    override suspend fun updateObligacion(id: String, obligacion: com.gamma.mobile.domain.model.Obligacion) = throw NotImplementedError()
    override suspend fun deleteObligacion(id: String): retrofit2.Response<Unit> = throw NotImplementedError()
    override suspend fun abonarObligacion(request: com.gamma.mobile.domain.model.AbonoRequest): Response<com.gamma.mobile.domain.model.AbonoResponse> = throw NotImplementedError()
    override suspend fun updateAviso(id: String, aviso: com.gamma.mobile.domain.model.Aviso): retrofit2.Response<Unit> = throw NotImplementedError()
    override suspend fun getMaterias(): Response<List<String>> = Response.success(emptyList())
    
    override suspend fun createMovimiento(movimiento: Movimiento): Response<Unit> {
        callCount++
        return if (shouldSucceed) {
            Response.success(Unit)
        } else {
            Response.error(500, okhttp3.ResponseBody.create(null, ""))
        }
    }
    override suspend fun interpretarMovimientoIa(request: com.gamma.mobile.data.remote.IaTextRequest): retrofit2.Response<com.gamma.mobile.domain.model.Movimiento> = retrofit2.Response.success(com.gamma.mobile.domain.model.Movimiento())

    override suspend fun interpretarMovimientoOcr(request: okhttp3.MultipartBody.Part): retrofit2.Response<com.gamma.mobile.domain.model.Movimiento> = retrofit2.Response.success(com.gamma.mobile.domain.model.Movimiento())
    override suspend fun marcarAsistencia(request: com.gamma.mobile.domain.model.AsistenciaMarcaRequest) = throw NotImplementedError()
    override suspend fun marcarMateria(request: com.gamma.mobile.domain.model.AsistenciaMateriaRequest) = throw NotImplementedError()
    override suspend fun ejecutarCierre(request: com.gamma.mobile.domain.model.CierreRequest) = throw NotImplementedError()
    override suspend fun getCierres(): Response<List<com.gamma.mobile.domain.model.CierreHistorialItem>> = throw NotImplementedError()
    override suspend fun descargarReporteCierre(filename: String): Response<okhttp3.ResponseBody> = throw NotImplementedError()
    override suspend fun descargarArchivoPdf(url: String) = throw NotImplementedError()
    override suspend fun getHealthStatus() = throw NotImplementedError()
    override suspend fun getTickets() = throw NotImplementedError()
    override suspend fun createTicket(request: com.gamma.mobile.domain.model.TicketCreate) = throw NotImplementedError()
    override suspend fun updateTicket(id: String, request: com.gamma.mobile.domain.model.TicketUpdate) = throw NotImplementedError()
    override suspend fun getLogs() = throw NotImplementedError()
    override suspend fun getCronogramaVencimientos(): Response<List<com.gamma.mobile.domain.model.VencimientoCronogramaItem>> = throw NotImplementedError()
    override suspend fun getDeudaFoco(criterio: String): Response<com.gamma.mobile.domain.model.DeudaFocoResponse> = throw NotImplementedError()
    override suspend fun proyectarCapacidad(request: com.gamma.mobile.domain.model.CapacidadPagoRequest): Response<com.gamma.mobile.domain.model.CapacidadPagoResponse> = throw NotImplementedError()
    override suspend fun getPeriodos(): Response<com.gamma.mobile.domain.model.PeriodosResponse> = throw NotImplementedError()
    override suspend fun getAnaliticas(hoja: String?): Response<com.gamma.mobile.domain.model.AnaliticasHorasResponse> = throw NotImplementedError()
    override suspend fun descargarReporteExcel(hoja: String?, descuento: Double, saldoAnterior: Double, incluirIva: Boolean): Response<okhttp3.ResponseBody> = throw NotImplementedError()
    override suspend fun descargarReportePdfAsistencia(hoja: String?, descuento: Double, saldoAnterior: Double, incluirIva: Boolean): Response<okhttp3.ResponseBody> = throw NotImplementedError()
}
