package com.gamma.mobile.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gamma.mobile.data.remote.GammaApi
import com.gamma.mobile.data.remote.NetworkResult
import com.gamma.mobile.data.remote.safeApiCall
import com.gamma.mobile.domain.repository.SecureStorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val api: GammaApi,
    private val secureStorage: SecureStorageRepository,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingState>(OnboardingState.Idle)
    val uiState: StateFlow<OnboardingState> = _uiState.asStateFlow()

    fun verifyAndSaveKey(apiKey: String) {
        if (apiKey.isBlank()) {
            _uiState.value = OnboardingState.Error("La API Key no puede estar vacía")
            return
        }

        _uiState.value = OnboardingState.Loading

        viewModelScope.launch {
            // Guardamos temporalmente para que el interceptor la use en el test
            secureStorage.saveApiKey(apiKey)

            when (val result = safeApiCall(dispatcher) { api.checkHealth() }) {
                is NetworkResult.Success -> {
                    _uiState.value = OnboardingState.Success
                }
                is NetworkResult.Error -> {
                    secureStorage.clearApiKey()
                    if (result.code == 401) {
                        _uiState.value = OnboardingState.Error("API Key inválida (401)")
                    } else {
                        _uiState.value = OnboardingState.Error("Error del servidor: ${result.code}")
                    }
                }
                is NetworkResult.Exception -> {
                    secureStorage.clearApiKey()
                    _uiState.value = OnboardingState.Error("Error de red o timeout")
                }
            }
        }
    }
}

sealed class OnboardingState {
    object Idle : OnboardingState()
    object Loading : OnboardingState()
    object Success : OnboardingState()
    data class Error(val message: String) : OnboardingState()
}
