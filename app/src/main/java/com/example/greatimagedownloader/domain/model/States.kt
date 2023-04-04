package com.example.greatimagedownloader.domain.model

import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.domain.ui.model.WifiDetails

sealed interface States {
    data class Init(val onInit: () -> Unit) : States

    data class RequestPermissions(val onPermissionsGranted: () -> Unit) : States

    data class RequestWifiCredentials(
        val onWifiCredentialsInput: (WifiDetails) -> Unit,
    ) : States

    data class ConnectWifi(
        val wifiDetails: WifiDetails,
        val onChangeWifiDetails: () -> Unit,
        val onConnectionSuccess: () -> Unit,
        val onConnectionLost: () -> Unit,
    ) : States

    object GetPhotos : States

    data class DownloadPhotos(
        // TODO: build correct object for this
        val downloadedPhotos: List<PhotoDownloadInfo>,
        val currentPhotoNum: Int,
        val totalPhotos: Int,
    ) : States

    object Disconnect : States

    data class Disconnected(
        val onRestart: () -> Unit,
    ) : States
}
