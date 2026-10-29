package com.adriantache.greatimagedownloader.domain

import android.util.Log
import com.adriantache.greatimagedownloader.domain.ble.BleUtil
import com.adriantache.greatimagedownloader.domain.ble.ConnectionStage
import com.adriantache.greatimagedownloader.domain.data.Repository
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.model.CameraStatus
import com.adriantache.greatimagedownloader.domain.model.DomainError
import com.adriantache.greatimagedownloader.domain.model.DomainException
import com.adriantache.greatimagedownloader.domain.model.Events
import com.adriantache.greatimagedownloader.domain.model.Events.SuccessfulDownload
import com.adriantache.greatimagedownloader.domain.model.FolderInfo
import com.adriantache.greatimagedownloader.domain.model.States
import com.adriantache.greatimagedownloader.domain.model.States.ChangeSettings
import com.adriantache.greatimagedownloader.domain.model.States.ConnectWifi
import com.adriantache.greatimagedownloader.domain.model.States.DownloadPhotos
import com.adriantache.greatimagedownloader.domain.model.States.GetPhotos
import com.adriantache.greatimagedownloader.domain.model.States.Init
import com.adriantache.greatimagedownloader.domain.model.States.RequestPermissions
import com.adriantache.greatimagedownloader.domain.model.States.RequestWifiCredentials
import com.adriantache.greatimagedownloader.domain.model.WifiDetailsEntity
import com.adriantache.greatimagedownloader.domain.utils.model.Event
import com.adriantache.greatimagedownloader.domain.wifi.WifiUtil
import com.adriantache.greatimagedownloader.service.DataTransferTool
import com.adriantache.greatimagedownloader.service.DataTransferTool.ServiceState
import com.adriantache.greatimagedownloader.service.DataTransferTool.WidgetState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

// TODO: add tests
class DownloadPhotosUseCaseImpl(
    private val repository: Repository,
    private val wifiUtil: WifiUtil,
    private val dataTransferTool: DataTransferTool,
    private val bleUtil: BleUtil? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DownloadPhotosUseCase {
    @Suppress("kotlin:S6305")
    @get:Synchronized
    override val state: MutableStateFlow<States> = MutableStateFlow(Init(onInit = ::onInit))

    @Suppress("kotlin:S6305")
    override val event: MutableStateFlow<Event<Events>?> = MutableStateFlow(null)

    private val scope = CoroutineScope(dispatcher)

    private var photosToDownload: List<PhotoFile> = emptyList()
    private val downloadedPhotoUris = LinkedHashMap<String, PhotoDownloadInfo>()
    private var connectionJob: Job? = null
    private var fetchJob: Job? = null
    private var keepAliveJob: Job? = null
    private var currentCameraStatus: CameraStatus? = null
    private var autoConnectRequested = false

    @Volatile
    private var isBackgroundSync = false

    @Volatile
    private var isCancelling = false

    init {
        scope.launch {
            dataTransferTool.imageUpdatesFlow.collect { info ->
                handleImageProgress(info)
            }
        }

        scope.launch {
            dataTransferTool.imageFlow.collect { info ->
                info?.let { handleImageProgress(it) }
            }
        }

        scope.launch {
            dataTransferTool.downloadFinishedFlow.collect { eventValue ->
                if (eventValue?.value != null) {
                    onDownloadFinished()
                }
            }
        }

        scope.launch {
            dataTransferTool.errorFlow.collect { eventValue ->
                val error = eventValue?.value ?: return@collect
                onError(error)
            }
        }

        scope.launch {
            dataTransferTool.serviceStateFlow.collect { serviceState ->
                onServiceStateChanged(serviceState)
            }
        }

        scope.launch {
            dataTransferTool.cameraStatusFlow.collect { status ->
                if (status != null) {
                    currentCameraStatus = status
                    val currentState = state.value
                    if (currentState is DownloadPhotos) {
                        state.value = currentState.copy(cameraStatus = status)
                    } else if (currentState is States.SelectFolders) {
                        state.value = currentState.copy(cameraStatus = status)
                    }
                }
            }
        }
    }

    @Synchronized
    private fun handleImageProgress(info: PhotoDownloadInfo) {
        val existing = downloadedPhotoUris[info.name]
        if (existing == info) return
        if (existing?.downloadProgress == 100 && info.downloadProgress < 100) return

        downloadedPhotoUris[info.name] = info
        onDownloadPhotoInfo(info)
    }

    private fun onServiceStateChanged(serviceState: ServiceState) {
        when (serviceState) {
            ServiceState.CONNECTING -> {
                val currentStage = dataTransferTool.connectionStageFlow.value ?: ConnectionStage.CONNECTING_BLE
                val stageText = when (currentStage) {
                    ConnectionStage.CONNECTING_BLE -> "BT…"
                    ConnectionStage.CAMERA_CONNECTED -> "Wake…"
                    ConnectionStage.STARTING_WIFI -> "Wi-Fi…"
                    ConnectionStage.JOINING_WIFI -> "Join…"
                }
                dataTransferTool.widgetStateFlow.value = WidgetState.Connecting(stageText)
                val wifiDetails = repository.getWifiDetails().getOrNull()?.toEntity() ?: WifiDetailsEntity()
                if (state.value !is ConnectWifi) {
                    state.value = buildConnectWifiState(
                        wifiDetails = wifiDetails,
                        isHardTimeout = false,
                        isConnecting = true,
                        connectionStage = currentStage,
                    )
                }
            }

            ServiceState.FETCHING -> {
                Log.d("DownloadPhotosUseCase", "onServiceStateChanged: Service is FETCHING, starting fetchPhotosFromCamera")
                dataTransferTool.widgetStateFlow.value = WidgetState.Checking
                state.value = GetPhotos
                fetchPhotosFromCamera()
            }

            ServiceState.DOWNLOADING -> Unit // Handled by imageFlow
            ServiceState.IDLE -> {
                if (state.value is States.StoppingDownload) {
                    onDownloadFinished()
                }
            }
            ServiceState.STOPPING -> {
                dataTransferTool.widgetStateFlow.value = WidgetState.Stopping
                state.value = States.StoppingDownload
            }
        }
    }

    private fun onError(error: DomainError) {
        connectionJob?.cancel()
        connectionJob = null
        fetchJob?.cancel()
        fetchJob = null
        stopKeepAlive()
        isBackgroundSync = false
        bleUtil?.disconnect()
        dataTransferTool.widgetStateFlow.value = WidgetState.Error
        dataTransferTool.serviceStateFlow.value = ServiceState.IDLE

        // Delete partially downloaded files on error
        downloadedPhotoUris.values
            .filter { it.downloadProgress < 100 }
            .forEach { info -> repository.deleteMedia(info.uri) }

        event.value = Event(
            Events.ErrorDialog(
                error = error,
                onRetry = {
                    event.value = null

                    // For any error, we try to reconnect to the camera automatically on retry.
                    val wifiDetails = repository.getWifiDetails().getOrNull()?.toEntity() ?: WifiDetailsEntity()
                    if (wifiDetails.isValid) {
                        state.value = buildConnectWifiState(
                            wifiDetails = wifiDetails,
                            isHardTimeout = false,
                            isConnecting = true,
                        )
                        startWifiConnection(wifiDetails)
                    } else {
                        connectToWifi()
                    }
                },
                onDismiss = {
                    event.value = null
                    photosToDownload = emptyList()
                    downloadedPhotoUris.clear()
                    scope.launch {
                        repository.shutDownCamera()
                        bleUtil?.disconnect()
                        wifiUtil.cleanup()
                        dataTransferTool.widgetStateFlow.value = WidgetState.Idle
                        dataTransferTool.reset()
                        connectToWifi()
                    }
                }
            )
        )
    }

    override fun triggerAutoConnect() {
        val currentState = state.value
        if (currentState is ConnectWifi) {
            autoConnectRequested = false
            state.value = currentState.copy(autoStartConnection = true, isConnecting = true)
            currentState.onConnect()
        } else {
            autoConnectRequested = true
        }
    }

    override fun startBackgroundSync() {
        val wifiDetails = repository.getWifiDetails().getOrNull()?.toEntity() ?: WifiDetailsEntity()
        if (!wifiDetails.isValid || wifiUtil.isWifiDisabled) {
            dataTransferTool.widgetStateFlow.value = WidgetState.Error
            dataTransferTool.serviceStateFlow.value = ServiceState.IDLE
            return
        }

        isBackgroundSync = true
        startWifiConnection(wifiDetails)
    }

    override fun cancelConnection() {
        if (isCancelling) return
        val hasActiveWork = connectionJob?.isActive == true ||
            fetchJob?.isActive == true ||
            keepAliveJob?.isActive == true ||
            state.value is GetPhotos ||
            state.value is States.SelectFolders ||
            (state.value as? ConnectWifi)?.isConnecting == true ||
            dataTransferTool.serviceStateFlow.value != ServiceState.IDLE
        if (!hasActiveWork) return

        isCancelling = true
        val shouldShutDownCamera = state.value is GetPhotos || state.value is States.SelectFolders
        connectionJob?.cancel()
        connectionJob = null
        fetchJob?.cancel()
        fetchJob = null
        stopKeepAlive()
        isBackgroundSync = false
        photosToDownload = emptyList()
        downloadedPhotoUris.clear()

        connectToWifi()

        scope.launch {
            try {
                if (shouldShutDownCamera) {
                    repository.shutDownCamera()
                }
                bleUtil?.disconnect()
                wifiUtil.cleanup()
                dataTransferTool.widgetStateFlow.value = WidgetState.Idle
                dataTransferTool.reset()
            } finally {
                isCancelling = false
            }
        }
    }

    private fun onInit() {
        state.value = RequestPermissions(::onPermissionsGranted)
    }

    private fun onPermissionsGranted() {
        connectToWifi()
    }

    // TODO: check if wifi is enabled first and show error message which redirects to wifi settings
    private fun connectToWifi(isHardTimeout: Boolean = false) {
        val wifiDetails = repository.getWifiDetails().getOrNull()?.toEntity() ?: WifiDetailsEntity()

        if (!wifiDetails.isValid) {
            connectionJob?.cancel()
            connectionJob = null
            state.value = RequestWifiCredentials(
                onWifiCredentialsInput = { onWifiCredentialsInput(it.toEntity()) },
                onSuggestWifiName = { wifiUtil.suggestNetwork() },
                onSuggestWifiCredentials = { bleUtil?.wakeAndEnableCameraWifi() },
                onDismiss = {},
            )

            return
        }

        val shouldAutoStart = autoConnectRequested && !isHardTimeout && !wifiUtil.isWifiDisabled
        autoConnectRequested = false

        state.value = buildConnectWifiState(
            wifiDetails = wifiDetails,
            isHardTimeout = isHardTimeout,
            autoStartConnection = shouldAutoStart,
            isConnecting = shouldAutoStart,
        )

        if (shouldAutoStart) {
            startWifiConnection(wifiDetails)
        }
    }

    private fun buildConnectWifiState(
        wifiDetails: WifiDetailsEntity,
        isHardTimeout: Boolean,
        autoStartConnection: Boolean = false,
        isConnecting: Boolean = false,
        connectionStage: ConnectionStage? = null,
        connectionAttempt: Int = 1,
    ): ConnectWifi {
        return ConnectWifi(
            isHardTimeout = isHardTimeout,
            onCheckWifiDisabled = { wifiUtil.isWifiDisabled },
            onConnect = { startWifiConnection(wifiDetails) },
            onChangeWifiDetails = {
                connectionJob?.cancel()
                connectionJob = null
                state.value = RequestWifiCredentials(
                    onWifiCredentialsInput = { onWifiCredentialsInput(it.toEntity()) },
                    onSuggestWifiName = { wifiUtil.suggestNetwork() },
                    onSuggestWifiCredentials = { bleUtil?.wakeAndEnableCameraWifi() },
                    onDismiss = { connectToWifi() }
                )
            },
            onAdjustSettings = ::openSettings,
            autoStartConnection = autoStartConnection,
            isConnecting = isConnecting,
            connectionStage = connectionStage,
            connectionAttempt = connectionAttempt,
            onCancelConnection = ::cancelConnection,
        )
    }

    private fun startWifiConnection(wifiDetails: WifiDetailsEntity) {
        connectionJob?.cancel()
        connectionJob = scope.launch {
            state.value = buildConnectWifiState(
                wifiDetails = wifiDetails,
                isHardTimeout = false,
                isConnecting = true,
                connectionStage = ConnectionStage.CONNECTING_BLE,
            )
            dataTransferTool.connectionStageFlow.value = ConnectionStage.CONNECTING_BLE
            dataTransferTool.connectionAttemptFlow.value = 1
            dataTransferTool.widgetStateFlow.value = WidgetState.Connecting("BT…")
            dataTransferTool.serviceStateFlow.value = ServiceState.CONNECTING

            var isConnected = false
            var newBssid: String? = null
            val maxRetries = 5

            // Attempt to wake the camera and enable its Wi-Fi Access Point via BLE if paired
            val bleWifiDetails = bleUtil?.wakeAndEnableCameraWifi { stage ->
                state.value = buildConnectWifiState(
                    wifiDetails = wifiDetails,
                    isHardTimeout = false,
                    isConnecting = true,
                    connectionStage = stage,
                )
                dataTransferTool.connectionStageFlow.value = stage
                val stageText = when (stage) {
                    ConnectionStage.CONNECTING_BLE -> "BT…"
                    ConnectionStage.CAMERA_CONNECTED -> "Wake…"
                    ConnectionStage.STARTING_WIFI -> "Wi-Fi…"
                    ConnectionStage.JOINING_WIFI -> "Join…"
                }
                dataTransferTool.widgetStateFlow.value = WidgetState.Connecting(stageText)
            }
            if (!isActive) return@launch

            val targetSsid = bleWifiDetails?.ssid?.takeIf { it.isNotBlank() } ?: wifiDetails.ssid!!
            val targetPassword = bleWifiDetails?.password?.takeIf { it.isNotBlank() } ?: wifiDetails.password!!

            for (attempt in 1..maxRetries) {
                if (!isActive) return@launch

                state.value = buildConnectWifiState(
                    wifiDetails = wifiDetails,
                    isHardTimeout = false,
                    isConnecting = true,
                    connectionStage = ConnectionStage.JOINING_WIFI,
                    connectionAttempt = attempt,
                )
                dataTransferTool.connectionStageFlow.value = ConnectionStage.JOINING_WIFI
                dataTransferTool.connectionAttemptFlow.value = attempt
                val stageText = if (attempt > 1) "Join $attempt…" else "Join…"
                dataTransferTool.widgetStateFlow.value = WidgetState.Connecting(stageText)

                // Notify Service/UI that we are attempting connection
                event.value = Event(
                    Events.ConnectWithService(
                        ssid = targetSsid,
                        password = targetPassword,
                        bssid = if (attempt == 1) wifiDetails.bssid else null, // Only try BSSID on first attempt
                    )
                )

                val (attemptIsConnected, attemptNewBssid) = wifiUtil.connectToWifi(
                    ssid = targetSsid,
                    password = targetPassword,
                    bssid = if (attempt == 1) wifiDetails.bssid else null,
                )

                if (attemptIsConnected) {
                    isConnected = true
                    newBssid = attemptNewBssid
                    break
                }

                if (attempt < maxRetries) delay(3.seconds)
            }

            if (!isActive) return@launch

            if (isConnected) {
                // Save new BSSID
                repository.getWifiDetails().getOrNull()?.let { currentDetails ->
                    repository.saveWifiDetails(
                        currentDetails.copy(
                            ssid = targetSsid,
                            password = targetPassword,
                            bssid = newBssid,
                        )
                    )
                }

                dataTransferTool.widgetStateFlow.value = WidgetState.Checking
                dataTransferTool.serviceStateFlow.value = ServiceState.FETCHING
                // fetchPhotosFromCamera() will be called via serviceStateFlow collector
            } else {
                isBackgroundSync = false
                bleUtil?.disconnect()
                wifiUtil.cleanup()
                dataTransferTool.widgetStateFlow.value = WidgetState.Error
                dataTransferTool.serviceStateFlow.value = ServiceState.IDLE

                state.value = buildConnectWifiState(
                    wifiDetails = wifiDetails,
                    isHardTimeout = true,
                    isConnecting = false,
                )
            }
        }
    }

    // TODO: move this to settings use case
    private fun openSettings() {
        // TODO: add functionalities and data to this use case
        // TODO: add change Wi-Fi
        // TODO: add remember last download
        // TODO: add delete all photos
        // TODO: add option to skip videos
        // TODO: add option to download all folders with warning for timelapses
        scope.launch {
            val settings = repository.getSettings().getOrNull() ?: return@launch

            state.value = ChangeSettings(
                settings = settings,
                onRememberLastDownloadedPhotos = {
                    scope.launch {
                        val currentSettings = repository.getSettings().getOrNull() ?: return@launch
                        val newSettings = currentSettings.copy(
                            rememberLastDownloadedPhotos = currentSettings.rememberLastDownloadedPhotos?.not() ?: true
                        )

                        repository.saveSettings(newSettings)
                        openSettings()
                    }
                },
                onToggleDownloadRawFiles = {
                    scope.launch {
                        val currentSettings = repository.getSettings().getOrNull() ?: return@launch
                        repository.saveSettings(currentSettings.copy(downloadRawFiles = !currentSettings.downloadRawFiles))
                        openSettings()
                    }
                },
                onToggleDownloadVideos = {
                    scope.launch {
                        val currentSettings = repository.getSettings().getOrNull() ?: return@launch
                        repository.saveSettings(currentSettings.copy(downloadVideos = !currentSettings.downloadVideos))
                        openSettings()
                    }
                },
                onToggleFastPreviewDownload = {
                    scope.launch {
                        val currentSettings = repository.getSettings().getOrNull() ?: return@launch
                        repository.saveSettings(currentSettings.copy(fastPreviewDownload = !currentSettings.fastPreviewDownload))
                        openSettings()
                    }
                },
                onDeleteAllPhotos = ::deleteAllPhotos,
                onExitSettings = ::connectToWifi,
            )
        }
    }

    // TODO: ensure latest photo is saved by checking setting is enabled and there is data, otherwise return different event
    private fun deleteAllPhotos() {
        val confirmDeletionEvent = Events.ConfirmDeleteAllPhotos(
            onConfirm = {
                scope.launch {
                    repository.deleteAll()

                    connectToWifi()
                }
            },
            onDismiss = { event.value = null },
        )
        event.value = Event(confirmDeletionEvent)
    }

    private fun onWifiCredentialsInput(details: WifiDetailsEntity) {
        if (!details.isValid) {
            event.value = Event(Events.InvalidWifiInput)
            return
        }

        // When the user manually inputs new credentials, the old BSSID is cleared.
        repository.saveWifiDetails(details.toData())

        startWifiConnection(details)
    }

    private fun fetchPhotosFromCamera() {
        fetchJob?.cancel()
        fetchJob = scope.launch {
            currentCameraStatus = repository.getCameraStatus().getOrNull()
            dataTransferTool.cameraStatusFlow.value = currentCameraStatus

            // If Android redacted the Wi-Fi BSSID, fall back to the MAC address reported by the camera API
            val cameraMac = currentCameraStatus?.macAddress
            if (!cameraMac.isNullOrBlank()) {
                repository.getWifiDetails().getOrNull()?.let { currentDetails ->
                    if (currentDetails.bssid.isNullOrBlank()) {
                        repository.saveWifiDetails(currentDetails.copy(bssid = cameraMac))
                    }
                }
            }

            Log.d("DownloadPhotosUseCase", "fetchPhotosFromCamera: Calling repository.getCameraPhotoList()")
            val result = repository.getCameraPhotoList()
            if (!isActive) return@launch
            Log.d("DownloadPhotosUseCase", "fetchPhotosFromCamera: Result isSuccess: ${result.isSuccess}")
            result.fold(
                onSuccess = { photos ->
                    if (photos.isEmpty()) {
                        onNoNewPhotosToDownload()
                        return@launch
                    }

                    val savedPhotos = repository.getSavedPhotos().getOrNull().orEmpty()
                    val savedMovies = repository.getSavedMovies().getOrNull().orEmpty()
                    val savedMedia = (savedPhotos.map { it.name } + savedMovies).distinct()
                    val savedFullNames = savedMedia.filter { it.contains(".") }.map { it.lowercase() }.toSet()
                    val savedBaseNames = savedMedia.filter { !it.contains(".") }.map { it.lowercase() }.toSet()

                    val newPhotos = photos.filter {
                        val lowerName = it.name.lowercase()
                        val baseName = lowerName.substringBefore(".")
                        lowerName !in savedFullNames && baseName !in savedBaseNames
                    }

                    val settings = repository.getSettings().getOrNull()
                    val includeRaw = settings?.downloadRawFiles == true
                    val includeVideos = settings?.downloadVideos != false

                    val filteredByType = newPhotos.filter { photo ->
                        (!photo.isRawFile || includeRaw) && (!photo.isVideoFile || includeVideos)
                    }

                    val shouldOnlyDownloadRecent = settings?.rememberLastDownloadedPhotos == true

                    val photosToProcess = if (shouldOnlyDownloadRecent) {
                        filterRecentPhotos(filteredByType)
                    } else {
                        filteredByType
                    }

                    if (photosToProcess.isEmpty()) {
                        onNoNewPhotosToDownload()
                        return@launch
                    }

                    if (!isBackgroundSync && photosToProcess.groupBy { it.directory }.size > 1) {
                        state.value = States.SelectFolders(
                            folderInfo = FolderInfo(photosToProcess),
                            cameraStatus = currentCameraStatus,
                            onFoldersSelect = { selectedFolders ->
                                stopKeepAlive()
                                if (selectedFolders.isEmpty()) {
                                    scope.launch {
                                        repository.shutDownCamera()
                                        bleUtil?.disconnect()
                                        wifiUtil.cleanup()
                                        dataTransferTool.widgetStateFlow.value = WidgetState.Idle
                                        dataTransferTool.reset()
                                        connectToWifi()
                                    }
                                } else {
                                    downloadPhotos(photosToProcess.filter { it.directory in selectedFolders })
                                }
                            }
                        )
                        startKeepAlive()
                    } else {
                        downloadPhotos(photosToProcess)
                    }
                },
                onFailure = { throwable ->
                    val error = (throwable as? DomainException)?.domainError ?: DomainError.Unknown(throwable.message)
                    onError(error)
                }
            )
        }
    }

    private suspend fun onNoNewPhotosToDownload() {
        stopKeepAlive()
        isBackgroundSync = false
        photosToDownload = emptyList()
        downloadedPhotoUris.clear()

        // Shut down the camera while Wi-Fi is still bound to the camera network
        repository.shutDownCamera()
        bleUtil?.disconnect()
        wifiUtil.cleanup()

        dataTransferTool.widgetStateFlow.value = WidgetState.Done(0)
        dataTransferTool.reset()

        connectToWifi()
        event.value = Event(Events.NoNewPhotosToDownload)
    }

    private suspend fun filterRecentPhotos(availableMediaToDownload: List<PhotoFile>): List<PhotoFile> {
        val latestDownloadedPhotos = repository.getLatestDownloadedPhotos().getOrNull().orEmpty()

        if (latestDownloadedPhotos.isEmpty()) {
            return availableMediaToDownload
        }

        return availableMediaToDownload.filter { currentFile ->
            val latestInDir = latestDownloadedPhotos.find { it.directory == currentFile.directory }
            if (latestInDir == null) {
                true
            } else {
                currentFile.name > latestInDir.name
            }
        }
    }

    private fun downloadPhotos(photos: List<PhotoFile>) {
        photosToDownload = photos
        downloadedPhotoUris.clear()
        dataTransferTool.pendingPhotosToDownload = photos
        dataTransferTool.widgetStateFlow.value = WidgetState.Downloading(0, photos.size)
        dataTransferTool.serviceStateFlow.value = ServiceState.DOWNLOADING

        event.value = Event(Events.DownloadPhotosWithService(photos))
    }

    private fun onDownloadPhotoInfo(info: PhotoDownloadInfo) {
        if (photosToDownload.isNotEmpty()) {
            val completedCount = downloadedPhotoUris.values.count { it.downloadProgress == 100 }
            dataTransferTool.widgetStateFlow.value = WidgetState.Downloading(
                current = completedCount.coerceAtLeast(1).coerceAtMost(photosToDownload.size),
                total = photosToDownload.size,
            )
        }

        val currentState = state.value
        if ((currentState is DownloadPhotos) || (currentState is GetPhotos) || (currentState is States.SelectFolders)) {
            state.value = DownloadPhotos(
                currentPhotoNum = downloadedPhotoUris.size,
                totalPhotos = photosToDownload.size,
                downloadedPhotos = downloadedPhotoUris.values.toList(),
                downloadSpeed = info.downloadSpeed,
                cameraStatus = currentCameraStatus,
                onStopDownloading = ::onStopDownloading,
            )
        }
    }

    private fun onDownloadFinished() {
        stopKeepAlive()
        isBackgroundSync = false
        val wasStopping = state.value is States.StoppingDownload
        val numDownloaded = downloadedPhotoUris.values.count { it.downloadProgress == 100 }
            .takeIf { it > 0 } ?: downloadedPhotoUris.size
        val photosSnapshot = photosToDownload
        val urisSnapshot = downloadedPhotoUris.toMap()

        photosToDownload = emptyList()
        downloadedPhotoUris.clear()
        dataTransferTool.widgetStateFlow.value = if (wasStopping) {
            WidgetState.Idle
        } else {
            WidgetState.Done(numDownloaded)
        }
        dataTransferTool.reset()
        bleUtil?.disconnect()
        wifiUtil.cleanup()

        updateLatestDownloadedPhotos(photosSnapshot, urisSnapshot)

        connectToWifi()

        if (!wasStopping) {
            event.value = Event(
                SuccessfulDownload(numDownloadedPhotos = numDownloaded)
            )
        }
    }

    private fun updateLatestDownloadedPhotos(
        photosToDownload: List<PhotoFile>,
        downloadedPhotoUris: Map<String, PhotoDownloadInfo>,
    ) {
        // Make sure we don't count files that for whatever reason weren't downloaded.
        val completedFiles = photosToDownload.filter {
            downloadedPhotoUris[it.name]?.downloadProgress == 100
        }

        val lastFiles = completedFiles.groupBy { it.directory }
            .mapValues { entry -> entry.value.maxBy { it.name } }

        if (lastFiles.isEmpty()) return

        scope.launch {
            val existingByDir = repository.getLatestDownloadedPhotos().getOrNull().orEmpty()
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
    }

    private fun onStopDownloading() {
        stopKeepAlive()
        isBackgroundSync = false
        dataTransferTool.widgetStateFlow.value = WidgetState.Stopping
        state.value = States.StoppingDownload
        event.value = Event(Events.StopDownload)
    }

    private fun startKeepAlive() {
        stopKeepAlive()
        keepAliveJob = scope.launch {
            repeat(20) {
                delay(15.seconds)
                if (!isActive || state.value !is States.SelectFolders) return@launch
                repository.pingCamera()
            }
        }
    }

    private fun stopKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = null
    }
}
