package com.example.greatimagedownloader.data

import android.util.Log
import com.example.greatimagedownloader.data.api.RicohApi
import com.example.greatimagedownloader.data.storage.FilesStorage
import com.example.greatimagedownloader.data.storage.WifiStorage
import com.example.greatimagedownloader.domain.data.Repository
import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.domain.data.model.PhotoFile
import com.example.greatimagedownloader.domain.data.model.WifiDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
        return withContext(Dispatchers.IO) {
            val response = ricohApi.getPhotos()

            if (response.isSuccessful) {
                val photoFile = response.body()?.dirs?.flatMap { it.toPhotoInfoList() }.orEmpty()

                Result.success(photoFile)
            } else {
                val exception = Exception(response.errorBody().toString())
                Result.failure(exception)
            }
        }
    }

    override suspend fun downloadPhotoToStorage(photo: PhotoFile): Flow<PhotoDownloadInfo> {
        return flow {
            Log.i("TAGXXX", "Getting photo $photo")

            val photoData = ricohApi.getPhoto(
                directory = photo.directory,
                file = photo.name
            )
            Log.i("TAGXXX", "Got responsebody $photoData")

            val progressFlow = filesStorage.savePhoto(
                responseBody = photoData,
                filename = photo.name
            ).map {
                it.toDomain()
            }

            emitAll(progressFlow)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun shutDownCamera() {
        ricohApi.finish()
    }
}
