package com.gamma.mobile.data.remote

import com.gamma.mobile.domain.repository.SecureStorageRepository
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val secureStorage: SecureStorageRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val configuredBase = secureStorage.getBaseUrl()
        val newHttpUrl = HttpUrl.parse(configuredBase)

        return if (newHttpUrl != null) {
            val newRequestUrl = originalRequest.url().newBuilder()
                .scheme(newHttpUrl.scheme())
                .host(newHttpUrl.host())
                .port(newHttpUrl.port())
                .build()
            val newRequest = originalRequest.newBuilder().url(newRequestUrl).build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
