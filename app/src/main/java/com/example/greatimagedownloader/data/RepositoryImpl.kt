package com.example.greatimagedownloader.data

import com.example.greatimagedownloader.data.api.RicohApi
import com.example.greatimagedownloader.data.storage.FilesStorage
import com.example.greatimagedownloader.data.storage.WifiStorage
import com.example.greatimagedownloader.domain.data.Repository
import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.domain.data.model.PhotoFile
import com.example.greatimagedownloader.domain.data.model.WifiDetails
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// TODO: [IMPORTANT] add error handling to all network calls
class RepositoryImpl(
    private val wifiStorage: WifiStorage,
    private val filesStorage: FilesStorage,
    private val ricohApi: RicohApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
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

    override fun getSavedMovies(): List<String> {
        return filesStorage.getSavedMovies()
    }

    override suspend fun getCameraPhotoList(): Result<List<PhotoFile>> {
        return withContext(ioDispatcher) {
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

    override fun downloadMediaToStorage(photo: PhotoFile): Flow<PhotoDownloadInfo> {
        return flow {
            // TODO: try catch this call in case of connection issues and maybe delete current pending file afterwards
            val imageResponse = ricohApi.getPhoto(
                directory = photo.directory,
                file = photo.name
            )

            // TODO: handle unsuccessful response
            val result = filesStorage.savePhoto(
                responseBody = imageResponse,
                filename = photo.name
            ).map {
                it.toDomain()
            }

            emitAll(result)
        }.flowOn(ioDispatcher)
    }

    override suspend fun shutDownCamera() {
        ricohApi.finish()
    }
}
