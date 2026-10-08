package com.gamma.mobile.data.remote

import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.model.Movimiento
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT

data class IaTextRequest(val texto: String)

interface GammaApi {

    @GET("health")
    suspend fun checkHealth(): Response<Unit>

    @GET("dashboard/summary")
    suspend fun getDashboardSummary(): Response<DashboardSummary>

    @GET("movimientos")
    suspend fun getMovimientos(): Response<List<Movimiento>>

    @POST("movimientos")
    suspend fun createMovimiento(@Body movimiento: Movimiento): Response<Unit>

    @POST("ia/interpretar")
    suspend fun interpretarMovimientoIa(@Body request: IaTextRequest): Response<Movimiento>

    @retrofit2.http.Multipart
    @POST("movimientos/ocr")
    suspend fun interpretarMovimientoOcr(@retrofit2.http.Part file: okhttp3.MultipartBody.Part): Response<Movimiento>

    @GET("avisos")
    suspend fun getAvisos(): Response<List<Aviso>>

    @POST("avisos")
    suspend fun createAviso(@Body aviso: Aviso): Response<Unit>

    @DELETE("avisos/{id}")
    suspend fun deleteAviso(@Path("id") id: String): Response<Unit>

    @POST("ia/interpretar/aviso")
    suspend fun interpretarAvisoIa(@Body request: IaTextRequest): Response<Aviso>

    @GET("obligaciones")
    suspend fun getObligaciones(): Response<List<com.gamma.mobile.domain.model.Obligacion>>

    @POST("obligaciones")
    suspend fun createObligacion(@Body obligacion: com.gamma.mobile.domain.model.Obligacion): Response<Unit>

    @PUT("obligaciones/{id}")
    suspend fun updateObligacion(
        @Path("id") id: String, 
        @Body obligacion: com.gamma.mobile.domain.model.Obligacion
    ): Response<Unit>
}

