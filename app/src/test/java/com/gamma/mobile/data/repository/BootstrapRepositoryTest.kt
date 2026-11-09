package com.gamma.mobile.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.gamma.mobile.data.local.GammaDatabase
import com.gamma.mobile.data.local.dao.AvisoDao
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.dto.BootstrapResponseDto
import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.model.HealthStatus
import com.gamma.mobile.domain.model.VencimientoCronogramaItem
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BootstrapRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: GammaDatabase
    private lateinit var avisoDao: AvisoDao
    private lateinit var fakeApi: FakeBootstrapApi
    private lateinit var repository: BootstrapRepositoryImpl
    private val testDispatcher = StandardTestDispatcher()
    private val gson = Gson()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, GammaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        avisoDao = db.avisoDao()
        fakeApi = FakeBootstrapApi()

        repository = BootstrapRepositoryImpl(
            api = fakeApi,
            avisoDao = avisoDao,
            context = context,
            gson = gson,
            dispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        db.close()
        context.getSharedPreferences("gamma_cache", Context.MODE_PRIVATE).edit().clear().apply()
    }

    @Test
    fun `test syncBootstrap success hydrates SharedPreferences and Room atomically`() = runTest(testDispatcher) {
        val summary = DashboardSummary(
            margenLibreDisponible = 4500000L,
            saldoAcumulado = 2100000L,
            posicionIva = 50000L,
            esDebitoFiscal = false
        )
        val avisos = listOf(
            Aviso(id = "a1", titulo = "Declaración IVA", fechaEvento = "20/11/2026 08:00")
        )
        val dto = BootstrapResponseDto(
            status = "ok",
            health = HealthStatus(status = "ok"),
            summary = summary,
            proximosAvisos = avisos,
            periodoActual = "Noviembre 2026",
            timestamp = "2026-11-09T12:00:00"
        )
        fakeApi.bootstrapResponse = Response.success(dto)

        val result = repository.syncBootstrap()
        assertTrue(result is NetworkResult.Success)

        // Verificar SharedPreferences
        val prefs = context.getSharedPreferences("gamma_cache", Context.MODE_PRIVATE)
        val cachedSummaryJson = prefs.getString(BootstrapRepositoryImpl.PREF_SUMMARY, null)
        assertNotNull(cachedSummaryJson)
        val parsedSummary = gson.fromJson(cachedSummaryJson, DashboardSummary::class.java)
        assertEquals(4500000L, parsedSummary.margenLibreDisponible)
        assertEquals("Noviembre 2026", prefs.getString(BootstrapRepositoryImpl.PREF_PERIODO, null))

        // Verificar Room
        val roomAvisos = avisoDao.getAll()
        assertEquals(1, roomAvisos.size)
        assertEquals("Declaración IVA", roomAvisos[0].titulo)
    }

    @Test
    fun `test syncBootstrap failure preserves existing cache without destructive wiping`() = runTest(testDispatcher) {
        // Pre-cargar caché previo
        val prefs = context.getSharedPreferences("gamma_cache", Context.MODE_PRIVATE)
        prefs.edit().putString(BootstrapRepositoryImpl.PREF_SUMMARY, "{\"margen_libre\": 1000000}").apply()

        // Simular fallo de red (500)
        fakeApi.bootstrapResponse = Response.error(500, okhttp3.ResponseBody.create(null, "Server Error"))

        val result = repository.syncBootstrap()
        assertTrue(result is NetworkResult.Error)

        // Constatar que la caché previa permanece intacta
        val cachedSummary = prefs.getString(BootstrapRepositoryImpl.PREF_SUMMARY, null)
        assertNotNull(cachedSummary)
        assertTrue(cachedSummary!!.contains("1000000"))
    }
}

class FakeBootstrapApi : com.gamma.mobile.data.remote.BaseFakeGammaApi() {
    var bootstrapResponse: Response<BootstrapResponseDto>? = null

    override suspend fun getBootstrap(): Response<BootstrapResponseDto> {
        return bootstrapResponse ?: Response.error(500, okhttp3.ResponseBody.create(null, "Not configured"))
    }
}
