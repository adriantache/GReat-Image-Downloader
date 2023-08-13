package com.example.greatimagedownloader.data.storage

import com.example.greatimagedownloader.domain.data.model.PhotoFile
import com.example.greatimagedownloader.domain.model.Settings

interface PreferencesStorage {
    suspend fun saveLatestDownloadedPhotos(photos: List<PhotoFile>)

    suspend fun getLatestDownloadedPhotos(): List<PhotoFile>

    suspend fun saveSettings(settings: Settings)

    suspend fun getSettings(): Settings
}