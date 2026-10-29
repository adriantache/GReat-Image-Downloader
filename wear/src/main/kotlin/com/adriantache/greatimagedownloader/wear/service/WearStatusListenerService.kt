package com.adriantache.greatimagedownloader.wear.service

import android.util.Log
import com.adriantache.greatimagedownloader.wear.WearConstants
import com.adriantache.greatimagedownloader.wear.data.WearCommunicationClient
import com.adriantache.greatimagedownloader.wear.model.WearDownloadStatus
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.serialization.json.Json

class WearStatusListenerService : WearableListenerService() {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        if (messageEvent.path == WearConstants.PATH_DOWNLOAD_STATUS) {
            val jsonString = String(messageEvent.data, Charsets.UTF_8)
            parseAndUpdate(jsonString)
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == WearConstants.PATH_DOWNLOAD_STATUS
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val jsonString = dataMap.getString(WearConstants.KEY_STATUS_JSON)
                if (!jsonString.isNullOrBlank()) {
                    parseAndUpdate(jsonString)
                }
            }
        }
    }

    private fun parseAndUpdate(jsonString: String) {
        runCatching {
            json.decodeFromString<WearDownloadStatus>(jsonString)
        }.onSuccess { status ->
            Log.d(TAG, "Background update received: $status")
            WearCommunicationClient.updateSharedStatus(status)
        }.onFailure { e ->
            Log.w(TAG, "Failed parsing background status update: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "WearStatusListener"
    }
}
