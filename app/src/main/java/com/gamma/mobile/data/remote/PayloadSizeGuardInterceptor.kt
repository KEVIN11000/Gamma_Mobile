package com.gamma.mobile.data.remote

import com.gamma.mobile.utils.AppLogBuffer
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody

/**
 * Interceptor preventivo de OkHttp que inspecciona el tamaño del cuerpo de la petición.
 * Si el payload supera el umbral configurado (default: 64 KB = 65,536 bytes), aborta
 * la transmisión de red y devuelve un error sintético HTTP 413 (Payload Too Large),
 * protegiendo el consumo de datos móviles y evitando fallos tardíos en el backend.
 */
class PayloadSizeGuardInterceptor(
    private val maxSizeBytes: Long = DEFAULT_MAX_SIZE_BYTES
) : Interceptor {

    companion object {
        const val DEFAULT_MAX_SIZE_BYTES = 64 * 1024L // 64 KB
        private val JSON_MEDIA_TYPE = MediaType.parse("application/json")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val body = request.body()

        if (body != null) {
            val contentLength = body.contentLength()
            if (contentLength > maxSizeBytes) {
                val errorMsg = "Tamaño de carga ($contentLength bytes) supera el límite permitido de $maxSizeBytes bytes (64 KB)."
                AppLogBuffer.error("HTTP 413 Preventivo: $errorMsg en ${request.url().encodedPath()}")

                val errorJson = """
                    {
                        "error": "PAYLOAD_TOO_LARGE",
                        "mensaje": "$errorMsg",
                        "bytes": $contentLength,
                        "limite_bytes": $maxSizeBytes
                    }
                """.trimIndent()

                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(413)
                    .message("Payload Too Large")
                    .body(ResponseBody.create(JSON_MEDIA_TYPE, errorJson))
                    .build()
            }
        }

        return chain.proceed(request)
    }
}
