package com.gamma.mobile.data.repository

import com.gamma.mobile.data.local.dao.ObligacionDao
import com.gamma.mobile.data.local.dao.SyncStateDao
import com.gamma.mobile.data.local.entity.SyncStateEntity
import com.gamma.mobile.data.local.entity.toDomain
import com.gamma.mobile.data.local.entity.toEntity
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.model.Obligacion
import com.gamma.mobile.domain.model.VencimientoCronogramaItem
import com.gamma.mobile.domain.model.DeudaFocoResponse
import com.gamma.mobile.domain.model.CapacidadPagoRequest
import com.gamma.mobile.domain.model.CapacidadPagoResponse
import com.gamma.mobile.domain.repository.ObligacionRepository
import com.gamma.mobile.utils.obtenerEstadoVencimiento
import com.gamma.mobile.utils.formatGuaranies
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Named

class ObligacionRepositoryImpl @Inject constructor(
    private val api: GammaApi,
    private val obligacionDao: ObligacionDao,
    private val syncStateDao: SyncStateDao,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) : ObligacionRepository {

    override suspend fun getObligaciones(forceRefresh: Boolean): NetworkResult<List<Obligacion>> {
        val syncState = syncStateDao.getSyncState(SYNC_KEY)
        val isCacheValid = syncState != null && (System.currentTimeMillis() - syncState.lastSyncTimestamp) < CACHE_TTL

        if (!forceRefresh && isCacheValid) {
            val localData = obligacionDao.getAll().map { it.toDomain() }
            if (localData.isNotEmpty()) {
                return NetworkResult.Success(localData)
            }
        }

        return when (val response = safeApiCall(dispatcher) { api.getObligaciones() }) {
            is NetworkResult.Success -> {
                obligacionDao.deleteAll()
                obligacionDao.insertAll(response.data.map { it.toEntity() })
                syncStateDao.insert(SyncStateEntity(SYNC_KEY, System.currentTimeMillis()))
                NetworkResult.Success(response.data)
            }
            else -> {
                val localData = obligacionDao.getAll().map { it.toDomain() }
                if (localData.isNotEmpty()) NetworkResult.Success(localData) else response
            }
        }
    }

    override suspend fun createObligacion(obligacion: Obligacion): NetworkResult<Unit> {
        val result = safeApiCall(dispatcher) { api.createObligacion(obligacion) }
        if (result is NetworkResult.Success) {
            obligacionDao.insertAll(listOf(obligacion.toEntity()))
        }
        return result
    }

    override suspend fun updateObligacion(obligacion: Obligacion): NetworkResult<Unit> {
        // Persistencia local en Room y sincronización con PUT /obligaciones/{id}
        obligacionDao.insertAll(listOf(obligacion.toEntity()))
        return safeApiCall(dispatcher) { api.updateObligacion(obligacion.id, obligacion) }
    }

    override suspend fun deleteObligacion(id: String): NetworkResult<Unit> {
        val result = safeApiCall(dispatcher) { api.deleteObligacion(id) }
        if (result is NetworkResult.Success) {
            obligacionDao.deleteById(id)
        }
        return result
    }

    override suspend fun abonarObligacion(idObligacion: String, monto: Long): NetworkResult<Unit> {
        val result = safeApiCall(dispatcher) {
            api.abonarObligacion(com.gamma.mobile.domain.model.AbonoRequest(idObligacion = idObligacion, monto = monto))
        }
        if (result is NetworkResult.Success) {
            syncStateDao.insert(SyncStateEntity(SYNC_KEY, 0L))
        }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(Unit)
            is NetworkResult.Error -> result
            is NetworkResult.Exception -> result
        }
    }

    override suspend fun getCronogramaVencimientos(forceRefresh: Boolean): NetworkResult<List<VencimientoCronogramaItem>> {
        val remoteRes = safeApiCall(dispatcher) { api.getCronogramaVencimientos() }
        if (remoteRes is NetworkResult.Success && remoteRes.data.isNotEmpty()) {
            return remoteRes
        }

        // Fallback Offline: Calcular cronograma local a partir de Room
        val localObligaciones = obligacionDao.getAll().map { it.toDomain() }
        if (localObligaciones.isNotEmpty()) {
            val cal = java.util.Calendar.getInstance()
            val diaActual = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val mesActual = cal.get(java.util.Calendar.MONTH) + 1
            val anioActual = cal.get(java.util.Calendar.YEAR)

            val cronogramaLocal = localObligaciones
                .filter { it.esActiva && it.diaVencimiento > 0 }
                .map { obligacion ->
                    val vencimientoInfo = obligacion.obtenerEstadoVencimiento(cal)
                    val vencido = vencimientoInfo?.esVencido ?: (diaActual > obligacion.diaVencimiento)
                    val diasRestantes = if (vencido) 0 else (obligacion.diaVencimiento - diaActual).coerceAtLeast(0)
                    val fechaStr = String.format(java.util.Locale.getDefault(), "%02d/%02d/%04d", obligacion.diaVencimiento, mesActual, anioActual)

                    VencimientoCronogramaItem(
                        id = obligacion.id,
                        nombre = obligacion.nombre,
                        entidad = obligacion.entidad,
                        diaVencimiento = obligacion.diaVencimiento,
                        cuotaGs = obligacion.cuotaReferenciaGs,
                        saldoActual = obligacion.saldoActual,
                        vencido = vencido,
                        diasRestantes = diasRestantes,
                        fechaVencimientoExacta = fechaStr
                    )
                }
                .sortedBy { it.diaVencimiento }

            return NetworkResult.Success(cronogramaLocal)
        }

        return remoteRes
    }

    override suspend fun getDeudaFoco(criterio: String): NetworkResult<DeudaFocoResponse> {
        val remoteRes = safeApiCall(dispatcher) { api.getDeudaFoco(criterio) }
        if (remoteRes is NetworkResult.Success && !remoteRes.data.idDeuda.isNullOrBlank()) {
            return remoteRes
        }

        // Fallback Offline: Calcular deuda foco localmente
        val activas = obligacionDao.getAll().map { it.toDomain() }.filter { it.esActiva && !it.esPausada }
        if (activas.isNotEmpty()) {
            val foco = if (criterio.equals("prioridad", ignoreCase = true)) {
                // Avalancha: mayor cuota o mayor saldo
                activas.maxByOrNull { it.cuotaReferenciaGs } ?: activas.maxByOrNull { it.saldoActual }
            } else {
                // Bola de Nieve: menor saldo actual
                activas.minByOrNull { it.saldoActual }
            }

            if (foco != null) {
                val mesesAhorro = if (foco.cuotaReferenciaGs > 0) (foco.saldoActual / foco.cuotaReferenciaGs).toInt() else 1
                val explicacionLocal = if (criterio.equals("prioridad", ignoreCase = true)) {
                    "Estrategia Avalancha: ${foco.nombre} tiene la cuota más alta (${formatGuaranies(foco.cuotaReferenciaGs)}). Liquidarla reduce el costo financiero más rápido."
                } else {
                    "Estrategia Bola de Nieve: ${foco.nombre} tiene el menor saldo (${formatGuaranies(foco.saldoActual)}). Liquidarla primero genera una victoria rápida."
                }

                return NetworkResult.Success(
                    DeudaFocoResponse(
                        idDeuda = foco.id,
                        nombre = foco.nombre,
                        entidad = foco.entidad,
                        saldoActual = foco.saldoActual,
                        cuotaGs = foco.cuotaReferenciaGs,
                        criterio = criterio,
                        ahorroEstimadoMeses = mesesAhorro,
                        explicacion = explicacionLocal
                    )
                )
            }
        }

        return remoteRes
    }

    override suspend fun proyectarCapacidad(request: CapacidadPagoRequest): NetworkResult<CapacidadPagoResponse> {
        return safeApiCall(dispatcher) { api.proyectarCapacidad(request) }
    }

    companion object {
        private const val SYNC_KEY = "obligaciones"
        private const val CACHE_TTL = 30 * 60 * 1000L // 30 minutes
    }
}
