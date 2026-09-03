package org.geoverity.android.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val locationName: String
)

class LocationProvider(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationResult? = suspendCancellableCoroutine { cont ->
        try {
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val locationName = reverseGeocode(location.latitude, location.longitude)
                        cont.resume(LocationResult(location.latitude, location.longitude, locationName))
                    } else {
                        // Fallback default coordinate for development / indoor
                        cont.resume(LocationResult(10.785234, 78.125432, "Karur, Tamil Nadu, India"))
                    }
                }
                .addOnFailureListener {
                    cont.resume(LocationResult(10.785234, 78.125432, "Karur, Tamil Nadu, India"))
                }

            cont.invokeOnCancellation { cts.cancel() }
        } catch (e: Exception) {
            cont.resume(LocationResult(10.785234, 78.125432, "Karur, Tamil Nadu, India"))
        }
    }

    private fun reverseGeocode(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val locality = addr.locality ?: addr.subAdminArea ?: ""
                val adminArea = addr.adminArea ?: ""
                val country = addr.countryName ?: ""
                listOf(locality, adminArea, country).filter { it.isNotBlank() }.joinToString(", ")
            } else {
                "Location ($lat, $lon)"
            }
        } catch (e: Exception) {
            "Karur, Tamil Nadu, India"
        }
    }
}
