package com.example.greatimagedownloader.domain

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


    // TODO: figure out a way to stream individual file download progress?
    // TODO: handle directories
    private fun getPhotos() {
        CoroutineScope(dispatcher).launch {
            val savedPhotos = repository.getSavedPhotos()
            val availablePhotos = repository.getCameraPhotoList()

            if (availablePhotos.isFailure) {
                event.value = Event(Events.CannotDownloadPhotos)
            }

            val photosToDownload = availablePhotos.getOrNull().orEmpty()
                .filter {
                    val nameWithoutExtension = it.name.split(".")[0]
                    !savedPhotos.contains(nameWithoutExtension)
                }

            photosToDownload.forEachIndexed { index, name ->
                state.value = DownloadPhotos(
                    currentPhotoNum = index + 1,
                    totalPhotos = photosToDownload.size,
                )

                repository.downloadPhotoToStorage(name)
            }

            state.value = Disconnect

            disconnect()
        }
    }

    private suspend fun disconnect() {
        // TODO: enable this when I figure out the concurrency
        repository.shutDownCamera() // Also shuts down the hotspot, no need to disconnect from WiFi.

        state.value = Disconnected(onRestart = {
            state.value = Init(::onInit)
        })
    }

    private fun onConnectionLost() {
        state.value = Disconnected(onRestart = {
            state.value = Init(::onInit)
        })
    }
}
