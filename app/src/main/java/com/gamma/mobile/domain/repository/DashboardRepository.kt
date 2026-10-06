package com.gamma.mobile.domain.repository

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.DashboardSummary

interface DashboardRepository {
    suspend fun getSummary(forceRefresh: Boolean = false): NetworkResult<DashboardSummary>
}
