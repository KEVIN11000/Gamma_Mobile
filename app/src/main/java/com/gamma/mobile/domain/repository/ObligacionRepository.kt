package com.gamma.mobile.domain.repository

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Obligacion

interface ObligacionRepository {
    suspend fun getObligaciones(forceRefresh: Boolean = false): NetworkResult<List<Obligacion>>
    suspend fun createObligacion(obligacion: Obligacion): NetworkResult<Unit>
    suspend fun updateObligacion(obligacion: Obligacion): NetworkResult<Unit>
    suspend fun deleteObligacion(id: String): NetworkResult<Unit>
}
