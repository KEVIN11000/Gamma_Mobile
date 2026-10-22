# Plan de Arquitectura e Implementación (Plan)
## Módulo: Proyector de Capacidad de Pago con Gastos Ad-Hoc y Escenarios Persistentes (v0.2.1)

- **Fecha:** 22 de Octubre de 2026
- **Versión de Plan:** 1.0.0
- **Estado:** En Revisión (Fase 0 SDD)

---

### 1. Modelos de Dominio y Contratos de Datos

#### 1.1 Modelo `GastoSimulado`
```kotlin
package com.gamma.mobile.domain.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class GastoSimulado(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    @SerializedName("concepto")
    val concepto: String = "",
    @SerializedName("monto")
    val monto: Long = 0L,
    @SerializedName("incluido")
    val incluido: Boolean = true
)
```

#### 1.2 Modelo `EscenarioCapacidad`
```kotlin
package com.gamma.mobile.domain.model

data class EscenarioCapacidad(
    val id: String = "",
    val nombre: String = "",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis(),
    val ingresoExtra: Long = 0L,
    val deudasPausadasIds: Set<String> = emptySet(),
    val abonosExtraordinarios: Map<String, Long> = emptyMap(),
    val gastosAdHoc: List<GastoSimulado> = emptyList()
)
```

#### 1.3 Extensión en `CapacidadPagoResponse`
```kotlin
data class CapacidadPagoResponse(
    val compromisoTotalMes: Long = 0L,
    val fondoDisponibleMes: Long = 0L,
    val totalGastosProyectados: Long = 0L, // NUEVO CAMPO
    val capacidadNeta: Long = 0L,
    val semaforo: String = "DEFICIT",
    val dtiProyectado: Double = 0.0,
    val mesesAhorradosPorAbonos: Map<String, Int> = emptyMap()
)
```

---

### 2. Capa de Persistencia Local (Room Database v5)

#### 2.1 Entidad `EscenarioCapacidadEntity`
Tabla: `escenarios_capacidad`
```kotlin
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
```

#### 2.2 DAO: `EscenarioCapacidadDao`
```kotlin
package com.gamma.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gamma.mobile.data.local.entity.EscenarioCapacidadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EscenarioCapacidadDao {
    @Query("SELECT * FROM escenarios_capacidad ORDER BY fecha_actualizacion DESC")
    fun getAllFlow(): Flow<List<EscenarioCapacidadEntity>>

    @Query("SELECT * FROM escenarios_capacidad ORDER BY fecha_actualizacion DESC")
    suspend fun getAll(): List<EscenarioCapacidadEntity>

    @Query("SELECT * FROM escenarios_capacidad WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EscenarioCapacidadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(escenario: EscenarioCapacidadEntity)

    @Query("DELETE FROM escenarios_capacidad WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM escenarios_capacidad")
    suspend fun deleteAll()
}
```

#### 2.3 Migración Room `MIGRATION_4_5` y Bump a Version 5
```kotlin
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `escenarios_capacidad` (
                `id` TEXT NOT NULL,
                `nombre` TEXT NOT NULL,
                `fecha_creacion` INTEGER NOT NULL,
                `fecha_actualizacion` INTEGER NOT NULL,
                `ingreso_extra` INTEGER NOT NULL,
                `deudas_pausadas_json` TEXT NOT NULL,
                `abonos_extra_json` TEXT NOT NULL,
                `gastos_adhoc_json` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}
```

---

### 3. Capa de Repositorio

#### 3.1 Interfaz `EscenarioCapacidadRepository`
```kotlin
package com.gamma.mobile.domain.repository

import com.gamma.mobile.domain.model.EscenarioCapacidad
import kotlinx.coroutines.flow.Flow

interface EscenarioCapacidadRepository {
    fun getEscenarios(): Flow<List<EscenarioCapacidad>>
    suspend fun guardarEscenario(escenario: EscenarioCapacidad)
    suspend fun eliminarEscenario(id: String)
}
```

#### 3.2 Implementación `EscenarioCapacidadRepositoryImpl`
- Realiza la serialización/deserialización de JSON (`Gson`) para listas de IDs, mapas de abonos y objetos `GastoSimulado`.
- Vinculado mediante `@Binds` en `AppModule.kt` y `@Provides` para `EscenarioCapacidadDao` en `DatabaseModule.kt`.

---

### 4. Capa de Presentación

#### 4.1 Modificaciones en `CapacidadPagoViewModel`
- **Flujos de Estado Adicionales:**
  - `gastosSimulados: StateFlow<List<GastoSimulado>>`
  - `escenariosGuardados: StateFlow<List<EscenarioCapacidad>>`
  - `escenarioActivoNombre: StateFlow<String?>`
- **Nuevas Funciones:**
  - `agregarGastoAdHoc(concepto: String, monto: Long)`
  - `toggleGastoAdHoc(id: String)`
  - `eliminarGastoAdHoc(id: String)`
  - `guardarEscenarioActual(nombre: String)`
  - `cargarEscenario(escenario: EscenarioCapacidad)`
  - `eliminarEscenario(id: String)`
- **Fórmula de Recálculo en `recalcular()`:**
  ```kotlin
  val totalGastos = _gastosSimulados.value.filter { it.incluido }.sumOf { it.monto }
  val compromisoTotalMes = deudasNoPausadas.sumOf { it.cuotaReferenciaGs }
  val totalAbonos = _abonosExtraordinarios.value.values.sum()
  val capacidadNeta = fondoDisponible - compromisoTotalMes - totalAbonos - totalGastos

  val totalEgresosSimulados = compromisoTotalMes + totalAbonos + totalGastos
  val dti = if (fondoDisponible > 0L) {
      (totalEgresosSimulados.toDouble() / fondoDisponible.toDouble()) * 100.0
  } else 100.0

  val semaforoStr = when {
      fondoDisponible <= 0L || capacidadNeta < 0L -> "DEFICIT"
      capacidadNeta < (fondoDisponible * 0.15).toLong() -> "AJUSTADO"
      else -> "SUPERAVIT"
  }
  ```

#### 4.2 Interfaz de Usuario en `CapacidadPagoScreen`
1. **TopAppBar Action:**
   - Botón `Icons.Default.Bookmark` o `Icons.Default.FolderOpen` que abre el `EscenariosBottomSheet`.
2. **`EscenariosBottomSheet`:**
   - Listado de escenarios con fechas formateadas.
   - Botón *"Guardar estado actual"* con diálogo para ingresar el nombre.
   - Botón para cargar el escenario seleccionado o borrarlo con ícono de eliminar.
3. **Sección *"Gastos Adicionales / Imprevistos"*:**
   - Encabezado con título y botón `FilledTonalButton("+ Agregar Gasto")`.
   - Diálogo modal de entrada (`AgregarGastoDialog`):
     - Campo de texto para `Concepto`.
     - Campo numérico para `Monto (Gs.)`.
     - Validaciones y botón *"Agregar"*.
   - Lista de `GastoSimuladoCard`:
     - Concepto, monto formateado.
     - Switch de inclusión: al desmarcarlo se muestra en tono atenuado y se descuenta del total.
     - Botón de papelera para eliminarlo de la simulación.
4. **Actualización de `MetricasCapacidadCard`:**
   - Fila dedicada: *"Gastos Proyectados: Gs. X"* visible siempre que haya gastos simulados.

---

### 5. Estrategia de Testing y Verificación Empírica
- **Test Unitario de Base de Datos (`EscenarioCapacidadDaoTest`):**
  - Inserción, lectura ordenada por fecha, reemplazo y borrado por ID en base en memoria de Room.
- **Test Unitario de Migración (`Migration4To5Test` o verificación en DaoTest):**
  - Validación de que la tabla `escenarios_capacidad` se crea con las columnas esperadas.
- **Test Unitario de Repositorio (`EscenarioCapacidadRepositoryTest`):**
  - Verificación del mapeo de JSON bidireccional entre la entidad Room y el modelo de dominio.
- **Test Unitario de ViewModel (`CapacidadPagoViewModelTest`):**
  - Inclusión y exclusión de gastos ad-hoc impactan `totalGastosProyectados`, `capacidadNeta` y `semaforoEnum`.
  - Guardado y carga de escenario restaura exactamente todos los valores simulados.
  - Reset limpia deudas pausadas, abonos, ingresos extra y gastos ad-hoc.
- **Gate Visual de UI:**
  - Renderizado interactivo HTML en el directorio de artefactos demostrando la sección de gastos, el modal de adición y el BottomSheet de escenarios.
