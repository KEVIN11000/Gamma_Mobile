package com.gamma.mobile.data.remote

import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor(private val getApiKey: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val apiKey = getApiKey()
        
        if (apiKey.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        val newRequest = originalRequest.newBuilder()
            .addHeader("X-API-KEY", apiKey)
            .build()
            
        return chain.proceed(newRequest)
    }
}
