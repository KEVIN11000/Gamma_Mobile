package com.gamma.mobile.domain.repository

interface SecureStorageRepository {
    fun saveApiKey(key: String)
    fun getApiKey(): String?
    fun clearApiKey()
    fun saveBaseUrl(url: String)
    fun getBaseUrl(): String
}
