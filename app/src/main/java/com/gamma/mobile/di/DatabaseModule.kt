package com.gamma.mobile.di

import android.content.Context
import androidx.room.Room
import com.gamma.mobile.data.local.GammaDatabase
import com.gamma.mobile.data.local.dao.AvisoDao
import com.gamma.mobile.data.local.dao.MovimientoDao
import com.gamma.mobile.data.local.dao.ObligacionDao
import com.gamma.mobile.data.local.dao.SyncStateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGammaDatabase(@ApplicationContext context: Context): GammaDatabase {
        return Room.databaseBuilder(
            context,
            GammaDatabase::class.java,
            "gamma_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideMovimientoDao(db: GammaDatabase): MovimientoDao = db.movimientoDao()

    @Provides
    fun provideAvisoDao(db: GammaDatabase): AvisoDao = db.avisoDao()

    @Provides
    fun provideObligacionDao(db: GammaDatabase): ObligacionDao = db.obligacionDao()

    @Provides
    fun provideSyncStateDao(db: GammaDatabase): SyncStateDao = db.syncStateDao()
}
