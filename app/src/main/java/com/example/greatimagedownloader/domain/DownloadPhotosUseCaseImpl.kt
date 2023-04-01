package com.example.greatimagedownloader.domain

import android.util.Log
import com.example.greatimagedownloader.domain.data.Repository
import com.example.greatimagedownloader.domain.model.Events
import com.example.greatimagedownloader.domain.model.States
import com.example.greatimagedownloader.domain.model.States.ConnectWifi
import com.example.greatimagedownloader.domain.model.States.Disconnect
import com.example.greatimagedownloader.domain.model.States.Disconnected
import com.example.greatimagedownloader.domain.model.States.DownloadPhotos
import com.example.greatimagedownloader.domain.model.States.GetPhotos
import com.example.greatimagedownloader.domain.model.States.Init
import com.example.greatimagedownloader.domain.model.States.RequestPermissions
import com.example.greatimagedownloader.domain.model.States.RequestWifiCredentials
import com.example.greatimagedownloader.domain.model.WifiDetailsEntity
import com.example.greatimagedownloader.domain.utils.model.Event
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// TODO: add tests
class DownloadPhotosUseCaseImpl(
    private val repository: Repository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DownloadPhotosUseCase {
    override val state: MutableStateFlow<States> = MutableStateFlow(Init(onInit = ::onInit))
    override val event: MutableStateFlow<Event<Events>?> = MutableStateFlow(null)

    private fun onInit() {
        state.value = RequestPermissions(::onPermissionsGranted)
    }

    private fun onPermissionsGranted() {
        connectToWifi()
    }

    private fun connectToWifi() {
        val wifiDetails = repository.getWifiDetails().toEntity()

        state.value = if (wifiDetails.isValid) {
            ConnectWifi(
                wifiDetails = wifiDetails.toUi(),
                onConnectionSuccess = ::onConnectionSuccess,
                onConnectionLost = ::onConnectionLost,
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

        getPhotos()
    }

    // TODO: handle directories
    // TODO: [IMPORTANT] handle videos!
    private fun getPhotos() {
        CoroutineScope(dispatcher).launch {
            val savedPhotos = repository.getSavedPhotos().map {
                // TODO: check this is still necessary after we fix the bug
                // We might need to remove the extension for files that get saved .JPG.jpg
                it.split(".")[0]
            }.distinct()
            val availablePhotos = repository.getCameraPhotoList()

            if (availablePhotos.isFailure) {
                event.value = Event(Events.CannotDownloadPhotos)
            }

            val photosToDownload = availablePhotos.getOrNull().orEmpty()
                .filter {
                    val nameWithoutExtension = it.name.split(".")[0]
                    !savedPhotos.contains(nameWithoutExtension)
                }
                // TODO: remove this filter when I fix OOM issues for videos and add workflow for them
                .filter {
                    val extension = it.name.split(".").lastOrNull()?.lowercase()
                    extension == "jpg"
                }

            state.value = DownloadPhotos(
                downloadedPhotos = emptyMap(),
                currentPhotoNum = 0,
                totalPhotos = photosToDownload.size,
            )

            val downloadedPhotoUris = mutableMapOf<String, Int>()

            photosToDownload.forEachIndexed { index, photo ->
                repository.downloadPhotoToStorage(photo).collect {
                    if (it.uri == null) return@collect

                    Log.i("TAGXXX", "Downloading $it")

                    downloadedPhotoUris[it.uri] = it.downloadProgress

                    state.value = DownloadPhotos(
                        currentPhotoNum = index + 1,
                        totalPhotos = photosToDownload.size,
                        downloadedPhotos = downloadedPhotoUris
                    )
                }
            }

            event.value = Event(Events.DownloadSuccess(downloadedPhotoUris.keys.size))
            state.value = Disconnect

            disconnect()
        }
    }

    private suspend fun disconnect() {
        repository.shutDownCamera() // Also shuts down the hotspot, no need to disconnect from WiFi.

        state.value = Disconnected(onRestart = { state.value = Init(::onInit) })
    }

    private fun onConnectionLost() {
        state.value = Disconnected(onRestart = { state.value = Init(::onInit) })
    }
}
