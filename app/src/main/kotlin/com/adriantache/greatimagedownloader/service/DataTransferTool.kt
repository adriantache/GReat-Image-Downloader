package com.adriantache.greatimagedownloader.service

import com.adriantache.greatimagedownloader.domain.ble.ConnectionStage
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.model.DomainError
import com.adriantache.greatimagedownloader.domain.utils.model.Event
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class DataTransferTool {
    val imageFlow = MutableStateFlow<PhotoDownloadInfo?>(null)
    val imageUpdatesFlow = MutableSharedFlow<PhotoDownloadInfo>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val errorFlow = MutableStateFlow<Event<DomainError>?>(null)
    val downloadFinishedFlow = MutableStateFlow<Event<Unit>?>(null)
    val serviceStateFlow = MutableStateFlow(ServiceState.IDLE)
    val widgetStateFlow = MutableStateFlow<WidgetState>(WidgetState.Idle)
    val connectionStageFlow = MutableStateFlow<ConnectionStage?>(null)
    val connectionAttemptFlow = MutableStateFlow(1)

    @Volatile
    var pendingPhotosToDownload: List<PhotoFile> = emptyList()

    fun emitImageProgress(info: PhotoDownloadInfo) {
        imageFlow.value = info
        imageUpdatesFlow.tryEmit(info)
    }

    fun consumePendingPhotos(): List<PhotoFile> {
        val photos = pendingPhotosToDownload
        pendingPhotosToDownload = emptyList()
        return photos
    }

    fun reset() {
        imageFlow.value = null
        errorFlow.value = null
        downloadFinishedFlow.value = null
        serviceStateFlow.value = ServiceState.IDLE
        connectionStageFlow.value = null
        connectionAttemptFlow.value = 1
        pendingPhotosToDownload = emptyList()
    }

    enum class ServiceState {
        IDLE, CONNECTING, FETCHING, DOWNLOADING, STOPPING
    }

    sealed interface WidgetState {
        data object Idle : WidgetState
        data class Connecting(val stageText: String = "BT…") : WidgetState
        data object Checking : WidgetState
        data class Downloading(val current: Int, val total: Int) : WidgetState
        data object Stopping : WidgetState
        data class Done(val count: Int) : WidgetState
        data object Error : WidgetState
    }
}
