package com.gamma.mobile

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class GammaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val logFile = File(filesDir, "crash.txt")
                logFile.writeText("Crash in thread ${thread.name}:\n" + throwable.stackTraceToString())
                Log.e("GammaApplication", "CRASH DETECTED", throwable)
            } catch (e: Exception) {
                // Ignore
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
