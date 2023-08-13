package com.example.greatimagedownloader.data.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.greatimagedownloader.domain.data.model.PhotoFile
import com.example.greatimagedownloader.domain.model.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val LATEST_PHOTOS_KEY = "LATEST_PHOTOS_KEY"
private const val SETTINGS_KEY = "SETTINGS_KEY"

class PreferencesStorageImpl(
    private val sharedPreferences: SharedPreferences,
) : PreferencesStorage {
    override suspend fun saveLatestDownloadedPhotos(photos: List<PhotoFile>) {
        val photosJson = Json.encodeToString(photos)

        sharedPreferences.edit {
            putString(LATEST_PHOTOS_KEY, photosJson)
        }
    }

    override suspend fun getLatestDownloadedPhotos(): List<PhotoFile> {
        val json = sharedPreferences.getString(LATEST_PHOTOS_KEY, null)
            ?: return emptyList()

        return Json.decodeFromString(json)
    }

    override suspend fun saveSettings(settings: Settings) {
        val json = Json.encodeToString(settings)

        sharedPreferences.edit {
            putString(SETTINGS_KEY, json)
        }
    }

    override suspend fun getSettings(): Settings {
        val json = sharedPreferences.getString(SETTINGS_KEY, null) ?: return Settings()

        return Json.decodeFromString(json)
    }
}