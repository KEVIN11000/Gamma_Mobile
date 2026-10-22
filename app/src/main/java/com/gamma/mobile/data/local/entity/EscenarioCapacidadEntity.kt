package com.gamma.mobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "escenarios_capacidad")
data class EscenarioCapacidadEntity(
    @PrimaryKey
    val id: String,
    val nombre: String,
    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long,
    @ColumnInfo(name = "fecha_actualizacion")
    val fechaActualizacion: Long,
    @ColumnInfo(name = "ingreso_extra")
    val ingresoExtra: Long,
    @ColumnInfo(name = "deudas_pausadas_json")
    val deudasPausadasJson: String,
    @ColumnInfo(name = "abonos_extra_json")
    val abonosExtraJson: String,
    @ColumnInfo(name = "gastos_adhoc_json")
    val gastosAdHocJson: String
)
