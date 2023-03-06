package com.example.greatimagedownloader.ui.wifi

import com.example.greatimagedownloader.domain.ui.model.WifiDetails

interface WifiUtil {
    suspend fun connectToWifi(
        wifiDetails: WifiDetails,
        onConnectionSuccess: () -> Unit,
        onConnectionLost: () -> Unit,
    )
}
