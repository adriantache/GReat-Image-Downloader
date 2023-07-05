package com.example.greatimagedownloader.domain

import com.example.greatimagedownloader.domain.data.Repository
import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.domain.model.Events
import com.example.greatimagedownloader.domain.model.States
import com.example.greatimagedownloader.domain.model.States.ConnectWifi
import com.example.greatimagedownloader.domain.model.States.Disconnected
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
    private fun getMedia() {
        CoroutineScope(dispatcher).launch {
            val savedMedia = (repository.getSavedPhotos() + repository.getSavedMovies()).distinct()
            val availablePhotos = repository.getCameraPhotoList()

            if (availablePhotos.isFailure) {
                event.value = Event(Events.CannotDownloadPhotos)
                state.value = Init(::onInit)
                return@launch
            }

            val photosToDownload = availablePhotos.getOrNull()
                .orEmpty()
                .filter {
                    val nameWithoutExtension = it.name.split(".")[0]
                    !savedMedia.contains(nameWithoutExtension)
                }

            state.value = DownloadPhotos(totalPhotos = photosToDownload.size)

            val downloadedPhotoUris = mutableMapOf<String, PhotoDownloadInfo>()

            photosToDownload.forEachIndexed { index, photo ->
                repository.downloadMediaToStorage(photo).collect { photoDownloadInfo ->
                    downloadedPhotoUris[photoDownloadInfo.name] = photoDownloadInfo

                    state.value = DownloadPhotos(
                        currentPhotoNum = index + 1,
                        totalPhotos = photosToDownload.size,
                        downloadedPhotos = downloadedPhotoUris.values.toList(),
                    )
                }
            }

            disconnect(downloadedPhotoUris.count { it.value.downloadProgress == 100 })
        }
    }

    private suspend fun disconnect(numDownloadedPhotos: Int) {
        repository.shutDownCamera() // Also shuts down the hotspot, no need to disconnect from WiFi.

        state.value = Disconnected(
            numDownloadedPhotos = numDownloadedPhotos,
            onRestart = { state.value = Init(::onInit) },
        )
    }

    private fun onConnectionLost() {
        // TODO: check current file progress and delete it if not 100%

        state.value = Disconnected(onRestart = { state.value = Init(::onInit) })
    }
}
