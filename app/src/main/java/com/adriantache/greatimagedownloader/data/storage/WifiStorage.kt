package com.adriantache.greatimagedownloader.data.storage

interface WifiStorage {
    fun getWifiSsid(): Result<String?>
    fun getWifiPassword(): Result<String?>
    fun getWifiBssid(): Result<String?>
    fun saveWifiSsid(ssid: String): Result<Unit>
    fun saveWifiPassword(password: String): Result<Unit>
    fun saveWifiBssid(bssid: String): Result<Unit>
}
