package com.adriantache.greatimagedownloader.domain

import android.util.Log
import com.adriantache.greatimagedownloader.domain.ble.FakeBleUtil
import com.adriantache.greatimagedownloader.domain.data.FakeRepository
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.data.model.WifiDetails
import com.adriantache.greatimagedownloader.domain.model.CameraStatus
import com.adriantache.greatimagedownloader.domain.model.DomainError
import com.adriantache.greatimagedownloader.domain.model.DomainException
import com.adriantache.greatimagedownloader.domain.model.Events
import com.adriantache.greatimagedownloader.domain.model.Settings
import com.adriantache.greatimagedownloader.domain.model.States
import com.adriantache.greatimagedownloader.domain.utils.model.Event
import com.adriantache.greatimagedownloader.domain.utils.model.Kbps
import com.adriantache.greatimagedownloader.domain.wifi.FakeWifiUtil
import com.adriantache.greatimagedownloader.service.DataTransferTool
import com.adriantache.greatimagedownloader.service.DataTransferTool.ServiceState
import com.adriantache.greatimagedownloader.service.DataTransferTool.WidgetState
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
    private val bleUtil = FakeBleUtil()
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
            bleUtil = bleUtil,
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
    fun `error during download deletes partially downloaded files`() = runTest {
        // Arrange: Emit a partially downloaded photo info
        dataTransferTool.imageFlow.value = PhotoDownloadInfo("photo1.jpg", "content://media/1", 50, Kbps(1.0))
        advanceUntilIdle()

        // Act: Emit error via dataTransferTool
        dataTransferTool.errorFlow.value = Event(DomainError.NetworkError as DomainError)
        advanceUntilIdle()

        // Assert
        assertThat(repository.deletedMediaUris).contains("content://media/1")
    }

    @Test
    fun `rapid consecutive emitImageProgress calls do not conflate or lose 100 percent completion`() = runTest {
        // Arrange: Emit photo1 50%, photo1 100%, and photo2 10% synchronously before dispatcher runs
        dataTransferTool.emitImageProgress(PhotoDownloadInfo("photo1.jpg", "content://media/1", 50, Kbps(100.0)))
        dataTransferTool.emitImageProgress(PhotoDownloadInfo("photo1.jpg", "content://media/1", 100, Kbps(100.0), isLandscape = true))
        dataTransferTool.emitImageProgress(PhotoDownloadInfo("photo2.jpg", "content://media/2", 10, Kbps(100.0)))
        advanceUntilIdle()

        // Act: Emit error while photo2 is at 10%
        dataTransferTool.errorFlow.value = Event(DomainError.NetworkError as DomainError)
        advanceUntilIdle()

        // Assert: Only incomplete photo2 is deleted; completed photo1 is preserved
        assertThat(repository.deletedMediaUris).containsExactly("content://media/2")
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
        repository.cameraPhotoListResult = Result.success(
            listOf(
                PhotoFile("folder1", "photo1.jpg"),
                PhotoFile("folder2", "photo2.jpg")
            )
        )

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
        assertThat(repository.pingCalledCount).isGreaterThan(0)
    }

    @Test
    fun `camera status macAddress is saved as fallback BSSID when wifi BSSID is null`() = runTest {
        // Arrange
        repository.wifiDetails = WifiDetails("SSID", "PASSWORD", null)
        wifiUtil.connectResult = Pair(true, null) // Android redacted BSSID
        repository.cameraStatusResult = Result.success(
            CameraStatus(model = "RICOH GR III", batteryLevel = 80, macAddress = "11:22:33:44:55:66")
        )
        repository.cameraPhotoListResult = Result.success(
            listOf(PhotoFile("100RICOH", "R0001234.JPG"))
        )

        // Act
        (useCase.state.value as States.Init).onInit()
        advanceUntilIdle()
        (useCase.state.value as States.RequestPermissions).onPermissionsGranted()
        advanceUntilIdle()
        (useCase.state.value as States.ConnectWifi).onConnect()
        advanceUntilIdle()

        // Assert
        assertThat(repository.wifiDetails.bssid).isEqualTo("11:22:33:44:55:66")
        assertThat(bleUtil.wakeCalledCount).isGreaterThan(0)
    }

    @Test
    fun `selecting folders triggers download event and populates pendingPhotosToDownload`() = runTest {
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
        assertThat(dataTransferTool.pendingPhotosToDownload).containsExactly(PhotoFile("folder1", "photo1.jpg"))
    }

    @Test
    fun `selecting empty folder list shuts down camera and returns to ConnectWifi`() = runTest {
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

        // Act: Select empty list (cancel)
        selectFolders.onFoldersSelect(emptyList())
        advanceUntilIdle()

        // Assert
        assertThat(repository.shutdownCalled).isTrue
        assertThat(wifiUtil.cleanupCalledCount).isGreaterThan(0)
        assertThat(useCase.state.value).isInstanceOf(States.ConnectWifi::class.java)
    }

    @Test
    fun `zero new photos shuts down camera resets service and emits NoNewPhotosToDownload`() = runTest {
        // Arrange: Camera has only already-saved photos
        repository.wifiDetails = WifiDetails("SSID", "PASSWORD", "BSSID")
        wifiUtil.connectResult = Pair(true, "BSSID")
        repository.savedPhotos = mutableListOf(PhotoDownloadInfo("R0001234.JPG", "", 100, Kbps(0.0)))
        repository.cameraPhotoListResult = Result.success(
            listOf(PhotoFile("100RICOH", "R0001234.JPG"))
        )

        (useCase.state.value as States.Init).onInit()
        advanceUntilIdle()
        (useCase.state.value as States.RequestPermissions).onPermissionsGranted()
        advanceUntilIdle()

        // Act
        (useCase.state.value as States.ConnectWifi).onConnect()
        advanceUntilIdle()

        // Assert
        assertThat(repository.shutdownCalled).isTrue
        assertThat(bleUtil.disconnectCalledCount).isGreaterThan(0)
        assertThat(wifiUtil.cleanupCalledCount).isGreaterThan(0)
        assertThat(dataTransferTool.serviceStateFlow.value).isEqualTo(ServiceState.IDLE)
        assertThat(dataTransferTool.widgetStateFlow.value).isEqualTo(WidgetState.Done(0))
        assertThat(useCase.state.value).isInstanceOf(States.ConnectWifi::class.java)
        assertThat(useCase.event.value?.value).isEqualTo(Events.NoNewPhotosToDownload)
    }

    @Test
    fun `cancelConnection during wifi retry loop aborts retries and resets to idle ConnectWifi`() = runTest {
        // Arrange: Wi-Fi fails so retry loop stays active
        repository.wifiDetails = WifiDetails("SSID", "PASSWORD", null)
        wifiUtil.connectResult = Pair(false, null)

        (useCase.state.value as States.Init).onInit()
        advanceUntilIdle()
        (useCase.state.value as States.RequestPermissions).onPermissionsGranted()
        advanceUntilIdle()

        // Start connecting (runs first attempt, then delays 3s before attempt 2)
        (useCase.state.value as States.ConnectWifi).onConnect()
        advanceTimeBy(1000)

        val connectingState = useCase.state.value as States.ConnectWifi
        assertThat(connectingState.isConnecting).isTrue
        assertThat(wifiUtil.connectCalledCount).isEqualTo(1)

        // Act: User presses Back while connecting
        connectingState.onCancelConnection()
        advanceUntilIdle()

        // Assert: Retries stopped after 1 attempt, service/widget reset to idle, state is idle ConnectWifi
        assertThat(wifiUtil.connectCalledCount).isEqualTo(1)
        assertThat(bleUtil.disconnectCalledCount).isGreaterThan(0)
        assertThat(wifiUtil.cleanupCalledCount).isGreaterThan(0)
        assertThat(dataTransferTool.serviceStateFlow.value).isEqualTo(ServiceState.IDLE)
        assertThat(dataTransferTool.widgetStateFlow.value).isEqualTo(WidgetState.Idle)
        val finalState = useCase.state.value as States.ConnectWifi
        assertThat(finalState.isConnecting).isFalse
        assertThat(finalState.isHardTimeout).isFalse
    }

    @Test
    fun `startBackgroundSync skips folder selection and starts download directly`() = runTest {
        // Arrange: Multiple folders present on camera
        repository.wifiDetails = WifiDetails("SSID", "PASSWORD", "BSSID")
        wifiUtil.connectResult = Pair(true, "BSSID")
        repository.cameraPhotoListResult = Result.success(
            listOf(
                PhotoFile("100RICOH", "R0001001.JPG"),
                PhotoFile("101RICOH", "R0002001.JPG"),
            )
        )

        // Act: Trigger headless background sync from widget
        useCase.startBackgroundSync()
        advanceUntilIdle()

        // Assert: Bypassed SelectFolders and proceeded straight to DOWNLOADING
        assertThat(dataTransferTool.serviceStateFlow.value).isEqualTo(ServiceState.DOWNLOADING)
        assertThat(dataTransferTool.pendingPhotosToDownload).hasSize(2)
        assertThat(dataTransferTool.widgetStateFlow.value).isEqualTo(WidgetState.Downloading(0, 2))
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
    fun `remember last downloaded photos allows new folders`() = runTest {
        // Arrange
        repository.settings = Settings(rememberLastDownloadedPhotos = true)
        repository.latestDownloadedPhotos = mutableListOf(PhotoFile("folder1", "photo1.jpg"))

        val availablePhotos = listOf(
            PhotoFile("folder1", "photo1.jpg"),
            PhotoFile("folder2", "photo1.jpg") // New folder
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
        assertThat(downloadEvent.photosToDownload.first().directory).isEqualTo("folder2")
        assertThat(downloadEvent.photosToDownload.first().name).isEqualTo("photo1.jpg")
    }

    @Test
    fun `saved photos filter excludes files already on device regardless of setting`() = runTest {
        // Arrange
        repository.settings = Settings(rememberLastDownloadedPhotos = false)
        repository.savedPhotos = mutableListOf(PhotoDownloadInfo("photo1.jpg", "", 100, Kbps(0.0)))

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
    fun `saved JPEG does not prevent downloading matching RAW DNG file when RAW setting enabled`() = runTest {
        // Arrange
        repository.settings = Settings(rememberLastDownloadedPhotos = false, downloadRawFiles = true)
        repository.savedPhotos = mutableListOf(PhotoDownloadInfo("R0001234.JPG", "", 100, Kbps(0.0)))

        val availablePhotos = listOf(
            PhotoFile("100RICOH", "R0001234.JPG"),
            PhotoFile("100RICOH", "R0001234.DNG"),
        )
        repository.cameraPhotoListResult = Result.success(availablePhotos)

        // Act
        (useCase.state.value as States.Init).onInit()
        advanceUntilIdle()
        (useCase.state.value as States.RequestPermissions).onPermissionsGranted()
        advanceUntilIdle()
        repository.wifiDetails = WifiDetails("SSID", "PASSWORD", "BSSID")
        (useCase.state.value as States.ConnectWifi).onConnect()
        advanceUntilIdle()
        dataTransferTool.serviceStateFlow.value = DataTransferTool.ServiceState.FETCHING
        advanceUntilIdle()

        // Assert
        val event = useCase.event.value?.value as Events.DownloadPhotosWithService
        assertThat(event.photosToDownload).containsExactly(PhotoFile("100RICOH", "R0001234.DNG"))
    }

    @Test
    fun `RAW DNG files are skipped by default when downloadRawFiles setting is false`() = runTest {
        // Arrange
        repository.settings = Settings(rememberLastDownloadedPhotos = false, downloadRawFiles = false)
        val availablePhotos = listOf(
            PhotoFile("100RICOH", "R0001234.JPG"),
            PhotoFile("100RICOH", "R0001234.DNG"),
        )
        repository.cameraPhotoListResult = Result.success(availablePhotos)

        // Act
        (useCase.state.value as States.Init).onInit()
        advanceUntilIdle()
        (useCase.state.value as States.RequestPermissions).onPermissionsGranted()
        advanceUntilIdle()
        repository.wifiDetails = WifiDetails("SSID", "PASSWORD", "BSSID")
        (useCase.state.value as States.ConnectWifi).onConnect()
        advanceUntilIdle()
        dataTransferTool.serviceStateFlow.value = DataTransferTool.ServiceState.FETCHING
        advanceUntilIdle()

        // Assert
        val event = useCase.event.value?.value as Events.DownloadPhotosWithService
        assertThat(event.photosToDownload).containsExactly(PhotoFile("100RICOH", "R0001234.JPG"))
    }

    @Test
    fun `video files are skipped when downloadVideos setting is false`() = runTest {
        // Arrange
        repository.settings = Settings(rememberLastDownloadedPhotos = false, downloadVideos = false)
        val availablePhotos = listOf(
            PhotoFile("100RICOH", "R0001234.JPG"),
            PhotoFile("100RICOH", "R0001235.MOV"),
        )
        repository.cameraPhotoListResult = Result.success(availablePhotos)

        // Act
        (useCase.state.value as States.Init).onInit()
        advanceUntilIdle()
        (useCase.state.value as States.RequestPermissions).onPermissionsGranted()
        advanceUntilIdle()
        repository.wifiDetails = WifiDetails("SSID", "PASSWORD", "BSSID")
        (useCase.state.value as States.ConnectWifi).onConnect()
        advanceUntilIdle()
        dataTransferTool.serviceStateFlow.value = DataTransferTool.ServiceState.FETCHING
        advanceUntilIdle()

        // Assert
        val event = useCase.event.value?.value as Events.DownloadPhotosWithService
        assertThat(event.photosToDownload).containsExactly(PhotoFile("100RICOH", "R0001234.JPG"))
    }

    @Test
    fun `onDownloadFinished transitions to ConnectWifi and emits SuccessfulDownload`() = runTest {
        // Act
        dataTransferTool.downloadFinishedFlow.value = Event(Unit)
        advanceUntilIdle()

        // Assert
        assertThat(wifiUtil.cleanupCalledCount).isGreaterThan(0)
        assertThat(useCase.state.value).isInstanceOf(States.ConnectWifi::class.java)
        assertThat(useCase.event.value?.value).isInstanceOf(Events.SuccessfulDownload::class.java)
    }

    @Test
    fun `onDownloadFinished merges latest downloaded photos across directories`() = runTest {
        // Arrange: folder1 already has a saved latest photo marker
        repository.latestDownloadedPhotos = mutableListOf(PhotoFile("100RICOH", "R0000100.JPG"))
        repository.settings = Settings(rememberLastDownloadedPhotos = false)
        repository.wifiDetails = WifiDetails("SSID", "PASSWORD", "BSSID")
        repository.cameraPhotoListResult = Result.success(
            listOf(PhotoFile("101RICOH", "R0000200.JPG"))
        )

        (useCase.state.value as States.Init).onInit()
        advanceUntilIdle()
        (useCase.state.value as States.RequestPermissions).onPermissionsGranted()
        advanceUntilIdle()
        (useCase.state.value as States.ConnectWifi).onConnect()
        advanceUntilIdle()
        dataTransferTool.serviceStateFlow.value = DataTransferTool.ServiceState.FETCHING
        advanceUntilIdle()

        // Emit completed download for 101RICOH
        dataTransferTool.imageFlow.value = PhotoDownloadInfo("R0000200.JPG", "content://media/200", 100, Kbps(500.0))
        advanceUntilIdle()
        dataTransferTool.downloadFinishedFlow.value = Event(Unit)
        advanceUntilIdle()

        // Assert both 100RICOH and 101RICOH markers are preserved
        assertThat(repository.latestDownloadedPhotos).containsExactlyInAnyOrder(
            PhotoFile("100RICOH", "R0000100.JPG"),
            PhotoFile("101RICOH", "R0000200.JPG"),
        )
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

        // Move time forward for 5 attempts
        advanceUntilIdle()

        // Assert
        assertThat(wifiUtil.connectCalledCount).isEqualTo(5)
        // Should eventually transition to ConnectWifi with isHardTimeout
        assertThat(useCase.state.value).isInstanceOf(States.ConnectWifi::class.java)
        assertThat((useCase.state.value as States.ConnectWifi).isHardTimeout).isTrue
        assertThat((useCase.state.value as States.ConnectWifi).isConnecting).isFalse
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
        useCase.state.value = States.ConnectWifi(false, { false }, {}, {}, ::openSettings)
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

    @Test
    fun `triggerAutoConnect starts wifi connection automatically once permissions are granted`() = runTest {
        // Arrange
        repository.wifiDetails = WifiDetails("SSID", "PASSWORD", "BSSID")
        wifiUtil.connectResult = Pair(true, "BSSID")

        // Act: Widget triggers auto-connect on cold start before permissions check completes
        useCase.triggerAutoConnect()
        (useCase.state.value as States.Init).onInit()
        advanceUntilIdle()
        (useCase.state.value as States.RequestPermissions).onPermissionsGranted()
        advanceUntilIdle()

        // Assert: Connection started automatically without requiring a manual onConnect() tap
        assertThat(wifiUtil.connectCalledCount).isEqualTo(1)
    }

    @Test
    fun `triggerAutoConnect starts wifi connection immediately when already in ConnectWifi state`() = runTest {
        // Arrange
        repository.wifiDetails = WifiDetails("SSID", "PASSWORD", "BSSID")
        wifiUtil.connectResult = Pair(true, "BSSID")

        (useCase.state.value as States.Init).onInit()
        advanceUntilIdle()
        (useCase.state.value as States.RequestPermissions).onPermissionsGranted()
        advanceUntilIdle()
        assertThat(useCase.state.value).isInstanceOf(States.ConnectWifi::class.java)

        // Act: Widget tapped while app is already sitting on ConnectWifi screen
        useCase.triggerAutoConnect()
        advanceUntilIdle()

        // Assert
        assertThat(wifiUtil.connectCalledCount).isEqualTo(1)
    }

    private fun openSettings() {
        val method = useCase.javaClass.getDeclaredMethod("openSettings")
        method.isAccessible = true
        method.invoke(useCase)
    }
}
