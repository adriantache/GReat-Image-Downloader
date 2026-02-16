package com.adriantache.greatimagedownloader.domain

import com.adriantache.greatimagedownloader.domain.data.Repository
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// TODO: IMPORTANT delete partially downloaded files on error
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

    private fun onInit() {
        state.value = RequestPermissions(::onPermissionsGranted)
    }

    private fun onPermissionsGranted() {
        connectToWifi()
    }

    // TODO: check if wifi is enabled first and show error message which redirects to wifi settings
    private fun connectToWifi(isHardTimeout: Boolean = false) {
        val wifiDetails = repository.getWifiDetails().toEntity()

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
            onConnect = { startWifiConnection(wifiDetails, isHardTimeout) },
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

    private fun startWifiConnection(wifiDetails: WifiDetailsEntity, isHardTimeout: Boolean) {
        scope.launch {
            var isConnected = false
            var newBssid: String? = null
            val maxRetries = 5

            for (attempt in 1..maxRetries) {
                val (attemptIsConnected, attemptNewBssid) = wifiUtil.connectToWifi(
                    ssid = wifiDetails.ssid!!,
                    password = wifiDetails.password!!,
                    bssid = wifiDetails.bssid,
                )

                if (attemptIsConnected) {
                    isConnected = true
                    newBssid = attemptNewBssid
                    break // Exit the loop on success
                }

                // Don't delay after the final attempt
                if (attempt < maxRetries) delay(5000L)
            }

            if (isConnected) {
                // Save the new BSSID for the next connection attempt.
                val newDetails = wifiDetails.copy(bssid = newBssid).toData()
                repository.saveWifiDetails(newDetails)
                onConnectionSuccess()
            } else {
                // If all attempts failed, we're in a hard timeout.
                onConnectionLost()
                if (!isHardTimeout) {
                    connectToWifi(isHardTimeout = true)
                }
            }
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

        // When the user manually inputs new credentials, the old BSSID is cleared.
        repository.saveWifiDetails(details.toData())

        connectToWifi()
    }

    private fun onConnectionSuccess() {
        if (state.value !is ConnectWifi) return

        state.value = GetPhotos

        getMedia()
    }

    // TODO: add logic for when we delete already downloaded images and opt-out mechanism
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
                        downloadMediaWithService(selectedMediaToDownload)
                    }
                )
            } else {
                downloadMediaWithService(mediaToDownload)
            }
        }
    }

    private fun downloadMediaWithService(photosToDownload: List<PhotoFile>) {
        val totalPhotos = photosToDownload.size
        val downloadedPhotoUris = mutableMapOf<String, PhotoDownloadInfo>()

        event.value = Event(
            Events.DownloadPhotosWithService(
                photosToDownload = photosToDownload,
                onDownloadInfo = {
                    downloadedPhotoUris[it.name] = it
                    onDownloadPhotoInfo(it, downloadedPhotoUris, totalPhotos)
                },
                onDownloadFinished = {
                    updateLatestDownloadedPhotos(photosToDownload, downloadedPhotoUris)

                    state.value = Init(::onInit)

                    event.value = Event(
                        SuccessfulDownload(numDownloadedPhotos = downloadedPhotoUris.size)
                    )
                }
            )
        )
    }

    private fun onDownloadPhotoInfo(
        info: PhotoDownloadInfo,
        downloadedPhotoUris: MutableMap<String, PhotoDownloadInfo>,
        totalPhotos: Int,
    ) {
        state.value = DownloadPhotos(
            currentPhotoNum = downloadedPhotoUris.size,
            totalPhotos = totalPhotos,
            downloadedPhotos = downloadedPhotoUris.values.toList(),
            downloadSpeed = info.downloadSpeed,
            onStopDownloading = ::onStopDownloading,
        )
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
            States.StoppingDownload,
                -> Unit
        }

        state.value = Init(::onInit)
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

    private suspend fun getOnlyRecentPhotos(availableMediaToDownload: List<PhotoFile>): List<PhotoFile> {
        val latestDownloadedPhotos = repository.getLatestDownloadedPhotos().groupBy { it.directory }
        val latestDownloadedDirectories = latestDownloadedPhotos.keys

        if (latestDownloadedPhotos.isEmpty()) {
            return availableMediaToDownload
        }

        return availableMediaToDownload.filter { currentFile ->
            currentFile.directory in latestDownloadedDirectories &&
                    currentFile.name !in latestDownloadedPhotos[currentFile.directory].orEmpty().map { it.name }
        }
    }

    private fun onStopDownloading() {
        state.value = States.StoppingDownload
        event.value = Event(Events.StopDownload)
    }
}
