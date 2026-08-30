package com.adriantache.greatimagedownloader.data

import com.adriantache.greatimagedownloader.data.api.FakeRicohApi
import com.adriantache.greatimagedownloader.data.storage.FakeFilesStorage
import com.adriantache.greatimagedownloader.data.storage.FakePreferencesStorage
import com.adriantache.greatimagedownloader.data.storage.FakeWifiStorage
import com.adriantache.greatimagedownloader.data.api.error.CameraException
import com.adriantache.greatimagedownloader.data.storage.error.FilesStorageException
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.model.DomainError
import com.adriantache.greatimagedownloader.domain.model.DomainException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryImplTest {
    private val wifiStorage = FakeWifiStorage()
    private val filesStorage = FakeFilesStorage()
    private val preferencesStorage = FakePreferencesStorage()
    private val ricohApi = FakeRicohApi()
    private lateinit var repository: RepositoryImpl

    @Before
    fun setUp() {
        repository = RepositoryImpl(wifiStorage, filesStorage, preferencesStorage, ricohApi)
    }

    @Test
    fun `getCameraPhotoList maps SocketTimeoutException to CameraDisconnected`() = runTest {
        ricohApi.getPhotosShouldThrow = SocketTimeoutException()

        val result = repository.getCameraPhotoList()

        assertThat(result.isFailure).isTrue
        val exception = result.exceptionOrNull() as DomainException
        assertThat(exception.domainError).isEqualTo(DomainError.CameraDisconnected)
    }

    @Test
    fun `downloadMediaToStorage emits failure when camera disconnects`() = runTest {
        val photo = PhotoFile("dir", "name")
        ricohApi.getPhotoShouldThrow = SocketTimeoutException()

        val flow = repository.downloadMediaToStorage(photo)
        
        flow.collect { result ->
            assertThat(result.isFailure).isTrue
            val exception = result.exceptionOrNull() as DomainException
            assertThat(exception.domainError).isEqualTo(DomainError.CameraDisconnected)
        }
    }

    @Test
    fun `getSavedPhotos maps FilesStorageException StorageFull to DomainError StorageFull`() = runTest {
        filesStorage.getSavedPhotosResult = Result.failure(FilesStorageException.StorageFull)

        val result = repository.getSavedPhotos()

        assertThat(result.isFailure).isTrue
        val exception = result.exceptionOrNull() as DomainException
        assertThat(exception.domainError).isEqualTo(DomainError.StorageFull)
    }
}
