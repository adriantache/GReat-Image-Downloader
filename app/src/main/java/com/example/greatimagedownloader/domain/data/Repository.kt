package com.example.greatimagedownloader.domain.data

import com.example.greatimagedownloader.domain.data.model.PhotoInfo
import com.example.greatimagedownloader.domain.data.model.WifiDetails

interface Repository {
    // Wifi
    fun getWifiDetails(): WifiDetails
    fun saveWifiDetails(wifiDetails: WifiDetails)

    // Data storage
    fun getSavedPhotos(): List<String>

    // Camera operations
    suspend fun getCameraPhotoList(): Result<List<PhotoInfo>>
    suspend fun downloadPhotoToStorage(photo: PhotoInfo)
    suspend fun shutDownCamera()
}
