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
import com.adriantache.greatimagedownloader.domain.data.FakeRepository
import com.adriantache.greatimagedownloader.domain.wifi.FakeWifiUtil
import com.adriantache.greatimagedownloader.service.DataTransferTool
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

        val init = useCase.state.value as States.Init
        init.onInit()
        advanceUntilIdle()

        val reqPerm = useCase.state.value as States.RequestPermissions
        reqPerm.onPermissionsGranted()
        advanceUntilIdle()

        val connectWifi = useCase.state.value as States.ConnectWifi
        connectWifi.onConnect()
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
        dataTransferTool.errorFlow.value = Event(DomainError.NetworkError)
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

        val event = useCase.event.value?.value as Events.ErrorDialog
        
        // Reset count
        wifiUtil.connectCalledCount = 0

        // Act: Click retry
        event.onRetry()
        advanceUntilIdle()

        // Assert
        assertThat(wifiUtil.connectCalledCount).isGreaterThan(0)
    }

    @Test
    fun `onConnectionLost during download phase cleans up incomplete file`() = runTest {
        // Arrange: Put UseCase into DownloadPhotos state
        val photoInfo = PhotoDownloadInfo("name", "uri", 50, com.adriantache.greatimagedownloader.domain.utils.model.Kbps(100.0))
        repository.savedPhotos.add(photoInfo)
        
        useCase.state.value = States.DownloadPhotos(
            downloadedPhotos = listOf(photoInfo),
            currentPhotoNum = 1,
            totalPhotos = 1,
            onStopDownloading = {}
        )

        // Act: Simulate connection lost
        val method = useCase.javaClass.getDeclaredMethod("onConnectionLost")
        method.isAccessible = true
        method.invoke(useCase)
        
        // Assert
        assertThat(repository.savedPhotos).isEmpty() 
        assertThat(useCase.state.value).isInstanceOf(States.Init::class.java)
    }
}
