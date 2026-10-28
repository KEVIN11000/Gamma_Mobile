package com.gamma.mobile.domain.repository

import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.domain.model.AnaliticasHorasResponse
import com.gamma.mobile.domain.model.AsistenciaMarcaResponse
import com.gamma.mobile.domain.model.AsistenciaMateriaResponse
import com.gamma.mobile.domain.model.PeriodosResponse

interface AsistenciaRepository {
    suspend fun marcarAsistencia(modo: String): NetworkResult<AsistenciaMarcaResponse>
    suspend fun marcarMateria(materia: String): NetworkResult<AsistenciaMateriaResponse>
    suspend fun getMaterias(): NetworkResult<List<String>>
    suspend fun getPeriodos(): NetworkResult<PeriodosResponse>
    suspend fun getAnaliticas(hoja: String? = null): NetworkResult<AnaliticasHorasResponse>
    suspend fun descargarReporteExcel(
        hoja: String? = null,
        descuento: Double = 0.0,
        saldoAnterior: Double = 0.0,
        incluirIva: Boolean = true
    ): NetworkResult<ByteArray>
    suspend fun descargarReportePdfAsistencia(
        hoja: String? = null,
        descuento: Double = 0.0,
        saldoAnterior: Double = 0.0,
        incluirIva: Boolean = true
    ): NetworkResult<ByteArray>
}
