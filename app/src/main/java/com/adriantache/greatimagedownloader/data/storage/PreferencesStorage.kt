package com.adriantache.greatimagedownloader.data.storage

import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.model.Settings

interface PreferencesStorage {
    suspend fun saveLatestDownloadedPhotos(photos: List<PhotoFile>)

    suspend fun getLatestDownloadedPhotos(): List<PhotoFile>

    suspend fun saveSettings(settings: Settings)

    suspend fun getSettings(): Settings
}
