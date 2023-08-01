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
import com.example.greatimagedownloader.domain.utils.model.delay
import com.example.greatimagedownloader.domain.wifi.WifiUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val CONNECT_TIMEOUT_MS = 30_000

// TODO: add tests
class DownloadPhotosUseCaseImpl(
    private val repository: Repository,
    private val wifiUtil: WifiUtil,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DownloadPhotosUseCase {
    @Suppress("kotlin:S6305")
    override val state: MutableStateFlow<States> = MutableStateFlow(Init(onInit = ::onInit))

    @Suppress("kotlin:S6305")
    override val event: MutableStateFlow<Event<Events>?> = MutableStateFlow(null)

    private fun onInit() {
        state.value = RequestPermissions(::onPermissionsGranted)
    }

    private fun onPermissionsGranted() {
        connectToWifi()
    }

    // TODO: check if wifi is enabled first and show error message which redirects to wifi settings
    private fun connectToWifi() {
        val wifiDetails = repository.getWifiDetails().toEntity()

        state.value = if (wifiDetails.isValid) {
            ConnectWifi(
                onCheckWifiDisabled = { wifiUtil.isWifiDisabled },
                onConnect = {
                    CoroutineScope(dispatcher).launch {
                        delay(CONNECT_TIMEOUT_MS)

                        if (state.value is ConnectWifi) {
                            wifiUtil.disconnectFromWifi()
                            state.value = Init(::onInit)
                        }
                    }

                    wifiUtil.connectToWifi(
                        wifiDetails = wifiDetails,
                        connectTimeoutMs = CONNECT_TIMEOUT_MS,
                        onWifiConnected = ::onConnectionSuccess,
                        onWifiDisconnected = ::onConnectionLost
                    )
                },
                onChangeWifiDetails = {
                    state.value = RequestWifiCredentials(onWifiCredentialsInput = {
                        onWifiCredentialsInput(it.toEntity())
                    })
                },
                onAdjustSettings = {
                    // TODO: add functionalities and data to this use case
                    // TODO: add change wifi
                    // TODO: add remember last download
                    // TODO: add delete all photos
                    // TODO: add option to skip videos
                    // TODO: add option to download all folders with warning for timelapses
                    state.value = ChangeSettings
                }
            )
        } else {
            RequestWifiCredentials(
                onWifiCredentialsInput = {
                    onWifiCredentialsInput(it.toEntity())
                }
            )
        }
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
        CoroutineScope(dispatcher).launch {
            val mediaToDownload = getPhotosToDownload() ?: return@launch

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
        state.value = DownloadPhotos(totalPhotos = photosToDownload.size)

        val downloadedPhotoUris = mutableMapOf<String, PhotoDownloadInfo>()

        CoroutineScope(dispatcher).launch {
            photosToDownload.forEachIndexed { index, photo ->
                repository.downloadMediaToStorage(photo).collect { photoDownloadInfo ->
                    downloadedPhotoUris[photoDownloadInfo.name] = photoDownloadInfo

                    state.value = DownloadPhotos(
                        currentPhotoNum = index + 1,
                        totalPhotos = photosToDownload.size,
                        downloadedPhotos = downloadedPhotoUris.values.toList(),
                        downloadSpeed = photoDownloadInfo.downloadSpeed,
                    )
                }
            }

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
        wifiUtil.disconnectFromWifi()
        repository.shutDownCamera()

        state.value = Init(::onInit)

        event.value = Event(
            SuccessfulDownload(numDownloadedPhotos = numDownloadedPhotos)
        )
    }

    private fun onConnectionLost() {
        // TODO: check current file progress and delete it if not 100%

        state.value = Init(::onInit)
    }
}
