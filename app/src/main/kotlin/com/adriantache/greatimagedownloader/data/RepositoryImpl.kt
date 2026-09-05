package com.adriantache.greatimagedownloader.data

import com.adriantache.greatimagedownloader.data.api.RicohApi
import com.adriantache.greatimagedownloader.data.api.error.CameraException
import com.adriantache.greatimagedownloader.data.mapper.mapDomainError
import com.adriantache.greatimagedownloader.data.mapper.toDomainException
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException

class RepositoryImpl(
    private val wifiStorage: WifiStorage,
    private val filesStorage: FilesStorage,
    private val preferencesStorage: PreferencesStorage,
    private val ricohApi: RicohApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Repository {
    override fun getWifiDetails(): Result<WifiDetails> {
        val ssidResult = wifiStorage.getWifiSsid()
        val passwordResult = wifiStorage.getWifiPassword()
        val bssidResult = wifiStorage.getWifiBssid()

        return if (ssidResult.isSuccess && passwordResult.isSuccess && bssidResult.isSuccess) {
            Result.success(
                WifiDetails(
                    ssid = ssidResult.getOrNull(),
                    password = passwordResult.getOrNull(),
                    bssid = bssidResult.getOrNull(),
                )
            )
        } else {
            // Pick the first failure if any.
            val failure = ssidResult.exceptionOrNull()
                ?: passwordResult.exceptionOrNull()
                ?: bssidResult.exceptionOrNull()
                ?: Exception("Unknown WifiStorage error")

            Result.failure(failure.toDomainException())
        }
    }

    override fun saveWifiDetails(wifiDetails: WifiDetails): Result<Unit> {
        return wifiStorage.saveWifiSsid(requireNotNull(wifiDetails.ssid))
            .onSuccess { wifiStorage.saveWifiPassword(requireNotNull(wifiDetails.password)) }
            .onSuccess { wifiStorage.saveWifiBssid(wifiDetails.bssid) }
            .mapDomainError()
    }

    override fun getSavedPhotos(): Result<List<PhotoDownloadInfo>> =
        filesStorage.getSavedPhotos().mapDomainError()

    override fun getSavedMovies(): Result<List<String>> =
        filesStorage.getSavedMovies().mapDomainError()

    override fun deleteMedia(uri: String): Result<Unit> =
        filesStorage.deleteMedia(uri).mapDomainError()

    override suspend fun deleteAll(): Result<Unit> = withContext(ioDispatcher) {
        filesStorage.deleteAll().mapDomainError()
    }


    override suspend fun getCameraPhotoList(): Result<List<PhotoFile>> {
        return withContext(ioDispatcher) {
            try {
                val response = ricohApi.getPhotos()

                if (response.isSuccessful) {
                    val photoFile = response.body()?.dirs?.flatMap { it.toPhotoInfoList() }.orEmpty()

                    Result.success(photoFile)
                } else {
                    val exception = CameraException.Unknown(response.errorBody().toString())
                    Result.failure(exception.toDomainException())
                }
            } catch (e: Exception) {
                val exception = when (e) {
                    is SocketTimeoutException -> CameraException.CameraDisconnected
                    is IOException -> CameraException.NetworkError(e)
                    else -> CameraException.Unknown(e.message)
                }
                Result.failure(exception.toDomainException())
            }
        }
    }

    override fun downloadMediaToStorage(photo: PhotoFile): Flow<Result<PhotoDownloadInfo>> {
        return flow {
            try {
                val imageResponse = ricohApi.getPhoto(
                    directory = photo.directory,
                    file = photo.name
                )

                filesStorage.savePhoto(
                    responseBody = imageResponse,
                    file = photo,
                ).collect { result ->
                    emit(result.mapDomainError())
                }
            } catch (e: Exception) {
                val exception = when (e) {
                    is SocketTimeoutException -> CameraException.CameraDisconnected
                    is IOException -> CameraException.NetworkError(e)
                    else -> CameraException.Unknown(e.message)
                }
                emit(Result.failure(exception.toDomainException()))
            }
        }.flowOn(ioDispatcher)
    }

    override suspend fun shutDownCamera(): Result<Unit> = withContext(ioDispatcher) {
        try {
            ricohApi.finish()
            Result.success(Unit)
        } catch (e: Exception) {
            val exception = when (e) {
                is SocketTimeoutException -> CameraException.CameraDisconnected
                is IOException -> CameraException.NetworkError(e)
                else -> CameraException.Unknown(e.message)
            }
            Result.failure(exception.toDomainException())
        }
    }

    override suspend fun saveLatestDownloadedPhotos(photos: List<PhotoFile>): Result<Unit> = withContext(ioDispatcher) {
        preferencesStorage.saveLatestDownloadedPhotos(photos).mapDomainError()
    }

    override suspend fun getLatestDownloadedPhotos(): Result<List<PhotoFile>> = withContext(ioDispatcher) {
        preferencesStorage.getLatestDownloadedPhotos().mapDomainError()
    }

    override suspend fun saveSettings(settings: Settings): Result<Unit> = withContext(ioDispatcher) {
        preferencesStorage.saveSettings(settings).mapDomainError()
    }

    override suspend fun getSettings(): Result<Settings> = withContext(ioDispatcher) {
        preferencesStorage.getSettings().mapDomainError()
    }
}
