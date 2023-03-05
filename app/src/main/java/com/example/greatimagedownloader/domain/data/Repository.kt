package com.example.greatimagedownloader.domain.data

interface Repository {
    // Wifi
    fun getWifiSsid(): String?
    fun getWifiPassword(): String?
    fun saveWifiSsid(ssid: String)
    fun saveWifiPassword(password: String)

    // Data storage
    fun getSavedPhotos(): List<String>

    // Camera operations
    fun getCameraPhotoList(): List<String>
    suspend fun downloadPhotoToStorage(name: String)
    fun shutDownCamera()
}
