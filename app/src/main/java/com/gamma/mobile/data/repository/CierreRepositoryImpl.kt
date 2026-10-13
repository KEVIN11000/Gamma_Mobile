package com.gamma.mobile.data.repository

import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.model.CierreRequest
import com.gamma.mobile.domain.model.CierreResponse
import com.gamma.mobile.domain.repository.CierreRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class CierreRepositoryImpl @Inject constructor(
    private val api: GammaApi,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) : CierreRepository {

    override suspend fun ejecutarCierre(hoja: String, aplicarDescuento: Boolean): NetworkResult<CierreResponse> {
        return safeApiCall(dispatcher) {
            api.ejecutarCierre(CierreRequest(hoja = hoja, aplicarDescuento = aplicarDescuento))
        }
    }

    override suspend fun descargarPdf(url: String): NetworkResult<ByteArray> {
        return withContext(dispatcher) {
            try {
                val response = api.descargarArchivoPdf(url)
                if (response.isSuccessful) {
                    val bytes = response.body()?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        NetworkResult.Success(bytes)
                    } else {
                        NetworkResult.Error(response.code(), "El archivo PDF recibido está vacío")
                    }
                } else {
                    NetworkResult.Error(response.code(), response.message())
                }
            } catch (e: Exception) {
                NetworkResult.Exception(e)
            }
        }
    }
}
