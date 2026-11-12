package com.gamma.mobile.domain.repository

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.GastoFijoCreateDto
import com.gamma.mobile.data.remote.dto.GastoFijoUpdateDto
import com.gamma.mobile.data.remote.dto.PresupuestoAccionResponseDto
import com.gamma.mobile.data.remote.dto.PresupuestoFijosResponseDto
import com.gamma.mobile.data.remote.dto.SalarioBaseUpdateDto
import com.gamma.mobile.data.remote.dto.TechoPresupuestarioItemDto
import com.gamma.mobile.data.remote.dto.TechoPresupuestarioUpdateDto

interface PresupuestoRepository {
    suspend fun getGastosFijos(forceRefresh: Boolean = false): NetworkResult<PresupuestoFijosResponseDto>
    suspend fun createGastoFijo(request: GastoFijoCreateDto): NetworkResult<PresupuestoAccionResponseDto>
    suspend fun updateGastoFijo(concepto: String, request: GastoFijoUpdateDto): NetworkResult<PresupuestoAccionResponseDto>
    suspend fun deleteGastoFijo(concepto: String): NetworkResult<PresupuestoAccionResponseDto>
    suspend fun updateSalarioBase(request: SalarioBaseUpdateDto): NetworkResult<PresupuestoAccionResponseDto>
    suspend fun getTechosPresupuestarios(forceRefresh: Boolean = false): NetworkResult<List<TechoPresupuestarioItemDto>>
    suspend fun updateTechoPresupuestario(categoria: String, request: TechoPresupuestarioUpdateDto): NetworkResult<PresupuestoAccionResponseDto>
}
