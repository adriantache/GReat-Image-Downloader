package com.example.greatimagedownloader.data

import com.example.greatimagedownloader.data.api.RicohApi
import com.example.greatimagedownloader.data.storage.FilesStorage
import com.example.greatimagedownloader.data.storage.WifiStorage
import com.example.greatimagedownloader.domain.data.Repository
import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.domain.data.model.PhotoFile
import com.example.greatimagedownloader.domain.data.model.WifiDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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

    override suspend fun getCameraPhotoList(): Result<List<PhotoFile>> {
        val response = ricohApi.getPhotos()

        return if (response.isSuccessful) {
            val photoFile = response.body()?.dirs?.flatMap { it.toPhotoInfoList() }.orEmpty()

            Result.success(photoFile)
        } else {
            val exception = Exception(response.errorBody().toString())
            Result.failure(exception)
        }
    }

    override suspend fun downloadPhotoToStorage(photo: PhotoFile): Flow<PhotoDownloadInfo> {
        val photoData = withContext(Dispatchers.IO) {
            ricohApi.getPhoto(photo.directory, photo.name)
        }
        return filesStorage.savePhoto(photoData, photo.name).map { it.toDomain() }
    }

    override suspend fun shutDownCamera() {
        ricohApi.finish()
    }
}
