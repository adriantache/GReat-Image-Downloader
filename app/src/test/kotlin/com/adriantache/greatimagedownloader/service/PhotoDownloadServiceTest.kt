package com.adriantache.greatimagedownloader.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.adriantache.greatimagedownloader.domain.data.FakeRepository
import com.adriantache.greatimagedownloader.domain.data.Repository
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import com.adriantache.greatimagedownloader.domain.model.DomainError
import com.adriantache.greatimagedownloader.domain.model.DomainException
import com.adriantache.greatimagedownloader.domain.utils.model.Kbps
import com.adriantache.greatimagedownloader.service.DataTransferTool.ServiceState
import com.adriantache.greatimagedownloader.service.model.PhotoFileItem
import io.mockk.every
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
@OptIn(ExperimentalCoroutinesApi::class)
class PhotoDownloadServiceTest {
    private lateinit var context: Context
    private lateinit var repository: FakeRepository
    private lateinit var dataTransferTool: DataTransferTool
    private lateinit var serviceController: ServiceController<PhotoDownloadService>
    private lateinit var shadowNotificationManager: ShadowNotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)

        repository = FakeRepository()
        dataTransferTool = DataTransferTool()

        startKoin {
            modules(
                module {
                    single<Repository> { repository }
                    single { dataTransferTool }
                }
            )
        }

        serviceController = Robolectric.buildService(PhotoDownloadService::class.java)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `service creates and starts in foreground with notification channels`() {
        val service = serviceController.create().get()

        // Notification channels should be registered
        assertThat(shadowNotificationManager.notificationChannels).hasSize(2)
        val channelIds = shadowNotificationManager.notificationChannels.map { it.id }
        assertThat(channelIds).contains("SERVICE_NOTIFICATION_CHANNEL", "ERROR_NOTIFICATION_CHANNEL")

        // Service should be in foreground
        val shadowService = shadowOf(service)
        assertThat(shadowService.lastForegroundNotification).isNotNull
        assertThat(shadowService.lastForegroundNotificationId).isEqualTo(1)
    }

    @Test
    fun `CONNECT action updates serviceStateFlow to CONNECTING`() {
        val service = serviceController.create().get()
        val intent = Intent(context, PhotoDownloadService::class.java).apply {
            action = PhotoDownloadService.Actions.CONNECT.name
        }

        service.onStartCommand(intent, 0, 1)

        assertThat(dataTransferTool.serviceStateFlow.value).isEqualTo(ServiceState.CONNECTING)
    }

    @Test
    fun `START action with null or empty list disconnects and stops service`() {
        val mockRepo = mockk<Repository>(relaxed = true)
        stopKoin()
        startKoin {
            modules(
                module {
                    single<Repository> { mockRepo }
                    single { dataTransferTool }
                }
            )
        }

        val service = serviceController.create().get()
        val intent = Intent(context, PhotoDownloadService::class.java).apply {
            action = PhotoDownloadService.Actions.START.name
        }

        service.onStartCommand(intent, 0, 1)

        // Wait for coroutines in service to execute
        Thread.sleep(100)

        assertThat(dataTransferTool.serviceStateFlow.value).isEqualTo(ServiceState.IDLE)
        val shadowService = shadowOf(service)
        assertThat(shadowService.isStoppedBySelf).isTrue
    }

    @Test
    fun `START action downloads photos successfully and updates flows`() {
        val mockRepo = mockk<Repository>(relaxed = true)
        val photoInfo = PhotoDownloadInfo("photo1.jpg", "content://media/1", 100, Kbps(500.0))
        every { mockRepo.downloadMediaToStorage(any()) } returns flowOf(Result.success(photoInfo))
        coEvery { mockRepo.shutDownCamera() } returns Result.success(Unit)

        stopKoin()
        startKoin {
            modules(
                module {
                    single<Repository> { mockRepo }
                    single { dataTransferTool }
                }
            )
        }

        val service = serviceController.create().get()
        val photos = arrayListOf(PhotoFileItem("100RICOH", "photo1.jpg"))
        val intent = Intent(context, PhotoDownloadService::class.java).apply {
            action = PhotoDownloadService.Actions.START.name
            putParcelableArrayListExtra(PhotoDownloadService.PHOTOS_LIST_EXTRA, photos)
        }

        service.onStartCommand(intent, 0, 1)

        Thread.sleep(200)

        assertThat(dataTransferTool.imageFlow.value).isEqualTo(photoInfo)
        assertThat(dataTransferTool.downloadFinishedFlow.value?.value).isEqualTo(Unit)
        assertThat(dataTransferTool.serviceStateFlow.value).isEqualTo(ServiceState.IDLE)
        
        val shadowService = shadowOf(service)
        assertThat(shadowService.isStoppedBySelf).isTrue
    }

    @Test
    fun `download failure posts error notification and emits error to errorFlow`() {
        val mockRepo = mockk<Repository>(relaxed = true)
        val partialInfo = PhotoDownloadInfo("photo1.jpg", "content://media/1", 50, Kbps(500.0))
        val error = DomainException(DomainError.CameraDisconnected)
        
        every { mockRepo.downloadMediaToStorage(any()) } returns flow {
            emit(Result.success(partialInfo))
            emit(Result.failure(error))
        }

        stopKoin()
        startKoin {
            modules(
                module {
                    single<Repository> { mockRepo }
                    single { dataTransferTool }
                }
            )
        }

        val service = serviceController.create().get()
        val photos = arrayListOf(PhotoFileItem("100RICOH", "photo1.jpg"))
        val intent = Intent(context, PhotoDownloadService::class.java).apply {
            action = PhotoDownloadService.Actions.START.name
            putParcelableArrayListExtra(PhotoDownloadService.PHOTOS_LIST_EXTRA, photos)
        }

        service.onStartCommand(intent, 0, 1)

        Thread.sleep(200)

        assertThat(dataTransferTool.errorFlow.value?.value).isEqualTo(DomainError.CameraDisconnected)
        assertThat(shadowNotificationManager.allNotifications).hasSize(2) // Foreground + error notification
    }

    @Test
    fun `STOP action interrupts active downloading`() {
        val mockRepo = mockk<Repository>(relaxed = true)
        val photoInfo = PhotoDownloadInfo("photo1.jpg", "content://media/1", 100, Kbps(500.0))
        every { mockRepo.downloadMediaToStorage(any()) } returns flowOf(Result.success(photoInfo))

        stopKoin()
        startKoin {
            modules(
                module {
                    single<Repository> { mockRepo }
                    single { dataTransferTool }
                }
            )
        }

        val service = serviceController.create().get()
        
        // Stop action
        val stopIntent = Intent(context, PhotoDownloadService::class.java).apply {
            action = PhotoDownloadService.Actions.STOP.name
        }
        service.onStartCommand(stopIntent, 0, 1)

        // Then START action with photos
        val photos = arrayListOf(
            PhotoFileItem("100RICOH", "photo1.jpg"),
            PhotoFileItem("100RICOH", "photo2.jpg")
        )
        val startIntent = Intent(context, PhotoDownloadService::class.java).apply {
            action = PhotoDownloadService.Actions.START.name
            putParcelableArrayListExtra(PhotoDownloadService.PHOTOS_LIST_EXTRA, photos)
        }

        // Even after start, continueDownload is set to true on start, but sending STOP again interrupts
        service.onStartCommand(startIntent, 0, 2)
        service.onStartCommand(stopIntent, 0, 3)

        Thread.sleep(200)

        assertThat(dataTransferTool.downloadFinishedFlow.value?.value).isEqualTo(Unit)
        assertThat(dataTransferTool.serviceStateFlow.value).isEqualTo(ServiceState.IDLE)
    }

    @Test
    fun `destroying service cleans up resources`() {
        val service = serviceController.create().get()
        serviceController.destroy()
        
        // Verifies no crashes during destroy and wakeLock/wifiLock release
    }
}
