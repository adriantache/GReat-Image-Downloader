package com.adriantache.greatimagedownloader.domain.wifi

interface WifiUtil {
    val isWifiDisabled: Boolean

    suspend fun connectToWifi(
        ssid: String,
        password: String,
        bssid: String?, // The new, optional BSSID for the "fast path"
    ): Pair<Boolean, String?> // Returns success status and the new BSSID if found

    suspend fun suggestNetwork(): String

    fun cleanup()
}
