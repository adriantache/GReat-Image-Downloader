package com.example.greatimagedownloader.domain.model

import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.domain.data.model.PhotoFile

sealed interface Events {
    data object InvalidWifiInput : Events

    data object CannotDownloadPhotos : Events

    data class SuccessfulDownload(val numDownloadedPhotos: Int) : Events

    data class ConfirmDeleteAllPhotos(
        val onConfirm: () -> Unit,
        val onDismiss: () -> Unit,
    ) : Events

    data class DownloadPhotosWithService(
        val photosToDownload: List<PhotoFile>,
        val onDownloadInfo: (PhotoDownloadInfo) -> Unit,
        val onDownloadFinished: () -> Unit,
    ) : Events
}
