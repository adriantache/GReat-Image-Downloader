package com.adriantache.greatimagedownloader.domain.data

import com.adriantache.greatimagedownloader.domain.data.Repository
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.data.model.WifiDetails
import com.adriantache.greatimagedownloader.domain.model.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow

class FakeRepository : Repository {
    var wifiDetails = WifiDetails("SSID", "PASSWORD", "BSSID")
    var savedPhotos = mutableListOf<PhotoDownloadInfo>()
    var savedMovies = mutableListOf<String>()
    var cameraPhotoListResult: Result<List<PhotoFile>> = Result.success(emptyList())
    var settings = Settings()
    var latestDownloadedPhotos = mutableListOf<PhotoFile>()
    
    var shutdownCalled = false
    val downloadFlow = MutableSharedFlow<Result<PhotoDownloadInfo>>()

    override fun getWifiDetails(): Result<WifiDetails> = Result.success(wifiDetails)

    override fun saveWifiDetails(wifiDetails: WifiDetails): Result<Unit> {
        this.wifiDetails = wifiDetails
        return Result.success(Unit)
    }

    override fun getSavedPhotos(): Result<List<PhotoDownloadInfo>> = Result.success(savedPhotos)

    override fun getSavedMovies(): Result<List<String>> = Result.success(savedMovies)

    override fun deleteMedia(uri: String): Result<Unit> {
        savedPhotos.removeAll { it.uri == uri }
        return Result.success(Unit)
    }

    override suspend fun deleteAll(): Result<Unit> {
        savedPhotos.clear()
        savedMovies.clear()
        return Result.success(Unit)
    }

    override suspend fun getCameraPhotoList(): Result<List<PhotoFile>> = cameraPhotoListResult

    override fun downloadMediaToStorage(photo: PhotoFile): Flow<Result<PhotoDownloadInfo>> = flow {
        downloadFlow.collect { emit(it) }
    }

    override suspend fun shutDownCamera(): Result<Unit> {
        shutdownCalled = true
        return Result.success(Unit)
    }

    override suspend fun saveLatestDownloadedPhotos(photos: List<PhotoFile>): Result<Unit> {
        latestDownloadedPhotos.clear()
        latestDownloadedPhotos.addAll(photos)
        return Result.success(Unit)
    }

    override suspend fun getLatestDownloadedPhotos(): Result<List<PhotoFile>> = Result.success(latestDownloadedPhotos)

    override suspend fun saveSettings(settings: Settings): Result<Unit> {
        this.settings = settings
        return Result.success(Unit)
    }

    override suspend fun getSettings(): Result<Settings> = Result.success(settings)
}
