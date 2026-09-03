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
            val generatedDeviceId = "dev_gv_" + UUID.randomUUID().toString().substring(0, 18)
            setDeviceId(generatedDeviceId)
        }
        // Set default demo API key if not set
        if (getApiKey().isBlank()) {
            setApiKey("gv_live_demo_android_app_key_2026_98a72")
        }
    }

    fun getApiKey(): String = sharedPreferences.getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(apiKey: String) {
        sharedPreferences.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
    }

    fun getServerUrl(): String = sharedPreferences.getString(KEY_SERVER_URL, "http://10.0.2.2:8080") ?: "http://10.0.2.2:8080"

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

    fun getMaskedApiKey(): String {
        val key = getApiKey()
        if (key.length <= 12) return "gv_live_************"
        return key.take(8) + "************" + key.takeLast(4)
    }

    companion object {
        private const val KEY_API_KEY = "api_key"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_LAST_TRUSTED_SERVER_TIME = "last_trusted_server_time"
        private const val KEY_LAST_TRUSTED_ELAPSED_REALTIME = "last_trusted_elapsed_realtime"
    }
}
