package com.adriantache.greatimagedownloader.ui

import androidx.lifecycle.ViewModel
import com.adriantache.greatimagedownloader.domain.DownloadPhotosUseCase

class MainScreenViewModel(
    private val downloadPhotosUseCase: DownloadPhotosUseCase,
) : ViewModel() {
    val downloadPhotosState = downloadPhotosUseCase.state
    val downloadPhotosEvents = downloadPhotosUseCase.event

    fun cancelConnection() {
        downloadPhotosUseCase.cancelConnection()
    }
}
