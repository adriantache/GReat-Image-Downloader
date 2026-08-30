package com.adriantache.greatimagedownloader.data.storage

import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.model.Settings

class FakePreferencesStorage : PreferencesStorage {
    var latestPhotos = mutableListOf<PhotoFile>()
    var settings = Settings()
    
    var saveLatestPhotosResult: Result<Unit> = Result.success(Unit)
    var getLatestPhotosResult: Result<List<PhotoFile>> = Result.success(emptyList())
    var saveSettingsResult: Result<Unit> = Result.success(Unit)
    var getSettingsResult: Result<Settings> = Result.success(Settings())

    override suspend fun saveLatestDownloadedPhotos(photos: List<PhotoFile>): Result<Unit> {
        latestPhotos.clear()
        latestPhotos.addAll(photos)
        return saveLatestPhotosResult
    }

    override suspend fun getLatestDownloadedPhotos(): Result<List<PhotoFile>> = getLatestPhotosResult.map { latestPhotos }

    override suspend fun saveSettings(settings: Settings): Result<Unit> {
        this.settings = settings
        return saveSettingsResult
    }

    override suspend fun getSettings(): Result<Settings> = getSettingsResult.map { settings }
}
