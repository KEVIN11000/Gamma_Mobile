package com.gamma.mobile.domain.repository

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.CierreHistorialItem

interface CierresRepository {
    suspend fun getCierres(forceRefresh: Boolean = false): NetworkResult<List<CierreHistorialItem>>
    suspend fun descargarReporteCierre(filename: String): NetworkResult<ByteArray>
}
