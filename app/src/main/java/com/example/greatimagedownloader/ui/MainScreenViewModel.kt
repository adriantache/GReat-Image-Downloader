package com.example.greatimagedownloader.ui

import androidx.lifecycle.ViewModel
import com.example.greatimagedownloader.domain.DownloadPhotosUseCase

class MainScreenViewModel(downloadPhotosUseCase: DownloadPhotosUseCase) : ViewModel() {
    val downloadPhotosState = downloadPhotosUseCase.state
    val downloadPhotosEvents = downloadPhotosUseCase.event
}
