package com.gamma.mobile.domain.repository

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Movimiento
import kotlinx.coroutines.flow.Flow

interface MovimientoRepository {
    suspend fun getMovimientos(forceRefresh: Boolean = false): NetworkResult<List<Movimiento>>
    fun getMovimientosLocal(): Flow<List<Movimiento>>
}
