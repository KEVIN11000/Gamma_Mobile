package com.gamma.mobile.domain.repository

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.FlujoCaja
import kotlinx.coroutines.flow.Flow

interface FlujoCajaRepository {
    fun getFlujoCajaFlow(): Flow<FlujoCaja?>
    suspend fun getFlujoCajaCached(): FlujoCaja?
    suspend fun refreshFlujoCaja(
        saldoInicial: Long? = null,
        gastoDiario: Long? = null,
        umbralReserva: Long? = null
    ): NetworkResult<FlujoCaja>
}
