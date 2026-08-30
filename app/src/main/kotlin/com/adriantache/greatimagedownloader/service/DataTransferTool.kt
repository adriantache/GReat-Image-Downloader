package com.adriantache.greatimagedownloader.service

import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.model.DomainError
import com.adriantache.greatimagedownloader.domain.utils.model.Event
import kotlinx.coroutines.flow.MutableStateFlow

class DataTransferTool {
    val imageFlow = MutableStateFlow<PhotoDownloadInfo?>(null)
    val errorFlow = MutableStateFlow<Event<DomainError>?>(null)
    val downloadFinishedFlow = MutableStateFlow<Event<Unit>?>(null)
    val serviceStateFlow = MutableStateFlow(ServiceState.IDLE)

    fun reset() {
        imageFlow.value = null
        errorFlow.value = null
        downloadFinishedFlow.value = null
        serviceStateFlow.value = ServiceState.IDLE
    }

    enum class ServiceState {
        IDLE, CONNECTING, FETCHING, DOWNLOADING, STOPPING
    }
}
