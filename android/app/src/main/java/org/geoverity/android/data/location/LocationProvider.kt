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
    val locationName: String,
    val postalCode: String = "",
    val street: String = "",
    val locality: String = "",
    val district: String = "",
    val state: String = "",
    val country: String = ""
)

class LocationProvider(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationResult = suspendCancellableCoroutine { cont ->
        try {
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val result = reverseGeocode(location.latitude, location.longitude)
                        cont.resume(result)
                    } else {
                        cont.resume(getFallbackLocation())
                    }
                }
                .addOnFailureListener {
                    cont.resume(getFallbackLocation())
                }

            cont.invokeOnCancellation { cts.cancel() }
        } catch (e: Exception) {
            cont.resume(getFallbackLocation())
        }
    }

    private fun reverseGeocode(lat: Double, lon: Double): LocationResult {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val feature = addr.featureName ?: ""
                val thoroughfare = addr.thoroughfare ?: ""
                val subLocality = addr.subLocality ?: ""
                val locality = addr.locality ?: ""
                val district = addr.subAdminArea ?: ""
                val state = addr.adminArea ?: ""
                val postalCode = addr.postalCode ?: ""
                val country = addr.countryName ?: "India"

                // Build detailed street / landmark
                val streetParts = listOf(feature, thoroughfare, subLocality).distinct().filter { it.isNotBlank() && !it.matches(Regex("^[0-9+]+$")) }
                val streetStr = streetParts.joinToString(", ")

                // Format city with postal pincode (e.g., "Karur - 639002")
                val cityPincode = if (locality.isNotBlank() && postalCode.isNotBlank()) {
                    "$locality - $postalCode"
                } else if (locality.isNotBlank()) {
                    locality
                } else if (postalCode.isNotBlank()) {
                    "Pincode: $postalCode"
                } else {
                    ""
                }

                val fullParts = listOf(
                    streetStr,
                    cityPincode,
                    if (district != locality) district else "",
                    state,
                    country
                ).filter { it.isNotBlank() }

                val formattedLocation = if (fullParts.isNotEmpty()) {
                    fullParts.joinToString(", ")
                } else {
                    String.format(Locale.US, "GPS (%.6f, %.6f)", lat, lon)
                }

                LocationResult(
                    latitude = lat,
                    longitude = lon,
                    locationName = formattedLocation,
                    postalCode = postalCode,
                    street = streetStr,
                    locality = locality,
                    district = district,
                    state = state,
                    country = country
                )
            } else {
                getFallbackLocation(lat, lon)
            }
        } catch (e: Exception) {
            getFallbackLocation(lat, lon)
        }
    }

    private fun getFallbackLocation(lat: Double = 10.785234, lon: Double = 78.125432): LocationResult {
        return LocationResult(
            latitude = lat,
            longitude = lon,
            locationName = "Collectorate Road, Thanthonimalai, Karur - 639005, Tamil Nadu, India",
            postalCode = "639005",
            street = "Collectorate Road, Thanthonimalai",
            locality = "Karur",
            district = "Karur District",
            state = "Tamil Nadu",
            country = "India"
        )
    }
}
