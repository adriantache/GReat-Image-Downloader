package com.adriantache.greatimagedownloader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.adriantache.greatimagedownloader.R

private const val NOTIFICATION_CHANNEL = "SERVICE_NOTIFICATION_CHANNEL"
private const val ERROR_CHANNEL = "ERROR_NOTIFICATION_CHANNEL"

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

    val stopIntent = Intent(context, PhotoDownloadService::class.java).apply {
        action = PhotoDownloadService.Actions.STOP.name
    }
    val stopPendingIntent = PendingIntent.getService(
        context,
        2,
        stopIntent,
        PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(Notification.CATEGORY_SERVICE)
        .setSmallIcon(R.drawable.ic_stat_gr)
        .setContentTitle("Downloading...")
        .setContentText(content)
        .setContentIntent(pendingIntent)
        .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
        .build()
}

fun getErrorNotification(
    context: Context,
    errorMessage: String,
): Notification {
    val pendingIntent = PendingIntent.getActivity(
        /* context = */ context,
        /* requestCode = */ 1,
        /* intent = */ context.packageManager.getLaunchIntentForPackage(context.packageName),
        /* flags = */ PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(context, ERROR_CHANNEL)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(Notification.CATEGORY_ERROR)
        .setSmallIcon(R.drawable.ic_stat_gr)
        .setContentTitle("Download Error")
        .setContentText(errorMessage)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
}

fun registerNotificationChannel(
    context: Context,
) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val progressChannel = NotificationChannel(
        /* id = */ NOTIFICATION_CHANNEL,
        /* name = */ "Download progress",
        /* importance = */ NotificationManager.IMPORTANCE_LOW,
    )
    notificationManager.createNotificationChannel(progressChannel)

    val errorChannel = NotificationChannel(
        /* id = */ ERROR_CHANNEL,
        /* name = */ "Download errors",
        /* importance = */ NotificationManager.IMPORTANCE_HIGH,
    )
    notificationManager.createNotificationChannel(errorChannel)
}
