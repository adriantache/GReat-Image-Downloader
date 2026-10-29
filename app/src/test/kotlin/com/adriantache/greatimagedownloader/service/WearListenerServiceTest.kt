package com.adriantache.greatimagedownloader.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.adriantache.greatimagedownloader.domain.data.FakeRepository
import com.adriantache.greatimagedownloader.domain.data.Repository
import com.adriantache.greatimagedownloader.domain.data.model.WifiDetails
import com.adriantache.greatimagedownloader.wear.WearConstants
import com.google.android.gms.wearable.MessageEvent
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
@OptIn(ExperimentalCoroutinesApi::class)
class WearListenerServiceTest {
    private lateinit var context: Context
    private lateinit var repository: FakeRepository
    private lateinit var wearableSyncManager: WearableSyncManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = FakeRepository()
        wearableSyncManager = mockk(relaxed = true)

        startKoin {
            modules(
                module {
                    single<Repository> { repository }
                    single { wearableSyncManager }
                }
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `REQUEST_STATUS message triggers sendCurrentStatusToNode`() {
        val service = Robolectric.buildService(WearListenerService::class.java).create().get()

        val mockEvent = mockk<MessageEvent>(relaxed = true) {
            io.mockk.every { path } returns WearConstants.PATH_REQUEST_STATUS
            io.mockk.every { sourceNodeId } returns "watch_node_456"
        }

        service.onMessageReceived(mockEvent)

        verify {
            wearableSyncManager.sendCurrentStatusToNode("watch_node_456")
        }
    }

    @Test
    fun `START_SYNC message with configured wifi starts PhotoDownloadService`() {
        repository.wifiDetails = WifiDetails("GR_WIFI", "password123", null)

        val service = Robolectric.buildService(WearListenerService::class.java).create().get()

        val mockEvent = mockk<MessageEvent>(relaxed = true) {
            io.mockk.every { path } returns WearConstants.PATH_START_SYNC
            io.mockk.every { sourceNodeId } returns "watch_node_456"
        }

        service.onMessageReceived(mockEvent)

        val nextIntent = shadowOf(service).nextStartedService
        assertThat(nextIntent).isNotNull
        assertThat(nextIntent.component?.className).isEqualTo(PhotoDownloadService::class.java.name)
        assertThat(nextIntent.action).isEqualTo(PhotoDownloadService.Actions.QUICK_DOWNLOAD.name)
    }

    @Test
    fun `STOP_SYNC message starts PhotoDownloadService with STOP action`() {
        val service = Robolectric.buildService(WearListenerService::class.java).create().get()

        val mockEvent = mockk<MessageEvent>(relaxed = true) {
            io.mockk.every { path } returns WearConstants.PATH_STOP_SYNC
            io.mockk.every { sourceNodeId } returns "watch_node_456"
        }

        service.onMessageReceived(mockEvent)

        val nextIntent = shadowOf(service).nextStartedService
        assertThat(nextIntent).isNotNull
        assertThat(nextIntent.component?.className).isEqualTo(PhotoDownloadService::class.java.name)
        assertThat(nextIntent.action).isEqualTo(PhotoDownloadService.Actions.STOP.name)
    }
}
