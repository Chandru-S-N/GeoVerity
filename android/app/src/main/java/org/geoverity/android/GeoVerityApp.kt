package org.geoverity.android

import android.app.Application
import org.geoverity.android.data.db.GeoVerityDatabase
import org.geoverity.android.data.security.SecureStorage

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
    }

    companion object {
        lateinit var instance: GeoVerityApp
            private set
    }
}
