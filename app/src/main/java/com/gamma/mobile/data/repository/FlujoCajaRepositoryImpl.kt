package com.gamma.mobile.data.repository

import com.gamma.mobile.data.local.dao.FlujoCajaDao
import com.gamma.mobile.data.local.entity.FlujoCajaEntity
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.FlujoCajaResponseDto
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.model.DiaCritico
import com.gamma.mobile.domain.model.FlujoCaja
import com.gamma.mobile.domain.model.FlujoCajaItemDiario
import com.gamma.mobile.domain.repository.FlujoCajaRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named

class FlujoCajaRepositoryImpl @Inject constructor(
    private val api: GammaApi,
    private val dao: FlujoCajaDao,
    private val gson: Gson,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) : FlujoCajaRepository {

    private val diasCriticosType = object : TypeToken<List<DiaCritico>>() {}.type
    private val diasType = object : TypeToken<List<FlujoCajaItemDiario>>() {}.type

    override fun getFlujoCajaFlow(): Flow<FlujoCaja?> {
        return dao.getFlujoCajaFlow().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getFlujoCajaCached(): FlujoCaja? {
        return dao.getFlujoCaja()?.toDomain()
    }

    override suspend fun refreshFlujoCaja(
        saldoInicial: Long?,
        gastoDiario: Long?,
        umbralReserva: Long?
    ): NetworkResult<FlujoCaja> {
        val result = safeApiCall(dispatcher) {
            api.getFlujoCaja(saldoInicial, gastoDiario, umbralReserva)
        }

        return when (result) {
            is NetworkResult.Success -> {
                val dto = result.data
                val entity = dto.toEntity()
                dao.insertFlujoCaja(entity)
                NetworkResult.Success(dto.toDomain())
            }
            is NetworkResult.Error -> NetworkResult.Error(result.code, result.message)
            is NetworkResult.Exception -> NetworkResult.Exception(result.e)
        }
    }

    private fun FlujoCajaEntity.toDomain(): FlujoCaja {
        val criticos: List<DiaCritico> = try {
            gson.fromJson(diasCriticosJson, diasCriticosType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        val diasList: List<FlujoCajaItemDiario> = try {
            gson.fromJson(diasJson, diasType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        return FlujoCaja(
            saldoActual = saldoActual,
            saldoProyectado30d = saldoProyectado30d,
            totalIngresosProyectados = totalIngresosProyectados,
            totalGastosFijos = totalGastosFijos,
            totalGastosVariablesEstimados = totalGastosVariablesEstimados,
            diasCriticos = criticos,
            dias = diasList
        )
    }

    private fun FlujoCajaResponseDto.toDomain(): FlujoCaja {
        return FlujoCaja(
            saldoActual = saldoActual,
            saldoProyectado30d = saldoProyectado30d,
            totalIngresosProyectados = totalIngresosProyectados,
            totalGastosFijos = totalGastosFijos,
            totalGastosVariablesEstimados = totalGastosVariablesEstimados,
            diasCriticos = diasCriticos.map { DiaCritico(it.fecha, it.saldo, it.motivo) },
            dias = dias.map {
                FlujoCajaItemDiario(
                    fecha = it.fecha,
                    saldoInicial = it.saldoInicial,
                    ingresos = it.ingresos,
                    egresosFijos = it.egresosFijos,
                    egresosVariables = it.egresosVariables,
                    saldoFinal = it.saldoFinal,
                    compromisos = it.compromisos
                )
            }
        )
    }

    private fun FlujoCajaResponseDto.toEntity(id: String = "current"): FlujoCajaEntity {
        val criticosDomain = diasCriticos.map { DiaCritico(it.fecha, it.saldo, it.motivo) }
        val diasDomain = dias.map {
            FlujoCajaItemDiario(
                fecha = it.fecha,
                saldoInicial = it.saldoInicial,
                ingresos = it.ingresos,
                egresosFijos = it.egresosFijos,
                egresosVariables = it.egresosVariables,
                saldoFinal = it.saldoFinal,
                compromisos = it.compromisos
            )
        }
        return FlujoCajaEntity(
            id = id,
            saldoActual = saldoActual,
            saldoProyectado30d = saldoProyectado30d,
            totalIngresosProyectados = totalIngresosProyectados,
            totalGastosFijos = totalGastosFijos,
            totalGastosVariablesEstimados = totalGastosVariablesEstimados,
            diasCriticosJson = gson.toJson(criticosDomain),
            diasJson = gson.toJson(diasDomain),
            updatedAt = System.currentTimeMillis()
        )
    }
}
