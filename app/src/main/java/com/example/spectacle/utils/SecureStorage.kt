package com.example.spectacle.utils

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SecureStorage {

    private const val KEYSET_FILE = "secure_prefs_keyset.json"
    private const val PREFS_FILE = "secure_prefs_data"

    init {
        AeadConfig.register()
    }

    private fun getAead(context: Context): Aead {
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_FILE, "secure_storage_master_key")
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://secure_prefs_master_key")
            .build()
            .keysetHandle

        return keysetHandle.getPrimitive(Aead::class.java)
    }

    private fun prefsFile(context: Context): File =
        File(context.filesDir, PREFS_FILE)

    private suspend fun writeData(context: Context, data: Map<String, String>) =
        withContext(Dispatchers.IO) {
            val aead = getAead(context)
            val plain = buildString {
                data.forEach { (k, v) -> append("$k=$v\n") }
            }.encodeToByteArray()

            prefsFile(context).writeBytes(aead.encrypt(plain, null))
        }

    private suspend fun readData(context: Context): MutableMap<String, String> =
        withContext(Dispatchers.IO) {
            val file = prefsFile(context)
            if (!file.exists()) return@withContext mutableMapOf()

            val aead = getAead(context)
            val decrypted = aead.decrypt(file.readBytes(), null).decodeToString()

            decrypted.lines()
                .filter { it.contains("=") }
                .associate {
                    val (k, v) = it.split("=", limit = 2)
                    k to v
                }
                .toMutableMap()
        }

    // Public API
    suspend fun save(context: Context, key: String, value: String) {
        val data = readData(context)
        data[key] = value
        writeData(context, data)
    }

    suspend fun load(context: Context, key: String): String? {
        val data = readData(context)
        return data[key]
    }

    suspend fun remove(context: Context, key: String) {
        val data = readData(context)
        data.remove(key)
        writeData(context, data)
    }

    suspend fun clear(context: Context) {
        writeData(context, emptyMap())
    }
}
