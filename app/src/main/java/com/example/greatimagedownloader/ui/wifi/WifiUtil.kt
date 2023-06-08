package com.example.greatimagedownloader.ui.wifi

import com.example.greatimagedownloader.domain.ui.model.WifiDetails

interface WifiUtil {
    val isWifiDisabled: Boolean

    suspend fun connectToWifi(
        wifiDetails: WifiDetails,
        onConnectionSuccess: () -> Unit,
        onConnectionLost: () -> Unit,
    )

    suspend fun suggestNetwork(): String
}
