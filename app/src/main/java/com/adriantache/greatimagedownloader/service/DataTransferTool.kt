package com.adriantache.greatimagedownloader.service

import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.utils.model.Event
import kotlinx.coroutines.flow.MutableStateFlow

class DataTransferTool {
    val imageFlow = MutableStateFlow<PhotoDownloadInfo?>(null)
    val downloadFinishedFlow = MutableStateFlow<Event<Unit>?>(null)
}
