package com.adriantache.greatimagedownloader.wear.data

import android.content.Context
import android.util.Log
import com.adriantache.greatimagedownloader.wear.WearConstants
import com.adriantache.greatimagedownloader.wear.model.WearDownloadStatus
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

class WearCommunicationClient(
    context: Context,
    private val nodeClient: NodeClient = Wearable.getNodeClient(context),
    private val messageClient: MessageClient = Wearable.getMessageClient(context),
    private val dataClient: DataClient = Wearable.getDataClient(context),
) : MessageClient.OnMessageReceivedListener, DataClient.OnDataChangedListener {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val statusFlow: StateFlow<WearDownloadStatus> = _statusFlow.asStateFlow()
    val isPhoneConnectedFlow: StateFlow<Boolean> = _isPhoneConnectedFlow.asStateFlow()

    fun startListening() {
        messageClient.addListener(this)
        dataClient.addListener(this)
        checkPhoneConnection()
        requestCurrentStatus()
    }

    fun stopListening() {
        messageClient.removeListener(this)
        dataClient.removeListener(this)
    }

    fun checkPhoneConnection(onResult: ((Boolean) -> Unit)? = null) {
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            val hasConnectedPhone = nodes.isNotEmpty()
            _isPhoneConnectedFlow.value = hasConnectedPhone
            onResult?.invoke(hasConnectedPhone)
        }.addOnFailureListener {
            _isPhoneConnectedFlow.value = false
            onResult?.invoke(false)
        }
    }

    fun sendStartSync() {
        _statusFlow.value = WearDownloadStatus(
            state = "CONNECTING",
            stageText = "Connecting via Bluetooth…",
        )
        sendMessageToAllNodes(WearConstants.PATH_START_SYNC)
    }

    fun sendStopSync() {
        _statusFlow.value = WearDownloadStatus(
            state = "STOPPING",
            stageText = "Stopping…",
        )
        sendMessageToAllNodes(WearConstants.PATH_STOP_SYNC)
    }

    fun requestCurrentStatus() {
        sendMessageToAllNodes(WearConstants.PATH_REQUEST_STATUS)
    }

    fun resetToIdle() {
        _statusFlow.value = WearDownloadStatus(state = "IDLE")
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived path=${messageEvent.path}")
        if (messageEvent.path == WearConstants.PATH_DOWNLOAD_STATUS) {
            parseAndUpdateStatus(String(messageEvent.data, Charsets.UTF_8))
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == WearConstants.PATH_DOWNLOAD_STATUS
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val jsonString = dataMap.getString(WearConstants.KEY_STATUS_JSON)
                if (!jsonString.isNullOrBlank()) {
                    parseAndUpdateStatus(jsonString)
                }
            }
        }
    }

    private fun parseAndUpdateStatus(jsonString: String) {
        runCatching {
            json.decodeFromString<WearDownloadStatus>(jsonString)
        }.onSuccess { status ->
            Log.d(TAG, "Parsed status update: $status")
            _statusFlow.value = status
            _isPhoneConnectedFlow.value = true
        }.onFailure { e ->
            Log.w(TAG, "Failed to parse WearDownloadStatus JSON: ${e.message}")
        }
    }

    private fun sendMessageToAllNodes(path: String, payload: ByteArray = ByteArray(0)) {
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            if (nodes.isEmpty()) {
                _isPhoneConnectedFlow.value = false
                Log.w(TAG, "Cannot send message '$path': No connected phone nodes found")
                return@addOnSuccessListener
            }
            _isPhoneConnectedFlow.value = true
            for (node in nodes) {
                messageClient.sendMessage(node.id, path, payload)
            }
        }.addOnFailureListener { e ->
            _isPhoneConnectedFlow.value = false
            Log.w(TAG, "Failed getting connected nodes: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "WearCommClient"
        private val _statusFlow = MutableStateFlow(WearDownloadStatus())
        private val _isPhoneConnectedFlow = MutableStateFlow(true)

        fun updateSharedStatus(status: WearDownloadStatus) {
            _statusFlow.value = status
            _isPhoneConnectedFlow.value = true
        }
    }
}
