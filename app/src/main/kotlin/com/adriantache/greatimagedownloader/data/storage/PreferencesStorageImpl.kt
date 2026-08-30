package com.adriantache.greatimagedownloader.data.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import com.adriantache.greatimagedownloader.data.storage.error.PreferencesStorageException
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.model.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val LATEST_PHOTOS_KEY = "LATEST_PHOTOS_KEY"
private const val SETTINGS_KEY = "SETTINGS_KEY"

class PreferencesStorageImpl(
    private val sharedPreferences: SharedPreferences,
) : PreferencesStorage {
    override suspend fun saveLatestDownloadedPhotos(photos: List<PhotoFile>): Result<Unit> = runCatching {
        val photosJson = Json.encodeToString(photos)

        sharedPreferences.edit {
            putString(LATEST_PHOTOS_KEY, photosJson)
        }
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(PreferencesStorageException.Unknown(it)) }
    )

    override suspend fun getLatestDownloadedPhotos(): Result<List<PhotoFile>> = runCatching {
        val json = sharedPreferences.getString(LATEST_PHOTOS_KEY, null)
            ?: return@runCatching emptyList()

        Json.decodeFromString<List<PhotoFile>>(json)
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = {
            val error = if (it is IllegalArgumentException) {
                PreferencesStorageException.SerializationError(it)
            } else {
                PreferencesStorageException.Unknown(it)
            }
            Result.failure(error)
        }
    )

    override suspend fun saveSettings(settings: Settings): Result<Unit> = runCatching {
        val json = Json.encodeToString(settings)

        sharedPreferences.edit {
            putString(SETTINGS_KEY, json)
        }
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(PreferencesStorageException.Unknown(it)) }
    )

    override suspend fun getSettings(): Result<Settings> = runCatching {
        val json = sharedPreferences.getString(SETTINGS_KEY, null) ?: return@runCatching Settings()

        Json.decodeFromString<Settings>(json)
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = {
            val error = if (it is IllegalArgumentException) {
                PreferencesStorageException.SerializationError(it)
            } else {
                PreferencesStorageException.Unknown(it)
            }
            Result.failure(error)
        }
    )
}
