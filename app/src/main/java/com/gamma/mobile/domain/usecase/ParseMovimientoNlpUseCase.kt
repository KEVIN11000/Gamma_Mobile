package com.gamma.mobile.domain.usecase

import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.ParseMovementRequestDto
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.model.ParsedMovement
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Named

class ParseMovimientoNlpUseCase @Inject constructor(
    private val api: GammaApi,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(texto: String): NetworkResult<ParsedMovement> {
        val cleanText = texto.trim()
        if (cleanText.isBlank()) {
            return NetworkResult.Error(code = 400, message = "El texto a interpretar no puede estar vacío.")
        }

        val result = safeApiCall(dispatcher) {
            api.parseMovimientoNlp(ParseMovementRequestDto(texto = cleanText))
        }

        return when (result) {
            is NetworkResult.Success -> {
                val dto = result.data
                NetworkResult.Success(
                    ParsedMovement(
                        valido = dto.valido,
                        monto = dto.monto,
                        moneda = dto.moneda,
                        tipoMovimiento = dto.tipoMovimiento,
                        categoria = dto.categoria,
                        proveedorCliente = dto.proveedorCliente,
                        descripcion = dto.descripcion,
                        confianza = dto.confianza
                    )
                )
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
            is NetworkResult.Exception -> NetworkResult.Exception(result.e)
        }
    }
}
