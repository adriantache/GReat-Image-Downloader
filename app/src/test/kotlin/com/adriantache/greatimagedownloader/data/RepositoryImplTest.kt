package com.adriantache.greatimagedownloader.data

import com.adriantache.greatimagedownloader.data.api.FakeRicohApi
import com.adriantache.greatimagedownloader.data.api.error.CameraException
import com.adriantache.greatimagedownloader.data.api.model.Dir
import com.adriantache.greatimagedownloader.data.api.model.PhotoInfo
import com.adriantache.greatimagedownloader.data.storage.FakeFilesStorage
import com.adriantache.greatimagedownloader.data.storage.FakePreferencesStorage
import com.adriantache.greatimagedownloader.data.storage.FakeWifiStorage
import com.adriantache.greatimagedownloader.data.storage.error.FilesStorageException
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.data.model.WifiDetails
import com.adriantache.greatimagedownloader.domain.model.DomainError
import com.adriantache.greatimagedownloader.domain.model.DomainException
import com.adriantache.greatimagedownloader.domain.model.Settings
import com.adriantache.greatimagedownloader.domain.utils.model.Kbps
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import retrofit2.Response
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

    // Wifi Operations
    @Test
    fun `getWifiDetails returns success when storage works`() {
        wifiStorage.ssid = "SSID"
        wifiStorage.password = "PASS"
        wifiStorage.bssid = "BSSID"

        val result = repository.getWifiDetails()

        assertThat(result.isSuccess).isTrue
        val details = result.getOrNull()
        assertThat(details?.ssid).isEqualTo("SSID")
        assertThat(details?.password).isEqualTo("PASS")
        assertThat(details?.bssid).isEqualTo("BSSID")
    }

    @Test
    fun `saveWifiDetails returns success when storage works`() {
        val details = WifiDetails("SSID", "PASS", "BSSID")

        val result = repository.saveWifiDetails(details)

        assertThat(result.isSuccess).isTrue
        assertThat(wifiStorage.ssid).isEqualTo("SSID")
        assertThat(wifiStorage.password).isEqualTo("PASS")
        assertThat(wifiStorage.bssid).isEqualTo("BSSID")
    }

    // Data Storage Operations
    @Test
    fun `getSavedPhotos returns success when storage works`() {
        val photos = listOf(PhotoDownloadInfo("name", "uri", 100, Kbps(0.0)))
        filesStorage.getSavedPhotosResult = Result.success(photos)

        val result = repository.getSavedPhotos()

        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrNull()).isEqualTo(photos)
    }

    @Test
    fun `getSavedMovies returns success when storage works`() {
        val movies = listOf("movie1", "movie2")
        filesStorage.getSavedMoviesResult = Result.success(movies)

        val result = repository.getSavedMovies()

        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrNull()).isEqualTo(movies)
    }

    @Test
    fun `deleteMedia returns success when storage works`() {
        val result = repository.deleteMedia("uri")
        assertThat(result.isSuccess).isTrue
    }

    @Test
    fun `deleteAll returns success when storage works`() = runTest {
        val result = repository.deleteAll()
        assertThat(result.isSuccess).isTrue
    }

    // Camera Operations
    @Test
    fun `getCameraPhotoList returns success when api works`() = runTest {
        val photoInfo = PhotoInfo(null, null, listOf(Dir("dir", listOf("file"))))
        ricohApi.photosResponse = Response.success(photoInfo)

        val result = repository.getCameraPhotoList()

        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrNull()).hasSize(1)
        assertThat(result.getOrNull()?.first()?.name).isEqualTo("file")
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
    fun `downloadMediaToStorage emits success when everything works`() = runTest {
        val photo = PhotoFile("dir", "name")
        val photoInfo = PhotoDownloadInfo("name", "uri", 100, Kbps(100.0))
        
        val flow = repository.downloadMediaToStorage(photo)
        
        // Use a background task to emit to the fake
        val job = launch {
            filesStorage.savePhotoFlow.emit(Result.success(photoInfo))
        }

        val result = flow.first()
        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrNull()).isEqualTo(photoInfo)
        job.cancel()
    }

    @Test
    fun `downloadMediaToStorage emits failure when camera disconnects`() = runTest {
        val photo = PhotoFile("dir", "name")
        ricohApi.getPhotoShouldThrow = SocketTimeoutException()

        val flow = repository.downloadMediaToStorage(photo)
        
        val result = flow.first()
        assertThat(result.isFailure).isTrue
        val exception = result.exceptionOrNull() as DomainException
        assertThat(exception.domainError).isEqualTo(DomainError.CameraDisconnected)
    }

    @Test
    fun `shutDownCamera returns success when api works`() = runTest {
        val result = repository.shutDownCamera()
        assertThat(result.isSuccess).isTrue
        assertThat(ricohApi.finishCalled).isTrue
    }

    // Settings Operations
    @Test
    fun `saveLatestDownloadedPhotos calls preferencesStorage`() = runTest {
        val photos = listOf(PhotoFile("dir", "name"))
        
        val result = repository.saveLatestDownloadedPhotos(photos)
        
        assertThat(result.isSuccess).isTrue
        assertThat(preferencesStorage.latestPhotos).isEqualTo(photos)
    }

    @Test
    fun `getLatestDownloadedPhotos calls preferencesStorage`() = runTest {
        val photos = listOf(PhotoFile("dir", "name"))
        preferencesStorage.latestPhotos.addAll(photos)
        
        val result = repository.getLatestDownloadedPhotos()
        
        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrNull()).isEqualTo(photos)
    }

    @Test
    fun `saveSettings calls preferencesStorage`() = runTest {
        val settings = Settings(rememberLastDownloadedPhotos = true)
        
        val result = repository.saveSettings(settings)
        
        assertThat(result.isSuccess).isTrue
        assertThat(preferencesStorage.settings).isEqualTo(settings)
    }

    @Test
    fun `getSettings calls preferencesStorage`() = runTest {
        val settings = Settings(rememberLastDownloadedPhotos = true)
        preferencesStorage.settings = settings
        
        val result = repository.getSettings()
        
        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrNull()).isEqualTo(settings)
    }

    @Test
    fun `getCameraPhotoList maps IOException to NetworkError`() = runTest {
        ricohApi.getPhotosShouldThrow = IOException("network")

        val result = repository.getCameraPhotoList()

        assertThat(result.isFailure).isTrue
        val exception = result.exceptionOrNull() as DomainException
        assertThat(exception.domainError).isEqualTo(DomainError.NetworkError)
    }

    @Test
    fun `downloadMediaToStorage maps FilesStorageException StorageFull correctly`() = runTest {
        val photo = PhotoFile("dir", "name")
        val flow = repository.downloadMediaToStorage(photo)
        
        // Use a background task to emit to the fake
        val job = launch {
            filesStorage.savePhotoFlow.emit(Result.failure(FilesStorageException.StorageFull))
        }

        val result = flow.first()
        assertThat(result.isFailure).isTrue
        val exception = result.exceptionOrNull() as DomainException
        assertThat(exception.domainError).isEqualTo(DomainError.StorageFull)
        job.cancel()
    }
}
