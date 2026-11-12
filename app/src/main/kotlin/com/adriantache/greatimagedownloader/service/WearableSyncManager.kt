package com.adriantache.greatimagedownloader.service

import android.content.Context
import android.util.Log
import com.adriantache.greatimagedownloader.domain.ble.ConnectionStage
import com.adriantache.greatimagedownloader.service.DataTransferTool.ServiceState
import com.adriantache.greatimagedownloader.service.DataTransferTool.WidgetState
import com.adriantache.greatimagedownloader.wear.WearConstants
import com.adriantache.greatimagedownloader.wear.model.WearDownloadStatus
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs

class WearableSyncManager(
    private val context: Context,
    private val dataTransferTool: DataTransferTool,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nodeClient: NodeClient = Wearable.getNodeClient(context),
    private val messageClient: MessageClient = Wearable.getMessageClient(context),
    private val dataClient: DataClient = Wearable.getDataClient(context),
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var lastProgressBroadcastTime = 0L
    private var lastProgressPercent = -1
    private var lastPhotoIndex = -1

    init {
        startObserving()
    }

    private fun startObserving() {
        // Broadcast major state changes immediately (and update persistent DataItem)
        scope.launch {
            combine(
                dataTransferTool.serviceStateFlow,
                dataTransferTool.widgetStateFlow,
                dataTransferTool.connectionStageFlow,
                dataTransferTool.connectionAttemptFlow,
            ) { _, _, _, _ ->
                buildCurrentStatus()
            }.distinctUntilChanged().collect { status ->
                Log.d(TAG, "Broadcasting state update: $status")
                broadcastStatus(status)
            }
        }

        // Throttle high-frequency byte progress to prevent Bluetooth channel saturation and watch UI lag
        scope.launch {
            dataTransferTool.imageUpdatesFlow.collect { imageInfo ->
                if (dataTransferTool.serviceStateFlow.value == ServiceState.DOWNLOADING) {
                    val currentWidgetState = dataTransferTool.widgetStateFlow.value as? WidgetState.Downloading
                    val currentPhoto = currentWidgetState?.current ?: 0
                    val now = System.currentTimeMillis()
                    val isCompletion = imageInfo.downloadProgress == 100
                    val isPhotoChanged = currentPhoto != lastPhotoIndex
                    val isPercentChangedSignificantly = abs(imageInfo.downloadProgress - lastProgressPercent) >= 2
                    val isTimeElapsed = (now - lastProgressBroadcastTime) >= 250L

                    if (isCompletion || isPhotoChanged || (isPercentChangedSignificantly && isTimeElapsed)) {
                        lastProgressBroadcastTime = now
                        lastProgressPercent = imageInfo.downloadProgress
                        lastPhotoIndex = currentPhoto

                        val roundedSpeed = (imageInfo.downloadSpeed.value * 10).toLong() / 10.0
                        val status = buildCurrentStatus().copy(
                            photoProgressPercent = imageInfo.downloadProgress,
                            downloadSpeedKbps = roundedSpeed,
                            currentFileName = imageInfo.name,
                        )
                        sendStreamingProgress(status)
                    }
                }
            }
        }
    }

    fun buildCurrentStatus(): WearDownloadStatus {
        val widgetState = dataTransferTool.widgetStateFlow.value
        val stage = dataTransferTool.connectionStageFlow.value
        val attempt = dataTransferTool.connectionAttemptFlow.value
        val image = dataTransferTool.imageFlow.value

        return when (widgetState) {
            WidgetState.Idle -> WearDownloadStatus(state = "IDLE")

            is WidgetState.Connecting -> {
                val (stageDesc, step) = when (stage) {
                    ConnectionStage.CONNECTING_BLE -> "Connecting via Bluetooth…" to 1
                    ConnectionStage.CAMERA_CONNECTED -> "Camera connected! Waking…" to 2
                    ConnectionStage.STARTING_WIFI -> "Starting camera Wi-Fi…" to 3
                    ConnectionStage.JOINING_WIFI -> {
                        val text = if (attempt > 1) "Joining Wi-Fi… ($attempt/5)" else "Joining camera Wi-Fi…"
                        text to 4
                    }
                    null -> "Connecting to camera…" to 1
                }
                WearDownloadStatus(
                    state = "CONNECTING",
                    stageText = stageDesc,
                    connectionStep = step,
                    totalConnectionSteps = 5,
                )
            }

            WidgetState.Checking -> WearDownloadStatus(
                state = "CHECKING",
                stageText = "Checking camera for new photos…",
                connectionStep = 5,
                totalConnectionSteps = 5,
            )

            is WidgetState.Downloading -> WearDownloadStatus(
                state = "DOWNLOADING",
                stageText = "Downloading photos…",
                currentPhoto = widgetState.current,
                totalPhotos = widgetState.total,
                photoProgressPercent = image?.downloadProgress ?: 0,
                downloadSpeedKbps = image?.downloadSpeed?.value ?: 0.0,
                currentFileName = image?.name.orEmpty(),
            )

            WidgetState.Stopping -> WearDownloadStatus(
                state = "STOPPING",
                stageText = "Stopping and disconnecting…",
            )

            is WidgetState.Done -> WearDownloadStatus(
                state = "DONE",
                stageText = if (widgetState.count == 0) "0 new photos" else "${widgetState.count} photos downloaded",
                downloadCount = widgetState.count,
            )

            WidgetState.Error -> WearDownloadStatus(
                state = "ERROR",
                stageText = "Connection or download failed",
                errorMessage = "Camera disconnected or Wi-Fi failed",
            )
        }
    }

    fun broadcastStatus(status: WearDownloadStatus) {
        val jsonString = runCatching { json.encodeToString(status) }.getOrNull() ?: return
        val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)

        // 1. Direct message to active watch nodes
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                messageClient.sendMessage(node.id, WearConstants.PATH_DOWNLOAD_STATUS, jsonBytes)
            }
        }.addOnFailureListener { e ->
            Log.w(TAG, "Failed to get connected nodes for broadcast: ${e.message}")
        }

        // 2. Put DataItem for persistent synchronization across disconnects
        runCatching {
            val putDataMapReq = PutDataMapRequest.create(WearConstants.PATH_DOWNLOAD_STATUS).apply {
                dataMap.putString(WearConstants.KEY_STATUS_JSON, jsonString)
                dataMap.putLong(WearConstants.KEY_TIMESTAMP, System.currentTimeMillis())
            }
            dataClient.putDataItem(putDataMapReq.asPutDataRequest().setUrgent())
        }
    }

    fun sendStreamingProgress(status: WearDownloadStatus) {
        val jsonString = runCatching { json.encodeToString(status) }.getOrNull() ?: return
        val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                messageClient.sendMessage(node.id, WearConstants.PATH_DOWNLOAD_STATUS, jsonBytes)
            }
        }.addOnFailureListener { e ->
            Log.w(TAG, "Failed to send streaming progress to nodes: ${e.message}")
        }
    }

    fun sendCurrentStatusToNode(nodeId: String) {
        val status = buildCurrentStatus()
        val jsonString = runCatching { json.encodeToString(status) }.getOrNull() ?: return
        val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)
        messageClient.sendMessage(nodeId, WearConstants.PATH_DOWNLOAD_STATUS, jsonBytes)
    }

    companion object {
        private const val TAG = "WearableSyncManager"
    }
}
