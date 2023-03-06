package com.example.greatimagedownloader.data

import com.example.greatimagedownloader.data.api.RicohApi
import com.example.greatimagedownloader.data.storage.FilesStorage
import com.example.greatimagedownloader.data.storage.WifiStorage
import com.example.greatimagedownloader.domain.data.Repository
import com.example.greatimagedownloader.domain.data.model.PhotoInfo
import com.example.greatimagedownloader.domain.data.model.WifiDetails

class RepositoryImpl(
    private val wifiStorage: WifiStorage,
    private val filesStorage: FilesStorage,
    private val ricohApi: RicohApi,
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
        return filesStorage.getSavedPhotos()
    }

    override suspend fun getCameraPhotoList(): Result<List<PhotoInfo>> {
        val response = ricohApi.getPhotos()

        return if (response.isSuccessful) {
            val photoInfo = response.body()?.dirs?.flatMap { dir ->
                dir.files.map { file ->
                    PhotoInfo(
                        directory = dir.name,
                        name = file
                    )
                }
            }.orEmpty()

            Result.success(photoInfo)
        } else {
            Result.failure(Throwable(response.errorBody().toString()))
        }
    }

    override suspend fun downloadPhotoToStorage(photo: PhotoInfo) {
        val photoData = ricohApi.getPhoto(photo.directory, photo.name)
        filesStorage.savePhoto(photoData, photo.name)
    }

    override suspend fun shutDownCamera() {
        ricohApi.finish()
    }
}
