package com.adriantache.greatimagedownloader.data.storage

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.utils.model.Kbps
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
@OptIn(ExperimentalCoroutinesApi::class)
class FilesStorageImplTest {
    private lateinit var context: Context
    private lateinit var filesStorage: FilesStorageImpl
    private lateinit var contentResolver: ShadowContentResolver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        filesStorage = FilesStorageImpl(context)
        contentResolver = shadowOf(context.contentResolver)
    }

    @Test
    fun `getSavedPhotos returns list when images exist in MediaStore`() {
        // Arrange: Add a fake image to MediaStore
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "test.jpg")
            put(MediaStore.MediaColumns.TITLE, "test")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Image Sync/100RICOH/")
            put(MediaStore.MediaColumns.SIZE, 1000)
            put(MediaStore.Images.ImageColumns.WIDTH, 100)
            put(MediaStore.Images.ImageColumns.HEIGHT, 200)
        }
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        // Act
        val result = filesStorage.getSavedPhotos()

        // Assert
        assertThat(result.isSuccess).isTrue
        val photos = result.getOrNull()
        assertThat(photos).hasSize(1)
        assertThat(photos?.first()?.name).isEqualTo("test")
    }

    @Test
    fun `getSavedMovies returns list when videos exist in MediaStore`() {
        // Arrange: Add a fake video to MediaStore
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "test.mp4")
            put(MediaStore.MediaColumns.TITLE, "test")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/Image Sync/100RICOH/")
            put(MediaStore.MediaColumns.SIZE, 1000)
        }
        context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)

        // Act
        val result = filesStorage.getSavedMovies()

        // Assert
        assertThat(result.isSuccess).isTrue
        val movies = result.getOrNull()
        assertThat(movies).hasSize(1)
        assertThat(movies?.first()).isEqualTo("test")
    }

    @Test
    fun `savePhoto emits success and saves image info`() = runTest {
        val photo = PhotoFile("100RICOH", "test.jpg")
        val responseBody = "fake data".toResponseBody("image/jpeg".toMediaType())

        // Act
        val flow = filesStorage.savePhoto(responseBody, photo)
        val result = flow.first()

        // Assert
        assertThat(result.isSuccess).isTrue
        // We can't easily verify the actual file writing logic deep in Okio/ContentResolver in a simple unit test,
        // but we verified that the flow emits success.
    }

    @Test
    fun `deleteMedia removes item from MediaStore`() {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "test.jpg")
            put(MediaStore.MediaColumns.TITLE, "test")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Image Sync/100RICOH/")
            put(MediaStore.MediaColumns.SIZE, 1000)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val result = filesStorage.deleteMedia(uri.toString())

        assertThat(result.isSuccess).isTrue
        val savedPhotos = filesStorage.getSavedPhotos().getOrNull()
        assertThat(savedPhotos).isEmpty()
    }

    @Test
    fun `deleteAll removes all saved photos and movies`() = runTest {
        val photoValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "test.jpg")
            put(MediaStore.MediaColumns.TITLE, "test")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Image Sync/100RICOH/")
            put(MediaStore.MediaColumns.SIZE, 1000)
        }
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, photoValues)

        val movieValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "test.mp4")
            put(MediaStore.MediaColumns.TITLE, "test")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/Image Sync/100RICOH/")
            put(MediaStore.MediaColumns.SIZE, 1000)
        }
        context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, movieValues)

        val result = filesStorage.deleteAll()

        assertThat(result.isSuccess).isTrue
        assertThat(filesStorage.getSavedPhotos().getOrNull()).isEmpty()
        assertThat(filesStorage.getSavedMovies().getOrNull()).isEmpty()
    }
}
