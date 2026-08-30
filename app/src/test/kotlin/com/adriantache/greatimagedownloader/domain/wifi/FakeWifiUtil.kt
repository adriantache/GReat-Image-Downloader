package com.adriantache.greatimagedownloader.domain.wifi

import android.net.Network
import com.adriantache.greatimagedownloader.domain.wifi.WifiUtil

class FakeWifiUtil : WifiUtil {
    var connectResult: Pair<Boolean, String?> = Pair(true, "BSSID")
    var isWifiDisabledValue = false
    var suggestNetworkResult = "SSID"

    var connectCalledCount = 0

    override suspend fun connectToWifi(ssid: String, password: String, bssid: String?): Pair<Boolean, String?> {
        connectCalledCount++
        return connectResult
    }

    override val isWifiDisabled: Boolean
        get() = isWifiDisabledValue

    override suspend fun suggestNetwork(): String = suggestNetworkResult

    override fun cleanup() {
        // No-op for fake
    }
}
