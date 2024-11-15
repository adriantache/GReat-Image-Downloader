package com.adriantache.greatimagedownloader.data

import com.adriantache.greatimagedownloader.data.api.RicohApi
import com.adriantache.greatimagedownloader.data.storage.FilesStorage
import com.adriantache.greatimagedownloader.data.storage.PreferencesStorage
import com.adriantache.greatimagedownloader.data.storage.WifiStorage
import com.adriantache.greatimagedownloader.domain.data.Repository
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.data.model.WifiDetails
import com.adriantache.greatimagedownloader.domain.model.Settings
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
    private val preferencesStorage: PreferencesStorage,
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

    override fun deleteMedia(uri: String) {
        filesStorage.deleteMedia(uri)
    }

    override suspend fun deleteAll() {
        filesStorage.deleteAll()
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
                file = photo,
            ).map {
                it.toDomain()
            }

            emitAll(result)
        }.flowOn(ioDispatcher)
    }

    override suspend fun shutDownCamera() {
        ricohApi.finish()
    }

    override suspend fun saveLatestDownloadedPhotos(photos: List<PhotoFile>) {
        preferencesStorage.saveLatestDownloadedPhotos(photos)
    }

    override suspend fun getLatestDownloadedPhotos(): List<PhotoFile> {
        return preferencesStorage.getLatestDownloadedPhotos()
    }

    override suspend fun saveSettings(settings: Settings) {
        preferencesStorage.saveSettings(settings)
    }

    override suspend fun getSettings(): Settings {
        return preferencesStorage.getSettings()
    }
}
