package com.adriantache.greatimagedownloader.domain.wifi

interface WifiUtil {
    val isWifiDisabled: Boolean

    suspend fun connectToWifi(
        ssid: String,
        password: String,
        onDisconnected: () -> Unit,
        onConnected: () -> Unit,
        onScanning: () -> Unit,
        onThrottled: () -> Unit,
    )

    suspend fun suggestNetwork(): String

    fun cleanup()
}
