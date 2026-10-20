package com.gamma.mobile.domain.repository

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Aviso

interface AvisoRepository {
    suspend fun getAvisos(forceRefresh: Boolean = false): NetworkResult<List<Aviso>>
    suspend fun createAviso(aviso: Aviso): NetworkResult<Unit>
    suspend fun updateAviso(aviso: Aviso): NetworkResult<Unit>
    suspend fun deleteAviso(id: String): NetworkResult<Unit>
    suspend fun interpretarAvisoIa(texto: String): NetworkResult<Aviso>
}
