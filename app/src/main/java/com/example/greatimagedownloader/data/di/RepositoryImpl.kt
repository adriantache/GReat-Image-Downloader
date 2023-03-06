package com.example.greatimagedownloader.data.di

import com.example.greatimagedownloader.domain.data.Repository

class RepositoryImpl: Repository {
    override fun getWifiSsid(): String? {
        TODO("Not yet implemented")
    }

    override fun getWifiPassword(): String? {
        TODO("Not yet implemented")
    }

    override fun saveWifiSsid(ssid: String) {
        TODO("Not yet implemented")
    }

    override fun saveWifiPassword(password: String) {
        TODO("Not yet implemented")
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
