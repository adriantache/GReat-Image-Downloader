package com.adriantache.greatimagedownloader.domain.wifi

class FakeWifiUtil : WifiUtil {
    var connectResult: Pair<Boolean, String?> = Pair(true, "BSSID")
    var isWifiDisabledValue = false
    var suggestNetworkResult = "SSID"
    var ensureBoundResult = true

    var connectCalledCount = 0
    var cleanupCalledCount = 0
    var ensureBoundCalledCount = 0
    val connectedBssids = mutableListOf<String?>()

    override suspend fun connectToWifi(ssid: String, password: String, bssid: String?): Pair<Boolean, String?> {
        connectCalledCount++
        connectedBssids.add(bssid)
        return connectResult
    }

    override val isWifiDisabled: Boolean
        get() = isWifiDisabledValue

    override suspend fun suggestNetwork(): String = suggestNetworkResult

    override fun ensureBound(): Boolean {
        ensureBoundCalledCount++
        return ensureBoundResult
    }

    override fun cleanup() {
        cleanupCalledCount++
    }
}
