package com.adriantache.greatimagedownloader.data.storage

interface WifiStorage {
    fun getWifiSsid(): String?
    fun getWifiPassword(): String?
    fun getWifiBssid(): String?
    fun saveWifiSsid(ssid: String)
    fun saveWifiPassword(password: String)
    fun saveWifiBssid(bssid: String)
}
