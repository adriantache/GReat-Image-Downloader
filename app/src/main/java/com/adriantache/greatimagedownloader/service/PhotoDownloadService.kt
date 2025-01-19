package com.adriantache.greatimagedownloader.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.adriantache.greatimagedownloader.domain.data.Repository
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.utils.model.Event
import com.adriantache.greatimagedownloader.service.model.PhotoFileItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class PhotoDownloadService : Service(), KoinComponent {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val repository: Repository by inject()
    private val dataTransferTool: DataTransferTool by inject()

    // Used to interrupt download without corrupting current file.
    private var continueDownload = false

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        registerNotificationChannel(this)

        val notification = getNotification(this)
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.START.name -> {
                val photosToDownload = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.extras?.getParcelableArrayList(PHOTOS_LIST_EXTRA, PhotoFileItem::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.extras?.getParcelableArrayList(PHOTOS_LIST_EXTRA)
                }
                start(photosToDownload?.toList())
            }

            Actions.STOP.name -> {
                if (continueDownload) {
                    continueDownload = false
                } else {
                    scope.cancel()
                    stopSelf()
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun start(photosToDownload: List<PhotoFileItem>?) {
        if (photosToDownload.isNullOrEmpty()) {
            Log.e(this::class.java.simpleName, "Photos list is null!")
            scope.launch { disconnect() }
            return
        }

        continueDownload = true

        scope.launch {
            downloadPhotos(photosToDownload)
        }
    }

    private fun downloadPhotos(photosToDownload: List<PhotoFileItem>) {
        scope.launch {
            for (index in photosToDownload.indices) {
                updateNotification(index + 1, photosToDownload.size)

                val photoItem = photosToDownload[index]
                val photo = PhotoFile(
                    directory = photoItem.directory,
                    name = photoItem.name,
                )

                if (!continueDownload) {
                    break
                }

                repository.downloadMediaToStorage(photo).collect { photoDownloadInfo ->
                    dataTransferTool.imageFlow.value = photoDownloadInfo
                }
            }

            dataTransferTool.downloadFinishedFlow.value = Event(Unit)

            disconnect()
        }
    }

    private fun updateNotification(currentImage: Int, totalImages: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = getNotification(this, currentImage, totalImages)
        notificationManager.notify(1, notification)
    }

    private suspend fun disconnect() {
        repository.shutDownCamera()

        scope.cancel()
        stopSelf()
    }

    enum class Actions {
        START, STOP
    }

    companion object {
        const val PHOTOS_LIST_EXTRA = "photos_list_extra"
    }
}
