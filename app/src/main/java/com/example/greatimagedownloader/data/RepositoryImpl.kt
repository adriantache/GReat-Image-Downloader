package com.example.greatimagedownloader.data

import com.example.greatimagedownloader.data.storage.WifiStorage
import com.example.greatimagedownloader.domain.data.Repository

class RepositoryImpl(
    private val wifiStorage: WifiStorage,
) : Repository {
    override fun getWifiSsid(): String? {
        return wifiStorage.getWifiSsid()
    }

    override fun getWifiPassword(): String? {
        return wifiStorage.getWifiPassword()
    }

    override fun saveWifiSsid(ssid: String) {
        wifiStorage.saveWifiSsid(ssid)
    }

    override fun saveWifiPassword(password: String) {
        wifiStorage.saveWifiPassword(password)
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
