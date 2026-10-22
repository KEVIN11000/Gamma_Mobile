package com.gamma.mobile.presentation.onboarding

import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.domain.repository.SecureStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: OnboardingViewModel
    private lateinit var fakeApi: FakeGammaApi
    private lateinit var fakeStorage: FakeSecureStorageRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeApi = FakeGammaApi()
        fakeStorage = FakeSecureStorageRepository()
        viewModel = OnboardingViewModel(fakeApi, fakeStorage, testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `verifyAndSaveKey valid key emits Success and saves key`() = runTest {
        fakeApi.healthResponse = Response.success(Unit)

        viewModel.verifyAndSaveKey("valid-key")
        advanceUntilIdle()

        assertEquals(OnboardingState.Success, viewModel.uiState.value)
        assertEquals("valid-key", fakeStorage.getApiKey())
    }

    @Test
    fun `verifyAndSaveKey invalid key emits Error and clears key`() = runTest {
        val errorBody = "{}".toResponseBody("application/json".toMediaTypeOrNull())
        fakeApi.healthResponse = Response.error(401, errorBody)

        viewModel.verifyAndSaveKey("invalid-key")
        advanceUntilIdle()

        assertEquals(OnboardingState.Error("API Key inválida (401)"), viewModel.uiState.value)
        assertEquals(null, fakeStorage.getApiKey())
    }

    @Test
    fun `verifyAndSaveKey network error emits Error and clears key`() = runTest {
        fakeApi.shouldThrowException = true

        viewModel.verifyAndSaveKey("network-error-key")
        advanceUntilIdle()

        assertEquals(OnboardingState.Error("Error de red o timeout"), viewModel.uiState.value)
        assertEquals(null, fakeStorage.getApiKey())
    }
}

class FakeGammaApi : GammaApi {
    var healthResponse: Response<Unit>? = null
    var shouldThrowException = false

    override suspend fun checkHealth(): Response<Unit> {
        if (shouldThrowException) throw java.net.SocketTimeoutException()
        return healthResponse ?: Response.success(Unit)
    }

    override suspend fun getDashboardSummary() = throw NotImplementedError()
    override suspend fun getMovimientos() = throw NotImplementedError()
    override suspend fun createMovimiento(movimiento: com.gamma.mobile.domain.model.Movimiento) = throw NotImplementedError()

    override suspend fun interpretarMovimientoIa(request: com.gamma.mobile.data.remote.IaTextRequest): retrofit2.Response<com.gamma.mobile.domain.model.Movimiento> = retrofit2.Response.success(com.gamma.mobile.domain.model.Movimiento())

    override suspend fun interpretarMovimientoOcr(request: okhttp3.MultipartBody.Part): retrofit2.Response<com.gamma.mobile.domain.model.Movimiento> = retrofit2.Response.success(com.gamma.mobile.domain.model.Movimiento())
    override suspend fun getAvisos() = throw NotImplementedError()
    override suspend fun createAviso(aviso: com.gamma.mobile.domain.model.Aviso): retrofit2.Response<Unit> = retrofit2.Response.success(Unit)
    override suspend fun deleteAviso(id: String): retrofit2.Response<Unit> = retrofit2.Response.success(Unit)
    override suspend fun interpretarAvisoIa(request: com.gamma.mobile.data.remote.IaTextRequest): retrofit2.Response<com.gamma.mobile.domain.model.Aviso> = retrofit2.Response.success(com.gamma.mobile.domain.model.Aviso())
    override suspend fun getObligaciones() = throw NotImplementedError()
    override suspend fun createObligacion(obligacion: com.gamma.mobile.domain.model.Obligacion) = throw NotImplementedError()
    override suspend fun updateObligacion(id: String, obligacion: com.gamma.mobile.domain.model.Obligacion) = throw NotImplementedError()
    override suspend fun deleteObligacion(id: String): retrofit2.Response<Unit> = throw NotImplementedError()
    override suspend fun abonarObligacion(request: com.gamma.mobile.domain.model.AbonoRequest): Response<com.gamma.mobile.domain.model.AbonoResponse> = throw NotImplementedError()
    override suspend fun updateAviso(id: String, aviso: com.gamma.mobile.domain.model.Aviso): retrofit2.Response<Unit> = throw NotImplementedError()
    override suspend fun getMaterias(): Response<List<String>> = Response.success(emptyList())
    override suspend fun marcarAsistencia(request: com.gamma.mobile.domain.model.AsistenciaMarcaRequest) = throw NotImplementedError()
    override suspend fun marcarMateria(request: com.gamma.mobile.domain.model.AsistenciaMateriaRequest) = throw NotImplementedError()
    override suspend fun ejecutarCierre(request: com.gamma.mobile.domain.model.CierreRequest) = throw NotImplementedError()
    override suspend fun getCierres(): Response<List<com.gamma.mobile.domain.model.CierreHistorialItem>> = throw NotImplementedError()
    override suspend fun descargarReporteCierre(filename: String): Response<okhttp3.ResponseBody> = throw NotImplementedError()
    override suspend fun descargarArchivoPdf(url: String) = throw NotImplementedError()
    override suspend fun getHealthStatus() = throw NotImplementedError()
    override suspend fun getTickets() = throw NotImplementedError()
    override suspend fun createTicket(request: com.gamma.mobile.domain.model.TicketCreate) = throw NotImplementedError()
    override suspend fun updateTicket(id: String, request: com.gamma.mobile.domain.model.TicketUpdate) = throw NotImplementedError()
    override suspend fun getLogs() = throw NotImplementedError()
}

class FakeSecureStorageRepository : SecureStorageRepository {
    private var key: String? = null
    private var baseUrl: String = "https://kevin11000.pythonanywhere.com/api/v1/"

    override fun saveApiKey(key: String) {
        this.key = key
    }

    override fun getApiKey(): String? = key

    override fun clearApiKey() {
        key = null
    }

    override fun saveBaseUrl(url: String) {
        baseUrl = url
    }

    override fun getBaseUrl(): String = baseUrl
}
