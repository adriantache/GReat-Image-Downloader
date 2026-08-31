package com.adriantache.greatimagedownloader.domain

import android.util.Log
import com.adriantache.greatimagedownloader.domain.data.Repository
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

// TODO: IMPORTANT delete partially downloaded files on error
// TODO: add tests
class DownloadPhotosUseCaseImpl(
    private val repository: Repository,
    private val wifiUtil: WifiUtil,
    private val dataTransferTool: DataTransferTool,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DownloadPhotosUseCase {
    @Suppress("kotlin:S6305")
    @get:Synchronized
    override val state: MutableStateFlow<States> = MutableStateFlow(Init(onInit = ::onInit))

    @Suppress("kotlin:S6305")
    override val event: MutableStateFlow<Event<Events>?> = MutableStateFlow(null)

    private val scope = CoroutineScope(dispatcher)

    private var photosToDownload: List<PhotoFile> = emptyList()
    private val downloadedPhotoUris = mutableMapOf<String, PhotoDownloadInfo>()

    init {
        scope.launch {
            dataTransferTool.imageFlow.collect { info ->
                info?.let {
                    downloadedPhotoUris[it.name] = it
                    onDownloadPhotoInfo(it)
                }
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
    }

    private fun onServiceStateChanged(serviceState: ServiceState) {
        when (serviceState) {
            ServiceState.CONNECTING -> {
                val wifiDetails = repository.getWifiDetails().getOrNull()?.toEntity() ?: WifiDetailsEntity()
                if (state.value !is ConnectWifi) {
                    state.value = ConnectWifi(
                        isHardTimeout = false,
                        onCheckWifiDisabled = { wifiUtil.isWifiDisabled },
                        onConnect = { startWifiConnection(wifiDetails) },
                        onChangeWifiDetails = {
                            state.value = RequestWifiCredentials(
                                onWifiCredentialsInput = { onWifiCredentialsInput(it.toEntity()) },
                                onSuggestWifiName = { wifiUtil.suggestNetwork() },
                                onDismiss = { connectToWifi() }
                            )
                        },
                        onAdjustSettings = ::openSettings,
                    )
                }
            }

            ServiceState.FETCHING -> {
                Log.d("DownloadPhotosUseCase", "onServiceStateChanged: Service is FETCHING, starting fetchPhotosFromCamera")
                state.value = GetPhotos
                fetchPhotosFromCamera()
            }
            ServiceState.DOWNLOADING -> Unit // Handled by imageFlow
            ServiceState.IDLE -> Unit
            ServiceState.STOPPING -> state.value = States.StoppingDownload
        }
    }

    private fun onError(error: DomainError) {
        event.value = Event(
            Events.ErrorDialog(
                error = error,
                onRetry = {
                    event.value = null

                    // For any error, we try to reconnect to the camera automatically on retry.
                    val wifiDetails = repository.getWifiDetails().getOrNull()?.toEntity() ?: WifiDetailsEntity()
                    if (wifiDetails.isValid) {
                        state.value = ConnectWifi(
                            isHardTimeout = false,
                            onCheckWifiDisabled = { wifiUtil.isWifiDisabled },
                            onConnect = { startWifiConnection(wifiDetails) },
                            onChangeWifiDetails = {
                                state.value = RequestWifiCredentials(
                                    onWifiCredentialsInput = { onWifiCredentialsInput(it.toEntity()) },
                                    onSuggestWifiName = { wifiUtil.suggestNetwork() },
                                    onDismiss = { connectToWifi() }
                                )
                            },
                            onAdjustSettings = ::openSettings,
                        )
                        startWifiConnection(wifiDetails)
                    } else {
                        connectToWifi()
                    }
                },
                onDismiss = {
                    event.value = null
                    scope.launch { repository.shutDownCamera() }
                    onDownloadFinished()
                }
            )
        )
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
            state.value = RequestWifiCredentials(
                onWifiCredentialsInput = { onWifiCredentialsInput(it.toEntity()) },
                onSuggestWifiName = { wifiUtil.suggestNetwork() },
                onDismiss = {},
            )

            return
        }

        state.value = ConnectWifi(
            isHardTimeout = isHardTimeout,
            onCheckWifiDisabled = { wifiUtil.isWifiDisabled },
            onConnect = { startWifiConnection(wifiDetails) },
            onChangeWifiDetails = {
                state.value = RequestWifiCredentials(
                    onWifiCredentialsInput = { onWifiCredentialsInput(it.toEntity()) },
                    onSuggestWifiName = { wifiUtil.suggestNetwork() },
                    onDismiss = { connectToWifi() }
                )
            },
            onAdjustSettings = ::openSettings,
        )
    }

    private fun startWifiConnection(wifiDetails: WifiDetailsEntity) {
        scope.launch {
            dataTransferTool.serviceStateFlow.value = ServiceState.CONNECTING

            var isConnected = false
            var newBssid: String? = null
            val maxRetries = 5

            for (attempt in 1..maxRetries) {
                // Notify Service/UI that we are attempting connection
                event.value = Event(
                    Events.ConnectWithService(
                        ssid = wifiDetails.ssid!!,
                        password = wifiDetails.password!!,
                        bssid = if (attempt == 1) wifiDetails.bssid else null, // Only try BSSID on first attempt
                    )
                )

                val (attemptIsConnected, attemptNewBssid) = wifiUtil.connectToWifi(
                    ssid = wifiDetails.ssid,
                    password = wifiDetails.password,
                    bssid = if (attempt == 1) wifiDetails.bssid else null,
                )

                if (attemptIsConnected) {
                    isConnected = true
                    newBssid = attemptNewBssid
                    break
                }

                if (attempt < maxRetries) delay(20.seconds)
            }

            if (isConnected) {
                // Save new BSSID
                repository.getWifiDetails().getOrNull()?.let { currentDetails ->
                    repository.saveWifiDetails(currentDetails.copy(bssid = newBssid))
                }

                dataTransferTool.serviceStateFlow.value = ServiceState.FETCHING
                // fetchPhotosFromCamera() will be called via serviceStateFlow collector
            } else {
                state.value = ConnectWifi(
                    isHardTimeout = true,
                    onCheckWifiDisabled = { wifiUtil.isWifiDisabled },
                    onConnect = { startWifiConnection(wifiDetails) },
                    onChangeWifiDetails = {
                        state.value = RequestWifiCredentials(
                            onWifiCredentialsInput = { onWifiCredentialsInput(it.toEntity()) },
                            onSuggestWifiName = { wifiUtil.suggestNetwork() },
                            onDismiss = { connectToWifi() }
                        )
                    },
                    onAdjustSettings = ::openSettings,
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
        scope.launch {
            Log.d("DownloadPhotosUseCase", "fetchPhotosFromCamera: Calling repository.getCameraPhotoList()")
            val result = repository.getCameraPhotoList()
            Log.d("DownloadPhotosUseCase", "fetchPhotosFromCamera: Result isSuccess: ${result.isSuccess}")
            result.fold(
                onSuccess = { photos ->
                    if (photos.isEmpty()) {
                        onDownloadFinished()
                        return@launch
                    }

                    val settings = repository.getSettings().getOrNull()
                    val shouldOnlyDownloadRecent = settings?.rememberLastDownloadedPhotos == true

                    val photosToProcess = if (shouldOnlyDownloadRecent) {
                        filterRecentPhotos(photos)
                    } else {
                        photos
                    }

                    if (photosToProcess.isEmpty()) {
                        onDownloadFinished()
                        return@launch
                    }

                    if (photosToProcess.groupBy { it.directory }.size > 1) {
                        state.value = States.SelectFolders(
                            folderInfo = FolderInfo(photosToProcess),
                            onFoldersSelect = { selectedFolders ->
                                downloadPhotos(photosToProcess.filter { it.directory in selectedFolders })
                            }
                        )
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

    private suspend fun filterRecentPhotos(availableMediaToDownload: List<PhotoFile>): List<PhotoFile> {
        val latestDownloadedPhotos = repository.getLatestDownloadedPhotos().getOrNull().orEmpty().groupBy { it.directory }
        val latestDownloadedDirectories = latestDownloadedPhotos.keys

        if (latestDownloadedPhotos.isEmpty()) {
            return availableMediaToDownload
        }

        return availableMediaToDownload.filter { currentFile ->
            currentFile.directory in latestDownloadedDirectories &&
                    currentFile.name !in latestDownloadedPhotos[currentFile.directory].orEmpty().map { it.name }
        }
    }

    private fun downloadPhotos(photos: List<PhotoFile>) {
        photosToDownload = photos
        downloadedPhotoUris.clear()

        event.value = Event(Events.DownloadPhotosWithService(photos))
    }

    private fun onDownloadPhotoInfo(info: PhotoDownloadInfo) {
        val currentState = state.value
        if ((currentState is DownloadPhotos) || (currentState is GetPhotos) || (currentState is States.SelectFolders)) {
            state.value = DownloadPhotos(
                currentPhotoNum = downloadedPhotoUris.size,
                totalPhotos = photosToDownload.size,
                downloadedPhotos = downloadedPhotoUris.values.toList(),
                downloadSpeed = info.downloadSpeed,
                onStopDownloading = ::onStopDownloading,
            )
        }
    }

    private fun onDownloadFinished() {
        val wasStopping = state.value is States.StoppingDownload

        updateLatestDownloadedPhotos(photosToDownload, downloadedPhotoUris)

        connectToWifi()

        if (!wasStopping) {
            event.value = Event(
                SuccessfulDownload(numDownloadedPhotos = downloadedPhotoUris.size)
            )
        }

        dataTransferTool.reset()
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

        scope.launch {
            repository.saveLatestDownloadedPhotos(lastFiles.values.toList())
        }
    }

    private fun onStopDownloading() {
        state.value = States.StoppingDownload
        event.value = Event(Events.StopDownload)
    }
}
