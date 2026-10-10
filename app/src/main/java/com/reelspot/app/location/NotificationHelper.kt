package com.reelspot.app.location

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.reelspot.app.MainActivity
import com.reelspot.app.R

object NotificationHelper {

    private const val CHANNEL_ID = "reelspot_geofence_channel"
    private const val CHANNEL_NAME = "ReelSpot Nearby Reminders"
    private const val CHANNEL_DESC = "Notifications when you are near a saved ReelSpot place"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showGeofenceNotification(context: Context, placeId: Long, placeName: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_PLACE_ID", placeId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            placeId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ReelSpot Nearby!")
            .setContentText("You saved $placeName on ReelSpot. You're nearby!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You saved $placeName on ReelSpot. You're nearby!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(placeId.toInt(), notification)
        } catch (e: SecurityException) {
            // Permission POST_NOTIFICATIONS missing or revoked
            e.printStackTrace()
        }
    }
}
