package com.example.greatimagedownloader.domain.model

import com.example.greatimagedownloader.domain.ui.model.WifiDetails

sealed interface States {
    class Init(val onInit: () -> Unit) : States

    class RequestPermissions(val onPermissionsGranted: () -> Unit) : States

    class RequestWifiCredentials(
        val onWifiCredentialsInput: (WifiDetails) -> Unit,
    ) : States

    class ConnectWifi(
        val wifiDetails: WifiDetails,
        val onChangeWifiDetails: () -> Unit,
        val onConnectionSuccess: () -> Unit,
        val onConnectionLost: () -> Unit,
    ) : States

    object GetPhotos : States

    class DownloadPhotos(
        val currentPhotoNum: Int,
        val totalPhotos: Int,
    ) : States

    object Disconnect : States

    class Disconnected(
        val onRestart: () -> Unit,
    ) : States
}
