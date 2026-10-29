package com.adriantache.greatimagedownloader.service

import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.adriantache.greatimagedownloader.MainActivity
import com.adriantache.greatimagedownloader.domain.data.Repository
import com.adriantache.greatimagedownloader.ui.permissions.PermissionsHelper
import com.adriantache.greatimagedownloader.wear.WearConstants
import com.adriantache.greatimagedownloader.wear.model.WearDownloadStatus
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WearListenerService : WearableListenerService(), KoinComponent {
    private val wearableSyncManager: WearableSyncManager by inject()
    private val repository: Repository by inject()

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "onMessageReceived path=${messageEvent.path} from=${messageEvent.sourceNodeId}")

        when (messageEvent.path) {
            WearConstants.PATH_START_SYNC -> {
                if (!PermissionsHelper.hasAllRequiredPermissions(this)) {
                    val activityIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(MainActivity.EXTRA_AUTO_CONNECT, true)
                    }
                    startActivity(activityIntent)

                    wearableSyncManager.broadcastStatus(
                        WearDownloadStatus(
                            state = "ERROR",
                            stageText = "Grant permissions in phone app",
                            errorMessage = "Please grant all required permissions in phone app first",
                        )
                    )
                    return
                }

                val wifiDetails = repository.getWifiDetails().getOrNull()
                if (wifiDetails == null || wifiDetails.ssid.isNullOrBlank() || wifiDetails.password.isNullOrBlank()) {
                    val activityIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(MainActivity.EXTRA_AUTO_CONNECT, true)
                    }
                    startActivity(activityIntent)

                    wearableSyncManager.broadcastStatus(
                        WearDownloadStatus(
                            state = "ERROR",
                            stageText = "Setup Wi-Fi in phone app first",
                            errorMessage = "Please enter camera Wi-Fi details in phone app first",
                        )
                    )
                    return
                }

                val serviceIntent = Intent(this, PhotoDownloadService::class.java).apply {
                    action = PhotoDownloadService.Actions.QUICK_DOWNLOAD.name
                }
                ContextCompat.startForegroundService(this, serviceIntent)
            }

            WearConstants.PATH_STOP_SYNC -> {
                val serviceIntent = Intent(this, PhotoDownloadService::class.java).apply {
                    action = PhotoDownloadService.Actions.STOP.name
                }
                ContextCompat.startForegroundService(this, serviceIntent)
            }

            WearConstants.PATH_REQUEST_STATUS -> {
                wearableSyncManager.sendCurrentStatusToNode(messageEvent.sourceNodeId)
            }
        }
    }

    companion object {
        private const val TAG = "WearListenerService"
    }
}
