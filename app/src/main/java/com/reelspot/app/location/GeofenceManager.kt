package com.reelspot.app.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.reelspot.app.data.SavedPlace

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun addGeofenceForPlace(place: SavedPlace) {
        val geofence = Geofence.Builder()
            .setRequestId(place.id.toString())
            .setCircularRegion(
                place.latitude,
                place.longitude,
                place.radius
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setNotificationResponsiveness(1000)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    android.util.Log.d("GeofenceManager", "Geofence added for ${place.name}")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("GeofenceManager", "Failed to add geofence for ${place.name}: ${e.message}")
                }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun removeGeofenceForPlace(placeId: Long) {
        geofencingClient.removeGeofences(listOf(placeId.toString()))
            .addOnSuccessListener {
                android.util.Log.d("GeofenceManager", "Geofence removed for ID $placeId")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("GeofenceManager", "Failed to remove geofence for ID $placeId: ${e.message}")
            }
    }

    @SuppressLint("MissingPermission")
    fun updateGeofences(places: List<SavedPlace>) {
        if (places.isEmpty()) {
            geofencingClient.removeGeofences(geofencePendingIntent)
            return
        }

        val geofenceList = places.map { place ->
            Geofence.Builder()
                .setRequestId(place.id.toString())
                .setCircularRegion(place.latitude, place.longitude, place.radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        try {
            geofencingClient.removeGeofences(geofencePendingIntent).addOnCompleteListener {
                geofencingClient.addGeofences(request, geofencePendingIntent)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
