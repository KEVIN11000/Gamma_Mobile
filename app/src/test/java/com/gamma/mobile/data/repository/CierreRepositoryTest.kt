package com.gamma.mobile.data.repository

import com.gamma.mobile.data.local.dao.CierreDao
import com.gamma.mobile.data.local.dao.SyncStateDao
import com.gamma.mobile.data.local.entity.CierreEntity
import com.gamma.mobile.data.local.entity.SyncStateEntity
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.IaTextRequest
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.CierreHistorialItem
import com.gamma.mobile.domain.model.CierreRequest
import com.gamma.mobile.domain.model.CierreResponse
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.model.Movimiento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class CierreRepositoryTest {

    private lateinit var repository: CierreRepositoryImpl
    private lateinit var fakeApi: FakeApiForCierreRepo
    private lateinit var fakeDao: FakeCierreDao
    private lateinit var fakeSyncDao: FakeSyncStateDaoForCierre
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        fakeApi = FakeApiForCierreRepo()
        fakeDao = FakeCierreDao()
        fakeSyncDao = FakeSyncStateDaoForCierre()
        repository = CierreRepositoryImpl(fakeApi, fakeDao, fakeSyncDao, dispatcher)
    }

    @Test
    fun `getCierres without cache fetches from API and saves to Room`() = runTest(dispatcher) {
        val remoteCierres = listOf(
            CierreHistorialItem(
                id = "c1",
                tipo = "contable",
                periodo = "10/2026",
                fecha = "2026-10-22",
                totalMonto = 10000000L,
                totalHoras = 0.0,
                resumen = "Cierre Contable",
                pdfUrl = "https://example.com/c1.pdf",
                pdfDisponible = true
            )
        )
        fakeApi.cierresResponse = Response.success(remoteCierres)

        val result = repository.getCierres(forceRefresh = false)

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals(1, data.size)
        assertEquals("c1", data[0].id)
        assertEquals(1, fakeDao.data.size)
        assertTrue(fakeSyncDao.data.containsKey("cierres"))
    }

    @Test
    fun `getCierres with valid cache returns Room data without calling API`() = runTest(dispatcher) {
        fakeDao.data.add(
            CierreEntity("c-cached", "laboral", "10/2026", "2026-10-20", 5000000L, 160.0, "Cached", null, false)
        )
        fakeSyncDao.data["cierres"] = SyncStateEntity("cierres", System.currentTimeMillis() - 1000)
        fakeApi.shouldThrow = true // Si llama a la API fallará

        val result = repository.getCierres(forceRefresh = false)

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals("c-cached", data[0].id)
    }

    @Test
    fun `getCierres with forceRefresh true calls API even with valid cache`() = runTest(dispatcher) {
        fakeDao.data.add(
            CierreEntity("c-old", "contable", "09/2026", "2026-09-30", 1000L, 0.0, "Old", null, false)
        )
        fakeSyncDao.data["cierres"] = SyncStateEntity("cierres", System.currentTimeMillis() - 1000)

        val freshCierres = listOf(
            CierreHistorialItem(id = "c-fresh", tipo = "contable", periodo = "10/2026", totalMonto = 2000L)
        )
        fakeApi.cierresResponse = Response.success(freshCierres)

        val result = repository.getCierres(forceRefresh = true)

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals("c-fresh", data[0].id)
        assertEquals("c-fresh", fakeDao.data[0].id)
    }

    @Test
    fun `getCierres on network error falls back to Room local data`() = runTest(dispatcher) {
        fakeDao.data.add(
            CierreEntity("c-fallback", "contable", "09/2026", "2026-09-30", 3000L, 0.0, "Fallback", null, false)
        )
        fakeApi.shouldThrow = true // Falla de red

        val result = repository.getCierres(forceRefresh = true)

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals("c-fallback", data[0].id)
    }

    @Test
    fun `getCierres on network error without Room data returns Error`() = runTest(dispatcher) {
        fakeDao.data.clear()
        fakeApi.shouldThrow = true

        val result = repository.getCierres(forceRefresh = true)

        assertTrue(result is NetworkResult.Exception)
    }

    @Test
    fun `descargarReporteCierre success returns byte array`() = runTest(dispatcher) {
        val pdfContent = "%PDF-1.4 test report bytes".toByteArray()
        fakeApi.reporteResponse = Response.success(pdfContent.toResponseBody("application/pdf".toMediaTypeOrNull()))

        val result = repository.descargarReporteCierre("reporte_oct_2026.pdf")

        assertTrue(result is NetworkResult.Success)
        assertTrue(pdfContent.contentEquals((result as NetworkResult.Success).data))
    }

    @Test
    fun `descargarReporteCierre empty body returns Error`() = runTest(dispatcher) {
        fakeApi.reporteResponse = Response.success("".toByteArray().toResponseBody("application/pdf".toMediaTypeOrNull()))

        val result = repository.descargarReporteCierre("vacio.pdf")

        assertTrue(result is NetworkResult.Error)
    }
}

class FakeCierreDao : CierreDao {
    val data = mutableListOf<CierreEntity>()

    override suspend fun getAll(): List<CierreEntity> = data.toList()

    override suspend fun getByTipo(tipo: String): List<CierreEntity> = data.filter { it.tipo == tipo }

    override suspend fun getById(id: String): CierreEntity? = data.find { it.id == id }

    override suspend fun insertAll(cierres: List<CierreEntity>) {
        data.removeAll { c -> cierres.any { it.id == c.id } }
        data.addAll(cierres)
    }

    override suspend fun insert(cierre: CierreEntity) {
        data.removeAll { it.id == cierre.id }
        data.add(cierre)
    }

    override suspend fun deleteById(id: String) {
        data.removeAll { it.id == id }
    }

    override suspend fun deleteAll() {
        data.clear()
    }
}

class FakeSyncStateDaoForCierre : SyncStateDao {
    val data = mutableMapOf<String, SyncStateEntity>()

    override suspend fun getSyncState(key: String): SyncStateEntity? = data[key]

    override suspend fun insert(syncState: SyncStateEntity) {
        data[syncState.key] = syncState
    }
}

class FakeApiForCierreRepo : GammaApi {
    var shouldThrow = false
    var cierresResponse: Response<List<CierreHistorialItem>> = Response.success(emptyList())
    var reporteResponse: Response<okhttp3.ResponseBody> = Response.success("".toResponseBody())

    override suspend fun getCierres(): Response<List<CierreHistorialItem>> {
        if (shouldThrow) throw java.io.IOException("Network unreachable")
        return cierresResponse
    }

    override suspend fun descargarReporteCierre(filename: String): Response<okhttp3.ResponseBody> {
        if (shouldThrow) throw java.io.IOException("Network error downloading file")
        return reporteResponse
    }

    override suspend fun descargarArchivoPdf(url: String): Response<okhttp3.ResponseBody> {
        if (shouldThrow) throw java.io.IOException("Network error downloading pdf")
        return reporteResponse
    }

    override suspend fun checkHealth(): Response<Unit> = throw NotImplementedError()
    override suspend fun getDashboardSummary(): Response<DashboardSummary> = throw NotImplementedError()
    override suspend fun getMovimientos(): Response<List<Movimiento>> = throw NotImplementedError()
    override suspend fun createMovimiento(movimiento: Movimiento): Response<Unit> = throw NotImplementedError()
    override suspend fun interpretarMovimientoIa(request: IaTextRequest): Response<Movimiento> = throw NotImplementedError()
    override suspend fun interpretarMovimientoOcr(file: okhttp3.MultipartBody.Part): Response<Movimiento> = throw NotImplementedError()
    override suspend fun getAvisos(): Response<List<Aviso>> = throw NotImplementedError()
    override suspend fun createAviso(aviso: Aviso): Response<Unit> = throw NotImplementedError()
    override suspend fun deleteAviso(id: String): Response<Unit> = throw NotImplementedError()
    override suspend fun updateAviso(id: String, aviso: Aviso): Response<Unit> = throw NotImplementedError()
    override suspend fun interpretarAvisoIa(request: IaTextRequest): Response<Aviso> = throw NotImplementedError()
    override suspend fun getObligaciones(): Response<List<com.gamma.mobile.domain.model.Obligacion>> = throw NotImplementedError()
    override suspend fun createObligacion(obligacion: com.gamma.mobile.domain.model.Obligacion): Response<Unit> = throw NotImplementedError()
    override suspend fun updateObligacion(id: String, obligacion: com.gamma.mobile.domain.model.Obligacion): Response<Unit> = throw NotImplementedError()
    override suspend fun deleteObligacion(id: String): Response<Unit> = throw NotImplementedError()
    override suspend fun abonarObligacion(request: com.gamma.mobile.domain.model.AbonoRequest): Response<com.gamma.mobile.domain.model.AbonoResponse> = throw NotImplementedError()
    override suspend fun getMaterias(): Response<List<String>> = throw NotImplementedError()
    override suspend fun marcarAsistencia(request: com.gamma.mobile.domain.model.AsistenciaMarcaRequest): Response<com.gamma.mobile.domain.model.AsistenciaMarcaResponse> = throw NotImplementedError()
    override suspend fun marcarMateria(request: com.gamma.mobile.domain.model.AsistenciaMateriaRequest): Response<com.gamma.mobile.domain.model.AsistenciaMateriaResponse> = throw NotImplementedError()
    override suspend fun ejecutarCierre(request: CierreRequest): Response<CierreResponse> = throw NotImplementedError()
    override suspend fun getHealthStatus(): Response<com.gamma.mobile.domain.model.HealthStatus> = throw NotImplementedError()
    override suspend fun getTickets(): Response<List<com.gamma.mobile.domain.model.TicketItem>> = throw NotImplementedError()
    override suspend fun createTicket(request: com.gamma.mobile.domain.model.TicketCreate): Response<com.gamma.mobile.domain.model.TicketItem> = throw NotImplementedError()
    override suspend fun updateTicket(id: String, request: com.gamma.mobile.domain.model.TicketUpdate): Response<com.gamma.mobile.domain.model.TicketItem> = throw NotImplementedError()
    override suspend fun getLogs(): Response<List<com.gamma.mobile.domain.model.LogEntry>> = throw NotImplementedError()
    override suspend fun getCronogramaVencimientos(): Response<List<com.gamma.mobile.domain.model.VencimientoCronogramaItem>> = throw NotImplementedError()
    override suspend fun getDeudaFoco(criterio: String): Response<com.gamma.mobile.domain.model.DeudaFocoResponse> = throw NotImplementedError()
    override suspend fun proyectarCapacidad(request: com.gamma.mobile.domain.model.CapacidadPagoRequest): Response<com.gamma.mobile.domain.model.CapacidadPagoResponse> = throw NotImplementedError()
}
