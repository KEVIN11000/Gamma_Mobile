package com.gamma.mobile.data.remote

import com.gamma.mobile.data.remote.dto.BootstrapResponseDto
import com.gamma.mobile.data.remote.dto.ClimaResponseDto
import com.gamma.mobile.data.remote.dto.DispositivoCreateDto
import com.gamma.mobile.data.remote.dto.DispositivoResponseDto
import com.gamma.mobile.data.remote.dto.FlujoCajaResponseDto
import com.gamma.mobile.data.remote.dto.ParseMovementRequestDto
import com.gamma.mobile.data.remote.dto.ParsedMovementDto
import com.gamma.mobile.data.remote.dto.SifenRucRequestDto
import com.gamma.mobile.data.remote.dto.SifenRucResponseDto
import com.gamma.mobile.domain.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Base fake implementation of [GammaApi] for unit tests with default NotImplementedError stubs.
 */
open class BaseFakeGammaApi : GammaApi {
    override suspend fun checkHealth(): Response<Unit> = throw NotImplementedError()
    override suspend fun getDashboardSummary(): Response<DashboardSummary> = throw NotImplementedError()
    override suspend fun getMovimientos(): Response<List<Movimiento>> = throw NotImplementedError()
    override suspend fun createMovimiento(movimiento: Movimiento): Response<Unit> = throw NotImplementedError()
    override suspend fun interpretarMovimientoIa(request: IaTextRequest): Response<Movimiento> = throw NotImplementedError()
    override suspend fun interpretarMovimientoOcr(file: MultipartBody.Part): Response<Movimiento> = throw NotImplementedError()
    override suspend fun getAvisos(): Response<List<Aviso>> = throw NotImplementedError()
    override suspend fun createAviso(aviso: Aviso): Response<Unit> = throw NotImplementedError()
    override suspend fun deleteAviso(id: String): Response<Unit> = throw NotImplementedError()
    override suspend fun updateAviso(id: String, aviso: Aviso): Response<Unit> = throw NotImplementedError()
    override suspend fun interpretarAvisoIa(request: IaTextRequest): Response<Aviso> = throw NotImplementedError()
    override suspend fun getObligaciones(): Response<List<Obligacion>> = throw NotImplementedError()
    override suspend fun createObligacion(obligacion: Obligacion): Response<Unit> = throw NotImplementedError()
    override suspend fun updateObligacion(id: String, obligacion: Obligacion): Response<Unit> = throw NotImplementedError()
    override suspend fun deleteObligacion(id: String): Response<Unit> = throw NotImplementedError()
    override suspend fun abonarObligacion(request: AbonoRequest): Response<AbonoResponse> = throw NotImplementedError()
    override suspend fun getCronogramaVencimientos(): Response<List<VencimientoCronogramaItem>> = throw NotImplementedError()
    override suspend fun getDeudaFoco(criterio: String): Response<DeudaFocoResponse> = throw NotImplementedError()
    override suspend fun proyectarCapacidad(request: CapacidadPagoRequest): Response<CapacidadPagoResponse> = throw NotImplementedError()
    override suspend fun getMaterias(): Response<List<String>> = throw NotImplementedError()
    override suspend fun marcarAsistencia(request: AsistenciaMarcaRequest): Response<AsistenciaMarcaResponse> = throw NotImplementedError()
    override suspend fun marcarMateria(request: AsistenciaMateriaRequest): Response<AsistenciaMateriaResponse> = throw NotImplementedError()
    override suspend fun getPeriodos(): Response<PeriodosResponse> = throw NotImplementedError()
    override suspend fun getAnaliticas(hoja: String?): Response<AnaliticasHorasResponse> = throw NotImplementedError()
    override suspend fun descargarReporteExcel(hoja: String?, descuento: Double, saldoAnterior: Double, incluirIva: Boolean): Response<ResponseBody> = throw NotImplementedError()
    override suspend fun descargarReportePdfAsistencia(hoja: String?, descuento: Double, saldoAnterior: Double, incluirIva: Boolean): Response<ResponseBody> = throw NotImplementedError()
    override suspend fun ejecutarCierre(request: CierreRequest): Response<CierreResponse> = throw NotImplementedError()
    override suspend fun getCierres(): Response<List<CierreHistorialItem>> = throw NotImplementedError()
    override suspend fun descargarReporteCierre(filename: String): Response<ResponseBody> = throw NotImplementedError()
    override suspend fun descargarArchivoPdf(url: String): Response<ResponseBody> = throw NotImplementedError()
    override suspend fun getHealthStatus(): Response<HealthStatus> = throw NotImplementedError()
    override suspend fun getTickets(): Response<List<TicketItem>> = throw NotImplementedError()
    override suspend fun createTicket(request: TicketCreate): Response<TicketItem> = throw NotImplementedError()
    override suspend fun updateTicket(id: String, request: TicketUpdate): Response<TicketItem> = throw NotImplementedError()
    override suspend fun getLogs(): Response<List<LogEntry>> = throw NotImplementedError()
    override suspend fun getBootstrap(): Response<BootstrapResponseDto> = throw NotImplementedError()
    override suspend fun getFlujoCaja(saldoInicial: Long?, gastoDiario: Long?, umbralReserva: Long?): Response<FlujoCajaResponseDto> = throw NotImplementedError()
    override suspend fun parseMovimientoNlp(request: ParseMovementRequestDto): Response<ParsedMovementDto> = throw NotImplementedError()
    override suspend fun getClima(): Response<ClimaResponseDto> = throw NotImplementedError()
    override suspend fun registrarDispositivo(request: DispositivoCreateDto): Response<DispositivoResponseDto> = throw NotImplementedError()
    override suspend fun consultarRucSifen(request: SifenRucRequestDto): Response<SifenRucResponseDto> = throw NotImplementedError()
    override suspend fun getMovimientosDelta(since: String): Response<List<Movimiento>> = throw NotImplementedError()
}
