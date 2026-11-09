package com.gamma.mobile.domain.repository

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.ClimaInfo
import kotlinx.coroutines.flow.Flow

interface ClimaRepository {
    fun getClimaFlow(): Flow<ClimaInfo?>
    suspend fun getClimaCached(): ClimaInfo?
    suspend fun refreshClima(): NetworkResult<ClimaInfo>
}
