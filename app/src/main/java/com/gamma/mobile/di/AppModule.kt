package com.gamma.mobile.di

import com.gamma.mobile.data.repository.SecureStorageRepositoryImpl
import com.gamma.mobile.domain.repository.SecureStorageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSecureStorageRepository(
        secureStorageRepositoryImpl: com.gamma.mobile.data.repository.SecureStorageRepositoryImpl
    ): com.gamma.mobile.domain.repository.SecureStorageRepository

    @Binds
    @Singleton
    abstract fun bindMovimientoRepository(
        impl: com.gamma.mobile.data.repository.MovimientoRepositoryImpl
    ): com.gamma.mobile.domain.repository.MovimientoRepository

    @Binds
    @Singleton
    abstract fun bindAvisoRepository(
        impl: com.gamma.mobile.data.repository.AvisoRepositoryImpl
    ): com.gamma.mobile.domain.repository.AvisoRepository

    @Binds
    @Singleton
    abstract fun bindObligacionRepository(
        impl: com.gamma.mobile.data.repository.ObligacionRepositoryImpl
    ): com.gamma.mobile.domain.repository.ObligacionRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(
        impl: com.gamma.mobile.data.repository.DashboardRepositoryImpl
    ): com.gamma.mobile.domain.repository.DashboardRepository
}
