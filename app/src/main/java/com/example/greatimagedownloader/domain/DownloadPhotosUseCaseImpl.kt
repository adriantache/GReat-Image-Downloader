package com.example.greatimagedownloader.domain

import com.example.greatimagedownloader.domain.data.Repository
import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.domain.data.model.PhotoFile
import com.example.greatimagedownloader.domain.model.Events
import com.example.greatimagedownloader.domain.model.Events.SuccessfulDownload
import com.example.greatimagedownloader.domain.model.FolderInfo
import com.example.greatimagedownloader.domain.model.States
import com.example.greatimagedownloader.domain.model.States.ChangeSettings
import com.example.greatimagedownloader.domain.model.States.ConnectWifi
import com.example.greatimagedownloader.domain.model.States.DownloadPhotos
import com.example.greatimagedownloader.domain.model.States.GetPhotos
import com.example.greatimagedownloader.domain.model.States.Init
import com.example.greatimagedownloader.domain.model.States.RequestPermissions
import com.example.greatimagedownloader.domain.model.States.RequestWifiCredentials
import com.example.greatimagedownloader.domain.model.WifiDetailsEntity
import com.example.greatimagedownloader.domain.utils.model.Event
import com.example.greatimagedownloader.domain.wifi.WifiUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

// TODO: add tests
class DownloadPhotosUseCaseImpl(
    private val repository: Repository,
    private val wifiUtil: WifiUtil,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DownloadPhotosUseCase {
    @Suppress("kotlin:S6305")
    @get:Synchronized
    override val state: MutableStateFlow<States> = MutableStateFlow(Init(onInit = ::onInit))

    @Suppress("kotlin:S6305")
    override val event: MutableStateFlow<Event<Events>?> = MutableStateFlow(null)

    private val scope = CoroutineScope(dispatcher)

    // Used to interrupt download without corrupting current file.
    private var continueDownload = true

    // Used to stop the wifi scan timeout.
    private var scanningTimeoutJob: Job? = null

    private fun onInit() {
        state.value = RequestPermissions(::onPermissionsGranted)
    }

    private fun onPermissionsGranted() {
        connectToWifi()
    }

    // TODO: check if wifi is enabled first and show error message which redirects to wifi settings
    private fun connectToWifi(
        isSoftTimeout: Boolean = false,
        isHardTimeout: Boolean = false,
    ) {
        val wifiDetails = repository.getWifiDetails().toEntity()

        if (!wifiDetails.isValid) {
            state.value = RequestWifiCredentials(
                onWifiCredentialsInput = { onWifiCredentialsInput(it.toEntity()) },
                onSuggestWifiName = { wifiUtil.suggestNetwork() }
            )

            return
        }

        state.value = ConnectWifi(
            isSoftTimeout = isSoftTimeout,
            isHardTimeout = isHardTimeout,
            onCheckWifiDisabled = { wifiUtil.isWifiDisabled },
            onConnect = { startWifiConnection(wifiDetails) },
            onChangeWifiDetails = {
                state.value = RequestWifiCredentials(
                    onWifiCredentialsInput = { onWifiCredentialsInput(it.toEntity()) },
                    onSuggestWifiName = { wifiUtil.suggestNetwork() }
                )
            },
            onSoftTimeoutRetry = {
                startWifiConnection(wifiDetails)

                // Update state to dismiss dialog.
                connectToWifi()
            },
            onAdjustSettings = ::openSettings,
        )
    }

    private fun startWifiConnection(wifiDetails: WifiDetailsEntity) {
        scope.launch {
            wifiUtil.connectToWifi(
                ssid = wifiDetails.ssid!!,
                password = wifiDetails.password!!,
                onScanning = { startWifiScanHintLoop() },
                onThrottled = { connectToWifi(isHardTimeout = true) },
                onConnected = {
                    onConnectionSuccess()
                    scanningTimeoutJob?.cancel()
                },
                onDisconnected = {
                    onConnectionLost()
                    scanningTimeoutJob?.cancel()
                }
            )
        }
    }

    private fun startWifiScanHintLoop() {
        scanningTimeoutJob?.cancel()

        scanningTimeoutJob = scope.launch {
            delay(20.seconds)

            connectToWifi(isSoftTimeout = true)
        }
    }

    // TODO: move this to settings use case
    private fun openSettings() {
        // TODO: add functionalities and data to this use case
        // TODO: add change wifi
        // TODO: add remember last download
        // TODO: add delete all photos
        // TODO: add option to skip videos
        // TODO: add option to download all folders with warning for timelapses
        scope.launch {
            state.value = ChangeSettings(
                settings = repository.getSettings(),
                onRememberLastDownloadedPhotos = {
                    scope.launch {
                        val settings = repository.getSettings()
                        val newSettings = settings.copy(
                            rememberLastDownloadedPhotos = settings.rememberLastDownloadedPhotos?.not() ?: true
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

        repository.saveWifiDetails(details.toData())

        connectToWifi()
    }

    private fun onConnectionSuccess() {
        state.value = GetPhotos

        getMedia()
    }

    // TODO: handle directories
    // TODO: add logic for when we delete already downloaded images and opt-out mechanism
    // TODO: add option to interrupt process, deleting current file in progress
    private fun getMedia() {
        scope.launch {
            val availableMediaToDownload = getPhotosToDownload() ?: return@launch

            val shouldOnlyDownloadRecent = repository.getSettings().rememberLastDownloadedPhotos == true

            val mediaToDownload = if (shouldOnlyDownloadRecent) {
                getOnlyRecentPhotos(availableMediaToDownload)
            } else {
                availableMediaToDownload
            }

            val folderInfo = FolderInfo(mediaToDownload)

            if (folderInfo.hasMultipleFolders) {
                state.value = States.SelectFolders(
                    folderInfo = folderInfo,
                    onFoldersSelect = { selectedFolders ->
                        val selectedMediaToDownload = mediaToDownload.filter { it.directory in selectedFolders }
                        downloadMedia(selectedMediaToDownload)
                    }
                )
            } else {
                downloadMedia(mediaToDownload)
            }
        }
    }

    private fun downloadMedia(photosToDownload: List<PhotoFile>) {
        state.value = DownloadPhotos(totalPhotos = photosToDownload.size, onStopDownloading = ::onStopDownloading)

        val downloadedPhotoUris = mutableMapOf<String, PhotoDownloadInfo>()

        scope.launch {
            for (index in photosToDownload.indices) {
                val photo = photosToDownload[index]

                if (!continueDownload) {
                    continueDownload = true
                    break
                }

                repository.downloadMediaToStorage(photo).collect { photoDownloadInfo ->
                    downloadedPhotoUris[photoDownloadInfo.name] = photoDownloadInfo

                    state.value = DownloadPhotos(
                        currentPhotoNum = index + 1,
                        totalPhotos = photosToDownload.size,
                        downloadedPhotos = downloadedPhotoUris.values.toList(),
                        downloadSpeed = photoDownloadInfo.downloadSpeed,
                        onStopDownloading = ::onStopDownloading,
                        isStopping = !continueDownload,
                    )
                }
            }

            updateLatestDownloadedPhotos(photosToDownload, downloadedPhotoUris)

            disconnect(downloadedPhotoUris.count { it.value.downloadProgress == 100 })
        }
    }

    private suspend fun getPhotosToDownload(): List<PhotoFile>? {
        val savedMedia = (repository.getSavedPhotos() + repository.getSavedMovies()).distinct()
        val availablePhotos = repository.getCameraPhotoList()

        if (availablePhotos.isFailure) {
            event.value = Event(Events.CannotDownloadPhotos)
            state.value = Init(::onInit)
            return null
        }

        return availablePhotos.getOrNull()
            .orEmpty()
            .filter {
                val nameWithoutExtension = it.name.split(".")[0]
                !savedMedia.contains(nameWithoutExtension)
            }
    }

    private suspend fun disconnect(numDownloadedPhotos: Int) {
        repository.shutDownCamera()

        state.value = Init(::onInit)

        event.value = Event(
            SuccessfulDownload(numDownloadedPhotos = numDownloadedPhotos)
        )
    }

    private fun onConnectionLost() {
        when (val state = state.value) {
            is DownloadPhotos -> {
                val currentMedia = state.downloadedPhotos[state.currentPhotoNum - 1]
                val currentProgress = currentMedia.downloadProgress

                // Delete the current file if it's incomplete.
                if (currentProgress != 100) {
                    repository.deleteMedia(currentMedia.uri)
                }
            }

            is ChangeSettings,
            is ConnectWifi,
            GetPhotos,
            is Init,
            is RequestPermissions,
            is RequestWifiCredentials,
            is States.SelectFolders,
                -> Unit
        }

        state.value = Init(::onInit)
    }

    private suspend fun updateLatestDownloadedPhotos(
        photosToDownload: List<PhotoFile>,
        downloadedPhotoUris: MutableMap<String, PhotoDownloadInfo>,
    ) {
        // Make sure we don't count files that for whatever reason weren't downloaded.
        val completedFiles = photosToDownload.filter {
            downloadedPhotoUris[it.name]?.downloadProgress == 100
        }

        val lastFiles = completedFiles.groupBy { it.directory }
            .mapValues { entry -> entry.value.maxBy { it.name } }

        repository.saveLatestDownloadedPhotos(lastFiles.values.toList())
    }

    private suspend fun getOnlyRecentPhotos(availableMediaToDownload: List<PhotoFile>): List<PhotoFile> {
        val latestDownloadedPhotos = repository.getLatestDownloadedPhotos()

        if (latestDownloadedPhotos.isEmpty()) {
            return availableMediaToDownload
        }

        return availableMediaToDownload.filter { currentFile ->
            val latestPhoto = latestDownloadedPhotos.find { it.directory == currentFile.directory } ?: return@filter true

            latestPhoto.name < currentFile.name
        }
    }

    private fun onStopDownloading() {
        if (!continueDownload) return

        continueDownload = false
    }
}
