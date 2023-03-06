package com.example.greatimagedownloader.data.storage

interface WifiStorage {
    fun getWifiSsid(): String?
    fun getWifiPassword(): String?
    fun saveWifiSsid(ssid: String)
    fun saveWifiPassword(password: String)
}
