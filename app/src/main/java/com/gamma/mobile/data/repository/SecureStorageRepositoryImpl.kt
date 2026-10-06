package com.gamma.mobile.data.repository

import android.content.Context
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

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "gamma_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun saveApiKey(key: String) {
        sharedPreferences.edit().putString(KEY_API, key).apply()
    }

    override fun getApiKey(): String? {
        return sharedPreferences.getString(KEY_API, null)
    }

    override fun clearApiKey() {
        sharedPreferences.edit().remove(KEY_API).apply()
    }

    companion object {
        private const val KEY_API = "X-API-KEY-SECURE"
    }
}
