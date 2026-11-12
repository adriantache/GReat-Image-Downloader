package com.adriantache.greatimagedownloader.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.glance.appwidget.updateAll
import com.adriantache.greatimagedownloader.domain.DownloadPhotosUseCase
import com.adriantache.greatimagedownloader.domain.ble.ConnectionStage
import com.adriantache.greatimagedownloader.domain.data.Repository
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.data.model.WifiDetails
import com.adriantache.greatimagedownloader.domain.model.DomainError
import com.adriantache.greatimagedownloader.domain.model.DomainException
import com.adriantache.greatimagedownloader.domain.utils.model.Event
import com.adriantache.greatimagedownloader.domain.wifi.WifiUtil
import com.adriantache.greatimagedownloader.service.DataTransferTool.ServiceState
import com.adriantache.greatimagedownloader.service.DataTransferTool.WidgetState
import com.adriantache.greatimagedownloader.service.model.PhotoFileItem
import com.adriantache.greatimagedownloader.widget.QuickDownloadWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.seconds

class PhotoDownloadService : Service(), KoinComponent {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val repository: Repository by inject()
    private val dataTransferTool: DataTransferTool by inject()
    private val wifiUtil: WifiUtil by inject()

    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var downloadJob: Job? = null

    @Volatile
    private var hasEnteredActiveState = false

    // Used to interrupt download without corrupting current file.
    @Volatile
    private var continueDownload = false

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        registerNotificationChannel(this)

        val notification = getNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(1, notification)
        }

        acquireLocks()

        scope.launch {
            dataTransferTool.serviceStateFlow.collect { state ->
                when (state) {
                    ServiceState.CONNECTING -> {
                        hasEnteredActiveState = true
                        val stage = dataTransferTool.connectionStageFlow.value
                        val message = when (stage) {
                            ConnectionStage.CONNECTING_BLE -> "Connecting via Bluetooth..."
                            ConnectionStage.CAMERA_CONNECTED -> "Camera connected! Waking..."
                            ConnectionStage.STARTING_WIFI -> "Starting camera Wi-Fi..."
                            ConnectionStage.JOINING_WIFI -> "Joining camera Wi-Fi..."
                            null -> "Establishing Wi-Fi connection"
                        }
                        updateStatusNotification(
                            title = "Connecting to camera...",
                            message = message,
                        )
                    }

                    ServiceState.FETCHING -> {
                        hasEnteredActiveState = true
                        updateStatusNotification(
                            title = "Checking camera...",
                            message = "Looking for new photos",
                        )
                    }

                    ServiceState.DOWNLOADING -> {
                        hasEnteredActiveState = true
                        Log.d("PhotoDownloadService", "serviceStateFlow collector: DOWNLOADING (downloadJobActive=${downloadJob?.isActive})")
                        if (downloadJob?.isActive != true) {
                            val pending = dataTransferTool.consumePendingPhotos()
                            Log.d("PhotoDownloadService", "serviceStateFlow collector: consumed ${pending.size} pending photos")
                            if (pending.isNotEmpty()) {
                                start(pending)
                            }
                        }
                    }

                    ServiceState.STOPPING -> {
                        updateStatusNotification(
                            title = "Stopping...",
                            message = "Disconnecting from camera",
                        )
                    }

                    ServiceState.IDLE -> {
                        if (hasEnteredActiveState && downloadJob?.isActive != true) {
                            hasEnteredActiveState = false
                            refreshWidget()
                            stopSelf()
                        }
                    }
                }
            }
        }

        scope.launch {
            combine(
                dataTransferTool.connectionStageFlow,
                dataTransferTool.connectionAttemptFlow,
            ) { stage, attempt ->
                stage to attempt
            }.collect { (stage, attempt) ->
                if (dataTransferTool.serviceStateFlow.value == ServiceState.CONNECTING && stage != null) {
                    val message = when (stage) {
                        ConnectionStage.CONNECTING_BLE -> "Connecting via Bluetooth..."
                        ConnectionStage.CAMERA_CONNECTED -> "Camera connected! Waking..."
                        ConnectionStage.STARTING_WIFI -> "Starting camera Wi-Fi..."
                        ConnectionStage.JOINING_WIFI -> {
                            if (attempt > 1) {
                                "Joining camera Wi-Fi (attempt $attempt/5)..."
                            } else {
                                "Joining camera Wi-Fi..."
                            }
                        }
                    }
                    updateStatusNotification(
                        title = "Connecting to camera...",
                        message = message,
                    )
                }
            }
        }

        scope.launch {
            dataTransferTool.widgetStateFlow.collect {
                refreshWidget()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.START.name -> {
                Log.d("PhotoDownloadService", "onStartCommand: START")
                hasEnteredActiveState = true
                if (downloadJob?.isActive == true) {
                    return START_STICKY
                }
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
                hasEnteredActiveState = true
                acquireLocks()
                dataTransferTool.serviceStateFlow.value = ServiceState.CONNECTING
            }

            Actions.QUICK_DOWNLOAD.name -> {
                Log.d("PhotoDownloadService", "onStartCommand: QUICK_DOWNLOAD")
                hasEnteredActiveState = true
                acquireLocks()
                if (downloadJob?.isActive == true) {
                    Log.d("PhotoDownloadService", "onStartCommand: QUICK_DOWNLOAD ignored, downloadJob already active")
                    return START_STICKY
                }
                val useCase = getKoin().getOrNull<DownloadPhotosUseCase>()
                if (useCase != null) {
                    useCase.startBackgroundSync()
                } else {
                    dataTransferTool.widgetStateFlow.value = WidgetState.Error
                    stopSelf()
                }
            }

            Actions.STOP.name -> {
                continueDownload = false
                if (downloadJob?.isActive == true) {
                    dataTransferTool.widgetStateFlow.value = WidgetState.Stopping
                } else {
                    getKoin().getOrNull<DownloadPhotosUseCase>()?.cancelConnection()
                    scope.launch { disconnect() }
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseLocks()
        scope.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun start(photosToDownload: List<PhotoFile>?) {
        Log.d("PhotoDownloadService", "start: photosToDownload count = ${photosToDownload?.size}")
        if (photosToDownload.isNullOrEmpty()) {
            Log.e(this::class.java.simpleName, "Photos list is null!")
            scope.launch { disconnect() }
            return
        }

        continueDownload = true
        acquireLocks()
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

        dataTransferTool.widgetStateFlow.value = WidgetState.Error
        dataTransferTool.errorFlow.value = Event(error)
    }

    private suspend fun downloadPhotos(photosToDownload: List<PhotoFile>) {
        Log.d("PhotoDownloadService", "downloadPhotos: Beginning download of ${photosToDownload.size} photos")
        var isError = false
        val completedPhotos = mutableListOf<PhotoFile>()

        for (index in photosToDownload.indices) {
            if (!continueDownload) break

            val photo = photosToDownload[index]
            dataTransferTool.imageFlow.value = PhotoDownloadInfo(
                name = photo.name,
                uri = "",
                downloadProgress = 0,
                downloadSpeed = com.adriantache.greatimagedownloader.domain.utils.model.Kbps(0.0),
                isLandscape = true,
            )
            updateNotification(index + 1, photosToDownload.size)
            dataTransferTool.widgetStateFlow.value = WidgetState.Downloading(index + 1, photosToDownload.size)

            Log.d("PhotoDownloadService", "downloadPhotos: [${index + 1}/${photosToDownload.size}] Starting ${photo.directory}/${photo.name}")

            // Ensure we are bound (or reconnected) to the camera's Wi-Fi network before requesting the file
            ensureCameraWifiConnected()

            // Refresh camera status (battery / thermal) periodically rather than before every photo
            // to minimize HTTP connection churn on the camera's single-threaded server
            if (index == 0 || index % 5 == 0) {
                val status = runCatching { repository.getCameraStatus().getOrNull() }.getOrNull()
                if (status != null) {
                    dataTransferTool.cameraStatusFlow.value = status
                }
            }

            var photoDownloaded = false
            var lastDomainError: DomainError? = null
            val maxRetries = 4

            for (attempt in 1..maxRetries) {
                if (!continueDownload) break

                var lastUri: String? = null
                var lastProgress = 0
                var attemptFailed = false

                try {
                    Log.d("PhotoDownloadService", "downloadPhotos: [${photo.name}] attempt $attempt/$maxRetries started")
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
                                    photoDownloaded = true
                                    Log.d("PhotoDownloadService", "downloadPhotos: [${photo.name}] successfully finished (URI: $lastUri)")
                                }
                            },
                            onFailure = { throwable ->
                                attemptFailed = true
                                Log.e("PhotoDownloadService", "downloadPhotos: [${photo.name}] attempt $attempt failed with error: ${throwable.message}")
                                lastUri?.let { uri ->
                                    if (lastProgress < 100) {
                                        repository.deleteMedia(uri)
                                    }
                                }

                                lastDomainError = (throwable as? DomainException)?.domainError
                                    ?: DomainError.Unknown(throwable.message)
                            }
                        )
                    }
                } catch (_: CancellationException) {
                    Log.d("PhotoDownloadService", "downloadPhotos: [${photo.name}] cancelled")
                    lastUri?.let { uri ->
                        if (lastProgress < 100) {
                            repository.deleteMedia(uri)
                        }
                    }
                    break
                }

                if (photoDownloaded) {
                    break
                }

                // If storage is full or user cancelled, don't retry
                if (lastDomainError == DomainError.StorageFull || !continueDownload) {
                    break
                }

                if (attemptFailed && attempt < maxRetries) {
                    Log.w("PhotoDownloadService", "Download of ${photo.name} failed (attempt $attempt/$maxRetries, error: $lastDomainError). Retrying...")
                    ensureCameraWifiConnected(forceReconnect = attempt >= 2)
                    delay(500L * attempt)
                }
            }

            if (!photoDownloaded && continueDownload) {
                isError = true
                Log.e("PhotoDownloadService", "downloadPhotos: All $maxRetries attempts failed for ${photo.name}")
                handleError(lastDomainError ?: DomainError.NetworkError)
                break
            }
        }

        if (completedPhotos.isNotEmpty()) {
            Log.d("PhotoDownloadService", "downloadPhotos: Updating latest downloaded photos for ${completedPhotos.size} files")
            updateLatestDownloadedPhotos(completedPhotos)
        }

        Log.d("PhotoDownloadService", "downloadPhotos: Shutting down camera (completed=${completedPhotos.size}, error=$isError)")
        repository.shutDownCamera()

        if (!isError || !continueDownload) {
            dataTransferTool.widgetStateFlow.value = if (!continueDownload) {
                WidgetState.Idle
            } else {
                WidgetState.Done(completedPhotos.size)
            }
            refreshWidget()
            dataTransferTool.downloadFinishedFlow.value = Event(Unit)
        }

        dataTransferTool.serviceStateFlow.value = ServiceState.IDLE
        Log.d("PhotoDownloadService", "downloadPhotos: Finished, stopping service")
        stopSelf()
    }

    private suspend fun ensureCameraWifiConnected(forceReconnect: Boolean = false): Boolean {
        if (!forceReconnect && wifiUtil.ensureBound()) {
            return true
        }

        val wifiDetails = runCatching { repository.getWifiDetails().getOrNull() }.getOrNull()
        val ssid = wifiDetails?.ssid?.takeIf { it.isNotBlank() }
        val password = wifiDetails?.password
        if (ssid == null || password == null) {
            return wifiUtil.ensureBound()
        }

        Log.w(
            "PhotoDownloadService",
            "ensureCameraWifiConnected: Reconnecting to camera Wi-Fi (ssid=$ssid, forceReconnect=$forceReconnect)",
        )
        val (connected, newBssid) = wifiUtil.connectToWifi(ssid, password, wifiDetails.bssid)
        if (connected) {
            val resolvedBssid = newBssid?.takeIf { it.isNotBlank() } ?: wifiDetails.bssid
            if (resolvedBssid != wifiDetails.bssid) {
                runCatching { repository.saveWifiDetails(WifiDetails(ssid, password, resolvedBssid)) }
            }
            wifiUtil.ensureBound()
            return true
        }

        return wifiUtil.ensureBound()
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

    private fun updateStatusNotification(title: String, message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val notification = getNotification(this, title = title, message = message)
        notificationManager.notify(1, notification)
    }

    private suspend fun refreshWidget() {
        runCatching {
            QuickDownloadWidget().updateAll(applicationContext)
        }
    }

    private suspend fun disconnect() {
        repository.shutDownCamera()
        dataTransferTool.serviceStateFlow.value = ServiceState.IDLE
        refreshWidget()
        stopSelf()
    }

    private fun acquireLocks() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val lockType = WifiManager.WIFI_MODE_FULL_HIGH_PERF
        if (wifiLock == null) {
            wifiLock = wifiManager.createWifiLock(lockType, "PhotoDownloadService:WifiLock").apply {
                setReferenceCounted(false)
            }
        }
        if (wifiLock?.isHeld != true) {
            wifiLock?.acquire()
        }

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PhotoDownloadService:WakeLock").apply {
                setReferenceCounted(false)
            }
        }
        if (wakeLock?.isHeld != true) {
            wakeLock?.acquire(60 * 60 * 1000L /* 1 hour safety timeout */)
        }
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
        START, CONNECT, QUICK_DOWNLOAD, STOP
    }

    companion object {
        const val PHOTOS_LIST_EXTRA = "photos_list_extra"
        const val WIFI_DETAILS_EXTRA = "wifi_details_extra"
    }
}
