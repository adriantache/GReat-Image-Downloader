package com.example.greatimagedownloader.domain.model

sealed interface Events {
    data object InvalidWifiInput : Events

    data object CannotDownloadPhotos : Events

    data class SuccessfulDownload(val numDownloadedPhotos: Int) : Events

    data class ConfirmDeleteAllPhotos(
        val onConfirm: () -> Unit,
        val onDismiss: () -> Unit,
    ) : Events
}
