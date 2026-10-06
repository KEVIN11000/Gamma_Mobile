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
    override suspend fun getAvisos() = throw NotImplementedError()
}

class FakeSecureStorageRepository : SecureStorageRepository {
    private var key: String? = null

    override fun saveApiKey(key: String) {
        this.key = key
    }

    override fun getApiKey(): String? = key

    override fun clearApiKey() {
        key = null
    }
}
