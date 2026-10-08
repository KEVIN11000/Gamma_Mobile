package com.gamma.mobile.data.remote

import com.gamma.mobile.domain.model.Aviso
import com.gamma.mobile.domain.model.DashboardSummary
import com.gamma.mobile.domain.model.Movimiento
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface GammaApi {

    @GET("health")
    suspend fun checkHealth(): Response<Unit>

    @GET("dashboard/summary")
    suspend fun getDashboardSummary(): Response<DashboardSummary>

    @GET("movimientos")
    suspend fun getMovimientos(): Response<List<Movimiento>>

    @POST("movimientos")
    suspend fun createMovimiento(@Body movimiento: Movimiento): Response<Unit>

    @GET("avisos")
    suspend fun getAvisos(): Response<List<Aviso>>

    @GET("obligaciones")
    suspend fun getObligaciones(): Response<List<com.gamma.mobile.domain.model.Obligacion>>

    @POST("obligaciones")
    suspend fun createObligacion(@Body obligacion: com.gamma.mobile.domain.model.Obligacion): Response<Unit>

    @retrofit2.http.PUT("obligaciones/{id}")
    suspend fun updateObligacion(
        @retrofit2.http.Path("id") id: String, 
        @Body obligacion: com.gamma.mobile.domain.model.Obligacion
    ): Response<Unit>
}
