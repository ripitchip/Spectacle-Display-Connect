package com.example.spectacle.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthorizationService

class AuthRepository(private val context: Context) {

    // Lazy-initialized AuthorizationService; no separate getter needed
    private val authService by lazy { AuthorizationService(context) }

    // Keys for SecureStorage
    private companion object {
        const val ACCESS_TOKEN_KEY = "access_token"
        const val REFRESH_TOKEN_KEY = "refresh_token"
        const val EXPIRY_KEY = "access_token_expiry"
    }

    // ✅ Check if access token exists and is not expired
    suspend fun isTokenValid(): Boolean = withContext(Dispatchers.IO) {
        val token = SecureStorage.load(context, ACCESS_TOKEN_KEY)
        val expiry = SecureStorage.load(context, EXPIRY_KEY)?.toLongOrNull() ?: return@withContext false
        return@withContext !token.isNullOrEmpty() && System.currentTimeMillis() < expiry
    }

    // ✅ Save tokens securely with expiry
    suspend fun saveTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Long) = withContext(Dispatchers.IO) {
        val expiryTime = System.currentTimeMillis() + expiresInSeconds * 1000
        SecureStorage.save(context, ACCESS_TOKEN_KEY, accessToken)
        refreshToken?.let { SecureStorage.save(context, REFRESH_TOKEN_KEY, it) }
        SecureStorage.save(context, EXPIRY_KEY, expiryTime.toString())
    }

    // ✅ Clear all stored tokens
    suspend fun clearTokens() = withContext(Dispatchers.IO) {
        SecureStorage.remove(context, ACCESS_TOKEN_KEY)
        SecureStorage.remove(context, REFRESH_TOKEN_KEY)
        SecureStorage.remove(context, EXPIRY_KEY)
    }

    // ✅ Expose authService safely without a separate get function
    fun provideAuthService(): AuthorizationService = authService

    // Dispose when repository is no longer needed
    fun dispose() = authService.dispose()
}
