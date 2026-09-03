package org.geoverity.android

import android.app.Application
import org.geoverity.android.data.db.GeoVerityDatabase
import org.geoverity.android.data.security.SecureStorage
import org.geoverity.android.offline.OfflineSyncManager

class GeoVerityApp : Application() {

    lateinit var database: GeoVerityDatabase
        private set

    lateinit var secureStorage: SecureStorage
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = GeoVerityDatabase.getDatabase(this)
        secureStorage = SecureStorage(this)

        // Initialize Automatic Real-Time Offline Auto-Sync on Network Connection
        OfflineSyncManager.initialize(this)
    }

    companion object {
        lateinit var instance: GeoVerityApp
            private set
    }
}
