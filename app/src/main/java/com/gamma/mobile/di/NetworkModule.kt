package com.gamma.mobile.di

import com.gamma.mobile.data.remote.ApiKeyInterceptor
import com.gamma.mobile.data.remote.GammaApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApiKeyInterceptor(secureStorage: com.gamma.mobile.domain.repository.SecureStorageRepository): ApiKeyInterceptor {
        return ApiKeyInterceptor { secureStorage.getApiKey() ?: "" }
    }

    @Provides
    @Singleton
    fun provideIdempotencyInterceptor(): com.gamma.mobile.data.remote.IdempotencyInterceptor {
        return com.gamma.mobile.data.remote.IdempotencyInterceptor()
    }

    @Provides
    @Singleton
    fun provideRateLimitInterceptor(): com.gamma.mobile.data.remote.RateLimitInterceptor {
        return com.gamma.mobile.data.remote.RateLimitInterceptor()
    }

    @Provides
    @Singleton
    fun providePayloadSizeGuardInterceptor(): com.gamma.mobile.data.remote.PayloadSizeGuardInterceptor {
        return com.gamma.mobile.data.remote.PayloadSizeGuardInterceptor()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        apiKeyInterceptor: ApiKeyInterceptor,
        dynamicBaseUrlInterceptor: com.gamma.mobile.data.remote.DynamicBaseUrlInterceptor,
        idempotencyInterceptor: com.gamma.mobile.data.remote.IdempotencyInterceptor,
        rateLimitInterceptor: com.gamma.mobile.data.remote.RateLimitInterceptor,
        payloadSizeGuardInterceptor: com.gamma.mobile.data.remote.PayloadSizeGuardInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(apiKeyInterceptor)
            .addInterceptor(idempotencyInterceptor)
            .addInterceptor(payloadSizeGuardInterceptor)
            .addInterceptor(rateLimitInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (com.gamma.mobile.BuildConfig.DEBUG) {
            builder.addInterceptor { chain ->
                val request = chain.request()
                val url = request.url().toString()
                android.util.Log.d("GammaNetwork", "--> ${request.method()} $url")
                val startNs = System.nanoTime()
                try {
                    val response = chain.proceed(request)
                    val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
                    android.util.Log.d("GammaNetwork", "<-- ${response.code()} (${tookMs}ms) $url")
                    response
                } catch (e: Exception) {
                    android.util.Log.e("GammaNetwork", "<-- FAILED: $url - ${e.message}")
                    throw e
                }
            }
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://kevin11000.pythonanywhere.com/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGammaApi(retrofit: Retrofit): GammaApi {
        return retrofit.create(GammaApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGitHubReleaseApi(): com.gamma.mobile.data.remote.GitHubReleaseApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "GammaMobile")
                    .header("Accept", "application/vnd.github+json")
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.gamma.mobile.data.remote.GitHubReleaseApi::class.java)
    }

    @Provides
    @Singleton
    @Named("NetworkDispatcher")
    fun provideNetworkDispatcher(): CoroutineDispatcher {
        // Permitir ejecución en Dispatchers.IO para evitar bloqueos en cola secuencial
        return Dispatchers.IO
    }
}
