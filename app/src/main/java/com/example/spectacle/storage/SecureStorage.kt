package com.example.spectacle.storage

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.aead.AeadConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Base64

private val Context.dataStore by preferencesDataStore("secure_token_store")

class SecureStorage(context: Context) {

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    }

    private val dataStore = context.dataStore

    private val aead: Aead by lazy {
        AeadConfig.register()
        val keysetHandle: KeysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "master_keyset", "master_key_pref")
            .withKeyTemplate(com.google.crypto.tink.aead.AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri("android-keystore://master_key")
            .build()
            .keysetHandle

        keysetHandle.getPrimitive(Aead::class.java)
    }

    private fun encrypt(plainText: String): String {
        val cipherText = aead.encrypt(plainText.toByteArray(), null)
        return Base64.getEncoder().encodeToString(cipherText)
    }

    private fun decrypt(cipherText: String): String {
        val decoded = Base64.getDecoder().decode(cipherText)
        return String(aead.decrypt(decoded, null))
    }

    suspend fun saveTokens(accessToken: String?, refreshToken: String?) {
        dataStore.edit { prefs ->
            accessToken?.let { prefs[ACCESS_TOKEN_KEY] = encrypt(it) }
            refreshToken?.let { prefs[REFRESH_TOKEN_KEY] = encrypt(it) }
        }
    }

    fun getAccessToken(): Flow<String?> = dataStore.data.map { prefs ->
        prefs[ACCESS_TOKEN_KEY]?.let { decrypt(it) }
    }

    fun getRefreshToken(): Flow<String?> = dataStore.data.map { prefs ->
        prefs[REFRESH_TOKEN_KEY]?.let { decrypt(it) }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
