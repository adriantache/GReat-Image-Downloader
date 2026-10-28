package com.adriantache.greatimagedownloader.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.adriantache.greatimagedownloader.domain.data.Repository
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.model.DomainError
import com.adriantache.greatimagedownloader.domain.model.DomainException
import com.adriantache.greatimagedownloader.domain.utils.model.Event
import com.adriantache.greatimagedownloader.service.DataTransferTool.ServiceState
import com.adriantache.greatimagedownloader.service.model.PhotoFileItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PhotoDownloadService : Service(), KoinComponent {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val repository: Repository by inject()
    private val dataTransferTool: DataTransferTool by inject()

    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var downloadJob: Job? = null

    // Used to interrupt download without corrupting current file.
    @Volatile
    private var continueDownload = false

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        registerNotificationChannel(this)

        val notification = getNotification(this)
        startForeground(1, notification)

        acquireLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.START.name -> {
                Log.d("PhotoDownloadService", "onStartCommand: START")
                val inMemoryPhotos = dataTransferTool.consumePendingPhotos()
                val photosToDownload = if (inMemoryPhotos.isNotEmpty()) {
                    inMemoryPhotos
                } else {
                    val parcelablePhotos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.extras?.getParcelableArrayList(PHOTOS_LIST_EXTRA, PhotoFileItem::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.extras?.getParcelableArrayList(PHOTOS_LIST_EXTRA)
                    }
                    parcelablePhotos?.map { PhotoFile(it.directory, it.name) }
                }
                start(photosToDownload)
            }

            Actions.CONNECT.name -> {
                Log.d("PhotoDownloadService", "onStartCommand: CONNECT")
                dataTransferTool.serviceStateFlow.value = ServiceState.CONNECTING
            }

            Actions.STOP.name -> {
                continueDownload = false
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseLocks()
        scope.cancel()
    }

    private fun start(photosToDownload: List<PhotoFile>?) {
        if (photosToDownload.isNullOrEmpty()) {
            Log.e(this::class.java.simpleName, "Photos list is null!")
            scope.launch { disconnect() }
            return
        }

        continueDownload = true
        downloadJob?.cancel()

        downloadJob = scope.launch {
            dataTransferTool.serviceStateFlow.value = ServiceState.DOWNLOADING
            downloadPhotos(photosToDownload)
        }
    }

    private fun handleError(error: DomainError) {
        val message = when (error) {
            DomainError.CameraDisconnected -> "Camera connection lost."
            DomainError.StorageFull -> "Storage full!"
            DomainError.NetworkError -> "Connection failed. Please check if the camera is on."
            is DomainError.Unknown -> "An error occurred: ${error.message}"
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2, getErrorNotification(this, message))

        dataTransferTool.errorFlow.value = Event(error)
    }

    private suspend fun downloadPhotos(photosToDownload: List<PhotoFile>) {
        var isError = false
        val completedPhotos = mutableListOf<PhotoFile>()

        for (index in photosToDownload.indices) {
            if (!continueDownload) {
                break
            }

            updateNotification(index + 1, photosToDownload.size)

            val photo = photosToDownload[index]
            var lastUri: String? = null
            var lastProgress = 0

            try {
                repository.downloadMediaToStorage(photo).collect { result ->
                    if (!continueDownload) {
                        throw CancellationException("Download stopped by user")
                    }

                    result.fold(
                        onSuccess = { info ->
                            lastUri = info.uri
                            lastProgress = info.downloadProgress
                            dataTransferTool.emitImageProgress(info)
                            if (info.downloadProgress == 100) {
                                completedPhotos.add(photo)
                            }
                        },
                        onFailure = { throwable ->
                            isError = true
                            lastUri?.let { uri ->
                                if (lastProgress < 100) {
                                    repository.deleteMedia(uri)
                                }
                            }

                            val domainError = (throwable as? DomainException)?.domainError
                                ?: DomainError.Unknown(throwable.message)
                            handleError(domainError)
                        }
                    )
                }
            } catch (_: CancellationException) {
                lastUri?.let { uri ->
                    if (lastProgress < 100) {
                        repository.deleteMedia(uri)
                    }
                }
                break
            }

            if (isError || !continueDownload) break
        }

        if (completedPhotos.isNotEmpty()) {
            updateLatestDownloadedPhotos(completedPhotos)
        }

        if (!isError) {
            dataTransferTool.downloadFinishedFlow.value = Event(Unit)
        }

        disconnect()
    }

    private suspend fun updateLatestDownloadedPhotos(downloadedPhotos: List<PhotoFile>) {
        val lastFiles = downloadedPhotos.groupBy { it.directory }
            .mapValues { entry -> entry.value.maxBy { it.name } }

        if (lastFiles.isEmpty()) return

        val existingByDir = runCatching { repository.getLatestDownloadedPhotos().getOrNull() }
            .getOrNull()
            .orEmpty()
            .associateBy { it.directory }
            .toMutableMap()

        for ((dir, newFile) in lastFiles) {
            val existing = existingByDir[dir]
            if (existing == null || newFile.name >= existing.name) {
                existingByDir[dir] = newFile
            }
        }

        repository.saveLatestDownloadedPhotos(existingByDir.values.toList())
    }

    private fun updateNotification(currentImage: Int, totalImages: Int) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notification = getNotification(this, currentImage, totalImages)
        notificationManager.notify(1, notification)
    }

    private suspend fun disconnect() {
        dataTransferTool.serviceStateFlow.value = ServiceState.IDLE
        repository.shutDownCamera()

        stopSelf()
    }

    private fun acquireLocks() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val lockType = WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        wifiLock = wifiManager.createWifiLock(lockType, "PhotoDownloadService:WifiLock")
        wifiLock?.acquire()

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PhotoDownloadService:WakeLock")
        wakeLock?.acquire(10 * 60 * 1000L /*10 minutes max*/)
    }

    private fun releaseLocks() {
        if (wifiLock?.isHeld == true) {
            wifiLock?.release()
        }
        wifiLock = null

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
    }

    enum class Actions {
        START, CONNECT, STOP
    }

    companion object {
        const val PHOTOS_LIST_EXTRA = "photos_list_extra"
        const val WIFI_DETAILS_EXTRA = "wifi_details_extra"
    }
}
