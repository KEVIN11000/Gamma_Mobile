package com.gamma.mobile.domain.repository

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.Aviso

interface AvisoRepository {
    suspend fun getAvisos(forceRefresh: Boolean = false): NetworkResult<List<Aviso>>
}
