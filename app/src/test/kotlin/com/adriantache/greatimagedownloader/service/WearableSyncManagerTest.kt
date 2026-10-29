package com.adriantache.greatimagedownloader.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.adriantache.greatimagedownloader.domain.ble.ConnectionStage
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.service.DataTransferTool.WidgetState
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
@OptIn(ExperimentalCoroutinesApi::class)
class WearableSyncManagerTest {
    private lateinit var context: Context
    private lateinit var dataTransferTool: DataTransferTool
    private lateinit var nodeClient: NodeClient
    private lateinit var messageClient: MessageClient
    private lateinit var dataClient: DataClient
    private lateinit var syncManager: WearableSyncManager

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataTransferTool = DataTransferTool()
        nodeClient = mockk(relaxed = true)
        messageClient = mockk(relaxed = true)
        dataClient = mockk(relaxed = true)

        every { nodeClient.connectedNodes } returns Tasks.forResult(emptyList())

        syncManager = WearableSyncManager(
            context = context,
            dataTransferTool = dataTransferTool,
            dispatcher = testDispatcher,
            nodeClient = nodeClient,
            messageClient = messageClient,
            dataClient = dataClient,
        )
    }

    @Test
    fun `buildCurrentStatus maps Idle state correctly`() {
        dataTransferTool.widgetStateFlow.value = WidgetState.Idle

        val status = syncManager.buildCurrentStatus()

        assertThat(status.state).isEqualTo("IDLE")
    }

    @Test
    fun `buildCurrentStatus maps Connecting stages correctly`() {
        dataTransferTool.widgetStateFlow.value = WidgetState.Connecting("BT…")
        dataTransferTool.connectionStageFlow.value = ConnectionStage.CONNECTING_BLE

        var status = syncManager.buildCurrentStatus()
        assertThat(status.state).isEqualTo("CONNECTING")
        assertThat(status.stageText).contains("Bluetooth")

        dataTransferTool.connectionStageFlow.value = ConnectionStage.CAMERA_CONNECTED
        status = syncManager.buildCurrentStatus()
        assertThat(status.stageText).contains("Waking")

        dataTransferTool.connectionStageFlow.value = ConnectionStage.STARTING_WIFI
        status = syncManager.buildCurrentStatus()
        assertThat(status.stageText).contains("Starting camera Wi-Fi")

        dataTransferTool.connectionStageFlow.value = ConnectionStage.JOINING_WIFI
        dataTransferTool.connectionAttemptFlow.value = 1
        status = syncManager.buildCurrentStatus()
        assertThat(status.stageText).contains("Joining camera Wi-Fi")

        dataTransferTool.connectionAttemptFlow.value = 3
        status = syncManager.buildCurrentStatus()
        assertThat(status.stageText).contains("3/5")
    }

    @Test
    fun `buildCurrentStatus maps Checking state correctly`() {
        dataTransferTool.widgetStateFlow.value = WidgetState.Checking

        val status = syncManager.buildCurrentStatus()

        assertThat(status.state).isEqualTo("CHECKING")
        assertThat(status.stageText).contains("Checking")
    }

    @Test
    fun `buildCurrentStatus maps Downloading state and progress correctly`() {
        dataTransferTool.widgetStateFlow.value = WidgetState.Downloading(current = 4, total = 10)
        dataTransferTool.imageFlow.value = PhotoDownloadInfo(
            name = "IMG001.JPG",
            uri = "content://media/1",
            downloadProgress = 65,
            downloadSpeed = com.adriantache.greatimagedownloader.domain.utils.model.Kbps(500.0),
        )

        val status = syncManager.buildCurrentStatus()

        assertThat(status.state).isEqualTo("DOWNLOADING")
        assertThat(status.currentPhoto).isEqualTo(4)
        assertThat(status.totalPhotos).isEqualTo(10)
        assertThat(status.photoProgressPercent).isEqualTo(65)
    }

    @Test
    fun `buildCurrentStatus maps Stopping state correctly`() {
        dataTransferTool.widgetStateFlow.value = WidgetState.Stopping

        val status = syncManager.buildCurrentStatus()

        assertThat(status.state).isEqualTo("STOPPING")
    }

    @Test
    fun `buildCurrentStatus maps Done state correctly`() {
        dataTransferTool.widgetStateFlow.value = WidgetState.Done(count = 7)

        val status = syncManager.buildCurrentStatus()

        assertThat(status.state).isEqualTo("DONE")
        assertThat(status.downloadCount).isEqualTo(7)
        assertThat(status.stageText).contains("7")
    }

    @Test
    fun `buildCurrentStatus maps Error state correctly`() {
        dataTransferTool.widgetStateFlow.value = WidgetState.Error

        val status = syncManager.buildCurrentStatus()

        assertThat(status.state).isEqualTo("ERROR")
    }

    @Test
    fun `sendCurrentStatusToNode sends message to specified node`() {
        val targetNodeId = "watch_node_123"
        every { messageClient.sendMessage(any(), any(), any()) } returns Tasks.forResult(0)

        dataTransferTool.widgetStateFlow.value = WidgetState.Done(count = 3)
        syncManager.sendCurrentStatusToNode(targetNodeId)

        verify {
            messageClient.sendMessage(
                targetNodeId,
                "/download-status",
                match { String(it, Charsets.UTF_8).contains("\"state\":\"DONE\"") },
            )
        }
    }
}
