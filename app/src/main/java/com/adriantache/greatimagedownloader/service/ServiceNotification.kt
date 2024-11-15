package com.adriantache.greatimagedownloader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import com.adriantache.greatimagedownloader.R

private const val NOTIFICATION_CHANNEL = "SERVICE_NOTIFICATION_CHANNEL"

fun getNotification(
    context: Context,
    currentImage: Int? = null,
    totalImages: Int? = null,
): Notification {
    val content = if (currentImage == null || totalImages == null) {
        "Starting download"
    } else {
        "Processing $currentImage/$totalImages"
    }

    val pendingIntent = PendingIntent.getActivity(
        /* context = */ context,
        /* requestCode = */ 0,
        /* intent = */ context.packageManager.getLaunchIntentForPackage(context.packageName),
        /* flags = */ PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
        .setOngoing(true)
        .setPriority(PRIORITY_MIN)
        .setCategory(Notification.CATEGORY_SERVICE)
        .setSmallIcon(R.drawable.ic_stat_gr)
        .setContentTitle("Downloading...")
        .setContentText(content)
        .setContentIntent(pendingIntent)
        .build()
}

fun registerNotificationChannel(
    context: Context,
) {
    val channel = NotificationChannel(
        /* id = */ NOTIFICATION_CHANNEL,
        /* name = */ "Download progress",
        /* importance = */ NotificationManager.IMPORTANCE_LOW,
    )

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    notificationManager.createNotificationChannel(channel)
}
