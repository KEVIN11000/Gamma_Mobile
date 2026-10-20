package com.gamma.mobile.data.repository

import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.model.Movimiento

/**
 * Cálculo local sobre el resumen del backend.
 *
 * Los movimientos PENDING (guardados en el dispositivo pero aún no enviados al servidor) no están
 * incluidos en el resumen remoto, por lo que se aplican como delta sobre el saldo acumulado para que
 * el Dashboard refleje de inmediato lo que el usuario acaba de registrar aunque esté offline.
 *
 * Margen libre y Posición IVA dependen de Presupuesto_Base / obligaciones / tasas de IVA que el modelo
 * [Movimiento] no transporta, por lo que siguen siendo calculados por el servidor y se actualizan con
 * el refresco forzado una vez sincronizado el movimiento.
 */
object DashboardSummaryCalculator {

    private val TIPOS_INGRESO = setOf("ingreso", "ingresos", "entrada", "venta")
    private val TIPOS_EGRESO = setOf("gasto", "gastos", "egreso", "egresos", "salida", "compra", "pago")

    fun deltaSaldo(pendientes: List<Movimiento>): Long =
        pendientes.sumOf { mov ->
            when (mov.tipo.trim().lowercase()) {
                in TIPOS_INGRESO -> mov.total
                in TIPOS_EGRESO -> -mov.total
                else -> 0L
            }
        }

    fun aplicarPendientes(summary: DashboardSummary, pendientes: List<Movimiento>): DashboardSummary {
        if (pendientes.isEmpty()) return summary
        return summary.copy(saldoAcumulado = summary.saldoAcumulado + deltaSaldo(pendientes))
    }
}
