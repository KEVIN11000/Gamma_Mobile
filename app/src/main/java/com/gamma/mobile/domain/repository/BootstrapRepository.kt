package com.gamma.mobile.domain.repository

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.BootstrapResponseDto

interface BootstrapRepository {
    /**
     * Ejecuta una llamada unificada a GET /api/v1/bootstrap e hidrata atómicamente
     * el estado local (DashboardSummary, Avisos, Vencimientos y Periodo activo)
     * en una única transacción de red.
     */
    suspend fun syncBootstrap(): NetworkResult<BootstrapResponseDto>
}
