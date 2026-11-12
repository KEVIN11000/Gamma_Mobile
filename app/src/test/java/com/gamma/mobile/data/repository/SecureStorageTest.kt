package com.gamma.mobile.data.repository

import com.gamma.mobile.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureStorageTest {

    @Test
    fun `DEFAULT_BASE_URL is injected from BuildConfig and ends with slash`() {
        assertEquals(BuildConfig.DEFAULT_BASE_URL, SecureStorageRepositoryImpl.DEFAULT_BASE_URL)
        assertTrue(SecureStorageRepositoryImpl.DEFAULT_BASE_URL.startsWith("https://"))
        assertTrue(SecureStorageRepositoryImpl.DEFAULT_BASE_URL.endsWith("/"))
    }

    @Test
    fun `DEFAULT_BASE_URL is a valid parseable HttpUrl for Retrofit and OkHttp`() {
        val parsed = SecureStorageRepositoryImpl.DEFAULT_BASE_URL.toHttpUrlOrNull()
        assertNotNull("DEFAULT_BASE_URL must be a valid HttpUrl", parsed)
        assertEquals("https", parsed?.scheme)
    }
}
