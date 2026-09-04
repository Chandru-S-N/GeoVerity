package org.geoverity.android.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.UUID

class SecureStorage(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "geoverity_secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        // Ensure device has a unique persistent device ID
        if (getDeviceId().isBlank()) {
            val generatedDeviceId = "gv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24)
            setDeviceId(generatedDeviceId)
        }
    }

    fun getApiKey(): String = sharedPreferences.getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(apiKey: String) {
        sharedPreferences.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
    }

    fun getServerUrl(): String {
        val saved = sharedPreferences.getString(KEY_SERVER_URL, null)
        if (saved.isNullOrBlank() || saved.contains("10.0.2.2") || saved.contains("localhost") || saved.contains("onrender.com")) {
            return "https://maker-important-radiation-wider.trycloudflare.com"
        }
        return saved
    }

    fun setServerUrl(url: String) {
        sharedPreferences.edit().putString(KEY_SERVER_URL, url.trim()).apply()
    }

    fun getDeviceId(): String = sharedPreferences.getString(KEY_DEVICE_ID, "") ?: ""

    fun setDeviceId(id: String) {
        sharedPreferences.edit().putString(KEY_DEVICE_ID, id).apply()
    }

    fun getLastTrustedServerTimestamp(): Long = sharedPreferences.getLong(KEY_LAST_TRUSTED_SERVER_TIME, 0L)

    fun getLastTrustedElapsedRealtime(): Long = sharedPreferences.getLong(KEY_LAST_TRUSTED_ELAPSED_REALTIME, 0L)

    fun saveTrustedTimeSync(serverTimestamp: Long, elapsedRealtime: Long) {
        sharedPreferences.edit()
            .putLong(KEY_LAST_TRUSTED_SERVER_TIME, serverTimestamp)
            .putLong(KEY_LAST_TRUSTED_ELAPSED_REALTIME, elapsedRealtime)
            .apply()
    }

    fun hasApiKey(): Boolean = getApiKey().isNotBlank()

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_LAST_TRUSTED_SERVER_TIME = "last_trusted_server_time"
        private const val KEY_LAST_TRUSTED_ELAPSED_REALTIME = "last_trusted_elapsed_realtime"
    }
}
