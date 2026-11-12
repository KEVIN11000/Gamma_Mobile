package com.gamma.mobile.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.gamma.mobile.data.remote.BaseFakeGammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.CapacidadesResponseDto
import com.gamma.mobile.data.remote.dto.RutaEndpointInfoDto
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CapacidadesRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val gson = Gson()
    private lateinit var context: Context
    private lateinit var fakeApi: FakeCapacidadesApi
    private lateinit var repository: CapacidadesRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("gamma_cache", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        fakeApi = FakeCapacidadesApi()
        repository = CapacidadesRepositoryImpl(
            api = fakeApi,
            context = context,
            gson = gson,
            dispatcher = testDispatcher
        )
    }

    @Test
    fun `syncCapacidades updates flow and caches values on success`() = runTest(testDispatcher) {
        val testDto = CapacidadesResponseDto(
            versionApi = "2.1.4",
            totalRutas = 3,
            modulos = listOf("presupuesto", "obligaciones"),
            capacidadesClave = mapOf(
                "presupuesto_fijos_delete" to false,
                "presupuesto_techos_put" to true,
                "custom_feature" to true
            ),
            rutas = listOf(
                RutaEndpointInfoDto(
                    modulo = "presupuesto",
                    ruta = "/api/v1/presupuesto/fijos",
                    metodos = listOf("GET")
                )
            )
        )
        fakeApi.capacidadesResponse = Response.success(testDto)

        val result = repository.syncCapacidades()
        assertTrue(result is NetworkResult.Success)

        // Verify flow
        val flowValue = repository.capacidadesFlow.value
        assertFalse(flowValue["presupuesto_fijos_delete"] == true)
        assertTrue(flowValue["presupuesto_techos_put"] == true)
        assertTrue(flowValue["custom_feature"] == true)

        // Verify isCapacidadHabilitada
        assertFalse(repository.isCapacidadHabilitada("presupuesto_fijos_delete"))
        assertTrue(repository.isCapacidadHabilitada("presupuesto_techos_put"))
        assertTrue(repository.isCapacidadHabilitada("non_existent_key", defaultSiNoExiste = true))
        assertFalse(repository.isCapacidadHabilitada("non_existent_key", defaultSiNoExiste = false))

        // Verify version and modulos
        assertEquals("2.1.4", repository.getVersionApi())
        assertEquals(listOf("presupuesto", "obligaciones"), repository.getModulos())
    }

    @Test
    fun `syncCapacidades handles network error gracefully and retains cached or default values`() = runTest(testDispatcher) {
        fakeApi.shouldThrow = true

        val result = repository.syncCapacidades()
        assertTrue(result is NetworkResult.Exception)

        // Still contains default capacities
        assertTrue(repository.isCapacidadHabilitada("presupuesto_techos_put"))
        assertTrue(repository.isCapacidadHabilitada("asistencia_excel_export"))
    }

    @Test
    fun `repository loads previous cache on instantiation`() = runTest(testDispatcher) {
        val prefs = context.getSharedPreferences("gamma_cache", Context.MODE_PRIVATE)
        val initialMap = mapOf("cached_feature" to true, "disabled_feature" to false)
        prefs.edit()
            .putString(CapacidadesRepositoryImpl.PREF_CAPACIDADES_CLAVE, gson.toJson(initialMap))
            .putString(CapacidadesRepositoryImpl.PREF_VERSION_API, "2.1.3")
            .commit()

        val newRepo = CapacidadesRepositoryImpl(
            api = fakeApi,
            context = context,
            gson = gson,
            dispatcher = testDispatcher
        )

        assertTrue(newRepo.isCapacidadHabilitada("cached_feature"))
        assertFalse(newRepo.isCapacidadHabilitada("disabled_feature"))
        assertEquals("2.1.3", newRepo.getVersionApi())
    }
}

class FakeCapacidadesApi : BaseFakeGammaApi() {
    var shouldThrow = false
    var capacidadesResponse: Response<CapacidadesResponseDto> = Response.success(CapacidadesResponseDto())

    override suspend fun getCapacidades(): Response<CapacidadesResponseDto> {
        if (shouldThrow) throw java.io.IOException("Network unreachable")
        return capacidadesResponse
    }
}
