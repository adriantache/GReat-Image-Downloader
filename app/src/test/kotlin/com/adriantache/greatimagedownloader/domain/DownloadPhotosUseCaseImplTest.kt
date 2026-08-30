package com.adriantache.greatimagedownloader.domain

import android.util.Log
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.data.model.WifiDetails
import com.adriantache.greatimagedownloader.domain.model.DomainError
import com.adriantache.greatimagedownloader.domain.model.DomainException
import com.adriantache.greatimagedownloader.domain.model.Events
import com.adriantache.greatimagedownloader.domain.model.Settings
import com.adriantache.greatimagedownloader.domain.model.States
import com.adriantache.greatimagedownloader.domain.utils.model.Event
import com.adriantache.greatimagedownloader.domain.utils.model.Kbps
import com.adriantache.greatimagedownloader.domain.data.FakeRepository
import com.adriantache.greatimagedownloader.domain.wifi.FakeWifiUtil
import com.adriantache.greatimagedownloader.service.DataTransferTool
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadPhotosUseCaseImplTest {
    private val repository = FakeRepository()
    private val wifiUtil = FakeWifiUtil()
    private val dataTransferTool = DataTransferTool()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var useCase: DownloadPhotosUseCaseImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        useCase = DownloadPhotosUseCaseImpl(
            repository = repository,
            wifiUtil = wifiUtil,
            dataTransferTool = dataTransferTool,
            dispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state is Init`() = runTest {
        assertThat(useCase.state.value).isInstanceOf(States.Init::class.java)
    }

    @Test
    fun `onInit transitions to RequestPermissions`() = runTest {
        val state = useCase.state.value as States.Init
        state.onInit()
        advanceUntilIdle()
        assertThat(useCase.state.value).isInstanceOf(States.RequestPermissions::class.java)
    }

    @Test
    fun `disconnection during synchronization shows error dialog`() = runTest {
        // Arrange
        repository.wifiDetails = WifiDetails("SSID", "PASSWORD", null)
        wifiUtil.connectResult = Pair(true, "BSSID")
        repository.cameraPhotoListResult = Result.failure(DomainException(DomainError.CameraDisconnected))

        (useCase.state.value as States.Init).onInit()
        advanceUntilIdle()

        (useCase.state.value as States.RequestPermissions).onPermissionsGranted()
        advanceUntilIdle()

        (useCase.state.value as States.ConnectWifi).onConnect()
        advanceUntilIdle()

        // Simulate Service connecting successfully
        dataTransferTool.serviceStateFlow.value = DataTransferTool.ServiceState.FETCHING
        advanceUntilIdle()

        // Assert
        assertThat(useCase.state.value).isEqualTo(States.GetPhotos)
        val event = useCase.event.value?.value
        assertThat(event).isInstanceOf(Events.ErrorDialog::class.java)
        val errorDialog = event as Events.ErrorDialog
        assertThat(errorDialog.error).isEqualTo(DomainError.CameraDisconnected)
    }

    @Test
    fun `disconnection during download phase shows error dialog`() = runTest {
        // Act: Emit error via dataTransferTool
        dataTransferTool.errorFlow.value = Event(DomainError.NetworkError as DomainError)
        advanceUntilIdle()

        // Assert
        val event = useCase.event.value?.value
        assertThat(event).isInstanceOf(Events.ErrorDialog::class.java)
        val errorDialog = event as Events.ErrorDialog
        assertThat(errorDialog.error).isEqualTo(DomainError.NetworkError)
    }

    @Test
    fun `retry in error dialog re-attempts connection`() = runTest {
        // Arrange
        repository.wifiDetails = WifiDetails("SSID", "PASSWORD", null)
        wifiUtil.connectResult = Pair(true, "BSSID")
        repository.cameraPhotoListResult = Result.failure(DomainException(DomainError.CameraDisconnected))

        (useCase.state.value as States.Init).onInit()
        advanceUntilIdle()
        (useCase.state.value as States.RequestPermissions).onPermissionsGranted()
        advanceUntilIdle()
        (useCase.state.value as States.ConnectWifi).onConnect()
        advanceUntilIdle()
        
        // Simulate Service connecting
        dataTransferTool.serviceStateFlow.value = DataTransferTool.ServiceState.FETCHING
        advanceUntilIdle()

        val eventValue = useCase.event.value?.value as Events.ErrorDialog
        
        // Reset count
        wifiUtil.connectCalledCount = 0

        // Act: Click retry
        eventValue.onRetry()
        advanceUntilIdle()

        // Assert
        assertThat(wifiUtil.connectCalledCount).isGreaterThan(0)
    }

    @Test
    fun `storage full during download shows error dialog`() = runTest {
        // Arrange
        dataTransferTool.errorFlow.value = Event(DomainError.StorageFull as DomainError)
        advanceUntilIdle()

        // Assert
        val eventValue = useCase.event.value?.value
        assertThat(eventValue).isInstanceOf(Events.ErrorDialog::class.java)
        val errorDialog = eventValue as Events.ErrorDialog
        assertThat(errorDialog.error).isEqualTo(DomainError.StorageFull)
    }

    @Test
    fun `successful connection and media fetch transitions to SelectFolders if multiple folders`() = runTest {
        // Arrange
        repository.wifiDetails = WifiDetails("SSID", "PASSWORD", null)
        wifiUtil.connectResult = Pair(true, "BSSID")
        repository.cameraPhotoListResult = Result.success(listOf(
            PhotoFile("folder1", "photo1.jpg"),
            PhotoFile("folder2", "photo2.jpg")
        ))

        // Act
        (useCase.state.value as States.Init).onInit()
        advanceUntilIdle()
        (useCase.state.value as States.RequestPermissions).onPermissionsGranted()
        advanceUntilIdle()
        (useCase.state.value as States.ConnectWifi).onConnect()
        advanceUntilIdle()

        // Simulate Service connected
        dataTransferTool.serviceStateFlow.value = DataTransferTool.ServiceState.FETCHING
        advanceUntilIdle()

        // Assert
        assertThat(useCase.state.value).isInstanceOf(States.SelectFolders::class.java)
    }

    @Test
    fun `selecting folders triggers download event`() = runTest {
        // Arrange
        repository.wifiDetails = WifiDetails("SSID", "PASSWORD", null)
        wifiUtil.connectResult = Pair(true, "BSSID")
        val photos = listOf(
            PhotoFile("folder1", "photo1.jpg"),
            PhotoFile("folder2", "photo2.jpg")
        )
        repository.cameraPhotoListResult = Result.success(photos)

        (useCase.state.value as States.Init).onInit()
        advanceUntilIdle()
        (useCase.state.value as States.RequestPermissions).onPermissionsGranted()
        advanceUntilIdle()
        (useCase.state.value as States.ConnectWifi).onConnect()
        advanceUntilIdle()

        // Simulate Service connected
        dataTransferTool.serviceStateFlow.value = DataTransferTool.ServiceState.FETCHING
        advanceUntilIdle()

        val selectFolders = useCase.state.value as States.SelectFolders
        
        // Act
        selectFolders.onFoldersSelect(listOf("folder1"))
        advanceUntilIdle()

        // Assert
        val eventValue = useCase.event.value?.value
        assertThat(eventValue).isInstanceOf(Events.DownloadPhotosWithService::class.java)
        val downloadEvent = eventValue as Events.DownloadPhotosWithService
        assertThat(downloadEvent.photosToDownload).hasSize(1)
        assertThat(downloadEvent.photosToDownload.first().directory).isEqualTo("folder1")
    }

    @Test
    fun `remember last downloaded photos setting filters available media`() = runTest {
        // Arrange
        repository.settings = Settings(rememberLastDownloadedPhotos = true)
        repository.latestDownloadedPhotos = mutableListOf(PhotoFile("folder1", "photo1.jpg"))
        
        val availablePhotos = listOf(
            PhotoFile("folder1", "photo1.jpg"),
            PhotoFile("folder1", "photo2.jpg")
        )
        repository.cameraPhotoListResult = Result.success(availablePhotos)

        // Act: trigger flow
        (useCase.state.value as States.Init).onInit()
        advanceUntilIdle()
        (useCase.state.value as States.RequestPermissions).onPermissionsGranted()
        advanceUntilIdle()
        
        repository.wifiDetails = WifiDetails("SSID", "PASS", "BSSID")
        (useCase.state.value as States.ConnectWifi).onConnect()
        advanceUntilIdle()
        
        // Simulate Service connected
        dataTransferTool.serviceStateFlow.value = DataTransferTool.ServiceState.FETCHING
        advanceUntilIdle()

        // Assert
        val event = useCase.event.value?.value
        assertThat(event).isInstanceOf(Events.DownloadPhotosWithService::class.java)
        val downloadEvent = event as Events.DownloadPhotosWithService
        assertThat(downloadEvent.photosToDownload).hasSize(1)
        assertThat(downloadEvent.photosToDownload.first().name).isEqualTo("photo2.jpg")
    }

    @Test
    fun `onDownloadFinished transitions to Init and emits SuccessfulDownload`() = runTest {
        // Act
        dataTransferTool.downloadFinishedFlow.value = Event(Unit)
        advanceUntilIdle()

        // Assert
        assertThat(useCase.state.value).isInstanceOf(States.Init::class.java)
        assertThat(useCase.event.value?.value).isInstanceOf(Events.SuccessfulDownload::class.java)
    }

    @Test
    fun `wifi connection retries 5 times before failing`() = runTest {
        // Arrange
        wifiUtil.connectResult = Pair(false, null)
        repository.wifiDetails = WifiDetails("SSID", "PASSWORD", null)
        
        (useCase.state.value as States.Init).onInit()
        advanceUntilIdle()
        (useCase.state.value as States.RequestPermissions).onPermissionsGranted()
        advanceUntilIdle()
        
        // Ensure we are in ConnectWifi state
        assertThat(useCase.state.value).isInstanceOf(States.ConnectWifi::class.java)
        
        // Act
        (useCase.state.value as States.ConnectWifi).onConnect()
        
        // Move time forward for 5 attempts with 20s delay
        advanceUntilIdle()

        // Assert
        assertThat(wifiUtil.connectCalledCount).isEqualTo(5)
        // Should eventually transition to ConnectWifi with isHardTimeout
        assertThat(useCase.state.value).isInstanceOf(States.ConnectWifi::class.java)
        assertThat((useCase.state.value as States.ConnectWifi).isHardTimeout).isTrue
    }

    @Test
    fun `onStopDownloading transitions to StoppingDownload state`() = runTest {
        // Act: Call private onStopDownloading
        val method = useCase.javaClass.getDeclaredMethod("onStopDownloading")
        method.isAccessible = true
        method.invoke(useCase)
        
        // Assert
        assertThat(useCase.state.value).isInstanceOf(States.StoppingDownload::class.java)
        assertThat(useCase.event.value?.value).isEqualTo(Events.StopDownload)
    }

    @Test
    fun `settings screen allows deleting all photos`() = runTest {
        // Arrange
        repository.settings = Settings()
        
        // Act: Open settings
        useCase.state.value = States.ConnectWifi(false, {false}, {}, {}, ::openSettings)
        val connectWifi = useCase.state.value as States.ConnectWifi
        connectWifi.onAdjustSettings()
        advanceUntilIdle()
        
        val changeSettings = useCase.state.value as States.ChangeSettings
        changeSettings.onDeleteAllPhotos()
        advanceUntilIdle()
        
        // Assert
        val eventValue = useCase.event.value?.value
        assertThat(eventValue).isInstanceOf(Events.ConfirmDeleteAllPhotos::class.java)
    }
    
    private fun openSettings() {
        val method = useCase.javaClass.getDeclaredMethod("openSettings")
        method.isAccessible = true
        method.invoke(useCase)
    }
}
