package org.geoverity.android.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.*
import java.util.concurrent.TimeUnit

object OfflineSyncManager {

    private const val UNIQUE_WORK_NAME = "GeoVerityAutoSyncWork"

    fun initialize(context: Context) {
        // 1. Register Real-Time Network State Callback
        registerNetworkCallback(context)

        // 2. Schedule Periodic Connected Constraint Worker
        schedulePeriodicSync(context)
    }

    fun triggerImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWork = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncWork
        )
    }

    private fun registerNetworkCallback(context: Context) {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(
                networkRequest,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        // Trigger immediate background auto-sync when online
                        triggerImmediateSync(context)
                    }
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncWork = PeriodicWorkRequestBuilder<OfflineSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "GeoVerityPeriodicAutoSync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncWork
        )
    }
}
