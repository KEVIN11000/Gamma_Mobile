package com.gamma.mobile.data.remote

import com.gamma.mobile.utils.AppLogBuffer
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.Response

suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher,
    apiCall: suspend () -> Response<T>
): NetworkResult<T> {
    return withContext(dispatcher) {
        try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    NetworkResult.Success(body)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    NetworkResult.Success(Unit as T)
                }
            } else {
                val errorMsg = try {
                    val rawBody = response.errorBody()?.string()
                    if (!rawBody.isNullOrBlank()) {
                        val parsed = JsonParser.parseString(rawBody)
                        if (parsed.isJsonObject) {
                            val json = parsed.asJsonObject
                            when {
                                json.has("mensaje") -> json.get("mensaje").asString
                                json.has("message") -> json.get("message").asString
                                json.has("error") -> {
                                    val err = json.get("error")
                                    if (err.isJsonObject && err.asJsonObject.has("message")) {
                                        err.asJsonObject.get("message").asString
                                    } else {
                                        err.asString
                                    }
                                }
                                else -> response.message()
                            }
                        } else {
                            rawBody
                        }
                    } else {
                        response.message()
                    }
                } catch (e: Exception) {
                    response.message()
                }
                val finalError = if (errorMsg.isNullOrBlank()) response.message() else errorMsg
                AppLogBuffer.warn("HTTP ${response.code()}: $finalError")
                NetworkResult.Error(
                    response.code(),
                    finalError
                )
            }
        } catch (e: Exception) {
            AppLogBuffer.error("Fallo de red (${e.javaClass.simpleName}): ${e.message ?: e.localizedMessage}")
            NetworkResult.Exception(e)
        }
    }
}
