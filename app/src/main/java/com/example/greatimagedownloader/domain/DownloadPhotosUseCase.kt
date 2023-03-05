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
import com.example.greatimagedownloader.domain.utils.model.Event
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val WIFI_PASS_MIN_LENGTH = 8

// TODO: add tests
class DownloadPhotosUseCase(
    private val repository: Repository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val state: MutableStateFlow<States> = MutableStateFlow(Init(onInit = ::onInit))
    val event: MutableStateFlow<Event<Events>?> = MutableStateFlow(null)

    private fun onInit() {
        state.value = RequestPermissions(::onPermissionsGranted)
    }

    private fun onPermissionsGranted() {
        connectToWifi()
    }

    private fun connectToWifi() {
        val ssid = repository.getWifiSsid()
        val password = repository.getWifiPassword()

        state.value = if (ssid != null && password != null) {
            ConnectWifi(
                savedSsid = ssid,
                savedPassword = password,
                onConnectionSuccess = ::onConnectionSuccess,
                onConnectionLost = ::onConnectionLost,
            )
        } else {
            RequestWifiCredentials(::onWifiCredentialsInput)
        }
    }

    private fun onWifiCredentialsInput(ssid: String, password: String) {
        if (ssid.isBlank() || password.isBlank() || password.length < WIFI_PASS_MIN_LENGTH) {
            event.value = Event(Events.InvalidWifiInput)
            return
        }

        repository.saveWifiSsid(ssid)
        repository.saveWifiPassword(password)

        connectToWifi()
    }

    private fun onConnectionSuccess() {
        state.value = GetPhotos

        getPhotos()
    }


    // TODO: figure out a way to stream individual file download progress?
    private fun getPhotos() {
        val savedPhotos = repository.getSavedPhotos()
        val availablePhotos = repository.getCameraPhotoList()

        val photosToDownload = availablePhotos.filter { !savedPhotos.contains(it) }

        CoroutineScope(dispatcher).launch {
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

    private fun disconnect() {
        repository.shutDownCamera() // Also shuts down the hotspot, no need to disconnect from WiFi.

        state.value = Disconnected
    }

    private fun onConnectionLost() {
        state.value = Disconnected
    }
}
