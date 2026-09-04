package org.geoverity.android

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.geoverity.android.data.db.GeoVerityDatabase
import org.geoverity.android.data.network.ServerHealthChecker
import org.geoverity.android.data.security.SecureStorage
import org.geoverity.android.offline.OfflineSyncManager

class GeoVerityApp : Application() {

    lateinit var database: GeoVerityDatabase
        private set

    lateinit var secureStorage: SecureStorage
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = GeoVerityDatabase.getDatabase(this)
        secureStorage = SecureStorage(this)

        // Initialize Automatic Real-Time Offline Auto-Sync on Network Connection
        OfflineSyncManager.initialize(this)

        // Auto-connect to server on startup: check configured URL, auto-scan if unreachable
        appScope.launch {
            val health = ServerHealthChecker.checkHealth()
            if (!health.isConnected) {
                ServerHealthChecker.scanAndAutoConnect()
            }
        }
    }

    companion object {
        lateinit var instance: GeoVerityApp
            private set
    }
}
