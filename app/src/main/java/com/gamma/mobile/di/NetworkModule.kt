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
    fun provideOkHttpClient(
        apiKeyInterceptor: ApiKeyInterceptor,
        dynamicBaseUrlInterceptor: com.gamma.mobile.data.remote.DynamicBaseUrlInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(apiKeyInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
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
    @Named("NetworkDispatcher")
    fun provideNetworkDispatcher(): CoroutineDispatcher {
        // PythonAnywhere solo soporta 1 worker WSGI concurrente
        return Dispatchers.IO.limitedParallelism(1)
    }
}
