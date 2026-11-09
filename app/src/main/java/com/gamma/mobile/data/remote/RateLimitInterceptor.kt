package com.gamma.mobile.data.remote

import com.gamma.mobile.utils.AppLogBuffer
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor de OkHttp que detecta respuestas 'HTTP 429 Too Many Requests',
 * extrae la cabecera 'Retry-After' (en segundos o fecha) y registra la advertencia
 * en AppLogBuffer para trazabilidad y diagnóstico en pantalla.
 */
class RateLimitInterceptor(
    private val onRateLimitListener: ((retryAfterSeconds: Long?) -> Unit)? = null
) : Interceptor {

    companion object {
        const val HEADER_RETRY_AFTER = "Retry-After"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code() == 429) {
            val retryAfterHeader = response.header(HEADER_RETRY_AFTER)
            val retryAfterSeconds = retryAfterHeader?.toLongOrNull()

            val logMsg = if (retryAfterSeconds != null) {
                "HTTP 429 Rate Limit detectado en ${request.url().encodedPath()}. Retry-After: ${retryAfterSeconds}s"
            } else {
                "HTTP 429 Rate Limit detectado en ${request.url().encodedPath()}"
            }
            AppLogBuffer.warn(logMsg)
            onRateLimitListener?.invoke(retryAfterSeconds)
        }

        return response
    }
}
