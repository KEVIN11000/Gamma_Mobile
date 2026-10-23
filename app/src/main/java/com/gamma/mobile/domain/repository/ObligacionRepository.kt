package com.gamma.mobile.domain.repository

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Obligacion

interface ObligacionRepository {
    suspend fun getObligaciones(forceRefresh: Boolean = false): NetworkResult<List<Obligacion>>
    suspend fun createObligacion(obligacion: Obligacion): NetworkResult<Unit>
    suspend fun updateObligacion(obligacion: Obligacion): NetworkResult<Unit>
    suspend fun deleteObligacion(id: String): NetworkResult<Unit>
    suspend fun abonarObligacion(idObligacion: String, monto: Long): NetworkResult<Unit>
    suspend fun getCronogramaVencimientos(forceRefresh: Boolean = false): NetworkResult<List<com.gamma.mobile.domain.model.VencimientoCronogramaItem>>
    suspend fun getDeudaFoco(criterio: String = "saldo"): NetworkResult<com.gamma.mobile.domain.model.DeudaFocoResponse>
    suspend fun proyectarCapacidad(request: com.gamma.mobile.domain.model.CapacidadPagoRequest): NetworkResult<com.gamma.mobile.domain.model.CapacidadPagoResponse>
}
