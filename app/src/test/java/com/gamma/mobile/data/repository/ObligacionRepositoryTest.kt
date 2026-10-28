package com.gamma.mobile.data.repository

import com.gamma.mobile.data.local.dao.ObligacionDao
import com.gamma.mobile.data.local.dao.SyncStateDao
import com.gamma.mobile.data.local.entity.ObligacionEntity
import com.gamma.mobile.data.local.entity.SyncStateEntity
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.IaTextRequest
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.CapacidadPagoRequest
import com.gamma.mobile.domain.model.CapacidadPagoResponse
import com.gamma.mobile.domain.model.CierreHistorialItem
import com.gamma.mobile.domain.model.CierreRequest
import com.gamma.mobile.domain.model.CierreResponse
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.model.DeudaFocoResponse
import com.gamma.mobile.domain.model.Movimiento
import com.gamma.mobile.domain.model.Obligacion
import com.gamma.mobile.domain.model.VencimientoCronogramaItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ObligacionRepositoryTest {

    private lateinit var repository: ObligacionRepositoryImpl
    private lateinit var fakeApi: FakeApiForObligacionRepo
    private lateinit var fakeDao: FakeObligacionDao
    private lateinit var fakeSyncDao: FakeSyncStateDaoForObligacion
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        fakeApi = FakeApiForObligacionRepo()
        fakeDao = FakeObligacionDao()
        fakeSyncDao = FakeSyncStateDaoForObligacion()
        repository = ObligacionRepositoryImpl(fakeApi, fakeDao, fakeSyncDao, dispatcher)
    }

    @Test
    fun `getCronogramaVencimientos returns remote data when API succeeds`() = runTest(dispatcher) {
        val remoteItems = listOf(
            VencimientoCronogramaItem(
                id = "o1",
                nombre = "Préstamo Auto",
                entidad = "Banco Familiar",
                diaVencimiento = 10,
                cuotaGs = 850000L,
                saldoActual = 12000000L,
                vencido = false,
                diasRestantes = 5,
                fechaVencimientoExacta = "10/10/2026"
            )
        )
        fakeApi.cronogramaResponse = Response.success(remoteItems)

        val result = repository.getCronogramaVencimientos()

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals(1, data.size)
        assertEquals("o1", data[0].id)
        assertEquals(850000L, data[0].cuotaGs)
    }

    @Test
    fun `getCronogramaVencimientos falls back to local calculation when API throws`() = runTest(dispatcher) {
        fakeApi.shouldThrow = true
        fakeDao.data.addAll(
            listOf(
                ObligacionEntity(
                    id = "local_1",
                    nombre = "Tarjeta Visa",
                    montoInicial = 5000000L,
                    saldoActual = 2500000L,
                    estado = "Activo",
                    entidad = "Itaú",
                    tipo = "Tarjeta",
                    cuotaReferenciaGs = 500000L,
                    cuotasTotales = 12,
                    cuotasRestantes = 5,
                    diaVencimiento = 15,
                    fechaInicio = "01/01/2026",
                    observaciones = ""
                )
            )
        )

        val result = repository.getCronogramaVencimientos()

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals(1, data.size)
        assertEquals("local_1", data[0].id)
        assertEquals(15, data[0].diaVencimiento)
        assertEquals(500000L, data[0].cuotaGs)
    }

    @Test
    fun `getDeudaFoco with criterio saldo returns remote response when API succeeds`() = runTest(dispatcher) {
        val remoteFoco = DeudaFocoResponse(
            idDeuda = "d1",
            nombre = "Préstamo Rápido",
            entidad = "Ueno",
            saldoActual = 1200000L,
            cuotaGs = 300000L,
            criterio = "saldo",
            ahorroEstimadoMeses = 4,
            explicacion = "Menor saldo"
        )
        fakeApi.focoResponse = Response.success(remoteFoco)

        val result = repository.getDeudaFoco("saldo")

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals("d1", data.idDeuda)
        assertEquals("saldo", data.criterio)
    }

    @Test
    fun `getDeudaFoco with criterio saldo falls back to local minimum saldo when API fails`() = runTest(dispatcher) {
        fakeApi.shouldThrow = true
        fakeDao.data.addAll(
            listOf(
                ObligacionEntity(
                    id = "mayor_saldo",
                    nombre = "Auto",
                    montoInicial = 20000000L,
                    saldoActual = 15000000L,
                    estado = "Activo",
                    entidad = "Banco",
                    tipo = "Deuda",
                    cuotaReferenciaGs = 1000000L,
                    cuotasTotales = 20,
                    cuotasRestantes = 15,
                    diaVencimiento = 5,
                    fechaInicio = "",
                    observaciones = ""
                ),
                ObligacionEntity(
                    id = "menor_saldo",
                    nombre = "Celular",
                    montoInicial = 3000000L,
                    saldoActual = 900000L,
                    estado = "Activo",
                    entidad = "Claro",
                    tipo = "Deuda",
                    cuotaReferenciaGs = 300000L,
                    cuotasTotales = 10,
                    cuotasRestantes = 3,
                    diaVencimiento = 10,
                    fechaInicio = "",
                    observaciones = ""
                )
            )
        )

        val result = repository.getDeudaFoco("saldo")

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals("menor_saldo", data.idDeuda)
        assertEquals(900000L, data.saldoActual)
        assertTrue(data.explicacion.contains("Bola de Nieve"))
    }

    @Test
    fun `getDeudaFoco with criterio prioridad falls back to local maximum cuota when API fails`() = runTest(dispatcher) {
        fakeApi.shouldThrow = true
        fakeDao.data.addAll(
            listOf(
                ObligacionEntity(
                    id = "menor_cuota",
                    nombre = "Celular",
                    montoInicial = 3000000L,
                    saldoActual = 900000L,
                    estado = "Activo",
                    entidad = "Claro",
                    tipo = "Deuda",
                    cuotaReferenciaGs = 150000L,
                    cuotasTotales = 10,
                    cuotasRestantes = 6,
                    diaVencimiento = 10,
                    fechaInicio = "",
                    observaciones = ""
                ),
                ObligacionEntity(
                    id = "mayor_cuota",
                    nombre = "Auto",
                    montoInicial = 20000000L,
                    saldoActual = 15000000L,
                    estado = "Activo",
                    entidad = "Banco",
                    tipo = "Deuda",
                    cuotaReferenciaGs = 1800000L,
                    cuotasTotales = 20,
                    cuotasRestantes = 8,
                    diaVencimiento = 5,
                    fechaInicio = "",
                    observaciones = ""
                )
            )
        )

        val result = repository.getDeudaFoco("prioridad")

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals("mayor_cuota", data.idDeuda)
        assertEquals(1800000L, data.cuotaGs)
        assertTrue(data.explicacion.contains("Avalancha"))
    }

    @Test
    fun `proyectarCapacidad calls remote api and returns success response`() = runTest(dispatcher) {
        val req = CapacidadPagoRequest(
            deudasPausadasSimuladas = listOf("d1"),
            abonosExtraordinarios = mapOf("d2" to 500000L),
            ajusteIngresoExtra = 1000000L
        )
        val expected = CapacidadPagoResponse(
            compromisoTotalMes = 1200000L,
            fondoDisponibleMes = 3500000L,
            capacidadNeta = 2300000L,
            semaforo = "SUPERAVIT",
            dtiProyectado = 34.2
        )
        fakeApi.proyectarResponse = Response.success(expected)

        val result = repository.proyectarCapacidad(req)

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals("SUPERAVIT", data.semaforo)
        assertEquals(2300000L, data.capacidadNeta)
    }

    @Test
    fun `updateObligacion updates Room and calls api`() = runTest(dispatcher) {
        val obligacion = Obligacion(
            id = "o_update",
            nombre = "Préstamo",
            montoInicial = 5000000L,
            saldoActual = 4000000L,
            estado = "Pausada"
        )
        fakeApi.updateObligacionResponse = Response.success(Unit)

        val result = repository.updateObligacion(obligacion)

        assertTrue(result is NetworkResult.Success)
        assertEquals(1, fakeDao.data.size)
        assertEquals("Pausada", fakeDao.data[0].estado)
        assertEquals("o_update", fakeApi.lastUpdatedId)
    }
}

class FakeApiForObligacionRepo : GammaApi {
    var shouldThrow = false
    var cronogramaResponse: Response<List<VencimientoCronogramaItem>> = Response.success(emptyList())
    var focoResponse: Response<DeudaFocoResponse> = Response.success(DeudaFocoResponse())
    var proyectarResponse: Response<CapacidadPagoResponse> = Response.success(CapacidadPagoResponse())
    var updateObligacionResponse: Response<Unit> = Response.success(Unit)
    var lastUpdatedId: String? = null

    override suspend fun getCronogramaVencimientos(): Response<List<VencimientoCronogramaItem>> {
        if (shouldThrow) throw java.io.IOException("Network unreachable")
        return cronogramaResponse
    }

    override suspend fun getDeudaFoco(criterio: String): Response<DeudaFocoResponse> {
        if (shouldThrow) throw java.io.IOException("Network unreachable")
        return focoResponse
    }

    override suspend fun proyectarCapacidad(request: CapacidadPagoRequest): Response<CapacidadPagoResponse> {
        if (shouldThrow) throw java.io.IOException("Network unreachable")
        return proyectarResponse
    }

    override suspend fun updateObligacion(id: String, obligacion: Obligacion): Response<Unit> {
        if (shouldThrow) throw java.io.IOException("Network unreachable")
        lastUpdatedId = id
        return updateObligacionResponse
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
    override suspend fun getObligaciones(): Response<List<Obligacion>> = throw NotImplementedError()
    override suspend fun createObligacion(obligacion: Obligacion): Response<Unit> = throw NotImplementedError()
    override suspend fun deleteObligacion(id: String): Response<Unit> = throw NotImplementedError()
    override suspend fun abonarObligacion(request: com.gamma.mobile.domain.model.AbonoRequest): Response<com.gamma.mobile.domain.model.AbonoResponse> = throw NotImplementedError()
    override suspend fun getMaterias(): Response<List<String>> = throw NotImplementedError()
    override suspend fun marcarAsistencia(request: com.gamma.mobile.domain.model.AsistenciaMarcaRequest): Response<com.gamma.mobile.domain.model.AsistenciaMarcaResponse> = throw NotImplementedError()
    override suspend fun marcarMateria(request: com.gamma.mobile.domain.model.AsistenciaMateriaRequest): Response<com.gamma.mobile.domain.model.AsistenciaMateriaResponse> = throw NotImplementedError()
    override suspend fun ejecutarCierre(request: CierreRequest): Response<CierreResponse> = throw NotImplementedError()
    override suspend fun getCierres(): Response<List<CierreHistorialItem>> = throw NotImplementedError()
    override suspend fun descargarReporteCierre(filename: String): Response<okhttp3.ResponseBody> = throw NotImplementedError()
    override suspend fun descargarArchivoPdf(url: String): Response<okhttp3.ResponseBody> = throw NotImplementedError()
    override suspend fun getHealthStatus(): Response<com.gamma.mobile.domain.model.HealthStatus> = throw NotImplementedError()
    override suspend fun getTickets(): Response<List<com.gamma.mobile.domain.model.TicketItem>> = throw NotImplementedError()
    override suspend fun createTicket(request: com.gamma.mobile.domain.model.TicketCreate): Response<com.gamma.mobile.domain.model.TicketItem> = throw NotImplementedError()
    override suspend fun updateTicket(id: String, request: com.gamma.mobile.domain.model.TicketUpdate): Response<com.gamma.mobile.domain.model.TicketItem> = throw NotImplementedError()
    override suspend fun getLogs(): Response<List<com.gamma.mobile.domain.model.LogEntry>> = throw NotImplementedError()
    override suspend fun getPeriodos(): Response<com.gamma.mobile.domain.model.PeriodosResponse> = throw NotImplementedError()
    override suspend fun getAnaliticas(hoja: String?): Response<com.gamma.mobile.domain.model.AnaliticasHorasResponse> = throw NotImplementedError()
    override suspend fun descargarReporteExcel(hoja: String?, descuento: Double, saldoAnterior: Double, incluirIva: Boolean): Response<okhttp3.ResponseBody> = throw NotImplementedError()
    override suspend fun descargarReportePdfAsistencia(hoja: String?, descuento: Double, saldoAnterior: Double, incluirIva: Boolean): Response<okhttp3.ResponseBody> = throw NotImplementedError()
}

class FakeObligacionDao : ObligacionDao {
    val data = mutableListOf<ObligacionEntity>()

    override suspend fun getAll(): List<ObligacionEntity> = data

    override suspend fun insertAll(obligaciones: List<ObligacionEntity>) {
        data.removeAll { entity -> obligaciones.any { it.id == entity.id } }
        data.addAll(obligaciones)
    }

    override suspend fun deleteAll() {
        data.clear()
    }

    override suspend fun deleteById(id: String) {
        data.removeAll { it.id == id }
    }
}

class FakeSyncStateDaoForObligacion : SyncStateDao {
    private val data = mutableMapOf<String, SyncStateEntity>()

    override suspend fun getSyncState(key: String): SyncStateEntity? = data[key]

    override suspend fun insert(syncState: SyncStateEntity) {
        data[syncState.key] = syncState
    }
}
