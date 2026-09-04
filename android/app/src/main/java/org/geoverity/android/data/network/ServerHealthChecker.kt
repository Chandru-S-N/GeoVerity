package org.geoverity.android.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.geoverity.android.GeoVerityApp
import java.net.Inet4Address
import java.net.NetworkInterface

data class ServerHealthState(
    val isConnected: Boolean = false,
    val serverUrl: String = "",
    val latencyMs: Long = 0L,
    val lastChecked: Long = 0L,
    val statusText: String = "Checking connection...",
    val serverVersion: String? = null,
    val errorMessage: String? = null,
    val isScanning: Boolean = false,
    val scanProgressText: String? = null
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
                val newState = _state.value.copy(
                    isConnected = true,
                    serverUrl = url,
                    latencyMs = latency,
                    lastChecked = System.currentTimeMillis(),
                    statusText = "Connected to Server Authority",
                    serverVersion = response.body()?.get("version")?.toString() ?: "1.0.0",
                    errorMessage = null,
                    isScanning = false,
                    scanProgressText = null
                )
                _state.value = newState
                newState
            } else {
                val newState = _state.value.copy(
                    isConnected = false,
                    serverUrl = url,
                    latencyMs = latency,
                    lastChecked = System.currentTimeMillis(),
                    statusText = "Server Error (HTTP ${response.code()})",
                    errorMessage = "HTTP ${response.code()}"
                )
                _state.value = newState
                newState
            }
        } catch (e: Exception) {
            val latency = SystemClock.elapsedRealtime() - start
            val newState = _state.value.copy(
                isConnected = false,
                serverUrl = url,
                latencyMs = latency,
                lastChecked = System.currentTimeMillis(),
                statusText = "Server Unreachable",
                errorMessage = e.message ?: "Failed to connect to $url"
            )
            _state.value = newState
            newState
        }
    }

    suspend fun testServerUrl(testUrl: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        val start = SystemClock.elapsedRealtime()
        try {
            val formattedUrl = if (testUrl.startsWith("http://") || testUrl.startsWith("https://")) testUrl else "http://$testUrl"
            val api = RetrofitClient.getApi(formattedUrl)
            val response = api.ping()
            val latency = SystemClock.elapsedRealtime() - start

            if (response.isSuccessful) {
                ConnectionTestResult(
                    isSuccess = true,
                    latencyMs = latency,
                    message = "Connected to Server Authority in ${latency}ms!"
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
                message = "Failed: ${e.javaClass.simpleName} (${e.message ?: "Unreachable"})"
            )
        }
    }

    fun getLocalDeviceIp(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        val hostAddress = address.hostAddress
                        if (hostAddress != null && (hostAddress.startsWith("192.168.") || hostAddress.startsWith("10.") || hostAddress.startsWith("172."))) {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }

    suspend fun scanAndAutoConnect(onProgress: ((String) -> Unit)? = null): String? = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(isScanning = true, scanProgressText = "Detecting Wi-Fi subnet...")
        onProgress?.invoke("Detecting Wi-Fi subnet...")

        val deviceIp = getLocalDeviceIp()
        val candidateIps = mutableListOf<String>()

        // 1. Add common standard emulator/host IPs
        candidateIps.add("10.0.2.2")
        candidateIps.add("127.0.0.1")

        // 2. If device has a LAN IP (e.g. 192.168.1.45), scan its subnet
        if (deviceIp != null) {
            val lastDotIndex = deviceIp.lastIndexOf('.')
            if (lastDotIndex != -1) {
                val subnetPrefix = deviceIp.substring(0, lastDotIndex + 1) // e.g. "192.168.1."
                val myHostNum = deviceIp.substring(lastDotIndex + 1).toIntOrNull() ?: 1

                // Add gateway and nearby IPs first for fast discovery
                candidateIps.add("${subnetPrefix}1")
                for (offset in 1..20) {
                    if (myHostNum - offset > 1) candidateIps.add("$subnetPrefix${myHostNum - offset}")
                    if (myHostNum + offset < 255) candidateIps.add("$subnetPrefix${myHostNum + offset}")
                }
                // Add remaining subnet hosts
                for (i in 2..254) {
                    val candidate = "$subnetPrefix$i"
                    if (!candidateIps.contains(candidate)) {
                        candidateIps.add(candidate)
                    }
                }
            }
        }

        // Also add common 192.168.1.x and 192.168.0.x gateways/ranges if not present
        listOf("192.168.1.1", "192.168.0.1", "192.168.1.2", "192.168.0.2", "192.168.1.100", "192.168.0.100").forEach {
            if (!candidateIps.contains(it)) candidateIps.add(it)
        }

        _state.value = _state.value.copy(scanProgressText = "Scanning ${candidateIps.size} candidate addresses on port 8080...")

        var foundServerUrl: String? = null

        // Scan concurrently in batches of 25
        val chunkSize = 25
        for (chunk in candidateIps.chunked(chunkSize)) {
            if (foundServerUrl != null) break

            val deferredResults = chunk.map { ip ->
                async {
                    val testUrl = "http://$ip:8080"
                    try {
                        val api = RetrofitClient.getApi(testUrl)
                        withTimeout(900L) {
                            val response = api.ping()
                            if (response.isSuccessful) testUrl else null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            for (res in deferredResults.awaitAll()) {
                if (res != null) {
                    foundServerUrl = res
                    break
                }
            }
        }

        if (foundServerUrl != null) {
            val secureStorage = GeoVerityApp.instance.secureStorage
            secureStorage.setServerUrl(foundServerUrl)
            _state.value = _state.value.copy(
                isConnected = true,
                serverUrl = foundServerUrl,
                statusText = "Connected to Server Authority",
                isScanning = false,
                scanProgressText = null
            )
            checkHealth()
            foundServerUrl
        } else {
            _state.value = _state.value.copy(
                isScanning = false,
                scanProgressText = null
            )
            null
        }
    }
}
