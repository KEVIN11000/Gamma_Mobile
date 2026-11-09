package com.gamma.mobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flujo_caja")
data class FlujoCajaEntity(
    @PrimaryKey
    val id: String = "current",
    @ColumnInfo(name = "saldo_actual")
    val saldoActual: Long,
    @ColumnInfo(name = "saldo_proyectado_30d")
    val saldoProyectado30d: Long,
    @ColumnInfo(name = "total_ingresos_proyectados")
    val totalIngresosProyectados: Long,
    @ColumnInfo(name = "total_gastos_fijos")
    val totalGastosFijos: Long,
    @ColumnInfo(name = "total_gastos_variables_estimados")
    val totalGastosVariablesEstimados: Long,
    @ColumnInfo(name = "dias_criticos_json")
    val diasCriticosJson: String,
    @ColumnInfo(name = "dias_json")
    val diasJson: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
