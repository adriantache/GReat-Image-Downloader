package com.example.greatimagedownloader.domain.data

import com.example.greatimagedownloader.domain.data.model.WifiDetails

interface Repository {
    // Wifi
    fun getWifiDetails(): WifiDetails
    fun saveWifiDetails(wifiDetails: WifiDetails)

    // Data storage
    fun getSavedPhotos(): List<String>

    // Camera operations
    fun getCameraPhotoList(): List<String>
    suspend fun downloadPhotoToStorage(name: String)
    fun shutDownCamera()
}
