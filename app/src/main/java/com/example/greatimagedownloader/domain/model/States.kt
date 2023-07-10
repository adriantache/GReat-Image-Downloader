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
        val onCheckWifiDisabled: () -> Boolean,
        val onConnect: () -> Unit,
        val onChangeWifiDetails: () -> Unit,
        val onAdjustSettings: () -> Unit,
    ) : States

    object GetPhotos : States

    data class DownloadPhotos(
        val downloadedPhotos: List<PhotoDownloadInfo> = emptyList(),
        val currentPhotoNum: Int = 0,
        val totalPhotos: Int,
    ) : States

    data class Disconnected(
        val numDownloadedPhotos: Int? = null,
        val onRestart: () -> Unit,
    ) : States

    object ChangeSettings : States
}
