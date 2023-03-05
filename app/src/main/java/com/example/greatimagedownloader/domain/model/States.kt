package com.example.greatimagedownloader.domain.model

sealed interface States {
    class Init(val onInit: () -> Unit) : States

    class RequestPermissions(val onPermissionsGranted: () -> Unit) : States

    class RequestWifiCredentials(
        onWifiCredentialsInput: (ssid: String, password: String) -> Unit,
    ) : States

    class ConnectWifi(
        val savedSsid: String,
        val savedPassword: String,
        val onConnectionSuccess: () -> Unit,
        val onConnectionLost: () -> Unit,
    ) : States

    object GetPhotos : States

    class DownloadPhotos(
        val currentPhotoNum: Int,
        val totalPhotos: Int,
    ) : States

    object Disconnect : States

    object Disconnected : States
}
