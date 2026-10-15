package com.gamma.mobile.data.repository

import com.gamma.mobile.BuildConfig
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.model.CierreRequest
import com.gamma.mobile.domain.model.CierreResponse
import com.gamma.mobile.domain.model.HealthStatus
import com.gamma.mobile.domain.model.LogEntry
import com.gamma.mobile.domain.model.TicketCreate
import com.gamma.mobile.domain.model.TicketItem
import com.gamma.mobile.domain.repository.SistemaRepository
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Named

class SistemaRepositoryImpl @Inject constructor(
    private val api: GammaApi,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) : SistemaRepository {

    override suspend fun getHealthStatus(): NetworkResult<HealthStatus> {
        return safeApiCall(dispatcher) { api.getHealthStatus() }
    }

    override suspend fun getTickets(): NetworkResult<List<TicketItem>> {
        return safeApiCall(dispatcher) { api.getTickets() }
    }

    override suspend fun createTicket(asunto: String, descripcion: String): NetworkResult<TicketItem> {
        return safeApiCall(dispatcher) {
            api.createTicket(TicketCreate(asunto = asunto, descripcion = descripcion))
        }
    }

    override suspend fun updateTicket(id: String, estado: String, resolucion: String?): NetworkResult<TicketItem> {
        return safeApiCall(dispatcher) {
            api.updateTicket(id, com.gamma.mobile.domain.model.TicketUpdate(estado = estado, resolucion = resolucion))
        }
    }

    override suspend fun getLogs(): NetworkResult<List<LogEntry>> {
        return safeApiCall(dispatcher) { api.getLogs() }
    }

    override suspend fun ejecutarCierreContable(hoja: String): NetworkResult<CierreResponse> {
        return safeApiCall(dispatcher) {
            api.ejecutarCierre(CierreRequest(hoja = hoja, tipo = "contable"))
        }
    }

    override fun getAppVersion(): String {
        return BuildConfig.VERSION_NAME
    }
}
