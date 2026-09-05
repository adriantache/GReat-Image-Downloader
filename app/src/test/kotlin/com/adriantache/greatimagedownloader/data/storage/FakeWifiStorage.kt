package com.adriantache.greatimagedownloader.data.storage

class FakeWifiStorage : WifiStorage {
    var ssid: String? = null
    var password: String? = null
    var bssid: String? = null
    
    var ssidResult: Result<String?> = Result.success(null)
    var passwordResult: Result<String?> = Result.success(null)
    var bssidResult: Result<String?> = Result.success(null)
    
    var saveSsidResult: Result<Unit> = Result.success(Unit)
    var savePasswordResult: Result<Unit> = Result.success(Unit)
    var saveBssidResult: Result<Unit> = Result.success(Unit)

    override fun getWifiSsid(): Result<String?> = ssidResult.map { ssid }

    override fun getWifiPassword(): Result<String?> = passwordResult.map { password }

    override fun getWifiBssid(): Result<String?> = bssidResult.map { bssid }

    override fun saveWifiSsid(ssid: String): Result<Unit> {
        this.ssid = ssid
        return saveSsidResult
    }

    override fun saveWifiPassword(password: String): Result<Unit> {
        this.password = password
        return savePasswordResult
    }

    override fun saveWifiBssid(bssid: String?): Result<Unit> {
        this.bssid = bssid
        return saveBssidResult
    }
}
