package com.example.greatimagedownloader.domain.wifi

import com.example.greatimagedownloader.domain.model.WifiDetailsEntity

interface WifiUtil {
    val isWifiDisabled: Boolean

    fun connectToWifi(
        wifiDetails: WifiDetailsEntity,
        connectTimeoutMs: Int,
        onWifiConnected: () -> Unit,
        onWifiDisconnected: () -> Unit,
    )

    fun disconnectFromWifi()

    fun suggestNetwork(): String
}
