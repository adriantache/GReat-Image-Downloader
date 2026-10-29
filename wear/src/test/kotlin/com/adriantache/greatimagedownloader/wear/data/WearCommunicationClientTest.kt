package com.adriantache.greatimagedownloader.wear.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.adriantache.greatimagedownloader.wear.WearConstants
import com.adriantache.greatimagedownloader.wear.model.WearDownloadStatus
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class WearCommunicationClientTest {
    private lateinit var context: Context
    private lateinit var nodeClient: NodeClient
    private lateinit var messageClient: MessageClient
    private lateinit var dataClient: DataClient
    private lateinit var client: WearCommunicationClient

    private val testNode = mockk<Node> {
        every { id } returns "phone_node_999"
        every { displayName } returns "Pixel Phone"
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        nodeClient = mockk(relaxed = true)
        messageClient = mockk(relaxed = true)
        dataClient = mockk(relaxed = true)

        every { nodeClient.connectedNodes } returns Tasks.forResult(listOf(testNode))
        every { messageClient.sendMessage(any(), any(), any()) } returns Tasks.forResult(0)

        client = WearCommunicationClient(
            context = context,
            nodeClient = nodeClient,
            messageClient = messageClient,
            dataClient = dataClient,
        )
    }

    @Test
    fun `sendStartSync sends message to connected phone nodes and sets connecting state`() {
        client.sendStartSync()
        ShadowLooper.idleMainLooper()

        assertThat(client.statusFlow.value.state).isEqualTo("CONNECTING")
        verify {
            messageClient.sendMessage("phone_node_999", WearConstants.PATH_START_SYNC, any())
        }
    }

    @Test
    fun `sendStopSync sends message to connected phone nodes and sets stopping state`() {
        client.sendStopSync()
        ShadowLooper.idleMainLooper()

        assertThat(client.statusFlow.value.state).isEqualTo("STOPPING")
        verify {
            messageClient.sendMessage("phone_node_999", WearConstants.PATH_STOP_SYNC, any())
        }
    }

    @Test
    fun `onMessageReceived with PATH_DOWNLOAD_STATUS updates statusFlow`() {
        val status = WearDownloadStatus(
            state = "DOWNLOADING",
            stageText = "Downloading…",
            currentPhoto = 3,
            totalPhotos = 8,
            photoProgressPercent = 50,
        )
        val jsonBytes = Json.encodeToString(status).toByteArray(Charsets.UTF_8)

        val mockEvent = mockk<MessageEvent> {
            every { path } returns WearConstants.PATH_DOWNLOAD_STATUS
            every { data } returns jsonBytes
        }

        client.onMessageReceived(mockEvent)

        assertThat(client.statusFlow.value).isEqualTo(status)
    }

    @Test
    fun `resetToIdle updates statusFlow back to IDLE`() {
        client.sendStartSync()
        assertThat(client.statusFlow.value.state).isEqualTo("CONNECTING")

        client.resetToIdle()
        assertThat(client.statusFlow.value.state).isEqualTo("IDLE")
    }
}
