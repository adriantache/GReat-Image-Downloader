package com.example.greatimagedownloader.domain.model

sealed interface Events {
    object InvalidWifiInput : Events
    object CannotDownloadPhotos : Events
}
