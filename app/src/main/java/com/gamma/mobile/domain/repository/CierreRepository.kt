package com.gamma.mobile.domain.repository

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.CierreResponse

interface CierreRepository {
    suspend fun ejecutarCierre(hoja: String, aplicarDescuento: Boolean, tipo: String = "horas"): NetworkResult<CierreResponse>
    suspend fun descargarPdf(url: String): NetworkResult<ByteArray>
}
