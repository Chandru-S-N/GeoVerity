package org.geoverity.android.data.network

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.geoverity.android.GeoVerityApp

data class ServerHealthState(
    val isConnected: Boolean = false,
    val serverUrl: String = "",
    val latencyMs: Long = 0L,
    val lastChecked: Long = 0L,
    val statusText: String = "Checking connection...",
    val serverVersion: String? = null,
    val errorMessage: String? = null
)

data class ConnectionTestResult(
    val isSuccess: Boolean,
    val latencyMs: Long,
    val message: String
)

object ServerHealthChecker {

    private val _state = MutableStateFlow(ServerHealthState())
    val state: StateFlow<ServerHealthState> = _state.asStateFlow()

    suspend fun checkHealth(): ServerHealthState = withContext(Dispatchers.IO) {
        val secureStorage = GeoVerityApp.instance.secureStorage
        val url = secureStorage.getServerUrl()
        val start = SystemClock.elapsedRealtime()

        try {
            val api = RetrofitClient.getApi(url)
            val response = api.ping()
            val latency = SystemClock.elapsedRealtime() - start

            if (response.isSuccessful) {
                val newState = ServerHealthState(
                    isConnected = true,
                    serverUrl = url,
                    latencyMs = latency,
                    lastChecked = System.currentTimeMillis(),
                    statusText = "Connected to Server Authority",
                    serverVersion = response.body()?.get("version")?.toString() ?: "1.0.0",
                    errorMessage = null
                )
                _state.value = newState
                newState
            } else {
                val newState = ServerHealthState(
                    isConnected = false,
                    serverUrl = url,
                    latencyMs = latency,
                    lastChecked = System.currentTimeMillis(),
                    statusText = "Server returned error: ${response.code()}",
                    errorMessage = "HTTP ${response.code()}"
                )
                _state.value = newState
                newState
            }
        } catch (e: Exception) {
            val latency = SystemClock.elapsedRealtime() - start
            val newState = ServerHealthState(
                isConnected = false,
                serverUrl = url,
                latencyMs = latency,
                lastChecked = System.currentTimeMillis(),
                statusText = "Server Unreachable (${e.javaClass.simpleName})",
                errorMessage = e.message ?: "Failed to connect to $url"
            )
            _state.value = newState
            newState
        }
    }

    suspend fun testServerUrl(testUrl: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        val start = SystemClock.elapsedRealtime()
        try {
            val api = RetrofitClient.getApi(testUrl)
            val response = api.ping()
            val latency = SystemClock.elapsedRealtime() - start

            if (response.isSuccessful) {
                ConnectionTestResult(
                    isSuccess = true,
                    latencyMs = latency,
                    message = "Connection Verified! Response received in ${latency}ms"
                )
            } else {
                ConnectionTestResult(
                    isSuccess = false,
                    latencyMs = latency,
                    message = "Server reached but returned HTTP ${response.code()}"
                )
            }
        } catch (e: Exception) {
            val latency = SystemClock.elapsedRealtime() - start
            ConnectionTestResult(
                isSuccess = false,
                latencyMs = latency,
                message = "Failed: ${e.message ?: e.javaClass.simpleName}"
            )
        }
    }
}
