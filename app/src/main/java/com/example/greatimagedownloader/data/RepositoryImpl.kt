package com.example.greatimagedownloader.data

import com.example.greatimagedownloader.data.storage.WifiStorage
import com.example.greatimagedownloader.domain.data.Repository
import com.example.greatimagedownloader.domain.data.model.WifiDetails

class RepositoryImpl(
    private val wifiStorage: WifiStorage,
) : Repository {
    override fun getWifiDetails(): WifiDetails {
        val ssid = wifiStorage.getWifiSsid()
        val password = wifiStorage.getWifiPassword()

        return WifiDetails(
            ssid = ssid,
            password = password,
        )
    }

    override fun saveWifiDetails(wifiDetails: WifiDetails) {
        wifiStorage.saveWifiSsid(requireNotNull(wifiDetails.ssid))
        wifiStorage.saveWifiPassword(requireNotNull(wifiDetails.password))
    }

    override fun getSavedPhotos(): List<String> {
        TODO("Not yet implemented")
    }

    override fun getCameraPhotoList(): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun downloadPhotoToStorage(name: String) {
        TODO("Not yet implemented")
    }

    override fun shutDownCamera() {
        TODO("Not yet implemented")
    }
}
