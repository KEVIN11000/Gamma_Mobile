package com.gamma.mobile.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

/**
 * Interceptor de OkHttp que garantiza la idempotencia en peticiones mutacionales
 * (POST, PUT, PATCH, DELETE) inyectando la cabecera 'X-Client-Id' con un UUID
 * único si la petición original no incluye una cabecera explícita.
 *
 * Evita la duplicación de transacciones, amortizaciones o registros de asistencia
 * ante reconexiones automáticas o reintentos de red móvil.
 */
class IdempotencyInterceptor(
    private val uuidGenerator: () -> String = { UUID.randomUUID().toString() }
) : Interceptor {

    companion object {
        const val HEADER_CLIENT_ID = "X-Client-Id"
        private val MUTATIONAL_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val method = originalRequest.method().uppercase()

        return if (method in MUTATIONAL_METHODS && originalRequest.header(HEADER_CLIENT_ID).isNullOrBlank()) {
            val requestWithId = originalRequest.newBuilder()
                .header(HEADER_CLIENT_ID, uuidGenerator())
                .build()
            chain.proceed(requestWithId)
        } else {
            chain.proceed(originalRequest)
        }
    }
}
