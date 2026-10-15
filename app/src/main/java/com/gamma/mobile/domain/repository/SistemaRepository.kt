package com.gamma.mobile.domain.repository

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.CierreResponse
import com.gamma.mobile.domain.model.HealthStatus
import com.gamma.mobile.domain.model.LogEntry
import com.gamma.mobile.domain.model.TicketItem

interface SistemaRepository {
    suspend fun getHealthStatus(): NetworkResult<HealthStatus>
    suspend fun getTickets(): NetworkResult<List<TicketItem>>
    suspend fun createTicket(asunto: String, descripcion: String): NetworkResult<TicketItem>
    suspend fun updateTicket(id: String, estado: String, resolucion: String? = null): NetworkResult<TicketItem>
    suspend fun getLogs(): NetworkResult<List<LogEntry>>
    suspend fun ejecutarCierreContable(hoja: String = "Período Actual"): NetworkResult<CierreResponse>
    fun getAppVersion(): String
}
