package com.adriantache.greatimagedownloader.domain.model

import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.ui.model.WifiDetails
import com.adriantache.greatimagedownloader.domain.utils.model.Kbps

sealed interface States {
    data class Init(val onInit: () -> Unit) : States

    data class RequestPermissions(val onPermissionsGranted: () -> Unit) : States

    data class RequestWifiCredentials(
        val onWifiCredentialsInput: (WifiDetails) -> Unit,
        val onSuggestWifiName: suspend () -> String,
        val onDismiss: () -> Unit,
    ) : States

    data class ConnectWifi(
        val isHardTimeout: Boolean, // Whether the user has connected too many times.
        val onCheckWifiDisabled: () -> Boolean,
        val onConnect: () -> Unit,
        // TODO: this should be in settings, or shown if we get an error connecting
        val onChangeWifiDetails: () -> Unit,
        // TODO: this shouldn't be here
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
        val onStopDownloading: () -> Unit,
    ) : States

    data object StoppingDownload : States

    data class ChangeSettings(
        val settings: Settings,
        val onRememberLastDownloadedPhotos: () -> Unit,
        val onDeleteAllPhotos: () -> Unit,
        val onExitSettings: () -> Unit,
    ) : States
}
