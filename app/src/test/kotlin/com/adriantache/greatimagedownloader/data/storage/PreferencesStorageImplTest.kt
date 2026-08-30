package com.adriantache.greatimagedownloader.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.model.Settings
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreferencesStorageImplTest {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var preferencesStorage: PreferencesStorageImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sharedPreferences = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        preferencesStorage = PreferencesStorageImpl(sharedPreferences)
    }

    @Test
    fun `getLatestDownloadedPhotos returns empty list initially`() = runTest {
        val result = preferencesStorage.getLatestDownloadedPhotos()
        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrNull()).isEmpty()
    }

    @Test
    fun `saveLatestDownloadedPhotos and getLatestDownloadedPhotos work correctly`() = runTest {
        val photos = listOf(PhotoFile("dir1", "photo1.jpg"), PhotoFile("dir2", "photo2.jpg"))
        preferencesStorage.saveLatestDownloadedPhotos(photos)
        
        val result = preferencesStorage.getLatestDownloadedPhotos()
        assertThat(result.getOrNull()).isEqualTo(photos)
    }

    @Test
    fun `getSettings returns default Settings initially`() = runTest {
        val result = preferencesStorage.getSettings()
        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrNull()).isEqualTo(Settings())
    }

    @Test
    fun `saveSettings and getSettings work correctly`() = runTest {
        val settings = Settings(rememberLastDownloadedPhotos = true)
        preferencesStorage.saveSettings(settings)
        
        val result = preferencesStorage.getSettings()
        assertThat(result.getOrNull()).isEqualTo(settings)
    }
}
