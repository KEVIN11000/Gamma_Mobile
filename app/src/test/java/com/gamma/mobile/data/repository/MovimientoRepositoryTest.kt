package com.gamma.mobile.data.repository

import com.gamma.mobile.data.local.dao.MovimientoDao
import com.gamma.mobile.data.local.dao.SyncStateDao
import com.gamma.mobile.data.local.entity.MovimientoEntity
import com.gamma.mobile.data.local.entity.SyncStateEntity
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.model.Movimiento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
class MovimientoRepositoryTest {

    private lateinit var repository: MovimientoRepositoryImpl
    private lateinit var fakeApi: FakeApiForRepo
    private lateinit var fakeDao: FakeMovimientoDao
    private lateinit var fakeSyncDao: FakeSyncStateDao
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        fakeApi = FakeApiForRepo()
        fakeDao = FakeMovimientoDao()
        fakeSyncDao = FakeSyncStateDao()
        repository = MovimientoRepositoryImpl(context, fakeApi, fakeDao, fakeSyncDao, dispatcher)
    }

    @Test
    fun `getMovimientos without cache fetches from API and saves to cache`() = runTest(dispatcher) {
        val networkData = listOf(Movimiento("1", "2026-01-01", "Ingreso", "Venta", 100L, "Virtual Legal"))
        fakeApi.movimientosResponse = Response.success(networkData)

        val result = repository.getMovimientos(forceRefresh = false)

        assertTrue(result is NetworkResult.Success)
        assertEquals(1, (result as NetworkResult.Success).data.size)
        assertEquals(1, fakeDao.data.size)
        assertTrue(fakeSyncDao.data.containsKey("movimientos"))
    }

    @Test
    fun `getMovimientos with valid cache returns local data without calling API`() = runTest(dispatcher) {
        fakeDao.data.add(MovimientoEntity("2", "2026-01-02", "Gasto", "Compra", 50L, "No Legal"))
        fakeSyncDao.data["movimientos"] = SyncStateEntity("movimientos", System.currentTimeMillis() - 1000)
        
        fakeApi.shouldThrow = true // Si llama a la API fallará el test

        val result = repository.getMovimientos(forceRefresh = false)

        assertTrue(result is NetworkResult.Success)
        assertEquals("2", (result as NetworkResult.Success).data[0].id)
    }

    @Test
    fun `getMovimientos network error falls back to expired cache`() = runTest(dispatcher) {
        fakeDao.data.add(MovimientoEntity("3", "2026-01-03", "Ingreso", "Servicio", 200L, "Virtual Legal"))
        // Expired cache (6 minutes ago)
        fakeSyncDao.data["movimientos"] = SyncStateEntity("movimientos", System.currentTimeMillis() - (6 * 60 * 1000))
        
        fakeApi.shouldThrow = true

        val result = repository.getMovimientos(forceRefresh = false)

        // API will throw, but repository intercepts and returns cache
        assertTrue(result is NetworkResult.Success)
        assertEquals("3", (result as NetworkResult.Success).data[0].id)
    }

    @Test
    fun `getMovimientos preserves pending local transactions when fetching remote data`() = runTest(dispatcher) {
        // Given: una transacción local no sincronizada (creada en modo offline)
        fakeDao.data.add(
            MovimientoEntity(
                id = "pending-1",
                fecha = "2026-01-01",
                tipo = "Gasto",
                categoria = "Comida",
                total = 50000L,
                comprobante = "Física Legal",
                syncStatus = "PENDING"
            )
        )
        
        // When: el repositorio sincroniza con el servidor
        val remoteData = listOf(
            Movimiento("remote-1", "2026-01-02", "Ingreso", "Honorarios", 1500000L, "Virtual Legal")
        )
        fakeApi.movimientosResponse = Response.success(remoteData)
        val result = repository.getMovimientos(forceRefresh = true)

        // Then: el resultado y el DAO local conservan TANTO la remota COMO la pendiente local
        assertTrue(result is NetworkResult.Success)
        val list = (result as NetworkResult.Success).data
        assertEquals(2, list.size)
        assertTrue(list.any { it.id == "pending-1" })
        assertTrue(list.any { it.id == "remote-1" })
        assertTrue(fakeDao.data.any { it.id == "pending-1" && it.syncStatus == "PENDING" })
    }
}

class FakeApiForRepo : GammaApi {
    var movimientosResponse: Response<List<Movimiento>>? = null
    var shouldThrow = false

    override suspend fun getMovimientos(): Response<List<Movimiento>> {
        if (shouldThrow) throw java.net.SocketTimeoutException()
        return movimientosResponse ?: Response.success(emptyList())
    }

    override suspend fun checkHealth() = throw NotImplementedError()
    override suspend fun getDashboardSummary() = throw NotImplementedError()
    override suspend fun createMovimiento(movimiento: Movimiento) = throw NotImplementedError()

    override suspend fun interpretarMovimientoIa(request: com.gamma.mobile.data.remote.IaTextRequest): retrofit2.Response<com.gamma.mobile.domain.model.Movimiento> = retrofit2.Response.success(com.gamma.mobile.domain.model.Movimiento())

    override suspend fun interpretarMovimientoOcr(request: okhttp3.MultipartBody.Part): retrofit2.Response<com.gamma.mobile.domain.model.Movimiento> = retrofit2.Response.success(com.gamma.mobile.domain.model.Movimiento())
    override suspend fun getAvisos() = throw NotImplementedError()
    override suspend fun createAviso(aviso: com.gamma.mobile.domain.model.Aviso): retrofit2.Response<Unit> = retrofit2.Response.success(Unit)
    override suspend fun deleteAviso(id: String): retrofit2.Response<Unit> = retrofit2.Response.success(Unit)
    override suspend fun interpretarAvisoIa(request: com.gamma.mobile.data.remote.IaTextRequest): retrofit2.Response<com.gamma.mobile.domain.model.Aviso> = retrofit2.Response.success(com.gamma.mobile.domain.model.Aviso())
    override suspend fun getObligaciones() = throw NotImplementedError()
    override suspend fun createObligacion(obligacion: com.gamma.mobile.domain.model.Obligacion) = throw NotImplementedError()
    override suspend fun updateObligacion(id: String, obligacion: com.gamma.mobile.domain.model.Obligacion) = throw NotImplementedError()
    override suspend fun marcarAsistencia(request: com.gamma.mobile.domain.model.AsistenciaMarcaRequest) = throw NotImplementedError()
    override suspend fun marcarMateria(request: com.gamma.mobile.domain.model.AsistenciaMateriaRequest) = throw NotImplementedError()
    override suspend fun ejecutarCierre(request: com.gamma.mobile.domain.model.CierreRequest) = throw NotImplementedError()
    override suspend fun descargarArchivoPdf(url: String) = throw NotImplementedError()
    override suspend fun getHealthStatus() = throw NotImplementedError()
    override suspend fun getTickets() = throw NotImplementedError()
    override suspend fun createTicket(request: com.gamma.mobile.domain.model.TicketCreate) = throw NotImplementedError()
    override suspend fun getLogs() = throw NotImplementedError()
}

class FakeMovimientoDao : MovimientoDao {
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

    override suspend fun updateSyncStatus(id: String) {
        val idx = data.indexOfFirst { it.id == id }
        if (idx != -1) {
            data[idx] = data[idx].copy(syncStatus = "SYNCED")
        }
    }

    override suspend fun deleteSynced() {
        data.removeAll { it.syncStatus == "SYNCED" }
    }

    override suspend fun deleteAll() {
        data.clear()
    }
}

class FakeSyncStateDao : SyncStateDao {
    val data = mutableMapOf<String, SyncStateEntity>()

    override suspend fun getSyncState(key: String): SyncStateEntity? = data[key]

    override suspend fun insert(syncState: SyncStateEntity) {
        data[syncState.key] = syncState
    }
}
