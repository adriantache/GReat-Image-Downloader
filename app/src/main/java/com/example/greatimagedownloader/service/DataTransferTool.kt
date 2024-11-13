package com.example.greatimagedownloader.service

import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.domain.utils.model.Event
import kotlinx.coroutines.flow.MutableStateFlow

class DataTransferTool {
    val imageFlow = MutableStateFlow<PhotoDownloadInfo?>(null)
    val downloadFinishedFlow = MutableStateFlow<Event<Unit>?>(null)
}
