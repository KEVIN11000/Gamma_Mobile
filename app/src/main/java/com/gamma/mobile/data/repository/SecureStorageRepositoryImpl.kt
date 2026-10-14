package com.gamma.mobile.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.gamma.mobile.domain.repository.SecureStorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SecureStorageRepository {

    private val sharedPreferences: SharedPreferences = try {
        createEncryptedSharedPreferences(context)
    } catch (e: Exception) {
        Log.e("SecureStorage", "Failed to create EncryptedSharedPreferences, resetting...", e)
        context.deleteSharedPreferences("gamma_secure_prefs")
        try {
            createEncryptedSharedPreferences(context)
        } catch (e2: Exception) {
            Log.e("SecureStorage", "Fallback to unencrypted prefs", e2)
            context.getSharedPreferences("gamma_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    override fun saveApiKey(key: String) {
        sharedPreferences.edit().putString(KEY_API, key).apply()
    }

    override fun getApiKey(): String? {
        return sharedPreferences.getString(KEY_API, null)
    }

    override fun clearApiKey() {
        sharedPreferences.edit().remove(KEY_API).apply()
    }

    override fun saveBaseUrl(url: String) {
        val sanitized = if (url.endsWith("/")) url else "$url/"
        sharedPreferences.edit().putString(KEY_BASE_URL, sanitized).apply()
    }

    override fun getBaseUrl(): String {
        return sharedPreferences.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    companion object {
        private const val KEY_API = "X-API-KEY-SECURE"
        private const val KEY_BASE_URL = "BASE_URL_SECURE"
        const val DEFAULT_BASE_URL = "https://kevin11000.pythonanywhere.com/api/v1/"

        private fun createEncryptedSharedPreferences(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                "gamma_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}
