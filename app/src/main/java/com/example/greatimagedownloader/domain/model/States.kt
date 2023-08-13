package com.example.greatimagedownloader.domain.model

import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.domain.ui.model.WifiDetails
import com.example.greatimagedownloader.domain.utils.model.Kbps

sealed interface States {
    data class Init(val onInit: () -> Unit) : States

    data class RequestPermissions(val onPermissionsGranted: () -> Unit) : States

    data class RequestWifiCredentials(
        val onWifiCredentialsInput: (WifiDetails) -> Unit,
        val onSuggestWifiName: suspend () -> String,
    ) : States

    data class ConnectWifi(
        val onCheckWifiDisabled: () -> Boolean,
        val onConnect: () -> Unit,
        val onChangeWifiDetails: () -> Unit,
        val onAdjustSettings: () -> Unit,
    ) : States

    data object GetPhotos : States

    data class SelectFolders(
        val folderInfo: FolderInfo,
        val onFoldersSelect: (List<String>) -> Unit,
    ) : States

    data class DownloadPhotos(
        val downloadedPhotos: List<PhotoDownloadInfo> = emptyList(),
        val currentPhotoNum: Int = 0,
        val totalPhotos: Int,
        val downloadSpeed: Kbps = Kbps(0.0),
    ) : States

    data class ChangeSettings(
        val settings: Settings,
        val onRememberLastDownloadedPhotos: (Boolean) -> Unit,
        val onDeleteAllPhotos: () -> Unit,
        val onExitSettings: () -> Unit,
    ) : States
}
