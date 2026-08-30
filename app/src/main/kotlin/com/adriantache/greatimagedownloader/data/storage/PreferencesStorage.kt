package com.adriantache.greatimagedownloader.data.storage

import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.model.Settings

interface PreferencesStorage {
    suspend fun saveLatestDownloadedPhotos(photos: List<PhotoFile>): Result<Unit>

    suspend fun getLatestDownloadedPhotos(): Result<List<PhotoFile>>

    suspend fun saveSettings(settings: Settings): Result<Unit>

    suspend fun getSettings(): Result<Settings>
}
