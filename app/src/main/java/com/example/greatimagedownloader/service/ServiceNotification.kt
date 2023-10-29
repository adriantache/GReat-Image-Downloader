package com.example.greatimagedownloader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import com.example.greatimagedownloader.R

private const val NOTIFICATION_CHANNEL = "SERVICE_NOTIFICATION_CHANNEL"

fun getNotification(
    context: Context,
): Notification {
    return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
        .setOngoing(true)
        .setPriority(PRIORITY_MIN)
        .setCategory(Notification.CATEGORY_SERVICE)
        .setSmallIcon(R.mipmap.ic_launcher_foreground)
        .setContentTitle("Downloading images from camera.")
        .setContentText("Processing 1/1")
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
