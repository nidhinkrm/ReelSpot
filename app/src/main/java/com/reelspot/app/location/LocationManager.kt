package com.reelspot.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                _currentLocation.value = location
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    _currentLocation.value = location
                }
            }

            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                10000L
            ).setMinUpdateIntervalMillis(5000L).build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                context.mainLooper
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    suspend fun searchAddress(query: String): List<Address> = withContext(Dispatchers.IO) {
        if (query.isBlank() || !Geocoder.isPresent()) return@withContext emptyList()
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                withTimeoutOrNull(4000L) {
                    suspendCancellableCoroutine<List<Address>> { continuation ->
                        try {
                            geocoder.getFromLocationName(query, 5) { addresses ->
                                if (continuation.isActive) {
                                    continuation.resume(addresses)
                                }
                            }
                        } catch (e: Exception) {
                            if (continuation.isActive) {
                                continuation.resume(emptyList())
                            }
                        }
                    }
                } ?: emptyList()
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, 5) ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAddressFromCoordinates(latitude: Double, longitude: Double): String =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) {
                return@withContext "%.4f, %.4f".format(latitude, longitude)
            }
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    withTimeoutOrNull(4000L) {
                        suspendCancellableCoroutine<List<Address>> { continuation ->
                            try {
                                geocoder.getFromLocation(latitude, longitude, 1) { list ->
                                    if (continuation.isActive) {
                                        continuation.resume(list)
                                    }
                                }
                            } catch (e: Exception) {
                                if (continuation.isActive) {
                                    continuation.resume(emptyList())
                                }
                            }
                        }
                    } ?: emptyList()
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1) ?: emptyList()
                }

                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    address.getAddressLine(0) ?: "${address.locality ?: ""}, ${address.countryName ?: ""}"
                } else {
                    "%.4f, %.4f".format(latitude, longitude)
                }
            } catch (e: Exception) {
                "%.4f, %.4f".format(latitude, longitude)
            }
        }

    companion object {
        fun calculateDistanceMeters(
            startLat: Double, startLng: Double,
            endLat: Double, endLng: Double
        ): Float {
            val results = FloatArray(1)
            Location.distanceBetween(startLat, startLng, endLat, endLng, results)
            return results[0]
        }

        fun formatDistance(meters: Float): String {
            return if (meters < 1000) {
                "${meters.toInt()} m"
            } else {
                "%.1f km".format(meters / 1000f)
            }
        }
    }
}
