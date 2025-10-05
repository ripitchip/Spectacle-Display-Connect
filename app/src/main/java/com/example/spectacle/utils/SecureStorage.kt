package com.example.spectacle.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SecureStorage {

    private const val PREFS_NAME = "secure_storage"

    // Lazy thread-safe SharedPreferences instance
    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    suspend fun save(context: Context, key: String, value: String) = withContext(Dispatchers.IO) {
        prefs(context).edit { putString(key, value) }
    }

    suspend fun load(context: Context, key: String): String? = withContext(Dispatchers.IO) {
        prefs(context).getString(key, null)
    }

    suspend fun remove(context: Context, key: String) = withContext(Dispatchers.IO) {
        prefs(context).edit { remove(key) }
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        prefs(context).edit { clear() }
    }
}
