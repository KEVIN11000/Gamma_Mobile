package com.gamma.mobile.domain.usecase

import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.SifenRucRequestDto
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.model.SifenRucInfo
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Named

class ConsultarRucSifenUseCase @Inject constructor(
    private val api: GammaApi,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(ruc: String): NetworkResult<SifenRucInfo> {
        val cleanRuc = ruc.trim()
        if (cleanRuc.isBlank()) {
            return NetworkResult.Error(code = 400, message = "El RUC proporcionado no es válido.")
        }

        val result = safeApiCall(dispatcher) {
            api.consultarRucSifen(SifenRucRequestDto(ruc = cleanRuc))
        }

        return when (result) {
            is NetworkResult.Success -> {
                val dto = result.data
                NetworkResult.Success(
                    SifenRucInfo(
                        ruc = dto.ruc.ifBlank { cleanRuc },
                        razonSocial = dto.razonSocial,
                        estado = dto.estado
                    )
                )
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
            is NetworkResult.Exception -> NetworkResult.Exception(result.e)
        }
    }
}
